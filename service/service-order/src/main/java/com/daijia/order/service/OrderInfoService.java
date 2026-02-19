package com.daijia.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.daijia.model.entity.order.OrderInfo;
import com.daijia.model.form.order.OrderInfoForm;
import com.daijia.model.vo.order.CurrentOrderInfoVo;

public interface OrderInfoService  extends IService<OrderInfo> {

    //乘客端查找当前订单
    CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId);

    //乘客下单
    Long saveOrderInfo(OrderInfoForm orderInfoForm);

    //根据订单id获取订单状态
    Integer getOrderStatus(Long orderId);

}
