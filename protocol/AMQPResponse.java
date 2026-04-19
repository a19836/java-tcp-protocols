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

public class AMQPResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    public String id;
    public Object result;
    public Exception error;

    public AMQPResponse(String id, Object result, Exception error) {
        this.id = id;
        this.result = result;
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public Exception getError() {
        return error;
    }
}
