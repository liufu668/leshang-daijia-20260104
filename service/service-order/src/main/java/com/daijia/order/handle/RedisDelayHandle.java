package com.daijia.order.handle;

import com.daijia.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

//监听延迟队列
@Component
@RequiredArgsConstructor
public class RedisDelayHandle {

    private final RedissonClient redissonClient;
    private final OrderInfoService orderInfoService;

    @PostConstruct //  项目一启动就开始监听
    public void listener() {
        new Thread(()->{ // 后台监听线程
            while(true) { //  死循环一直监听，永不停止
                //获取延迟队列里面阻塞队列
                RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue("queue_cancel");

                //从队列获取消息
                try {
                    String orderId = blockingQueue.take();

                    //取消订单
                    if(StringUtils.hasText(orderId)) {
                        //调用方法取消订单
                        orderInfoService.orderCancel(Long.parseLong(orderId));
                    }

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        }).start();
    }
}
