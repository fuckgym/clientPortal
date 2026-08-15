//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.http;

import com.lx.gw.Config;
import com.lx.gw.core.CookieManager;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.HashSet;
import java.util.Set;

public class GwWebsocketHandler implements Handler<ServerWebSocket> {
    private static final Logger logger = LoggerFactory.getLogger(GwWebsocketHandler.class);
    private static final String STRING_ENCODING = "ISO-8859-1";
    private HttpClient httpClient;
    private final String proxyBase;
    private final Set<String> webSockethanlders = new HashSet();
    private final boolean isHttps;
    private final String env;

    public GwWebsocketHandler(Vertx vertx, Config config) {
        this.isHttps = config.getProxyRemoteSsl();
        this.env = config.getServiceEnvironment();
        this.proxyBase = config.getPortalBaseURL();
        HttpClientOptions options = new HttpClientOptions();
        if (this.isHttps) {
            String host = config.getProxyRemoteHost().replace("https://", "");
            options.setDefaultHost(host).setDefaultPort(443).setSsl(true);
        } else {
            String hostPort = config.getProxyRemoteHost().replace("http://", "");
            String[] arr = hostPort.split(":");
            if (arr.length == 2) {
                options.setDefaultHost(arr[0]).setDefaultPort(Integer.parseInt(arr[1])).setSsl(false);
            } else {
                logger.error("Proxy remote host is not in right format: {}", new Object[]{config.getProxyRemoteHost()});
            }
        }

        this.httpClient = vertx.createHttpClient(options);
    }

    public void handle(ServerWebSocket ws) {
        String relativeUrl = this.proxyBase + ws.uri();
        this.webSockethanlders.add(ws.textHandlerID());
        MultiMap map = MultiMap.caseInsensitiveMultiMap();
        map.add("Cookie", CookieManager.getInstance().cookies(true, ws.path()));
        map.add("User-Agent", "ClientPortalGW/1");
        logger.info("starting ws: {} -> {}. id: {}", new Object[]{ws.uri(), relativeUrl, ws.textHandlerID()});
        if (logger.isDebugEnabled()) {
            logger.info(" ws headers: {}", new Object[]{map.entries()});
        }

        this.httpClient.websocket(relativeUrl, map, (res) -> {
            logger.info("connected to server: {}", new Object[]{ws.uri()});
            this.webSockethanlders.add(res.textHandlerID());
            res.handler((data) -> {
                logger.debug("Websocket Proxy response body: " + data.toString("ISO-8859-1"));
                ws.write(data);
            });
            res.exceptionHandler((e) -> logger.error("Websocket Proxy error: " + e.getMessage()));
            res.endHandler((v) -> {
                this.webSockethanlders.remove(res.textHandlerID());
                logger.info("disconnect to server");
                if (this.webSockethanlders.contains(ws.textHandlerID())) {
                    ws.close();
                    this.webSockethanlders.remove(ws.textHandlerID());
                }

            });
            ws.handler((buffer) -> this.handleWebMessage(buffer, res));
            ws.closeHandler((__) -> this.handleWebClosed(ws.textHandlerID(), res));
            ws.exceptionHandler((throwable) -> this.handleWebException(throwable));
        });
    }

    private void handleWebClosed(String wsId, WebSocket serversocket) {
        logger.info("connection to {} closed", new Object[]{wsId});
        this.webSockethanlders.remove(wsId);
        if (this.webSockethanlders.contains(serversocket.textHandlerID())) {
            serversocket.close();
            this.webSockethanlders.remove(serversocket.textHandlerID());
        }

    }

    private void handleWebException(Throwable throwable) {
        logger.error("web socket exception for : {}", throwable);
    }

    private void handleWebMessage(Buffer buffer, WebSocket serverSocket) {
        logger.debug("user message received from front-end: " + buffer);
        serverSocket.writeTextMessage(buffer.toString());
    }
}
