package com.daijia.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WeChatProperties {
    private String appId = "default-value";
    private String appSecret = "default-value";
}
