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
package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Compact binary request/response framing (protobuf-style: schema + bytes on the wire),
 * not Java {@link java.io.ObjectOutputStream} serialization.
 *
 * <p>Request: magic (4) | version (1) | opcode (1) | utf8Length (4) | utf8Payload
 * <p>Response: ok (1) | utf8Length (4) | utf8Result
 */
public final class BinaryTaskWire {

	public static final int MAGIC = 0x4F505431; // "OPT1" as big-endian int
	public static final byte VERSION = 1;
	public static final byte OPCODE_ECHO = 1;
	public static final byte OPCODE_ECHO_WITH_HEADERS = 2;

	private BinaryTaskWire() {
	}

	public static void writeEchoRequest(DataOutputStream out, String message) throws IOException {
		byte[] utf8 = message.getBytes(StandardCharsets.UTF_8);
		out.writeInt(MAGIC);
		out.writeByte(VERSION);
		out.writeByte(OPCODE_ECHO);
		out.writeInt(utf8.length);
		out.write(utf8);
		out.flush();
	}

	public static void writeExecuteRequestWithHeaders(DataOutputStream out, Map<String, String> headers, String message) throws IOException {
		StringBuilder payload = new StringBuilder();
		// Headers count and headers
		payload.append(headers.size()).append('\0');
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			payload.append(entry.getKey()).append('\0').append(entry.getValue()).append('\0');
		}
		// message
		payload.append(message).append('\0');

		byte[] utf8 = payload.toString().getBytes(StandardCharsets.UTF_8);
		out.writeInt(MAGIC);
		out.writeByte(VERSION);
		out.writeByte(OPCODE_ECHO_WITH_HEADERS);
		out.writeInt(utf8.length);
		out.write(utf8);
		out.flush();
	}

	public static Request readRequest(DataInputStream in) throws IOException {
		int magic = in.readInt();
		if (magic != MAGIC) {
			throw new IOException("Bad magic: 0x" + Integer.toHexString(magic));
		}
		byte ver = in.readByte();
		if (ver != VERSION) {
			throw new IOException("Unsupported version: " + ver);
		}
		byte opcode = in.readByte();
		int len = in.readInt();
		if (len < 0 || len > 16_000_000) {
			throw new IOException("Invalid payload length: " + len);
		}
		byte[] buf = new byte[len];
		in.readFully(buf);
		String payload = new String(buf, StandardCharsets.UTF_8);
		return new Request(opcode, payload);
	}

	public static void writeOkResponse(DataOutputStream out, String result) throws IOException {
		byte[] utf8 = result.getBytes(StandardCharsets.UTF_8);
		out.writeByte(1);
		out.writeInt(utf8.length);
		out.write(utf8);
		out.flush();
	}

	public static void writeErrorResponse(DataOutputStream out, String errorMessage) throws IOException {
		byte[] utf8 = errorMessage.getBytes(StandardCharsets.UTF_8);
		out.writeByte(0);
		out.writeInt(utf8.length);
		out.write(utf8);
		out.flush();
	}

	public static String readResponse(DataInputStream in) throws IOException {
		byte ok = in.readByte();
		int len = in.readInt();
		if (len < 0 || len > 16_000_000) {
			throw new IOException("Invalid result length: " + len);
		}
		byte[] buf = new byte[len];
		in.readFully(buf);
		String text = new String(buf, StandardCharsets.UTF_8);
		if (ok != 1) {
			throw new IOException("Server error: " + text);
		}
		return text;
	}

	public static final class Request {
		public final byte opcode;
		public final String payload;

		public Request(byte opcode, String payload) {
			this.opcode = opcode;
			this.payload = payload;
		}
	}
}
