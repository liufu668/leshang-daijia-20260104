package com.example.daijia;

import com.example.daijia.common.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 微信登录生成JWT
     */
    @Test
    void testGenerateToken() {
        // 1. 生成Token
        String wxOpenId = "test_openid_123";
        String token = jwtTokenProvider.generateToken(wxOpenId);

        // 2. 断言检查
        assertNotNull(token, "Token不能为空");
        assertTrue(token.split("\\.").length == 3, "JWT应该是三段式");

        System.out.println("生成的Token: " + token);
        // 生成的Token: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0X29wZW5pZF8xMjMiLCJpYXQiOjE3Njc2OTM3OTAsImV4cCI6MTc2Nzc4MDE5MH0.1hwU3JR5faZQ0clNb5sxtanXsceyH6WJS-KFEjSLGaw
    }

    /**
     * JWT验证拦截器测试
     */
    @Test
    void testValidateToken() {
        // 1. 生成Token
        String wxOpenId = "test_openid_456";
        String token = jwtTokenProvider.generateToken(wxOpenId);

        // 2. 验证Token
        boolean isValid = jwtTokenProvider.validateToken(token);

        // 3. 断言
        assertTrue(isValid, "合法Token应该验证通过");
    }

    @Test
    void testInvalidToken() {
        // 1. 伪造Token
        String fakeToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
                "fake-signature";

        // 2. 验证应该失败
        boolean isValid = jwtTokenProvider.validateToken(fakeToken);

        // 3. 断言
        assertFalse(isValid, "伪造Token应该验证失败");
        // JWT Token无效: JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted.
    }

    @Test
    void testGetUserIdFromToken() {
        // 1. 生成Token
        String expectedOpenId = "test_openid_789";
        String token = jwtTokenProvider.generateToken(expectedOpenId);

        // 2. 解析Token
        String actualOpenId = jwtTokenProvider.getUserIdFromToken(token);

        // 3. 断言
        assertEquals(expectedOpenId, actualOpenId, "从Token解析出的OpenId应该一致");
    }


}
