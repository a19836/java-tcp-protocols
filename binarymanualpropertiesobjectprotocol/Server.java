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
package binarymanualpropertiesobjectprotocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import protocol.BinaryTaskWire;
import protocol.EchoTask;

/**
 * Reads binary-framed tasks ({@link BinaryTaskWire}) and executes the operation implied by
 * opcode (same idea as a gRPC method + message bytes, without Java serialization).
 */
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

				try (DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
					DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream())) {

					while (true) {
						try {
							BinaryTaskWire.Request req = BinaryTaskWire.readRequest(dataIn);
							if (req.opcode == BinaryTaskWire.OPCODE_ECHO) {
								System.out.println("## Request received without headers");
								EchoTask et = new EchoTask(req.payload);
								System.out.println(et.execute());
								BinaryTaskWire.writeOkResponse(dataOut, et.getMessage());
							} else if (req.opcode == BinaryTaskWire.OPCODE_ECHO_WITH_HEADERS) {
								System.out.println("## Request received with headers");

								// Parse payload: headersCount\0header1\0value1\0...\0message\0...
								String[] parts = req.payload.split("\0");
								int headersCount = Integer.parseInt(parts[0]);
								Map<String, String> headers = new HashMap<>();
								int idx = 1;
								for (int i = 0; i < headersCount; i++) {
									headers.put(parts[idx], parts[idx + 1]);
									idx += 2;
								}
								String message = parts[idx];
								
								System.out.println("Received headers: " + headers);
								
								EchoTask et = new EchoTask(message);
								System.out.println(et.execute());
								BinaryTaskWire.writeOkResponse(dataOut, et.getMessage());
							} else {
								BinaryTaskWire.writeErrorResponse(dataOut, "Unknown opcode: " + req.opcode);
							}
							System.out.println("## Response sent");
						} catch (EOFException e) {
							System.out.println("# Client closed connection");
							break;
						} catch (IOException e) {
							System.out.println("## Bad request: " + e.getMessage());
							try {
								BinaryTaskWire.writeErrorResponse(dataOut, e.getMessage());
							} catch (IOException ignored) {
								break;
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
