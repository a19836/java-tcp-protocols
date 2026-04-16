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
import java.net.InetSocketAddress;
import java.net.Socket;

import protocol.RpcRequest;
import protocol.RpcResponse;

public class Client {
    public static final int TIMEOUT_MS = 60_000;
	private static final String HOST = "localhost";
	private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        Client client = new Client();

        String res = (String) client.call("sayHello", "João");
        System.out.println(res);

        int sum = (int) client.call("add", 5, 3);
        System.out.println(sum);
    }

    public Object call(String method, Object... params) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            // Client must open ObjectOutputStream first so the server can open ObjectInputStream.
            try (ObjectOutputStream objectOut = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objectIn = new ObjectInputStream(socket.getInputStream())) {

                objectOut.flush();

                objectOut.writeObject(new RpcRequest(method, params));
                objectOut.flush();

                RpcResponse res = (RpcResponse) objectIn.readObject();

                if (res.error != null) {
                    throw res.error;
                }

                return res.result;
            }
        }
    }
}
