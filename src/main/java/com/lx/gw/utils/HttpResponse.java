//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.utils;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class HttpResponse {
    public static final int STATUS_ACCESS_DENIED = 401;
    public static final int STATUS_NOT_FOUND = 404;

    public HttpResponse() {
    }

    public static void sendAccessDenied(HttpServerRequest clientReq) {
        clientReq.response().setChunked(false);
        clientReq.response().setStatusCode(401);
        clientReq.response().end();
    }

    public static void sendNotFound(HttpServerRequest clientReq, String uri) {
        String res = "Unknown path " + uri;
        sendResponse(clientReq, res);
    }

    public static void sendResponse(HttpServerRequest clientReq, String res) {
        clientReq.response().setChunked(false);
        clientReq.response().putHeader("Content-Length", "" + res.length());
        clientReq.response().setStatusCode(404);
        clientReq.response().write(res).end();
    }

    public static void sendResponse(RoutingContext ctx, JsonObject res) {
        ctx.response().setStatusCode(200).putHeader("Content-Type", "application/json; charset=utf-8").end(res.toBuffer());
    }
}
