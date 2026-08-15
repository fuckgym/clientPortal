//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw;

import com.lx.gw.http.GatewayHttpProxy;
import com.lx.gw.http.GwWebsocketHandler;
import com.lx.gw.utils.OptionUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;

public class GatewayVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(GatewayVerticle.class);
    private Config config;
    private HttpServer httpServer;

    public GatewayVerticle() {
    }

    public void start(Future<Void> fut) {
        this.config = OptionUtils.loadSharedGlobalConfig(this.vertx);
        HttpServerOptions options = new HttpServerOptions();
        GatewayHttpProxy gatewayHttpProxy = new GatewayHttpProxy(this.vertx, this.config);
        if (this.config.getListenSsl()) {
            options.setSsl(true).setKeyStoreOptions(this.getJksOptions());
        }

        this.httpServer = this.vertx.createHttpServer(options);
        HttpServer var10000 = this.httpServer;
        Router var10001 = gatewayHttpProxy.getRouter();
        var10000.requestHandler(var10001::accept);
        this.httpServer.websocketStream().handler(new GwWebsocketHandler(this.vertx, this.config));
        logger.info("version: a27ed42161ad96c53e715ca5c5e3e3fa4cff5262 Mon, 24 Apr 2023 15:41:53 -0400");
        logger.info("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("****************************************************");
        System.out.println("version: a27ed42161ad96c53e715ca5c5e3e3fa4cff5262 Mon, 24 Apr 2023 15:41:53 -0400");
        System.out.println("****************************************************");
        System.out.println("This is the Client Portal Gateway");
        System.out.println("for any issues, please contact api@ibkr.com");
        System.out.println("and include a copy of your logs");
        System.out.println("****************************************************");
        System.out.println("https://www.interactivebrokers.com/api/doc.html");
        System.out.println("****************************************************");
        System.out.println("Open " + (this.config.getListenSsl() ? "https://" : "http://") + "localhost:" + this.config.getListenPort() + " to login");
        System.out.println("App demo is available after you login under: " + (this.config.getListenSsl() ? "https://" : "http://") + "localhost:" + this.config.getListenPort() + "/demo#/");
        this.httpServer.listen(this.config.getListenPort(), (res) -> {
            if (res.succeeded()) {
                fut.complete();
            } else {
                fut.fail(res.cause().getMessage());
                System.err.println("Server listen failed " + res.cause().getMessage());
            }

        });
    }

    public void stop(Future<Void> stopFuture) {
    }

    private JksOptions getJksOptions() {
        return (new JksOptions()).setPath(this.config.getSslCert()).setPassword(this.config.getSslPwd());
    }
}
