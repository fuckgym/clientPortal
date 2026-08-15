//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.core;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Cookie;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

public class CookieManager {
    private static final List<String> allowed = Arrays.asList("URL_PARAM", "RL", "JSESSIONID", "cp", "cp.qa", "api", "api.nn", "api.alpha", "portal", "cp.alpha", "cp.beta", "USERID", "XYZAB", "REGION", "XYZAB_AM.LOGIN", "web");
    private static final String VERSION_NUMBER = "1.0.0";
    private static final List<String> clientCaptures = Arrays.asList("XYZAB", "XYZAB_AM.LOGIN");
    private static CookieManager instance = null;
    private static final Logger logger = LoggerFactory.getLogger(CookieManager.class);
    private static final Logger msgLogger = LoggerFactory.getLogger("HttpMessageLogger");
    private Map<CookieWrapper, String> cookies = new HashMap();

    private CookieManager() {
        this.cookies.put(CookieManager.CookieWrapper.newInstance("JSESSIONID", "", "/sso"), "");
        this.cookies.put(CookieManager.CookieWrapper.newInstance("JSESSIONID", "", "/AccountManagement"), "");
        this.cookies.put(CookieManager.CookieWrapper.newInstance("REGION", "", "/"), "");
        this.cookies.put(CookieManager.CookieWrapper.newInstance("USERID", "", "/"), "");
        this.cookies.put(CookieManager.CookieWrapper.newInstance("VERSION", "1.0.0", "/sso/Login"), "1.0.0");
    }

    public void clear() {
        this.cookies.clear();
        this.cookies.put(CookieManager.CookieWrapper.newInstance("JSESSIONID", "", "/sso"), "");
        this.cookies.put(CookieManager.CookieWrapper.newInstance("JSESSIONID", "", "/AccountManagement"), "");
        this.cookies.put(CookieManager.CookieWrapper.newInstance("REGION", "", "/"), "");
        this.cookies.put(CookieManager.CookieWrapper.newInstance("USERID", "", "/"), "");
        this.cookies.put(CookieManager.CookieWrapper.newInstance("VERSION", "1.0.0", "/sso/Login"), "1.0.0");
    }

    public void add(String name, String val, String path) {
        CookieWrapper wrap = CookieManager.CookieWrapper.newInstance(name, val, path);
        String curr = (String)this.cookies.get(wrap);
        if (curr == null || !curr.equals(val)) {
            this.cookies.remove(wrap);
            this.cookies.put(wrap, val);
            msgLogger.debug("added cookie {} {} {}", new Object[]{name, path, val});
        }

    }

    public void capture(HttpClientResponse res) {
        if (!res.cookies().isEmpty()) {
            for(String str : res.cookies()) {
                Cookie c = this.createCookie(str);
                if (c != null) {
                    if (c.getValue() != null && !c.getValue().isEmpty()) {
                        this.replace(c);
                    } else {
                        logger.debug("ignored empty cookie {} {}", new Object[]{c.getName(), c.getPath()});
                    }
                }
            }
        }

    }

    public void capture(HttpServerRequest req) {
        String cookieStr = req.headers().get("Cookie");
        if (cookieStr != null && !cookieStr.isEmpty()) {
            logger.debug("capture cookies from server request {}", new Object[]{req.headers().entries()});
            String[] reqCookies = cookieStr.split("; ");

            for(String str : reqCookies) {
                Cookie c = this.createCookie(str);
                if (c != null && clientCaptures.contains(c.getName())) {
                    if (c.getValue() != null && !c.getValue().isEmpty()) {
                        this.replace(c);
                    } else {
                        logger.debug("ignored empty cookie {} {}", new Object[]{c.getName(), c.getPath()});
                    }
                }
            }

        }
    }

    public void capture(List<String> cookieList) {
        logger.info("set cookies {}", new Object[]{cookieList});

        for(String str : cookieList) {
            Cookie c = this.createCookie(str);
            if (c != null) {
                if (c.getValue() != null && !c.getValue().isEmpty()) {
                    this.replace(c);
                } else {
                    logger.debug("ignored empty cookie {} {}", new Object[]{c.getName(), c.getPath()});
                }
            }
        }

    }

