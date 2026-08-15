//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.http;

import com.lx.gw.Config;
import com.lx.gw.core.CookieManager;
import com.lx.gw.services.BaseServiceProxy;
import com.lx.gw.services.ClientPortalService;
import com.lx.gw.services.ServiceEvent;
import com.lx.gw.services.SsoService;
import com.lx.gw.utils.HttpResponse;
import com.lx.gw.utils.IPv4Validator;
import com.lx.gw.utils.RequestUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import java.util.Map;
import java.util.Objects;

public class GatewayHttpProxy {
    private static final Logger logger = LoggerFactory.getLogger(GatewayHttpProxy.class);
    private Config config;
    private int cpLoginRetry = 0;
    private final ClientPortalService cpService;
    private final HttpProxy httpProxy;
    private final HttpClient ibClient;
    private boolean loggedIn = false;
    private final Router router;
    private final SsoService ssoService;
    private final IPv4Validator validator;
    private final Vertx vertx;

    public GatewayHttpProxy(Vertx vertx, Config config) {
        HttpClientOptions options = (new HttpClientOptions()).setSsl(true);
        options.setConnectTimeout(15000);
        this.config = (Config)Objects.requireNonNull(config);
        this.vertx = (Vertx)Objects.requireNonNull(vertx);
        this.ibClient = vertx.createHttpClient(options);
        this.validator = new IPv4Validator(config);
        this.httpProxy = new HttpProxy(config, this.ibClient);
        this.cpService = new ClientPortalService(vertx, this.ibClient, config);
        this.ssoService = new SsoService(vertx, this.ibClient, config);
        this.cpService.listener((event) -> this.onEvent(event));
        this.ssoService.listener((event) -> this.onEvent(event));
        this.router = Router.router(vertx);
        this.router.route("/*").handler(this::securityCheck);
        if (config.getCors() != null) {
            Map<String, Object> corsConfig = config.getCors();
            String corsOrig = (String)corsConfig.getOrDefault("origin.allowed", "*");
            boolean corsCred = (Boolean)corsConfig.getOrDefault("allowCredentials", false);
            CorsHandler allowCredentials = CorsHandler.create(corsOrig).allowCredentials(corsCred).allowedMethod(HttpMethod.POST).allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST).allowedMethod(HttpMethod.OPTIONS).allowedMethod(HttpMethod.DELETE).allowedMethod(HttpMethod.PUT).allowedHeader("Content-Type").allowedHeader("portal").allowedHeader("Cookie");
            this.router.route().handler(allowCredentials);
        }

        this.router.route("/").handler(this::loginRedirect);
        this.router.route("/status").handler(this::handleStatus);
        this.router.route("/clear").handler((req) -> {
            CookieManager.getInstance().clear();
            req.response().end("cleared");
        });
        this.router.routeWithRegex("/" + config.getServiceEnvironment() + "/(portal|api)/iserver/reauthenticate").handler(this::handleReauthenticate);

        for(Config.WebApps app : config.getWebApps()) {
            logger.info(" -> mount {} on {}", new Object[]{app.name, "/" + app.name});
            System.out.println(" -> mount " + app.name + " on /" + app.name);
            this.router.route("/" + app.name + "/*").handler(StaticHandler.create().setWebRoot("root/webapps/" + app.name).setDirectoryListing(app.listing).setCachingEnabled(app.cache));
        }

