package com.example.daijia.common;


import com.example.daijia.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 核心工具类
 * 生成 Token,验证 Token,解析 Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    // 获取签名密钥
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * 生成 Token
     * @param wxOpenId
     * @return
     */
    public String generateToken(String wxOpenId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpirationMillis());

        return Jwts.builder()
                .setSubject(String.valueOf(wxOpenId))
                .setIssuedAt(now)                       // 签发时间
                .setExpiration(expiryDate)              // 过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 签名
                .compact();
    }

    /**
     * 验证 Token 是否有效
     * @param token
     * @return
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT Token无效: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 中获取 wxOpenId
     * @param token
     * @return
     */
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * 获取 Token 过期时间
     * @param token
     * @return
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration();
    }
}
