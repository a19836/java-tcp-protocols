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

public class RpcResponse implements Serializable {
    public Object result;
    public Exception error;

    public RpcResponse(Object result, Exception error) {
        this.result = result;
        this.error = error;
    }
}
