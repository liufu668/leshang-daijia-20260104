package com.daijia.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        SecurityAutoConfiguration.class
})//取消数据源自动配置和security自动配置
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {
        "com.daijia.order.client",
        "com.daijia.customer.client",
        "com.daijia.map.client",
        "com.daijia.rules.client"
})
@ComponentScan(basePackages = {
        "com.daijia.security", //service-security模块
        "com.daijia.common", // service-util模块
        "com.daijia.customer" //当前模块
})
public class WebCustomerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebCustomerApplication.class, args);
    }
}
