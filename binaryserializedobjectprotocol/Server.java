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
package binaryserializedobjectprotocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import protocol.RemoteExecutable;

public class Server {
	public static final int THREAD_POOL_SIZE = 10;

	public static void main(String[] args) {
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

				// Server opens ObjectInputStream first (client opened ObjectOutputStream first).
				try (ObjectInputStream objectIn = new ObjectInputStream(clientSocket.getInputStream());
						ObjectOutputStream objectOut = new ObjectOutputStream(clientSocket.getOutputStream())) {

					objectOut.flush();

					while (true) {
						Object received;
						try {
							// Read headers first
							received = objectIn.readObject();
							if (!(received instanceof Map)) {
								String msg = "Expected Map for headers, got: " + received.getClass().getName();
								System.out.println("## " + msg);
								objectOut.writeObject("Error: " + msg);
								objectOut.flush();
								continue;
							}
							@SuppressWarnings("unchecked")
							Map<String, String> headers = (Map<String, String>) received;
							System.out.println("## Received headers: " + headers);
							
							// Then read the task
							received = objectIn.readObject();
						} catch (java.io.EOFException e) {
							System.out.println("# Client closed connection");
							break;
						}

						if (!(received instanceof RemoteExecutable)) {
							String msg = "Expected RemoteExecutable, got: " + received.getClass().getName();
							System.out.println("## " + msg);
							objectOut.writeObject("Error: " + msg);
							objectOut.flush();
							continue;
						}

						RemoteExecutable task = (RemoteExecutable) received;
						System.out.println("## Received task: " + task.getClass().getName());

						String result = task.execute();
						System.out.println("## execute() returned: " + result);

						objectOut.writeObject(result);
						objectOut.flush();
						System.out.println("## Response sent");
					}
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
