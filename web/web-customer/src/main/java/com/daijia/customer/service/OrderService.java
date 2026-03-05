package com.daijia.customer.service;

import com.daijia.model.form.customer.ExpectOrderForm;
import com.daijia.model.form.customer.SubmitOrderForm;
import com.daijia.model.form.map.CalculateDrivingLineForm;
import com.daijia.model.form.payment.CreateWxPaymentForm;
import com.daijia.model.vo.base.PageVo;
import com.daijia.model.vo.customer.ExpectOrderVo;
import com.daijia.model.vo.driver.DriverInfoVo;
import com.daijia.model.vo.map.DrivingLineVo;
import com.daijia.model.vo.map.OrderLocationVo;
import com.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.daijia.model.vo.order.CurrentOrderInfoVo;
import com.daijia.model.vo.order.OrderInfoVo;
import com.daijia.model.vo.payment.WxPrepayVo;

public interface OrderService {

    //乘客查找当前订单
    CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId);

    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);

    //预估订单数据
    ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm);

    //乘客下单
    Long submitOrder(SubmitOrderForm submitOrderForm);

    //查询订单状态
    Integer getOrderStatus(Long orderId);

    OrderInfoVo getOrderInfo(Long orderId, Long customerId);

    DriverInfoVo getDriverInfo(Long orderId, Long customerId);

    OrderLocationVo getCacheOrderLocation(Long orderId);

    OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId);

    PageVo findCustomerOrderPage(Long customerId, Long page, Long limit);

    WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm);

    Boolean queryPayStatus(String orderNo);

}
