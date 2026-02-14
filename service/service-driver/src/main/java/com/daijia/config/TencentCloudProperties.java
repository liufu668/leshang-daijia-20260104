package com.daijia.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "tencent.cloud")
public class TencentCloudProperties {

    private String secretId;

    private String secretKey;

    private String region;

    private String bucketPrivate;

    private String personGroupId;

    public TencentCloudProperties() {
        // 读取环境变量
        this.secretId = System.getenv("TENCENT_SECRETID");
        this.secretKey = System.getenv("TENCENT_SECRETKEY");
        this.region = "ap-nanjing";
        this.bucketPrivate = System.getenv("TENCENT_BUCKET");
        this.personGroupId = "daijia-driver";
    }
}
