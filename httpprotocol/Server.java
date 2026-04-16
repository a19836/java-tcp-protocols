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
package httpprotocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.SocketTimeoutException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
	public static final int PORT = 8080;
	public static final int TIMEOUT_MS = 60_000; //timeout of 60 secs
	public static final int THREAD_POOL_SIZE = 10;
	
	public static void main(String[] args) {
		// Create a fixed thread pool for handling client requests
		final ExecutorService threadPool = Executors.newFixedThreadPool(Server.THREAD_POOL_SIZE);

		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT);
			System.out.println("Server started on port " + PORT);

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
			try (
				BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
			) {
				System.out.println();
				System.out.println("--------------------------------");
				System.out.println("Client connected - " + clientSocket.hashCode());
				System.out.println("--------------------------------");
				System.out.println();

				clientSocket.setSoTimeout(Server.TIMEOUT_MS);

				while (true) {
					String requestLine = reader.readLine();
					if (requestLine == null) {
						break;
					}

					if (requestLine.isEmpty()) {
						continue;
					}

					//parse headers
					String host = null;
					String message = "";
					int requestContentLength = 0;
					boolean keepAlive = true;
					String headerLine;

					while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
						String lower = headerLine.toLowerCase();
						
						//parse host header
						if (lower.startsWith("host:")) {
							host = headerLine.substring(5).trim();
						//parse content-length header
						} else if (lower.startsWith("content-length:")) {
							String rawLength = headerLine.substring(15).trim();
							requestContentLength = Integer.parseInt(rawLength);
						//parse connection header: keepalive or close
						} else if (lower.startsWith("connection:")) {
							String connectionValue = headerLine.substring(11).trim();
							if ("close".equalsIgnoreCase(connectionValue)) {
								keepAlive = false;
							}
						}
					}

					//get body
					//avoids buffer overflow hacks by only reading the correct content based in the content-length
					if (requestContentLength > 0) {
						char[] body = new char[requestContentLength];
						int readTotal = 0;
						while (readTotal < requestContentLength) {
							int readNow = reader.read(body, readTotal, requestContentLength - readTotal);
							if (readNow == -1) {
								break;
							}
							readTotal += readNow;
						}
						message = new String(body, 0, readTotal);
					}

					System.out.println();
					System.out.println("--------------------------------");
					System.out.println("# Client - " + clientSocket.hashCode());
					System.out.println("## Request:");
					System.out.println(requestLine);
					if (host != null) {
						System.out.println("Host: " + host);
					}
					System.out.println("Content-Length: " + requestContentLength);
					System.out.println("Connection: " + (keepAlive ? "keep-alive" : "close"));
					System.out.println("Body: " + message);

					String responseBody = "<html><body><h1>Your message: " + message + "</h1></body></html>";
					int contentLength = responseBody.getBytes(StandardCharsets.UTF_8).length;

					writer.write("HTTP/1.1 200 OK\r\n");
					writer.write("Content-Type: text/html; charset=UTF-8\r\n");
					writer.write("Content-Length: " + contentLength + "\r\n");
					writer.write("Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n");
					writer.write("\r\n");
					writer.write(responseBody);
					writer.flush();
					System.out.println("## Response sent");

					if (!keepAlive) {
						break;
					}
				}
			} catch (SocketTimeoutException e) {
				System.out.println();
				System.out.println("# Client timed out after " + (Server.TIMEOUT_MS / 1000) + " seconds - " + clientSocket.hashCode());
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					clientSocket.close();
					System.out.println();
					System.out.println("# Client disconnected - " + clientSocket.hashCode());
					System.out.println();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
