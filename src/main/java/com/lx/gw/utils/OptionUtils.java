//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.utils;

import com.lx.gw.Config;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;

public class OptionUtils {
    public OptionUtils() {
    }

    public static Config loadSharedGlobalConfig(Vertx vertx) {
        SharedData sharedData = vertx.sharedData();
        return (Config)sharedData.getLocalMap("local").get("global");
    }

    public static void setSharedGlobalConfig(Vertx vertx, Config config) {
        SharedData sharedData = vertx.sharedData();
        LocalMap<Object, Object> localMap = sharedData.getLocalMap("local");
        localMap.put("global", config);
    }
}
