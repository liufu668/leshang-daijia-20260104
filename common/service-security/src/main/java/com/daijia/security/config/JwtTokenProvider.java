package com.daijia.security.config;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

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
     */
    public String generateToken(Long id) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpirationHours() * 60 * 60 * 1000);

        String token =  Jwts.builder()
                .setSubject(String.valueOf(id))
                .setId(UUID.randomUUID().toString())  // 添加唯一ID
                .setIssuedAt(now)                       // 签发时间
                //.setExpiration(expiryDate)              // 过期时间
                .setExpiration(null)                        // 永不过期
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 签名
                .compact();

        log.info("生成新Token: {}", token);
        return token;
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
     * 从 Token 中获取 ID
     * @param token
     * @return
     */
    public Long getIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }

    ///**
    // * 获取 Token 过期时间
    // * @param token
    // * @return
    // */
    //public Date getExpirationDateFromToken(String token) {
    //    Claims claims = Jwts.parserBuilder()
    //            .setSigningKey(getSigningKey())
    //            .build()
    //            .parseClaimsJws(token)
    //            .getBody();
    //    return claims.getExpiration();
    //}
}
