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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import protocol.EchoTask;
import protocol.RemoteExecutable;

public class Client {
	public static final int TIMEOUT_MS = 60_000;
	private static final String HOST = "localhost";
	private static final int PORT = 8080;

	public static void main(String[] args) {
		try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_MS);
				socket.setSoTimeout(TIMEOUT_MS);

				// Client must open ObjectOutputStream first so the server can open ObjectInputStream.
				try (ObjectOutputStream objectOut = new ObjectOutputStream(socket.getOutputStream());
						ObjectInputStream objectIn = new ObjectInputStream(socket.getInputStream())) {

					objectOut.flush();

					System.out.println("Enter text to send inside EchoTask (or 'exit' to quit):");
					while (true) {
						System.out.print("> ");
						String line = console.readLine();
						if (line == null || "exit".equalsIgnoreCase(line.trim())) {
							break;
						}
						if (line.trim().isEmpty()) {
							continue;
						}

						// Pass a concrete object implementing RemoteExecutable; server calls execute().
						RemoteExecutable task = new EchoTask(line);
						
						// Send headers
						Map<String, String> headers = new HashMap<>();
						headers.put("Content-Type", "application/echo");
						headers.put("User-Agent", "JavaClient/1.0");
						headers.put("Request-Id", String.valueOf(System.currentTimeMillis()));
						objectOut.writeObject(headers);
						
						objectOut.writeObject(task);
						objectOut.flush();
						System.out.println("Task object sent");

						Object response = objectIn.readObject();
						if (response instanceof String) {
							System.out.println("Server result: " + response);
						} else {
							System.out.println("Unexpected response type: " + response);
						}
						System.out.println();
					}
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
