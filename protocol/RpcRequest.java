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

public class RpcRequest implements Serializable {
    public String method;
    public Object[] params;

    public RpcRequest(String method, Object... params) {
        this.method = method;
        this.params = params;
    }
}
