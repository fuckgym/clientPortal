//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.services;

import ibgroup.security.auth.client.lib.Device;
import ibgroup.security.auth.client.lib.SsoCombined;
import com.lx.gw.Config;
import com.lx.gw.services.ServiceEvent.EventType;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.math.BigInteger;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientPortalService extends BaseServiceProxy {
    public static final Logger logger = LoggerFactory.getLogger(ClientPortalService.class);
    private Boolean authenticated = null;
    private AtomicBoolean challengeSent = new AtomicBoolean(false);
    private Boolean competing = null;
    private final String env;
    private BigInteger K;
    private final String proxyBase;
    private JsonObject user = new JsonObject();
    private int authAttempts = 0;
    private boolean useCcp;

    public ClientPortalService(Vertx vertx, HttpClient client, Config config) {
        super(vertx, client, config);
        this.env = config.getServiceEnvironment();
        this.proxyBase = config.getPortalBaseURL();
        this.useCcp = config.getCcp();
    }

    public void authenticate(Handler<AsyncResult<JsonObject>> handler) {
        this.proxy.get(ServiceEndPoints.SSO_VALIDATE_URL, (reply) -> {
            logger.debug("portal validate response: {}", new Object[]{reply.result()});
            if (reply.succeeded()) {
                this.user = ((Buffer)reply.result()).toJsonObject();
                handler.handle(Future.succeededFuture(this.user));
                this.on(EventType.ON_SSO_VALIDATION, this.user.getInteger("USER_ID", 0));
            } else {
                handler.handle(Future.failedFuture(reply.cause()));
                this.on(EventType.CP_LOGIN_FAILED);
            }

        }).end();
    }

    public String baseUrl() {
        return "portal";
    }

    public Set<String> getUriMap() {
        return null;
    }

    public void getUser() {
        this.proxy.get(ServiceEndPoints.USER_URL, (reply) -> {
            logger.info("get user reply {}", new Object[]{reply.result()});
            if (reply.succeeded()) {
                this.user.mergeIn(((Buffer)reply.result()).toJsonObject());
            } else {
                logger.error("get user failed {}", reply.cause());
            }

        }).end();
    }

    public boolean isAuthenticated() {
        return this.authenticated && !this.competing;
    }

    public void onProxyRequest(HttpServerRequest clientReq, Handler<HttpClientResponse> onEnd) {
        String uri = clientReq.uri();
        if (uri.contains("/v1/portal")) {
            uri = uri.replace("/v1/portal", "/v1/api");
        }

        this.proxy.internalProxy(this.proxyBase + uri, clientReq, onEnd);
    }

    public void onTimerEvent(Long time) {
        this.tickleSession();
    }

    public void reset() {
        this.authenticated = false;
        this.competing = null;
        this.challengeSent.set(false);
    }

    public void setK(BigInteger K) {
        this.K = K;
        this.getStatus();
    }

    public void validateAccounts(JsonArray accts) {
        this.proxy.post(ServiceEndPoints.VALIDATE_ACCOUNTS, accts, (reply) -> logger.debug("validate accts reply: {}", new Object[]{reply.toString()})).end();
    }

    private void autoLogout() {
        this.proxy.get(ServiceEndPoints.PORTAL_LOGOUT, (reply) -> {
            logger.debug("portal logout {}", new Object[]{reply.result()});
            if (reply.succeeded()) {
                this.on(EventType.ON_PORTAL_AUTO_LOGOUT);
            } else {
                logger.error("logout failed", reply.cause());
            }

        }).end();
    }

    private void authenticateBrokerage() {
        if (this.K != null && this.user != null) {
            if (!this.challengeSent.compareAndSet(false, true)) {
                logger.info("chanllenge already sent {}", new Object[]{this.challengeSent});
            } else {
                JsonObject post = (new JsonObject()).put("username", this.user.getString("USER_NAME", "")).put("machineId", Device.genRandom()).put("compete", true);
                logger.debug("auth brokerage, k={}, post={}", new Object[]{this.K.toString(16), post});
                String initUrl = this.useCcp ? ServiceEndPoints.CCP_INIT : ServiceEndPoints.SSODH_INIT;
                this.proxy.post(initUrl, post, (reply) -> {
                    logger.debug("iserver init : {}", new Object[]{reply.result()});
                    if (reply.succeeded()) {
                        JsonObject obj = ((Buffer)reply.result()).toJsonObject();
                        if (obj.containsKey("error")) {
                            logger.error("{} init failed {}", new Object[]{this.useCcp ? "CCP" : "SSODH", obj});
                        } else if (obj.getBoolean("authenticated", false)) {
                            if (this.authAttempts++ < 5) {
                                this.processAuthStatus(obj);
                            } else {
                                logger.warn("Reauthentication failed 5 times, stopping");
                            }
                        } else if (obj.getBoolean("wait", false)) {
                            logger.info("waiting");
                            this.vertx().setTimer(5000L, (l) -> this.authenticateBrokerage());
                        } else {
                            BigInteger challenge = new BigInteger(obj.getString("challenge"), 16);
                            String sk = SsoCombined.compute_sk(challenge, this.K);
                            this.ssoDHResponse(sk);
                        }
                    } else {
                        logger.error("iserver init failed", reply.cause());
                        this.challengeSent.set(false);
                        this.on(EventType.ON_SSODH_FAILED);
                    }

                }).end();
            }
        } else {
            logger.info("can't authenticate yet K {}, user {}", new Object[]{this.K == null, this.user == null});
        }
    }

    private void getStatus() {
        String statusUrl = this.useCcp ? ServiceEndPoints.CCP_STATUS : ServiceEndPoints.ISERVER_STATUS;
        this.proxy.get(statusUrl, (reply) -> {
            logger.debug("status {}", new Object[]{reply.result()});
            if (reply.succeeded()) {
                this.processAuthStatus(((Buffer)reply.result()).toJsonObject());
            } else {
                logger.error("status failed", reply.cause());
            }

        }).end();
    }

    private void processAuthStatus(JsonObject status) {
        logger.info("process auth status {}", new Object[]{status});
        if (status.getBoolean("authenticated") != null) {
            this.authenticated = status.getBoolean("authenticated");
        }

        if (status.getBoolean("competing") != null) {
            this.competing = status.getBoolean("competing");
        }

        if (this.isAuthenticated()) {
            this.authAttempts = 0;
            String accUrl = this.useCcp ? ServiceEndPoints.CCP_ACCOUNTS : ServiceEndPoints.ISERVER_ACCOUNTS;
            this.proxy.get(accUrl, (data) -> logger.debug("iserver accounts {}", new Object[]{data.result()})).end();
            this.startTimer(this.config.getTickleDelay());
        } else {
            this.authenticateBrokerage();
        }

    }

    private void ssoDHResponse(String sk) {
        logger.info("sending sso dh response, k={}", new Object[]{sk});
        JsonObject post = (new JsonObject()).put("response", sk);
        String responseUrl = this.useCcp ? ServiceEndPoints.CCP_RESPONSE : ServiceEndPoints.SSODH_RESPONSE;
        this.proxy.post(responseUrl, post, (reply) -> {
            logger.info("server sso dh response {}", new Object[]{reply.result()});
            this.challengeSent.set(false);
            if (reply.succeeded()) {
                this.processAuthStatus(((Buffer)reply.result()).toJsonObject());
            } else {
                logger.error("sso sh response failed", reply.cause());
            }

        }).end();
    }

    private void tickleSession() {
        this.proxy.get(ServiceEndPoints.PORTAL_TICKLE, (data) -> {
            logger.debug("tickle {}", new Object[]{data.result()});
            if (data.failed()) {
                logger.error("tickle cp session failed {}", data.cause());
                this.stopTimer();
                this.on(EventType.ON_CP_TICKLE_FAILED);
            } else {
                JsonObject result = new JsonObject((Buffer)data.result());
                JsonObject iserver = result.getJsonObject("iserver");
                if (iserver != null) {
                    JsonObject authStatus = iserver.getJsonObject("authStatus");
                    if (authStatus != null) {
                        boolean status = authStatus.getBoolean("authenticated", true);
                        boolean connected = authStatus.getBoolean("connected", false);
                        if (connected && !status) {
                            this.autoLogout();
                        }
                    } else if (iserver.containsKey("error")) {
                        this.on(EventType.ON_ISERVER_SESSION_FAILED);
                    }
                }

            }
        }).end();
    }
}
