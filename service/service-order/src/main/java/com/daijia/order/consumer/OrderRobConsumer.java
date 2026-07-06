package com.daijia.order.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.daijia.common.config.rabbitmq.RabbitConfig;
import com.daijia.common.exception.GuiguException;
import com.daijia.common.model.OrderRobMsg;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.model.entity.order.OrderInfo;
import com.daijia.model.enums.OrderStatus;
import com.daijia.order.mapper.OrderInfoMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.amqp.core.Message;

import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderRobConsumer {

    private final OrderInfoMapper orderInfoMapper;

    // 定义监听用户消费队列名称
    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void consume(OrderRobMsg msg, Channel channel, Message message) {
        Long orderId = msg.getOrderId();
        Long driverId = msg.getDriverId();
        long deliveryTag = message.getMessageProperties().getDeliveryTag();


        try {
            // 1. 先查version（和抢单时逻辑一致）
            LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(OrderInfo::getId, OrderInfo::getVersion)
                    .eq(OrderInfo::getId, orderId);
            OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);

            if (orderInfo == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 异步更新DB（status + version 乐观锁）
            LambdaUpdateWrapper<OrderInfo> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(OrderInfo::getId, orderId)
                    .eq(OrderInfo::getStatus, OrderStatus.WAITING_ACCEPT.getStatus())
                    .eq(OrderInfo::getVersion, orderInfo.getVersion())
                    .set(OrderInfo::getVersion, orderInfo.getVersion() + 1)
                    .set(OrderInfo::getStatus, OrderStatus.ACCEPTED.getStatus())
                    .set(OrderInfo::getDriverId, driverId)
                    .set(OrderInfo::getAcceptTime, new Date());

            int rows = orderInfoMapper.update(null, updateWrapper);

            if(rows != 1) {
                //抢单失败
                throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
            }

            // 3. 手动ack
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("消费异常", e);
            // 异常重试/死信队列
            try {
                // 第三个参数 false：不重回原队列 → 转入死信队列
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
            }
        }
    }
}
