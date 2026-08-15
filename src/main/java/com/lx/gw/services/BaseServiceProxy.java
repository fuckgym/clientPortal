//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.services;

import com.lx.gw.Config;
import com.lx.gw.core.CookieManager;
import com.lx.gw.http.HttpProxy;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class BaseServiceProxy {
    private static final Logger logger = LoggerFactory.getLogger(BaseServiceProxy.class);
    private static final Logger msgLogger = LoggerFactory.getLogger("HttpMessageLogger");
    private static Map<String, BaseServiceProxy> svcMap = new HashMap();
    protected final CookieManager cm;
    protected final HttpProxy proxy;
    protected long timer = 0L;
    private final String host;
    private List<Handler<ServiceEvent>> listeners = new ArrayList();
    private final Vertx vertx;
    Config config;

    public BaseServiceProxy(Vertx vertx, HttpClient client, Config config) {
        this.proxy = new HttpProxy(config, client);
        this.config = config;
        this.vertx = (Vertx)Objects.requireNonNull(vertx);
        this.host = config.getProxyRemoteHost();
        this.cm = CookieManager.getInstance();
        ServiceEndPoints.setEnv(config);
        svcMap.put(this.baseUrl(), this);
    }

    public void listener(Handler<ServiceEvent> listener) {
        this.listeners.add(listener);
    }

    public void onTimerEvent(Long time) {
        logger.info("onTimer {}", new Object[]{time});
    }

    protected String host() {
        return this.host;
    }

    protected void on(ServiceEvent.EventType event) {
        this.on(event, (Object)null);
    }

    protected void on(ServiceEvent.EventType event, Object payload) {
        this.vertx.runOnContext((run) -> {
            logger.debug("forward event {}, payload {}", new Object[]{event, payload == null ? "none" : payload});
            this.listeners.forEach((h) -> h.handle(new ServiceEvent(event, payload)));
        });
    }

    protected void startTimer(long period) {
        if (this.timer == 0L) {
            logger.debug("start timer {} {}", new Object[]{period, this.getClass().getSimpleName()});
            this.timer = this.vertx().setPeriodic(period, this::onTimerEvent);
        }

    }

    protected void stopTimer() {
        logger.debug("cancel timer {} {}", new Object[]{this.getClass().getSimpleName(), this.timer});
        this.vertx().cancelTimer(this.timer);
        this.timer = 0L;
    }

    protected Vertx vertx() {
        return this.vertx;
    }

    abstract String baseUrl();

    abstract void onProxyRequest(HttpServerRequest var1, Handler<HttpClientResponse> var2);

    public static void fwdToSvc(HttpServerRequest clientReq, String svc, Handler<HttpClientResponse> onEnd) {
        BaseServiceProxy service = (BaseServiceProxy)svcMap.get(svc);
        if (service == null) {
            clientReq.response().setChunked(true);
            clientReq.response().write("Unknown service ").write(svc).end();
        } else {
            service.onProxyRequest(clientReq, onEnd);
        }
    }

    public static void traceClientRequest(String svc, String url, HttpClientRequest clientReq) {
        msgLogger.info("-> {} {} {}", new Object[]{svc, clientReq.method(), url});
        msgLogger.debug(" headers: {}", new Object[]{clientReq.headers().entries()});
    }

    public static void traceServerResponse(String url, HttpClientResponse res) {
        res.bodyHandler((body) -> msgLogger.info("<- {} {} {} {}", new Object[]{res.statusCode(), HttpMethod.GET, url, body.length()}));
    }
}
