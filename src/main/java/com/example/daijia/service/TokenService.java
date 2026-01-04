package com.example.daijia.service;

import com.example.daijia.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CustomerService customerService;

    private static final String TOKEN_PREFIX = "auth_token:";
    private static final long TOKEN_EXPIRE_HOURS = 24;

    /**
     * 创建Token并存储到Redis
     * @param wxOpenId
     * @return
     */
    public String createToken(String wxOpenId) {
        // 生成UUID token
        String token = UUID.randomUUID().toString();
        String key = TOKEN_PREFIX + token;

        // 存储用户信息到Redis
        Customer customer = customerService.loadUserByWxOpenId(wxOpenId);
        redisTemplate.opsForValue().set(key, customer, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);

        return token;
    }

    /**
     * 验证Token是否有效
     * @param token
     * @return
     */
    public boolean validateToken(String token) {
        String key = TOKEN_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 根据 Token 获取用户信息
     * @param token
     * @return
     */
    public Customer getUsernameByToken(String token) {
        String key = TOKEN_PREFIX + token;
        return (Customer) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除 Token
     * @param token
     */
    public void deleteToken(String token) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.delete(key);
    }

    /**
     * 刷新 Token 过期时间
     * @param token
     */
    public void refreshToken(String token) {
        String key = TOKEN_PREFIX + token;
        if(Boolean.TRUE.equals(redisTemplate.hasKey(key))){
            redisTemplate.expire(key, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        }
    }
}
