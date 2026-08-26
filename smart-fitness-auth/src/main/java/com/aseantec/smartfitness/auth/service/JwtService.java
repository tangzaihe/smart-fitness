package com.aseantec.smartfitness.auth.service;

import com.aseantec.smartfitness.auth.config.JwtProperties;
import com.aseantec.smartfitness.common.context.CurrentUser;
import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties properties;

    public String createAccessToken(Long userId, Long athleteId, String audience) {
        return build(userId, athleteId, audience, "access", properties.getAccessTtlSeconds());
    }

    public String createRefreshToken(Long userId, Long athleteId, String audience) {
        return build(userId, athleteId, audience, "refresh", properties.getRefreshTtlSeconds());
    }

    public CurrentUser parse(String token, boolean admin) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key(admin))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"access".equals(claims.get("typ", String.class))) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "not an access token");
            }
            Long userId = Long.valueOf(claims.getSubject());
            Object athlete = claims.get("athleteId");
            return CurrentUser.builder()
                    .userId(userId)
                    .athleteId(athlete == null ? null : Long.valueOf(athlete.toString()))
                    .audience(readAudience(claims))
                    .tokenId(claims.getId())
                    .admin(admin)
                    .build();
        } catch (ExpiredJwtException ex) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        } catch (BizException ex) {
            throw ex;
        } catch (JwtException ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "invalid token");
        } catch (Exception ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "invalid token");
        }
    }

    public CurrentUser parseRefresh(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key(false)).build()
                    .parseSignedClaims(token).getPayload();
            if (!"refresh".equals(claims.get("typ", String.class))) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "not a refresh token");
            }
            Object athlete = claims.get("athleteId");
            return CurrentUser.builder()
                    .userId(Long.valueOf(claims.getSubject()))
                    .athleteId(athlete == null ? null : Long.valueOf(athlete.toString()))
                    .audience("app")
                    .tokenId(claims.getId())
                    .admin(false)
                    .build();
        } catch (ExpiredJwtException ex) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    public long accessTtlSeconds() {
        return properties.getAccessTtlSeconds();
    }

    private String build(Long userId, Long athleteId, String audience, String typ, long ttl) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("typ", typ)
                .audience().add(audience).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttl)))
                .signWith(key("admin".equals(audience)));
        if (athleteId != null) {
            builder.claim("athleteId", athleteId.toString());
        }
        return builder.compact();
    }

    private SecretKey key(boolean admin) {
        String secret = admin ? properties.getAdminSecret() : properties.getSecret();
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private static String readAudience(Claims claims) {
        Object aud = claims.get("aud");
        if (aud == null) {
            return null;
        }
        if (aud instanceof String value) {
            return value;
        }
        if (aud instanceof Collection<?> values && !values.isEmpty()) {
            return String.valueOf(values.iterator().next());
        }
        return aud.toString();
    }
}
