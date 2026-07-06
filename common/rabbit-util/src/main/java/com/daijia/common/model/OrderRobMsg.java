package com.daijia.common.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderRobMsg implements Serializable {
    private Long orderId;
    private Long driverId;
    // 死信队列里的消息重复消费的最大次数
    private Integer retryCount;

}
