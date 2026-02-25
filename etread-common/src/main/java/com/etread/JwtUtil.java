package com.etread;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

public class JwtUtil {

    // 绝密印章的材质：必须是足够长、足够乱的字符串
    // 妈妈给你准备了一个超长字符串，保证安全！
    private static final String SECRET_STR = "EtreadCloudLibrarySuperSecretKeyForJwtAuthentication2026_YourMomLoveYou!";

    // 生成一个符合 HS256 算法要求的秘钥对象
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET_STR.getBytes());

    // 令牌有效期：比如 24 小时（单位是毫秒）
    private static final long EXPIRATION = 24 * 60 * 60 * 1000L;

    /**
     * 【重要方法】打印一张通行证（生成 Token）
     * @param userId 用户的 ID
     * @return 一串长长的、加密过的字符串
     */
    public static String createToken(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString()) // 把用户 ID 塞进去
                .setIssuedAt(new Date())       // 签发时间：现在
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION)) // 到期时间
                .signWith(KEY, SignatureAlgorithm.HS256) // 用秘钥和算法加密
                .compact();
    }

    /**
     * 【后续要用】验真伪（解析 Token）
     * @param token 通行证字符串
     * @return 里面存的用户 ID
     */
    public static Long parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }
}