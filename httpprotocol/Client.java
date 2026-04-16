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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
	public static final int TIMEOUT_MS = 120_000; //timeout of 120 secs
	private static final String HOST = "localhost";
	private static final int PORT = 8080;

	public static void main(String[] args) {
		Socket socket = null;
		BufferedWriter writer = null;
		BufferedReader reader = null;

		//create console reader to read user input
		try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
			//create connected socket to server
			socket = createConnectedSocket();
			System.out.println("Client is now connected to server");

			//create writer and reader based in new socket
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

			//main loop to read user input and send requests to server
			while (true) {
				//read sentence from user input
				System.out.println("--------------------------------");
				System.out.println("Type a sentence and press Enter (type 'exit' to quit).");
				System.out.print("> ");
				String sentence = console.readLine();
				System.out.println();

				//is sentence is null or "exit", send request to server with no body, but with connection: close header
				if (sentence == null || "exit".equalsIgnoreCase(sentence.trim())) {
					//ask server to close connection
					requestClosingConnection(writer);
					System.out.println("## Request closing connection");

					//read and print response from server, confirming if connection is closed
					readHttpResponse(reader);
					break;
				}
				//if sentence is empty, skip
				else if (sentence.trim().isEmpty()) {
					continue;
				}
				//else {
					//otherwise, send request to server with sentence as body
					sendHttpRequest(writer, sentence);
					System.out.println("# Request sent to server");
				
					//read and print response from server
					boolean responseReceived = readHttpResponse(reader);
					
					//if response is not received, reconnect to server and retry request
					if (!responseReceived) {
						System.out.println("## Reconnecting to server and retrying request...");
						
						//close previous connection
						closeQuietly(socket);

						//create new connection
						socket = createConnectedSocket();

						//update writer and reader based in new socket
						writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
						reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

						//send request to server again
						sendHttpRequest(writer, sentence);
						System.out.println("# Request sent to server (retry)");
						
						//read and print response from server
						readHttpResponse(reader);
					}
					System.out.println();
				//}
			}
			
			//print final message
			System.out.println();
			System.out.println("Client is now disconnected");
			System.out.println();
		} catch (IOException e) {
			//print error message
			System.out.println();
			System.out.println("Client encountered an error: " + e.getMessage());
			System.out.println();

			e.printStackTrace();
		} finally {
			//close socket quietly
			closeQuietly(socket);
		}
	}

	//ask server to close connection
	private static void requestClosingConnection(BufferedWriter writer) throws IOException {
		writer.write("POST / HTTP/1.1\r\n");
		writer.write("Host: localhost:8080\r\n");
		writer.write("Connection: close\r\n");
		writer.write("Content-Length: 0\r\n");
		writer.write("\r\n");
		writer.flush();
	}

	//send request to server with sentence as body
	private static void sendHttpRequest(BufferedWriter writer, String sentence) throws IOException {
		writer.write("POST / HTTP/1.1\r\n");
		writer.write("Host: " + HOST + ":" + PORT + "\r\n");
		writer.write("Content-Length: " + sentence.length() + "\r\n");
		writer.write("Connection: keep-alive\r\n");
		writer.write("\r\n");
		writer.write(sentence);
		writer.flush();
	}

	//read and print response from server
	private static boolean readHttpResponse(BufferedReader reader) throws IOException {
		System.out.println("# Response received");

		//read status line - first line of response from server with protocol and status code
		String statusLine = reader.readLine();
		
		if (statusLine == null) {
			System.out.println("## Server closed the connection.");
			return false;
		}

		System.out.println(statusLine);

		//parse headers
		int contentLength = 0;
		String headerLine;
		while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
			System.out.println(headerLine);
			String lower = headerLine.toLowerCase();

			//parse content-length header
			if (lower.startsWith("content-length:")) {
				String rawLength = headerLine.substring(15).trim();
				contentLength = Integer.parseInt(rawLength);
			}
		}

		//get body
		//avoids buffer overflow hacks by only reading the correct content based in the content-length
		if (contentLength > 0) {
			char[] body = new char[contentLength];
			int readTotal = 0;
			while (readTotal < contentLength) {
				int readNow = reader.read(body, readTotal, contentLength - readTotal);
				if (readNow == -1) {
					break;
				}
				readTotal += readNow;
			}

			String bodyString = new String(body, 0, readTotal);
			System.out.println("## Body:\n" + bodyString);
		}
		return true;
	}

	//create a new connected socket to server
	private static Socket createConnectedSocket() throws IOException {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_MS);
		socket.setSoTimeout(Client.TIMEOUT_MS);
		return socket;
	}

	//close socket quietly
	private static void closeQuietly(Socket socket) {
		if (socket != null) {
			try {
				socket.close(); //by default it closes also the writer and reader
			} catch (IOException ignored) {
				// Ignore close errors.
			}
		}
	}
}
