package com.example.daijia;

import com.example.daijia.common.JwtTokenProvider;
import com.example.daijia.model.Customer;
import com.example.daijia.repository.CustomerRepository;
import com.example.daijia.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class TokenServiceTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private CustomerRepository customerRepository;

    /**
     * JWT篡改测试
     */
    @Test
    void testTamperedToken() {
        // 1. 生成合法Token
        String originalToken = jwtTokenProvider.generateToken("test_123");

        // 2. 尝试篡改（修改中间部分）
        String[] parts = originalToken.split("\\.");
        String tamperedPayload = parts[1] + "tampered";
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        // 3. 验证应该失败
        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
        // 2026-01-06T18:07:18.344+08:00  WARN 7576 --- [daijia-parent] [           main] c.e.daijia.common.JwtTokenProvider       : JWT Token无效: JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted.
    }

    /**
     * Token过期测试
     * @throws InterruptedException
     */
    @Test
    void testExpiredToken() throws InterruptedException {
        // 这个测试需要修改JWT配置，让Token1秒过期
        // 1. 修改application-test.yml
        // jwt.expiration-hours: 0.000000278（约1秒）

        // 2. 生成Token
        String token = jwtTokenProvider.generateToken("test_expire");

        // 3. 等待2秒
        Thread.sleep(2000);

        // 4. 验证应该过期
        assertFalse(jwtTokenProvider.validateToken(token));
    }

    /**
     * Redis黑名单测试
     */
    @Test
    void testBlacklistedToken() {
        Customer customer = new Customer();
        customer.setWxOpenId("test_openid_111");
        customer.setNickname("测试用户");
        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());
        customer.setGender("1");
        customer.setStatus(1);
        customer.setIsDeleted(0);
        customerRepository.save(customer);

        // 1. 生成Token
        String token = tokenService.createToken(customer);

        // 2. 验证Token有效
        assertTrue(tokenService.validateToken(token));

        // 3. 加入黑名单（退出登录）
        tokenService.logout(token);

        // 4. 再次验证应该失败
        assertFalse(tokenService.validateToken(token));
    }
}