        this.router.routeWithRegex("^/(css|scripts|stylesheet|images|fonts).*").handler(this::forwardRequest);
        this.router.route("/en/includes/general/gdpr-am.php").handler(this::forwardRequest);
        this.router.routeWithRegex("^/sso/(lib|images)/.*").handler(this::forwardRequest);
        this.router.routeWithRegex("^/(sso)/.*").handler(this::generalHandler);
        this.router.routeWithRegex("^/portal.proxy/(v1|qa|ci|alpha|local)/gstat/bulletins").handler(this::forwardRequest);
        this.router.routeWithRegex("/:version/:service/logout").handler(this::handleLogout);
        this.router.routeWithRegex("^/(v1|qa|ci|alpha|local)/(portal|am|api|tws)/(.*)").handler(this::generalHandler);
        this.router.route("/*").handler(this::handleRedirect);
    }

    public Router getRouter() {
        return this.router;
    }

    private void handleLogout(RoutingContext ctx) {
        HttpServerRequest clientReq = ctx.request();
        this.cpService.onProxyRequest(clientReq, (serverRes) -> {
            if (serverRes.statusCode() == 200) {
                clientReq.response().setStatusCode(200);
                clientReq.response().write("\nClient logout succeeded\n");
                clientReq.response().end();
                logger.info("Client logout succeeded");
                this.loggedIn = false;
                CookieManager.getInstance().clear();
            } else {
                clientReq.response().end();
            }

        });
    }

    private void generalHandler(RoutingContext ctx) {
        HttpServerRequest clientReq = ctx.request();
        String service = clientReq.getParam("param1");
        if (service == null) {
            service = clientReq.getParam("param0");
        }

        if (service == null) {
            HttpResponse.sendNotFound(clientReq, clientReq.uri());
        } else {
            if (service.equals("api")) {
                service = "portal";
            }

            BaseServiceProxy.fwdToSvc(clientReq, service, (serverRes) -> {
                if (serverRes.statusCode() == 302 && clientReq.uri().startsWith("/sso/Dispatcher")) {
                    clientReq.response().setStatusCode(200);
                    clientReq.response().write("Client login succeeds");
                    clientReq.response().end();
                    logger.info("Client login succeeds");
                    this.onLoggedIn();
                } else {
                    clientReq.response().end();
                }

            });
        }
    }

    private void forwardRequest(RoutingContext ctx) {
        HttpServerRequest request = ctx.request();
        this.httpProxy.internalProxy(request.uri(), request, (serverRes) -> request.response().end());
    }

    private void handleReauthenticate(RoutingContext ctx) {
        logger.info("reauthenticate {}", new Object[]{this.loggedIn});
        if (!this.loggedIn) {
            HttpResponse.sendResponse(ctx, (new JsonObject()).put("error", "not logged in"));
        } else {
            HttpResponse.sendResponse(ctx, (new JsonObject()).put("message", "triggered"));
            this.loginToCp();
        }

    }

    private void loginRedirect(RoutingContext ctx) {
        RequestUtils.redirect(ctx, "/sso/Login?forwardTo=22&RL=1&ip2loc=" + this.config.getIp2loc());
    }

    private void handleRedirect(RoutingContext ctx) {
        RequestUtils.redirect(ctx, this.config.getProxyRemoteHost() + ctx.normalisedPath());
    }

    private void handleStatus(RoutingContext ctx) {
    }

    private void loginToCp() {
        ++this.cpLoginRetry;
        this.cpService.reset();
        this.cpService.authenticate((res) -> {
            if (res.succeeded()) {
                logger.info("authenticated to cp");
                this.cpLoginRetry = 0;
                this.cpService.getUser();
                this.ssoService.ssoDHInit();
            } else {
                logger.error("authentication to cp failed, retry {}", res.cause(), new Object[]{this.cpLoginRetry});
                if (this.cpLoginRetry < 5) {
                    this.vertx.setTimer(3000L, (run) -> this.loginToCp());
                } else {
                    logger.error("giving up ...");
                }
            }

        });
    }

    private void onEvent(ServiceEvent event) {
        switch (event.type) {
            case ON_SSODH_FAILED:
                logger.error("ssodh failed, retry with /iserver/reauthenticate");
                break;
            case ON_SSO_VALIDATION:
                this.ssoService.setUserId((Integer)event.payload);
                break;
            case ON_SSODH_COMPLETED:
                logger.debug("delaying authentication for {}", new Object[]{this.config.getAuthDelay()});
                this.vertx.setTimer((long)this.config.getAuthDelay(), (run) -> this.cpService.setK(this.ssoService.getK()));
                this.ssoService.setTstToken();
                break;
            case ON_SSOST_COMPLETED:
                this.onLoggedIn();
            case ON_CP_TICKLE_FAILED:
                if (this.loggedIn && !this.config.getTst()) {
                    this.loginToCp();
                }
            case ON_ISERVER_UNAUTHENTICATED:
            case ON_PORTAL_AUTO_LOGOUT:
            case ON_ISERVER_SESSION_FAILED:
                if (this.loggedIn && this.config.getTst()) {
                    this.ssoService.reqChallengeForTst();
                }
                break;
            default:
                logger.info("Unhandled event {}", new Object[]{event.type});
        }

    }

    private void onLoggedIn() {
        this.loggedIn = true;
        this.ssoService.reset();
        if (this.config.getTst()) {
            this.ssoService.resetTst();
        }

        this.loginToCp();
    }

    private void securityCheck(RoutingContext ctx) {
        HttpServerRequest clientReq = ctx.request();
        SocketAddress remoteIp = clientReq.remoteAddress();
        String ip = remoteIp.host();
        if (!this.validator.isAllowedIPAddress(ip)) {
            HttpResponse.sendResponse(clientReq, "Access Denied");
        } else {
            ctx.next();
        }
    }
}
