package com.aseantec.smartfitness.common.context;

import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;

public final class UserContext {
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static CurrentUser require() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long requireAthleteId() {
        CurrentUser user = require();
        if (user.getAthleteId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user.getAthleteId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
