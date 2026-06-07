package com.tinyshop.common.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * JWT 工具类
 *
 * @author TinyShop Team
 */
@Slf4j
public class JwtUtil {

    /** 密钥 */
    private static final String SECRET = "TinyShopSecretKey2024";

    /** 过期时间：7天 */
    private static final long EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000;

    /** 签发者 */
    private static final String ISSUER = "TinyShop";

    /**
     * 生成 Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return JWT Token
     */
    public static String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + EXPIRE_TIME);

        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        return JWT.create()
                .withIssuer(ISSUER)
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withIssuedAt(now)
                .withExpiresAt(expire)
                .sign(algorithm);
    }

    /**
     * 验证 Token
     *
     * @param token Token字符串
     * @return DecodedJWT
     */
    public static DecodedJWT verifyToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        return verifier.verify(token);
    }

    /**
     * 从 Token 中获取用户ID
     */
    public static Long getUserId(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("userId").asLong();
    }

    /**
     * 从 Token 中获取用户名
     */
    public static String getUsername(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("username").asString();
    }
}
