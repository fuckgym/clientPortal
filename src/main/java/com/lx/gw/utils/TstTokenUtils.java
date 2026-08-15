//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.utils;

import ibgroup.security.auth.client.lib.SsoCombined;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class TstTokenUtils {
    public static final Logger logger = LoggerFactory.getLogger(TstTokenUtils.class);

    public TstTokenUtils() {
    }

    public static String getMacAddress() {
        StringBuilder sb = new StringBuilder();

        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            byte[] mac = network.getHardwareAddress();

            for(int i = 0; i < mac.length; ++i) {
                sb.append(String.format("%02X%s", mac[i], i < mac.length - 1 ? "-" : ""));
            }

            logger.info("Current MAC address : {}", new Object[]{sb});
        } catch (UnknownHostException e) {
            logger.error("failed to get mac address: {}", new Object[]{e.getMessage()});
        } catch (SocketException e) {
            logger.error("failed to get mac address: {}", new Object[]{e.getMessage()});
        }

        return sb.toString();
    }

    public static String generateTSTK(String deviceId, BigInteger K) {
        String tag1 = getBytes(deviceId + "TST");
        String sha1 = SsoCombined.calcSHA1Hex(trimLeadingHexZero(tag1) + trimLeadingHexZero(K.toString(16)));
        return sha1;
    }

    private static String getBytes(String str) {
        if (str.isEmpty()) {
            return str;
        } else {
            String ret = "";

            for(int i = 0; i < str.length(); ++i) {
                int c = Character.codePointAt(str, i);
                ret = ret + Integer.toHexString(c);
            }

            return ret;
        }
    }

    private static String trimLeadingHexZero(String str) {
        while(str.length() > 1 && str.substring(0, 2).equals("00")) {
            str = str.substring(2);
        }

        return str;
    }
}
