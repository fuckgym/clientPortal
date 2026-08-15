//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.services;

import ibgroup.security.auth.client.lib.Device;
import ibgroup.security.auth.client.lib.SsoCombined;
import com.lx.gw.Config;
import com.lx.gw.http.DataIntercept;
import com.lx.gw.services.ServiceEvent.EventType;
import com.lx.gw.utils.TstTokenUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SsoService extends BaseServiceProxy {
    private static final Logger logger = LoggerFactory.getLogger(SsoService.class);
    private BigInteger K;
    private final int ssoPing;
    private String mac;
    private String deviceId;
    private String tstToken;
    private int userId;

    public SsoService(Vertx vertx, HttpClient client, Config config) {
        super(vertx, client, config);
        this.K = BigInteger.ZERO;
        this.userId = 0;
        this.ssoPing = config.getSsoPing();
    }

    public void stComplete(String chlgRes) {
        String url = this.host() + ServiceEndPoints.SSOST_COMPLETE + "&UID=" + this.userId + "&CHALLENGE_RESPONSE=" + chlgRes;
        HttpClientRequest clientReq = this.proxy.getClient().requestAbs(HttpMethod.GET, url, (res) -> {
            logger.info("sso tst complete url status={}, url={}", new Object[]{res.statusCode(), url});
            traceServerResponse(url, res);
            res.exceptionHandler((e) -> {
                logger.error("sso tst complete exception {}", e);
                this.on(EventType.ON_SSOST_FAILED);
            });
            res.handler((data) -> {
                logger.debug("sso tst challenge reply:\n{}", new Object[]{data.toString()});
                JsonObject ret = new JsonObject(data);
                if (ret.containsKey("XYZAB_AM.LOGIN")) {
                    this.cm.add("XYZAB_AM.LOGIN", ret.getString("XYZAB_AM.LOGIN"), "/");
                    this.cm.add("XYZAB", ret.getString("XYZAB_AM.LOGIN"), "/");
                }

                if (ret.getBoolean("RESULT", false)) {
                    this.on(EventType.ON_SSOST_COMPLETED);
                }

            });
            res.endHandler((v) -> this.proxy.captureCookies(res));
        });
        this.proxy.prepareRequest(clientReq);
        traceClientRequest("sso", url, clientReq);
        clientReq.end();
    }

    public String getDeviceId() {
        if (this.deviceId == null || this.deviceId.isEmpty()) {
            String id = Device.genRandom();
            this.deviceId = id + "|" + this.getMac();
        }

        return this.deviceId;
    }

    public BigInteger getK() {
        return this.K;
    }

    private String getMac() {
        if (this.mac == null || this.mac.isEmpty()) {
            this.mac = TstTokenUtils.getMacAddress();
        }

        return this.mac;
    }

    public void setTstToken() {
        if (this.tstToken == null || this.tstToken.isEmpty()) {
            this.tstToken = TstTokenUtils.generateTSTK(this.getDeviceId(), this.K);
            logger.debug("tst token generated: {}", new Object[]{this.tstToken});
            this.publishTstToken();
        }

    }

    public void setUserId(int val) {
        this.userId = val;
    }

    public void onTimerEvent(Long time) {
        this.tickleSession();
    }

    public void reset() {
        this.K = BigInteger.ZERO;
    }

    public void resetTst() {
        this.mac = null;
        this.deviceId = null;
        this.tstToken = null;
    }

    private void publishTstToken() {
        String url = this.host() + ServiceEndPoints.PUBLISH_TST_TOKEN + "&DEVICE_ID=" + this.getDeviceId();
        HttpClientRequest clientReq = this.proxy.getClient().requestAbs(HttpMethod.GET, url, (res) -> {
            logger.info("publish tst token status={}, url={}", new Object[]{res.statusCode(), url});
            traceServerResponse(url, res);
            res.exceptionHandler((e) -> logger.error("publish tst token exception {}", e));
            res.handler((data) -> logger.debug("publish tst token reply:\n{}", new Object[]{data}));
        });
        this.proxy.prepareRequest(clientReq);
        traceClientRequest("sso", url, clientReq);
        clientReq.end();
    }

    public void reqChallengeForTst() {
        String url = this.host() + ServiceEndPoints.REQ_CHALLENGE_FOR_TST + "&UID=" + this.userId + "&DEVICE_ID=" + this.deviceId;
        HttpClientRequest clientReq = this.proxy.getClient().requestAbs(HttpMethod.GET, url, (res) -> {
            logger.info("request challenge for tst status={}, url={}", new Object[]{res.statusCode(), url});
            traceServerResponse(url, res);
            res.exceptionHandler((e) -> logger.error("request challenge for tst exception {}", e));
            res.handler((data) -> {
                logger.debug("received challenge for tst:\n{}", new Object[]{data});
                JsonObject result = new JsonObject(data);
                String c = result.getString("CHALLENGE", "");
                if (!c.isEmpty()) {
                    String chResp = SsoCombined.compute_sk(new BigInteger(c, 16), new BigInteger(this.tstToken, 16));
                    this.stComplete(chResp);
                } else {
                    logger.error("request challenge for tst failed {}", new Object[]{data});
                }

            });
        });
        this.proxy.prepareRequest(clientReq);
        traceClientRequest("sso", url, clientReq);
        clientReq.end();
    }

    private void setK() {
        String url = this.host() + ServiceEndPoints.SSODH_GET_ST;
        HttpClientRequest clientReq = this.proxy.getClient().requestAbs(HttpMethod.GET, url, (res) -> {
            res.exceptionHandler((e) -> {
                logger.error("error getting ST {}", e);
                this.on(EventType.ON_SSODH_FAILED);
            });
            res.handler((data) -> {
                JsonObject jo = data.toJsonObject();
                if (jo != null) {
                    String st = jo.getString("st");
                    if (st == null) {
                        logger.error("No st in response: {}", new Object[]{jo});
                        this.on(EventType.ON_SSODH_FAILED);
                        return;
                    }

                    this.K = new BigInteger(st, 16);
                    logger.debug("K {}", new Object[]{this.K.toString(16)});
                    this.on(EventType.ON_SSODH_COMPLETED);
                } else {
                    logger.error("ST response null or not json: {}", new Object[]{data});
                    this.on(EventType.ON_SSODH_FAILED);
                }

            });
            res.endHandler((v) -> this.proxy.captureCookies(res));
        });
        this.proxy.prepareRequest(clientReq);
        traceClientRequest("sso", url, clientReq);
        clientReq.end();
    }

    public void ssoDHInit() {
        this.stopTimer();
        this.startTimer(TimeUnit.MINUTES.toMillis((long)this.ssoPing));
        String url = this.host() + ServiceEndPoints.SSODH_INIT_URL;
        HttpClientRequest clientReq = this.proxy.getClient().requestAbs(HttpMethod.GET, url, (res) -> {
            logger.info("sso init status={}, url={}", new Object[]{res.statusCode(), url});
            traceServerResponse(url, res);
            res.exceptionHandler((e) -> {
                logger.error("ssodh init exception {}", e);
                this.on(EventType.ON_SSODH_FAILED);
            });
            res.handler((data) -> {
                JsonObject jo = data.toJsonObject();
                if (jo != null) {
                    if (jo.getBoolean("success") != null) {
                        this.setK();
                    } else {
                        logger.error("ssodh response error: {}", new Object[]{data});
                        this.on(EventType.ON_SSODH_FAILED);
                    }
                } else {
                    logger.error("ssodh response error: {}", new Object[]{data});
                    this.on(EventType.ON_SSODH_FAILED);
                }

            });
            res.endHandler((v) -> this.proxy.captureCookies(res));
        });
        HttpClientRequest request = this.proxy.prepareRequest(clientReq);
        traceClientRequest("sso", url, clientReq);
        request.end();
    }

    String baseUrl() {
        return "sso";
    }

    void onProxyRequest(HttpServerRequest clientReq, Handler<HttpClientResponse> onEnd) {
        this.proxy.internalProxy(clientReq.uri(), clientReq, onEnd, new DataIntercept() {
            public String intercept(Buffer in) {
                String data = in.toString();
                data = data.replaceAll("http.*?\\/images", "\\/images");
                data = data.replaceAll("http.*?\\/scripts", "\\/scripts");
                data = data.replaceAll("http.*?\\/fonts", "\\/fonts");
                data = data.replaceAll("http.*?\\/stylesheet", "\\/stylesheet");
                return data;
            }
        });
    }

    private BigInteger extractBigInt(String data, String elem) {
        try {
            Pattern p = Pattern.compile("<" + elem + ">(.*)</" + elem + ">");
            Matcher m = p.matcher(data);
            if (m.find()) {
                return new BigInteger(m.group(1), 16);
            }
        } catch (Exception var5) {
            logger.error("failed to extract bigint {}", new Object[]{elem});
        }

        return null;
    }

    private void tickleSession() {
        this.proxy.get(ServiceEndPoints.SSO_PING, (data) -> logger.info("sso ping {}", new Object[]{data.result()})).end();
    }
}
