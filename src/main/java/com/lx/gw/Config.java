//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw;

import io.vertx.core.VertxOptions;
import io.vertx.core.shareddata.Shareable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config implements Shareable {
    public static final String MESSAGE_LOGGER = "HttpMessageLogger";
    private int authDelay = 3000;
    private Map<String, Object> cors = new HashMap();
    private String ip2loc = "US";
    private Map<String, List<String>> ips = new HashMap();
    private int listenPort = 5000;
    private boolean listenSsl = true;
    private boolean tst = false;
    private String portalBaseURL;
    private String twsBaseURL = "/tws.proxy";
    private String proxyRemoteHost = "gdcdyn.interactivebrokers.com";
    private boolean proxyRemoteSsl = true;
    private String sslCert = "vertx.jks";
    private String sslPwd = "mywebapi";
    private int ssoPing = 5;
    private String svcEnvironment = "v1";
    private long tickleDelay = 30000L;
    private Map<String, Integer> vertxOptions = new HashMap();
    private List<WebApps> webapps = new ArrayList();
    private boolean ccp = false;

    public Config() {
    }

    public boolean getCcp() {
        return this.ccp;
    }

    public int getAuthDelay() {
        return this.authDelay;
    }

    public Map<String, Object> getCors() {
        return this.cors;
    }

    public String getIp2loc() {
        return this.ip2loc;
    }

    public Map<String, List<String>> getIps() {
        return this.ips;
    }

    public int getListenPort() {
        return this.listenPort;
    }

    public boolean getListenSsl() {
        return this.listenSsl;
    }

    public String getPortalBaseURL() {
        return this.portalBaseURL;
    }

    public String getProxyRemoteHost() {
        return this.proxyRemoteHost;
    }

    public boolean getProxyRemoteSsl() {
        return this.proxyRemoteSsl;
    }

    public String getServiceEnvironment() {
        return this.svcEnvironment;
    }

    public String getSslCert() {
        return this.sslCert;
    }

    public String getSslPwd() {
        return this.sslPwd;
    }

    public int getSsoPing() {
        return this.ssoPing;
    }

    public String getSvcEnvironment() {
        return this.svcEnvironment;
    }

    public long getTickleDelay() {
        return this.tickleDelay;
    }

    public boolean getTst() {
        return this.tst;
    }

    public String getTwsBaseURL() {
        return this.twsBaseURL;
    }

    public VertxOptions getVertxOptions() {
        VertxOptions opts = new VertxOptions();
        if (this.vertxOptions.containsKey("blockedThreadCheckInterval")) {
            opts.setBlockedThreadCheckInterval((long)(Integer)this.vertxOptions.get("blockedThreadCheckInterval"));
        }

        if (this.vertxOptions.containsKey("eventLoopPoolSize")) {
            opts.setEventLoopPoolSize((Integer)this.vertxOptions.get("eventLoopPoolSize"));
        }

        if (this.vertxOptions.containsKey("workerPoolSize")) {
            opts.setWorkerPoolSize((Integer)this.vertxOptions.get("workerPoolSize"));
        }

        if (this.vertxOptions.containsKey("maxWorkerExecuteTime")) {
            opts.setMaxWorkerExecuteTime((long)(Integer)this.vertxOptions.get("maxWorkerExecuteTime"));
        }

        if (this.vertxOptions.containsKey("internalBlockingPoolSize")) {
            opts.setInternalBlockingPoolSize((Integer)this.vertxOptions.get("internalBlockingPoolSize"));
        }

        return opts;
    }

    public List<WebApps> getWebapps() {
        return this.webapps;
    }

    public List<WebApps> getWebApps() {
        return this.webapps;
    }

    public void setCcp(boolean ccp) {
        this.ccp = ccp;
    }

    public void setAuthDelay(int in) {
        this.authDelay = in;
    }

    public void setCors(Map<String, Object> map) {
        this.cors = map;
    }

    public void setIp2loc(String ip2loc) {
        this.ip2loc = ip2loc;
    }

    public void setIps(Map<String, List<String>> map) {
        this.ips = map;
    }

    public void setListenPort(int val) {
        this.listenPort = val;
    }

    public void setListenSsl(boolean val) {
        this.listenSsl = val;
    }

    public void setPortalBaseURL(String portalBaseURL) {
        this.portalBaseURL = portalBaseURL;
    }

    public void setProxyRemoteHost(String val) {
        this.proxyRemoteHost = val;
    }

    public void setProxyRemoteSsl(boolean proxySsl) {
        this.proxyRemoteSsl = proxySsl;
    }

    public void setServerOptions(Map<String, Integer> map) {
        this.vertxOptions = map;
    }

    public void setSslCert(String sslCert) {
        this.sslCert = sslCert;
    }

    public void setSslPwd(String sslPwd) {
        this.sslPwd = sslPwd;
    }

    public void setSsoPing(int ssoPing) {
        this.ssoPing = ssoPing;
    }

    public void setSvcEnvironment(String environment) {
        this.svcEnvironment = environment;
    }

    public void setTst(boolean val) {
        this.tst = val;
    }

    public void setTwsBaseURL(String twsBaseURL) {
        this.twsBaseURL = twsBaseURL;
    }

    public void setWebApps(List<WebApps> webapps) {
        this.webapps = webapps;
    }

    public static class WebApps {
        public boolean cache = true;
        public String index = "index.html";
        public boolean listing = false;
        public String name = "";
        public String proxy = "";

        public WebApps() {
        }
    }
}
