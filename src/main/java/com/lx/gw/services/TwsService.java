//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.services;

import com.lx.gw.Config;
import com.lx.gw.http.DataIntercept;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TwsService extends BaseServiceProxy {
    private static final Logger logger = LoggerFactory.getLogger(TwsService.class);
    private final String proxyBase;

    public TwsService(Vertx vertx, HttpClient client, Config config) {
        super(vertx, client, config);
        this.proxyBase = config.getTwsBaseURL();
    }

    public void onTimerEvent(Long time) {
    }

    String baseUrl() {
        return "tws";
    }

    void onProxyRequest(HttpServerRequest clientReq, Handler<HttpClientResponse> onEnd) {
        String uri = clientReq.uri().replace("v1/tws/", "");
        this.proxy.internalProxy(this.proxyBase + uri, clientReq, onEnd, (DataIntercept)null);
    }
}
