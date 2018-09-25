package com.inspur.eipatomapi.util;

import java.net.URI;
//import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

//@NotThreadSafe
class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
    public static final String METHOD_NAME = "DELETE";

    public String getMethod() {
        return "DELETE";
    }

    public HttpDeleteWithBody(String uri) {
        this.setURI(URI.create(uri));
    }

}