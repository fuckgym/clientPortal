//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.utils;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class RequestUtils {
    public RequestUtils() {
    }

    public static void redirect(RoutingContext req, String location) {
        req.response().setChunked(true);
        req.response().setStatusCode(302);
        MultiMap headers = req.response().headers();
        headers.set("Location", location);
        req.response().end();
    }
}
