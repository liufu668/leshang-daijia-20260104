package com.daijia.driver.service;

import com.daijia.model.vo.order.CurrentOrderInfoVo;
import com.daijia.model.vo.order.NewOrderDataVo;

import java.util.List;

public interface OrderService {

    Integer getOrderStatus(Long orderId);

    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);

    CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId);

    Boolean robNewOrder(Long driverId, Long orderId);

}
