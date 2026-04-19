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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import protocol.AMQPRequest;
import protocol.AMQPResponse;

public class Client {
    public static final int TIMEOUT_MS = 60_000;
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final Map<String, List<QueueListener>> queueListeners = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            Object writeLock = new Object();
            Map<String, BlockingQueue<AMQPResponse>> responseMap = new ConcurrentHashMap<>();

            // Start listener thread to handle incoming responses and notifications
            Thread listener = new Thread(() -> {
                try {
                    while (true) {
                        Object obj = in.readObject();

                        if (obj instanceof AMQPResponse response) {
                            BlockingQueue<AMQPResponse> queue = responseMap.get(response.id);
                            
                            if (queue != null) {
                                queue.put(response); //this will unblock the code: AMQPResponse response = queue.take(); in the sendRequest method
                            }
                        } else if (obj instanceof AMQPRequest req) {
                            // Handle incoming notifications from server (e.g., from queues) and dispatch to registered listeners, sending back an AMQPResponse to acknowledge receipt
                            if ("notify".equals(req.method)) {
                                String queueName = (String) req.params[0];
                                Object eventData = req.params[1];
                                boolean success = receiveFromQueue(queueName, eventData);
                                
                                synchronized (writeLock) {
                                    out.writeObject(new AMQPResponse(req.id, success, null));
                                    out.flush();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "client-listener");
            listener.setDaemon(true);
            listener.start();

            // Register queues
            registerQueue(out, writeLock, responseMap, "A");
            registerQueue(out, writeLock, responseMap, "B");

            // Register local listeners for queue notifications
            registerQueueListener("A", Client::receiveFromQueueA);
            registerQueueListener("B", Client::receiveFromQueueB);

            // Send events to queues
            sendToQueue(out, writeLock, responseMap, "A", "Event for A");
            sendToQueue(out, writeLock, responseMap, "B", "Event for B");

            // Keep the client running to receive notifications
            Thread.sleep(10000);
        }
    }

    //register a queue with the server
    public static void registerQueue(ObjectOutputStream out, Object writeLock, Map<String, BlockingQueue<AMQPResponse>> responseMap, String queueName) throws InterruptedException, IOException {
        AMQPResponse resp = sendRequest(out, writeLock, responseMap, "register", queueName);
        System.out.println("Queue '" + queueName + "' registered: " + resp.result);
    }

    //send an event to a queue
    public static void sendToQueue(ObjectOutputStream out, Object writeLock, Map<String, BlockingQueue<AMQPResponse>> responseMap, String queueName, Object eventData) throws InterruptedException, IOException {
        AMQPResponse resp = sendRequest(out, writeLock, responseMap, "send", new Object[]{queueName, eventData});
        System.out.println("Event sent to queue '" + queueName + "': " + resp.result);
    }

    //register a local listener for a queue to be executed when the server sends a notification for that queue
    public static void registerQueueListener(String queueName, QueueListener listener) {
        queueListeners.computeIfAbsent(queueName, key -> new CopyOnWriteArrayList<>()).add(listener);
        System.out.println("Listener registered for queue '" + queueName + "'.");
    }

    //receive an event from a queue and dispatch to registered listeners, returning true if all listeners succeeded, false if any listener failed
    public static boolean receiveFromQueue(String queueName, Object eventData) {
        List<QueueListener> listeners = queueListeners.get(queueName);
        if (listeners == null || listeners.isEmpty()) {
            System.err.println("No listeners registered for queue '" + queueName + "'.");
            return false;
        }

        boolean allSucceeded = true;
        for (QueueListener listener : listeners) {
            boolean success = listener.onMessage(eventData);
            allSucceeded &= success;
        }
        return allSucceeded;
    }

    //example listener implementations for queues A
    public static boolean receiveFromQueueA(Object eventData) {
        System.out.println("Processing eventData from queue A: " + eventData);
        // TODO: Simulate processing logic here...
        return true;
    }

    //example listener implementations for queues B
    public static boolean receiveFromQueueB(Object eventData) {
        System.out.println("Processing eventData from queue B: " + eventData);
        // TODO: Simulate processing logic here...
        return true;
    }

    //helper method to send an AMQPRequest and wait for the corresponding AMQPResponse, using a unique request ID and a BlockingQueue to block until the response is received by the listener thread
    private static AMQPResponse sendRequest(ObjectOutputStream out, Object writeLock, Map<String, BlockingQueue<AMQPResponse>> responseMap, String method, Object... params) throws InterruptedException, IOException {
        String requestId = UUID.randomUUID().toString();
        AMQPRequest request = new AMQPRequest(requestId, method, params);
        BlockingQueue<AMQPResponse> queue = new LinkedBlockingQueue<>();

        responseMap.put(requestId, queue);

        synchronized (writeLock) {
            out.writeObject(request);
            out.flush();
        }

        AMQPResponse response = queue.take(); // Wait for the response (this will be unblocked by the listener thread when it receives the response)

        responseMap.remove(requestId);
        
        return response;
    }

    // Functional interface for queue listeners
    @FunctionalInterface
    public interface QueueListener {
        boolean onMessage(Object eventData);
    }
}
