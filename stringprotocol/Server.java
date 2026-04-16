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
package stringprotocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
	public static final int THREAD_POOL_SIZE = 10;
	
	public static void main(String[] args) {
		// Create a fixed thread pool for handling client requests
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
		private Socket clientSocket;

		public ClientHandler(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			try {
				System.out.println("--------------------------------");
				System.out.println("# Client connected");
				
				// Read the request from the client
				BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				StringBuilder requestBuilder = new StringBuilder();
				String request = reader.readLine();
				String action = null;
				String message = null;

				while (request != null) {
					if (request.isEmpty() || request.trim().equals("CLIENT DONE")) break;
					else {
						requestBuilder.append(request);
						requestBuilder.append("\n");

						if (request.startsWith("X-Action:")) {
							action = request.substring(request.indexOf(":") + 1).trim();
						} else if (request.startsWith("X-Message:")) {
							message = request.substring(request.indexOf(":") + 1).trim();
						}
					}

					request = reader.readLine();
				}
				//System.out.println("DONE RECEIVING");
				System.out.println("## Request:\n" + requestBuilder.toString().trim());
				String outputMessage = "";
				
				if (action != null) {
					System.out.println("## Action: " + action);
					System.out.println("## Message: " + message);

					if (action.equals("A")) {
						boolean status = true;
						System.out.println("## Doing action A: " + action + " with status: " + status);
						outputMessage = "Action A was executed with status: " + status;
					} else if (action.equals("B")) {
						boolean status = true;
						System.out.println("## Doing action B: " + action + " with status: " + status);
						outputMessage = "Action A was executed with status: " + status;
					} else {
						System.out.println("## Action is not valid");
						outputMessage = "Action is not valid";
					}
				} else {
					System.out.println("## Action or message is null");
					outputMessage = "Action or message is null";
				}

				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				writer.write("HTTP/1.1 200 OK\r\n");
				writer.write("Content-Type: text/html\r\n");
				writer.write("\r\n");
				writer.write("<html><body><h1>" + outputMessage + "</h1></body></html>");
				writer.write("\r\nSERVER DONE\r\n");
				writer.flush();
				System.out.println("## Response sent");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

