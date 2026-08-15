//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.services;

import com.lx.gw.Config;
import java.lang.reflect.Field;

public class ServiceEndPoints {
    public static String ACCTS_URL = "/AccountManagement/OneBarPicker/Picker/Data";
    public static String AUTHENTICATE_URL = "/AccountManagement/OneBarAuthentication?json=1";
    public static String ISERVER_ACCOUNTS = "{portalBase}/{env}/{portal}/iserver/accounts";
    public static String ISERVER_STATUS = "{portalBase}/{env}/{portal}/iserver/auth/status";
    public static String PORTAL_LOGOUT = "{portalBase}/{env}/{portal}/logout";
    public static String PORTAL_TICKLE = "{portalBase}/{env}/{portal}/tickle";
    public static String SSO_PING = "/sso/ping";
    public static String PICKER_URL = "/AccountManagement/OneBarPicker/Picker/Info";
    public static String SSO_VALIDATE_URL = "{portalBase}/{env}/{portal}/sso/validate?gw=1";
    public static String SSOST_COMPLETE = "/sso/AuthenticateST?ACTION=COMPLETE&SERVICE=AM.LOGIN";
    public static String PUBLISH_TST_TOKEN = "/sso/Authenticator?ACTION=PUBLISH_TST&RESP_TYPE=JSON";
    public static String REQ_CHALLENGE_FOR_TST = "/sso/AuthenticateST?ACTION=INIT&SERVICE=AM.LOGIN";
    public static String SSODH_INIT = "{portalBase}/{env}/{portal}/iserver/auth/ssodh/init";
    public static String SSODH_INIT_URL = "{portalBase}/{env}/{portal}/ssodh/init";
    public static String SSODH_GET_ST = "{portalBase}/{env}/{portal}/ssodh/st";
    public static String SSODH_RESPONSE = "{portalBase}/{env}/{portal}/iserver/auth/ssodh/response";
    public static String CCP_INIT = "{portalBase}/{env}/{portal}/ccp/auth/init";
    public static String CCP_RESPONSE = "{portalBase}/{env}/{portal}/ccp/auth/response";
    public static String CCP_STATUS = "{portalBase}/{env}/{portal}/ccp/status";
    public static String CCP_ACCOUNTS = "{portalBase}/{env}/{portal}/ccp/accounts";
    public static String USER_URL = "{portalBase}/{env}/{portal}/one/user";
    public static String VALIDATE_ACCOUNTS = "{portalBase}/{env}/{portal}/portfolio/validate";

    public ServiceEndPoints() {
    }

    public static void setEnv(Config config) {
        String env = config.getServiceEnvironment();
        String portalBase = config.getPortalBaseURL();
        boolean isApi = config.getProxyRemoteHost().contains("api");

        for(Field f : ServiceEndPoints.class.getDeclaredFields()) {
            try {
                String val = (String)f.get(f);
                val = val.replace("{env}", env);
                val = val.replace("{portalBase}", portalBase);
                val = val.replace("{portal}", isApi ? "api" : "portal");
                f.set(f, val);
            } catch (Exception e) {
                System.err.println(e);
            }
        }

    }
}
