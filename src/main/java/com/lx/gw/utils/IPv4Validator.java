//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw.utils;

import com.lx.gw.Config;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPv4Validator {
    private static final String RANGE = "([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])";
    private static final Pattern RANGE_PATTERN = Pattern.compile("([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])");
    private static final Logger logger = LoggerFactory.getLogger(IPv4Validator.class);
    private final List<String> allowedIPs;
    private final List<String> deniedIPs;
    private Pattern IPv4Pattern;
    private String ipRegex;

    public IPv4Validator(Config config) {
        Map<String, List<String>> ips = config.getIps();
        this.allowedIPs = (List)ips.get("allow");
        this.deniedIPs = (List)ips.get("deny");
    }

    public List<String> getAllowedIPs() {
        return this.allowedIPs;
    }

    public List<String> getDeniedIPs() {
        return this.deniedIPs;
    }

    public boolean isAllowedIPAddress(String ip) {
        if (ip != null && this.deniedIPs != null && this.allowedIPs != null) {
            if (this.matchAddress(ip, this.deniedIPs)) {
                logger.error("ipv4 address {} is forbidden", new Object[]{ip});
                return false;
            }

            if (!this.matchAddress(ip, this.allowedIPs)) {
                logger.error("ipv4 address {} is forbidden", new Object[]{ip});
                return false;
            }
        }

        return true;
    }

    public boolean matchAddress(String address, List<String> ips) {
        for(Object obj : ips) {
            String ip = (String)obj;
            this.setPattern(ip);
            if (this.validate(address)) {
                return true;
            }
        }

        return false;
    }

    public void setPattern(String address) {
        String[] arr = address.split("\\.");
        if (address.equals("*")) {
            this.ipRegex = address;
        } else {
            if (arr.length <= 1) {
                logger.error("invalidate ip address {}", arr);
                return;
            }

            if (arr.length == 2) {
                if (!arr[1].equals("*") || !this.validate(RANGE_PATTERN, arr[0])) {
                    logger.error("invalidate ip address {}", arr);
                    return;
                }

                this.ipRegex = arr[0] + "\\." + "([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])" + "\\." + "([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])" + "\\." + "([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])";
            } else if (arr.length == 3) {
                if (!arr[2].equals("*") || !this.validate(RANGE_PATTERN, arr[0]) || !this.validate(RANGE_PATTERN, arr[1])) {
                    logger.error("invalidate ip address {}", arr);
                    return;
                }

                this.ipRegex = arr[0] + "\\." + arr[1] + "\\." + "([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])" + "\\." + "([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])";
            } else if (arr.length == 4) {
                if (arr[3].equals("*") && this.validate(RANGE_PATTERN, arr[0]) && this.validate(RANGE_PATTERN, arr[1]) && this.validate(RANGE_PATTERN, arr[2])) {
                    this.ipRegex = arr[0] + "\\." + arr[1] + "\\." + arr[2] + "\\." + "([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])";
                } else {
                    this.ipRegex = arr[0] + "\\." + arr[1] + "\\." + arr[2] + "\\." + arr[3];
                }
            }
        }

        this.IPv4Pattern = Pattern.compile(this.ipRegex);
    }

    private boolean validate(Pattern p, String str) {
        Matcher matcher = p.matcher(str);
        return matcher.matches();
    }

    public boolean validate(String str) {
        return this.IPv4Pattern != null && !str.isEmpty() && str != null ? this.validate(this.IPv4Pattern, str) : false;
    }
}
