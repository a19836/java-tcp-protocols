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
import java.net.InetSocketAddress;
import java.net.Socket;

public class Client {
	public static final int TIMEOUT_MS = 60_000; //timeout of 120 secs
	private static final String HOST = "localhost";
	private static final int PORT = 8080;

	public static void main(String[] args) {
		try {
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_MS);
				socket.setSoTimeout(TIMEOUT_MS);

				String action = "A";
				String sentence = "TEST NEW PROTOCOL";
				
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				writer.write("GET / HTTP/1.1\r\n");
				writer.write("Host: " + HOST + ":" + PORT + "\r\n");
				writer.write("X-Action: " + action + "\r\n");
				writer.write("X-Message: " + sentence + "\r\n");
				writer.write("CLIENT DONE\r\n");
				writer.flush();
				System.out.println("Request sent");

				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String line = reader.readLine();
				System.out.println("Response received:");
				while (line != null) {
					if (line.trim().equals("SERVER DONE")) break;

					System.out.println(line);
					line = reader.readLine();
				}
				System.out.println();

				// INSERT_YOUR_CODE
				// Close the writer and reader to properly release the resources
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

