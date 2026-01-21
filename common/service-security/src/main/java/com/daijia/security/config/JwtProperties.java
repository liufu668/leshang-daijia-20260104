package com.daijia.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "default-value";

    private Long expirationHours = 24L;

    // 工具方法:获取毫秒数
    //public long getExpirationMillis() {
    //    //return 1000; // jwt token 1000毫秒过期
    //    return expirationHours * 60 * 60 * 1000; //jwt token 24小时后过期
    //}
}
