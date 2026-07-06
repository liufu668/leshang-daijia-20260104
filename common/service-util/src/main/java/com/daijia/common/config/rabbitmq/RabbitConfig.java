//package com.daijia.common.config.rabbitmq;
//
//import org.springframework.amqp.core.Binding;
//import org.springframework.amqp.core.BindingBuilder;
//import org.springframework.amqp.core.DirectExchange;
//import org.springframework.amqp.support.converter.SimpleMessageConverter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import org.springframework.amqp.core.Queue;
//
//import java.util.Collections;
//
//@Configuration
//public class RabbitConfig {
//    public static final String EXCHANGE = "orderRobExchange";
//    public static final String QUEUE = "orderRobQueue";
//    public static final String ROUTING_KEY = "order.rob";
//
//    @Bean
//    public DirectExchange exchange() {
//        return new DirectExchange(EXCHANGE);
//    }
//
//    /**
//     * 第二个参数 true = 队列持久化（durable=true）
//     * ✅ 队列会被持久化到磁盘
//     * ✅ RabbitMQ 重启后，队列不会消失
//     * ✅ 消息会自动持久化（Spring AMQP 默认消息持久化）
//     * @return
//     */
//    @Bean
//    public Queue queue() {
//        return new Queue(QUEUE, true);
//    }
//
//    @Bean
//    public Binding binding() {
//        return BindingBuilder.bind(queue())
//                .to(exchange())
//                .with(ROUTING_KEY);
//    }
//
//    @Bean
//    public SimpleMessageConverter messageConverter() {
//        SimpleMessageConverter converter = new SimpleMessageConverter();
//        // 加入白名单
//        converter.setAllowedListPatterns(Collections.singletonList("com.daijia.common.model.*"));
//        return converter;
//    }
//}

package com.daijia.common.config.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {
    // ===================== 主队列（抢单）常量 =====================
    public static final String EXCHANGE = "orderRobExchange";
    public static final String QUEUE = "orderRobQueue";
    public static final String ROUTING_KEY = "order.rob";

    // ===================== 死信队列 DLQ 常量（新增）=====================
    /** 抢单死信交换机 */
    public static final String ROB_DLQ_EXCHANGE = "orderRobDlqExchange";
    /** 抢单死信队列（你定时任务里用到的 ROB_DLQ_QUEUE） */
    public static final String ROB_DLQ_QUEUE = "orderRobDlqQueue";
    /** 死信路由键 */
    public static final String ROB_DLQ_ROUTING_KEY = "order.rob.dlq";


    // ---------------- 主交换机（原有不变）----------------
    //用的是 Direct 直连交换机
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    // ---------------- 主队列【重点修改】：绑定死信参数 ----------------
    @Bean
    public Queue queue() {
        Map<String, Object> args = new HashMap<>(4);
        // 绑定死信交换机
        args.put("x-dead-letter-exchange", ROB_DLQ_EXCHANGE);
        // 绑定死信路由键
        args.put("x-dead-letter-routing-key", ROB_DLQ_ROUTING_KEY);
        // 1. 全局TTL：单位 ms，例 10分钟过期
        args.put("x-message-ttl", 10 * 60 * 1000);
        // 2. 队列最大长度：最多存放 2000 条消息，超限头部消息转为死信
        args.put("x-max-length", 2000);
        // 队列持久化 + 携带死信参数
        return new Queue(QUEUE, true, false, false, args);
    }

    // ---------------- 主队列绑定（原有不变）----------------
    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue())
                .to(exchange())
                .with(ROUTING_KEY);
    }

    // ===================== 以下全部为【新增死信相关Bean】=====================
    /** 死信交换机 */
    @Bean
    public DirectExchange dlqExchange() {
        // 持久化、不自动删除
        return new DirectExchange(ROB_DLQ_EXCHANGE, true, false);
    }

    /** 死信队列 */
    @Bean
    public Queue dlqQueue() {
        // 死信队列也做持久化
        return new Queue(ROB_DLQ_QUEUE, true);
    }

    /** 死信队列与死信交换机绑定 */
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqQueue())
                .to(dlqExchange())
                .with(ROB_DLQ_ROUTING_KEY);
    }

    // ---------------- 消息转换器（原有不变）----------------
    @Bean
    public SimpleMessageConverter messageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        converter.setAllowedListPatterns(Collections.singletonList("com.daijia.common.model.*"));
        return converter;
    }
}