package com.daijia.security.service;

import com.daijia.security.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String TOKEN_PREFIX = "auth_token:";

    /**
     * 创建Token
     */
    public String createToken(Long id) {
        String token = jwtTokenProvider.generateToken(id);
        return token;
    }

    /**
     * 验证Token是否有效
     * 1. Redis黑名单检查
     * 2. JWT验证
     * @param token
     * @return
     */
    public boolean validateToken(String token) {
        // 检查是否在黑名单
        if(isTokenBlacklisted(token)) {
            log.warn("Token在黑名单中:{}", token);
            return false;
        }

        // JWT验证(签名,过期时间)
        return jwtTokenProvider.validateToken(token);
    }

    /**
     * 检查 Token 是否在黑名单
     * @param token
     * @return
     */
    private boolean isTokenBlacklisted(String token) {
        String blacklistKey = TOKEN_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    /**
     * 退出登录后,将 Token 加入 Redis 黑名单
     * @param token
     */
    public void logout(String token) {
        try {
            blacklistToken(token);
            log.info("用户退出登录,Token已加入黑名单!");
        } catch (Exception e) {
            log.error("退出登录失败!", e);
            throw new RuntimeException("退出登录失败!", e);
        }
    }

    /**
     * 刷新 Token 过期时间
     * @param token
     */
    public String refreshToken(String token) {
        if(!jwtTokenProvider.validateToken(token)) {
            throw new SecurityException("Token无效,无法刷新!");
        }
        // 从旧 Token 中获取用户信息
        Long id = jwtTokenProvider.getIdFromToken(token);
        // 将旧 Token 加入黑名单
        blacklistToken(token);
        // 生成新 Token
        String newToken = jwtTokenProvider.generateToken(id);
        log.info("Token刷新成功,用户ID:{}", id);
        return newToken;
    }

    /**
     * 将 Token 加入 Redis 黑名单
     * @param token
     */
    private void blacklistToken(String token) {
        if(jwtTokenProvider.validateToken(token)) {
            Date expiration = jwtTokenProvider.getExpirationDateFromToken(token);
            long ttl = expiration.getTime() - System.currentTimeMillis();
            if(ttl > 0) {
                String blacklistKey = TOKEN_PREFIX + token;
                redisTemplate.opsForValue().set(blacklistKey, "blacklisted", ttl, TimeUnit.MILLISECONDS);
                log.info("Token加入黑名单,剩余有效期:{}秒", ttl);
            }
        }
        log.info("Token已过期,无需加入黑名单");
    }

    /**
     * 删除 Token (清理用)
     * @param token
     */
    public void deleteToken(String token) {
        String blacklistKey = TOKEN_PREFIX + token;
        redisTemplate.delete(blacklistKey);
        log.info("清除 Token 黑名单记录:{}", token);
    }
}
