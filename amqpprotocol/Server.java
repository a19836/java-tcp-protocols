/*
 * Copyright (c) 2026 Joao Pinto (http://jplpinto.com)
 * 
 * Multi-licensed: BSD 3-Clause | Apache 2.0 | GNU LGPL v3
 * Choose one license that best fits your needs.
 *
 * Original Repo: https://github.com/a19836/java-tcp-protocols/
 *
 * YOU ARE NOT AUTHORIZED TO MODIFY OR REMOVE ANY PART OF THIS NOTICE!
 */
package amqpprotocol;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import protocol.Event;
import protocol.AMQPRequest;
import protocol.AMQPResponse;

public class Server {
    public static final int THREAD_POOL_SIZE = 10;
    private static final long NOTIFY_RESPONSE_TIMEOUT_SECONDS = 10;

    private static final Map<String, Queue<Event>> queues = new ConcurrentHashMap<>();
    private static final Map<String, Queue<Event>> dlq = new ConcurrentHashMap<>();
    private static final Map<Socket, Set<String>> subscriptions = new ConcurrentHashMap<>();
    private static final Map<Socket, ClientContext> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        final ExecutorService threadPool = Executors.newFixedThreadPool(Server.THREAD_POOL_SIZE);

        //Thread to process queues and notify clients about events
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                try {
                    for (String queueName : queues.keySet()) {
                        // Get the queue for the queueName and if empty, continue to next queue
                        Queue<Event> queue = queues.get(queueName);

                        if (queue == null || queue.isEmpty()) {
                            continue;
                        }

                        // Get the event at the head of the queue without removing it
                        Event event = queue.peek();
                        if (event == null) {
                            continue;
                        }

                        // Get the clients subscribed to this queue
                        Set<Socket> clientsForQueue = subscriptions.entrySet().stream()
                            .filter(item -> item.getValue().contains(queueName))
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toCollection(LinkedHashSet::new));

                        //loop through clients and notify them about the event, if any client responds with success, remove the event from the queue, otherwise increment retry count and if exceeds threshold, move to DLQ
                        for (Socket clientSocket : clientsForQueue) {
                            // Get the client context
                            ClientContext ctx = clients.get(clientSocket);

                            if (ctx == null || !ctx.isActive()) {
                                continue;
                            }

                            // Notify the client and wait for response
                            AMQPResponse response = ctx.notify(queueName, event);

                            if (response == null) {
                                continue;
                            }

                            // Process the response and if success, remove the event from the queue
                            boolean success = Boolean.TRUE.equals(response.result);
                            if (success) {
                                queue.poll();
                                break; //get out of client loop and move to next event
                            }

                            //Otherwise, increment retry count and if exceeds threshold, move to DLQ
                            queue.poll();
                            event.setRetryCount(event.getRetryCount() + 1);

                            if (event.getRetryCount() >= 5) {
                                dlq.computeIfAbsent(queueName, k -> new LinkedBlockingQueue<>()).add(event);
                            } else {
                                queue.add(event);
                            }
                            break; //get out of client loop and move to next event
                        }
                    }