    List<CookieWrapper> cookieList(String path) {
        return (List)this.cookies.keySet().stream().filter((p) -> {
            if (path == null) {
                return false;
            } else {
                String path2 = p.getCookie().getPath();
                return path2 == null ? true : path.contains(path2);
            }
        }).collect(Collectors.toList());
    }

    public String cookies(boolean toServer, String path) {
        return (String)this.cookieList(path).stream().map((c) -> this.encodeCookie(c, toServer)).collect(Collectors.joining("; "));
    }

    public int size() {
        return this.cookies.size();
    }

    private Cookie createCookie(String str) {
        Cookie ret = null;
        String[] args = str.split(";");
        if (args.length == 0) {
            return ret;
        } else {
            for(int x = 0; x < args.length; ++x) {
                String kvStr = args[x];
                int idx = kvStr.indexOf("=");
                String key = "";
                String val = "";
                if (idx != -1) {
                    key = kvStr.substring(0, idx);
                    val = kvStr.substring(idx + 1);
                } else {
                    key = kvStr;
                }

                if (x == 0) {
                    if (!allowed.contains(key)) {
                        logger.trace("cookie not allowed {}", new Object[]{key});
                        return null;
                    }

                    ret = Cookie.cookie(key, val);
                } else if (ret != null) {
                    key = key.toLowerCase().trim();
                    val = val.trim();
                    switch (key) {
                        case "domain":
                            ret.setDomain(val);
                            break;
                        case "path":
                            if (!val.isEmpty()) {
                                ret.setPath(val);
                            }
                            break;
                        case "secure":
                            ret.setSecure(true);
                            break;
                        case "httponly":
                            ret.setHttpOnly(true);
                    }
                }
            }

            return ret;
        }
    }

    private String encodeCookie(CookieWrapper c, boolean toServer) {
        return toServer ? c.getId() + "=" + c.getCookie().getValue() : c.getCookie().encode();
    }

    private void replace(Cookie c) {
        CookieWrapper wrap = CookieManager.CookieWrapper.wrap(c);
        String current = (String)this.cookies.get(wrap);
        if (current == null || !current.equals(c.getValue())) {
            this.cookies.remove(wrap);
            this.cookies.put(wrap, c.getValue());
            msgLogger.debug("replaced cookie: {} path={} old={} new={}", new Object[]{c.getName(), c.getPath(), current == null ? "null" : current, c.getValue()});
        }

    }

    public static CookieManager getInstance() {
        if (instance == null) {
            instance = new CookieManager();
        }

        return instance;
    }

    public static class CookieWrapper {
        private final Cookie cookie;
        private final String id;

        public CookieWrapper(String id, Cookie cookie) {
            this.id = id;
            this.cookie = cookie;
        }

        public boolean equals(Object obj) {
            CookieWrapper rhs = (CookieWrapper)obj;
            return (new EqualsBuilder()).append(this.id, rhs.getId()).append(this.cookie.getPath(), rhs.getCookie().getPath()).isEquals();
        }

        public Cookie getCookie() {
            return this.cookie;
        }

        public String getId() {
            return this.id;
        }

        public int hashCode() {
            return (new HashCodeBuilder()).append(this.id).append(this.cookie.getPath()).hashCode();
        }

        public String toString() {
            return ReflectionToStringBuilder.toString(this).toString();
        }

        public static CookieWrapper newInstance(String key, String val, String path) {
            Cookie c = Cookie.cookie(key, val);
            if (!path.isEmpty()) {
                c.setPath(path);
            }

            return new CookieWrapper(key, c);
        }

        public static CookieWrapper wrap(Cookie c) {
            if (c.getPath() == null || c.getPath().isEmpty()) {
                c.setPath("/");
            }

            return new CookieWrapper(c.getName(), c);
        }
    }
}
