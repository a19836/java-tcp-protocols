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
package binaryserializedobjectgrpcprotocol;

import java.io.*;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import protocol.RpcRequest;
import protocol.RpcResponse;

public class Server {
    public static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) throws Exception {
        final ExecutorService threadPool = Executors.newFixedThreadPool(Server.THREAD_POOL_SIZE);

		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(8080);
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
            try {
				System.out.println("--------------------------------");
				System.out.println("# Client connected");

                try (
                    ObjectInputStream objectIn = new ObjectInputStream(clientSocket.getInputStream());
                    ObjectOutputStream objectOut = new ObjectOutputStream(clientSocket.getOutputStream())
                ) {
                    RpcRequest req = (RpcRequest) objectIn.readObject();

                    Object result = null;
                    Exception error = null;

                    try {
                        Method method = null;

                        if ("sayHello".equals(req.method)) {
                            method = ActionHandler.class.getMethod("sayHello", String.class);
                        } else if ("add".equals(req.method)) {
                            method = ActionHandler.class.getMethod("add", int.class, int.class);
                        } else {
                            error = new NoSuchMethodException("Method not found: " + req.method);
                        }
                        
                        if (method != null)
                            result = method.invoke(ActionHandler.class, req.params);
                    } catch (Exception e) {
                        error = e;
                    }

                    objectOut.writeObject(new RpcResponse(result, error));
                    objectOut.flush();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
				e.printStackTrace();
			}
        }
    }

    private static class ActionHandler {
	    public static String sayHello(String name) {
            return "Hello " + name;
        }

        public static int add(int a, int b) {
            return a + b;
        }
    }
}
