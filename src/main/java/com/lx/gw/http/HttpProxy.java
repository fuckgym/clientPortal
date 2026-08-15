//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.http;

import com.lx.gw.Config;
import com.lx.gw.core.CookieManager;
import com.lx.gw.services.BaseServiceProxy;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class HttpProxy {
    public static final String USER_AGENT = "ClientPortalGW/1";
    private static final Logger logger = LoggerFactory.getLogger(BaseServiceProxy.class);
    private static final Logger msgLogger = LoggerFactory.getLogger("HttpMessageLogger");
    private final CookieManager cm;
    private final Map<String, String> extraHeaders = new HashMap();
    private final String host;
    private final HttpClient proxy;

    public HttpProxy(Config config, HttpClient client) {
        this.proxy = client;
        this.cm = CookieManager.getInstance();
        this.host = config.getProxyRemoteHost();
    }

    public void captureCookies(HttpClientResponse res) {
        this.cm.capture(res);
    }

    public HttpClientRequest get(String getUrl, Handler<AsyncResult<Buffer>> reply) {
        return this.get(getUrl, reply, (Handler)null);
    }

    public HttpClientRequest get(String getUrl, Handler<AsyncResult<Buffer>> reply, Handler<Void> done) {
        long start = System.currentTimeMillis();
        HttpClientRequest clientReq = this.proxy.requestAbs(HttpMethod.GET, this.host() + getUrl, (res) -> {
            long took = System.currentTimeMillis() - start;
            logger.info("-> GET {},{}|{}ms", new Object[]{getUrl, res.statusCode(), took});
            msgLogger.info("-> GET {},{}|{}ms", new Object[]{getUrl, res.statusCode(), took});
            res.exceptionHandler((e) -> this.failed(reply, getUrl, e.toString()));
            res.handler((data) -> {
                this.captureCookies(res);
                if (res.statusCode() != 200) {
                    this.failed(reply, getUrl, this.getFailedReason(res, data));
                } else {
                    this.handleData(reply, getUrl, data);
                }

            });
            res.endHandler((v) -> {
                this.captureCookies(res);
                if (done != null) {
                    done.handle(null);
                }

                if (res.statusCode() != 200) {
                    this.failed(reply, getUrl, this.getFailedReason(res, (Buffer)null));
                }

            });
        });
        return this.prepareRequest(clientReq);
    }

    public HttpClient getClient() {
        return this.proxy;
    }

    public Map<String, String> getExtraHeaders() {
        return this.extraHeaders;
    }

    public String host() {
        return this.host;
    }

    public void internalProxy(String redirectTo, HttpServerRequest clientReq, Handler<HttpClientResponse> onEnd) {
        this.internalProxy(redirectTo, clientReq, onEnd, (DataIntercept)null);
    }

    public void internalProxy(String redirectTo, HttpServerRequest clientReq, Handler<HttpClientResponse> onEnd, DataIntercept intercept) {
        String uri = clientReq.uri();
        msgLogger.info("-> {} {}", new Object[]{clientReq.method(), uri});
        msgLogger.trace("client headers: {}", new Object[]{clientReq.headers().entries()});
        clientReq.headers().remove("Accept-Encoding");
        if (clientReq.path().startsWith("/sso")) {
            this.cm.capture(clientReq);
        }

        long start = System.currentTimeMillis();
        HttpClientRequest fwdReq = this.proxy.requestAbs(clientReq.method(), this.host + redirectTo, (serverRes) -> {
            long took = System.currentTimeMillis() - start;
            logger.info("-> GET {},{}|{}ms", new Object[]{uri, serverRes.statusCode(), took});
            msgLogger.info("<- {} {} {}", new Object[]{serverRes.statusCode(), clientReq.method(), uri});
            clientReq.response().setChunked(true);
            clientReq.response().setStatusCode(serverRes.statusCode());
            this.remapHeadersToClient(clientReq, serverRes);
            serverRes.handler((data) -> {
                String header = serverRes.getHeader("content-type");
                if (header != null && intercept != null && header.startsWith("text")) {
                    data = Buffer.buffer(intercept.intercept(data));
                }

                if (msgLogger.isTraceEnabled()) {
                    msgLogger.trace("| {}", new Object[]{data.toString(Charset.forName("UTF-8"))});
                } else if (msgLogger.isDebugEnabled()) {
                    msgLogger.debug("body size {}", new Object[]{data.length()});
                }

                clientReq.response().write(data);
            });
            serverRes.endHandler((e) -> onEnd.handle(serverRes));
        });
        fwdReq.exceptionHandler((arg) -> msgLogger.error("uri {} failed", arg, new Object[]{uri}));
        fwdReq.setChunked(false);
        clientReq.headers().remove("Host");
        fwdReq.headers().setAll(clientReq.headers());
        String cookies = this.cm.cookies(true, clientReq.path());
        fwdReq.headers().set("Cookie", cookies);
        if (!this.extraHeaders.isEmpty()) {
            fwdReq.headers().addAll(this.extraHeaders);
        }

        msgLogger.debug("fwd headers {}", new Object[]{fwdReq.headers().entries()});
        clientReq.handler((data) -> {
            msgLogger.debug("fwd data {}", new Object[]{data.length()});
            fwdReq.write(data);
        });
        clientReq.endHandler((v) -> fwdReq.end());
    }

    public HttpClientRequest post(String postUrl, Object payload, Handler<AsyncResult<Buffer>> reply) {
        String url = this.host() + postUrl;
        long start = System.currentTimeMillis();
        HttpClientRequest clientReq = this.proxy.requestAbs(HttpMethod.POST, url, (res) -> {
            long took = System.currentTimeMillis() - start;
            logger.info("-> POST {},{}|{}ms", new Object[]{url, res.statusCode(), took});
            msgLogger.info("-> POST {},{}|{}ms", new Object[]{url, res.statusCode(), took});
            if (msgLogger.isTraceEnabled()) {
                msgLogger.trace("payload | {}", new Object[]{payload});
            }

            res.exceptionHandler((e) -> this.failed(reply, url, e.toString()));
            res.handler((data) -> {
                this.captureCookies(res);
                if (res.statusCode() != 200) {
                    this.failed(reply, url, this.getFailedReason(res, data));
                } else {
                    this.handleData(reply, url, data);
                }

            });
            res.endHandler((v) -> {
                this.captureCookies(res);
                if (res.statusCode() != 200) {
                    this.failed(reply, postUrl, this.getFailedReason(res, (Buffer)null));
                }

            });
        });
        this.prepareRequest(clientReq);
        String post = payload.toString();
        clientReq.putHeader("Content-Length", "" + post.length());
        clientReq.putHeader("Content-Type", "application/json");
        clientReq.write(post.toString());
        return clientReq;
    }

    public HttpClientRequest prepareRequest(HttpClientRequest clientReq) {
        String cookies = this.cm.cookies(true, clientReq.path());
        clientReq.headers().set("Cookie", cookies);
        clientReq.headers().set("User-Agent", "ClientPortalGW/1");
        if (!this.extraHeaders.isEmpty()) {
            clientReq.headers().addAll(this.extraHeaders);
        }

        clientReq.exceptionHandler(this::handleException);
        msgLogger.info("-> request: {}", new Object[]{clientReq.uri()});
        msgLogger.trace("headers: \n {}", new Object[]{clientReq.headers()});
        return clientReq;
    }

    protected void handleException(Throwable t) {
        logger.error("request failed {}", t, new Object[]{t.getMessage()});
    }

    private void failed(Handler<AsyncResult<Buffer>> reply, String url, String reason) {
        logger.warn("failed {} | reason {}", new Object[]{url, reason});
        if (reply != null) {
            reply.handle(Future.failedFuture(reason));
        }

    }

    private String getFailedReason(HttpClientResponse res, Buffer data) {
        if (res.statusCode() == 401) {
            return "Access Denied";
        } else if (res.statusCode() == 601) {
            return "Session Expired";
        } else {
            return data != null ? data.toString() : "" + res.statusCode();
        }
    }

    private void handleData(Handler<AsyncResult<Buffer>> reply, String url, Buffer data) {
        if (msgLogger.isTraceEnabled()) {
            msgLogger.trace("{} | {}", new Object[]{url, data.toString()});
        }

        if (reply != null) {
            reply.handle(Future.succeededFuture(data));
        }

    }

    private void remapHeadersToClient(HttpServerRequest clientReq, HttpClientResponse serverRes) {
        MultiMap serverHeaders = serverRes.headers();
        String location = serverHeaders.get("Location");
        if (location != null) {
            location.replace(this.host, "");
        }

        serverHeaders.remove("Content-Length");
        serverHeaders.remove("Content-Security-Policy");
        MultiMap clientHeaders = clientReq.response().headers();
        if (serverHeaders.contains("Set-Cookie") && clientReq.path().startsWith("/sso")) {
            this.cm.capture(serverHeaders.getAll("Set-Cookie"));
            String cookies = this.cm.cookies(false, "");
            logger.debug("Remapping Set-cookies {} -> {}", new Object[]{serverHeaders.getAll("Set-Cookie"), cookies});
            clientHeaders.remove("Set-Cookie");
            clientHeaders.add("Set-Cookie", cookies);
        }

        clientHeaders.setAll(serverHeaders);
    }
}
