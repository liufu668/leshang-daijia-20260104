package com.daijia.driver.service.impl;

import com.daijia.dispatch.client.NewOrderFeignClient;
import com.daijia.driver.service.OrderService;
import com.daijia.model.vo.order.CurrentOrderInfoVo;
import com.daijia.model.vo.order.NewOrderDataVo;
import com.daijia.order.client.OrderInfoFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderInfoFeignClient orderInfoFeignClient;
    private final NewOrderFeignClient newOrderFeignClient;

    @Override
    public Integer getOrderStatus(Long orderId) {
        return orderInfoFeignClient.getOrderStatus(orderId).getData();
    }

    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        return newOrderFeignClient.findNewOrderQueueData(driverId).getData();
    }

    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        return orderInfoFeignClient.searchDriverCurrentOrder(driverId).getData();
    }


    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        return orderInfoFeignClient.robNewOrder(driverId,orderId).getData();
    }

}