                    // Sleep for a short period before checking the queues again to avoid busy waiting
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        //Thread to clean up old events from DLQ after 30 days
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                try {
                    long now = System.currentTimeMillis();

                    // Loop through all queues in DLQ and remove events that have been in the DLQ for more than 30 days
                    for (Queue<Event> q : dlq.values()) {
                        q.removeIf(ev -> now - ev.getTimestamp() > 30L * 24 * 60 * 60 * 1000);
                    }

                    // Sleep for a day before checking the DLQ again
                    Thread.sleep(60 * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Start the server socket and listen for incoming connections
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Server started on port 8080");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            final ClientContext[] ctxRef = new ClientContext[1];
            Thread readerThread = null;

            try {
                System.out.println("--------------------------------");
                System.out.println("# Client connected");

                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                ClientContext ctx = new ClientContext(clientSocket, out);

                ctxRef[0] = ctx;
                clients.put(clientSocket, ctx);

                //Thread to read messages from client
                readerThread = new Thread(() -> {
                    try {
                        ClientContext readerCtx = ctxRef[0];

                        while (readerCtx != null && readerCtx.isActive()) {
                            //read an object from the client.
                            Object obj = in.readObject();
                            
                            // If it's a request, put it in the request queue for processing by main thread, if it's a response, complete the pending response future
                            if (obj instanceof AMQPRequest req) {
                                readerCtx.requestQueue.put(req); //this will unblock the code: ctx.requestQueue.take();
                            } 
                            // If it's a response, complete the pending response future
                            else if (obj instanceof AMQPResponse resp) {
                                readerCtx.completeResponse(resp); //this will unblock the code: responseQueue.poll(NOTIFY_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                            }
                        }
                    } catch (Exception ignored) {
                    } finally {
                        ClientContext readerCtx = ctxRef[0];
                        if (readerCtx != null) {
                            readerCtx.close();
                        }
                    }
                });
                readerThread.setDaemon(true);
                readerThread.start();

                // Main loop to process requests from the client
                while (ctx.isActive()) {
                    AMQPRequest req = ctx.requestQueue.take();
                    
                    //register a queue.
                    if ("register".equals(req.method)) {
                        String queueName = (String) req.params[0];
                        
                        subscriptions.computeIfAbsent(clientSocket, k -> ConcurrentHashMap.newKeySet()).add(queueName);
                        ctx.sendResponse(new AMQPResponse(req.id, true, null));
                    } 
                    //send an event to a queue
                    else if ("send".equals(req.method)) {
                        String queueName = (String) req.params[0];
                        Object eventData = req.params[1];
                        Event event = new Event(eventData);
                        
                        queues.computeIfAbsent(queueName, k -> new LinkedBlockingQueue<>()).add(event);
                        ctx.sendResponse(new AMQPResponse(req.id, true, null));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ClientContext ctx = ctxRef[0];
                if (ctx != null) {
                    ctx.close();
                }

                subscriptions.remove(clientSocket);
                clients.remove(clientSocket);

                if (readerThread != null && readerThread.isAlive()) {
                    readerThread.interrupt();
                }
                try {
                    clientSocket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static class ClientContext {
        private final Socket socket;
        private final ObjectOutputStream out;
        private final BlockingQueue<AMQPRequest> requestQueue = new LinkedBlockingQueue<>();
        private final Map<String, BlockingQueue<AMQPResponse>> pendingResponses = new ConcurrentHashMap<>();
        private final Object writeLock = new Object();
        private volatile boolean active = true;

        public ClientContext(Socket socket, ObjectOutputStream out) {
            this.socket = socket;
            this.out = out;
        }

        public boolean isActive() {
            return active && !socket.isClosed();
        }

        public void close() {
            active = false;
        }

        // Complete a pending response future when a response is received from the client
        public void completeResponse(AMQPResponse response) {
            BlockingQueue<AMQPResponse> queue = pendingResponses.get(response.id);
            if (queue != null) {
                queue.offer(response); //this will unblock the code: responseQueue.poll(NOTIFY_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        }

        // Notify the client about an event and wait for the response, returning null if timeout or error occurs
        public AMQPResponse notify(String queueName, Event event) {
            String correlationId = UUID.randomUUID().toString();
            BlockingQueue<AMQPResponse> responseQueue = new LinkedBlockingQueue<>();

            pendingResponses.put(correlationId, responseQueue);

            try {
                send(new AMQPRequest(correlationId, "notify", new Object[]{queueName, event}));
                
                return responseQueue.poll(NOTIFY_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                return null;
            } finally {
                pendingResponses.remove(correlationId);
            }
        }

        // Send a response back to the client
        public void sendResponse(AMQPResponse response) throws IOException {
            send(response);
        }

        // Helper method to send an object to the client with synchronization on the write lock
        private void send(Object message) throws IOException {
            synchronized (writeLock) {
                out.writeObject(message);
                out.flush();
            }
        }
    }
}
