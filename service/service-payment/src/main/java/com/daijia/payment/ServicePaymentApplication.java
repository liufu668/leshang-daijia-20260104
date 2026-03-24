package com.daijia.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {
        "com.daijia.order.client",
        "com.daijia.driver.client"
})
@ComponentScan(basePackages = {
        "com.daijia.common", // rabbit-util模块
})
public class ServicePaymentApplication {

    public static void main(String[] args) {
        //
        //for(int i = 11; i <= 211; i++) {
        //    System.out.println(i);
        //}

        SpringApplication.run(ServicePaymentApplication.class, args);
    }
}
