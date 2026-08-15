//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.services;

public class ServiceEvent {
    public Object payload;
    public EventType type;

    ServiceEvent(EventType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public static enum EventType {
        CP_LOGIN_FAILED,
        ON_ACCOUNTS,
        ON_CP_TICKLE_FAILED,
        ON_ISERVER_SESSION_FAILED,
        ON_PORTAL_AUTO_LOGOUT,
        ON_ISERVER_UNAUTHENTICATED,
        ON_MAC_ADRESS,
        ON_SSO_VALIDATION,
        ON_SSO_AUTHENTICATED,
        ON_SSODH_COMPLETED,
        ON_SSODH_FAILED,
        ON_SSOST_COMPLETED,
        ON_SSOST_FAILED;

        private EventType() {
        }
    }
}
