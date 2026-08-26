package com.aseantec.smartfitness.common.constant;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class RedisKeys {
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private RedisKeys() {
    }

    public static String loginFail(String account) {
        return "auth:login:fail:" + account.toLowerCase();
    }

    public static String jwtBlacklist(String jti) {
        return "auth:jwt:blacklist:" + jti;
    }

    public static String systemUsage(Long athleteId, LocalDate date) {
        return "usage:sys:" + athleteId + ":" + date.format(DAY);
    }

    public static String rpm(Long athleteId, String minuteBucket) {
        return "usage:rpm:" + athleteId + ":" + minuteBucket;
    }
}
