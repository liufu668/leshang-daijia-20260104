package com.daijia.security.service;


import com.daijia.customer.client.CustomerInfoFeignClient;
import com.daijia.driver.client.DriverInfoFeignClient;
import com.daijia.security.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    //private final CustomerInfoMapper customerInfoMapper;
    private static final String TOKEN_PREFIX = "auth_token:";
    private final CustomerInfoFeignClient customerInfoFeignClient;
    private final DriverInfoFeignClient driverInfoFeignClient;

    /**
     * 创建Token
     */
    public String createToken(String wxOpenId) {
        String token = jwtTokenProvider.generateToken(wxOpenId);
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
     * 根据 Token 从 JWT 解析 wxOpenId,进而获取用户ID
     * @param token
     * @return
     */
    public Long getIdByToken(String token) {
        // 验证 Token
        if(!validateToken(token)) {
            throw new SecurityException("Token无效或已过期");
        }
        String wxOpenId = jwtTokenProvider.getWxOpenIdFromToken(token);
        Long id = customerInfoFeignClient.getCustomerIdByWxOpenId(wxOpenId).getData();

        // 从customer_info表中找不到,就说明要从driver_info表中找
        if(id == null) {
            id  = driverInfoFeignClient.getDriverIdByWxOpenId(wxOpenId).getData();
        }
        log.info("根据 Token 从 JWT 解析 wxOpenId,进而获取用户ID: " + id);
        return id;
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
        String wxOpenId = jwtTokenProvider.getWxOpenIdFromToken(token);
        // 将旧 Token 加入黑名单
        blacklistToken(token);
        // 生成新 Token
        String newToken = jwtTokenProvider.generateToken(wxOpenId);
        log.info("Token刷新成功,用户wxOpenId:{}", wxOpenId);
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
