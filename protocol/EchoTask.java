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

/**
 * Example {@link RemoteExecutable} the client can send; the server runs
 * {@link #execute()} and returns the result string to the client.
 */
public class EchoTask implements RemoteExecutable {

	private static final long serialVersionUID = 1L;

	private final String message;

	public EchoTask(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String execute() {
		return "EchoTask executed on server: " + message;
	}
}
