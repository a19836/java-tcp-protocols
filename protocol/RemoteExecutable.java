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

import java.io.Serializable;

/**
 * Objects sent from the client must implement this interface. The server
 * deserializes the instance and calls {@link #execute()}.
 */
public interface RemoteExecutable extends Serializable {

	String execute();
}
