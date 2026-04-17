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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import protocol.BinaryTaskWire;
import protocol.EchoTask;

/**
 * Sends task parameters in a compact binary frame ({@link BinaryTaskWire}), like gRPC/protobuf
 * wire encoding, rather than Java object serialization.
 */
public class Client {
	public static final int TIMEOUT_MS = 60_000;
	private static final String HOST = "localhost";
	private static final int PORT = 8080;

	public static void main(String[] args) {
		try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_MS);
				socket.setSoTimeout(TIMEOUT_MS);

				try (DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
					DataInputStream dataIn = new DataInputStream(socket.getInputStream())) {

					System.out.println("Enter text for echo task (binary wire) or 'exit' to quit:");
					while (true) {
						System.out.print("> ");
						String line = console.readLine();
						if (line == null || "exit".equalsIgnoreCase(line.trim())) {
							break;
						}
						if (line.trim().isEmpty()) {
							continue;
						}

						EchoTask et = new EchoTask(line);

						//Method 1: simple echo request without headers
						BinaryTaskWire.writeEchoRequest(dataOut, et.getMessage());
						System.out.println("Binary request sent without headers");

						String response = BinaryTaskWire.readResponse(dataIn);
						System.out.println("Server result: " + response);

						//Method 2: execute request with headers (more complex, but more flexible)
						Map<String, String> headers = new HashMap<>();
						headers.put("Content-Type", "application/echo");
						headers.put("User-Agent", "BinaryClient/1.0");
						
						BinaryTaskWire.writeExecuteRequestWithHeaders(dataOut, headers, et.getMessage());
						System.out.println("Binary request sent with headers");

						response = BinaryTaskWire.readResponse(dataIn);
						System.out.println("Server result: " + response);
						System.out.println();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
