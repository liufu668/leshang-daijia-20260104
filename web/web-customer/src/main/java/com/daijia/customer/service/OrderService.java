package com.daijia.customer.service;

import com.daijia.model.form.customer.ExpectOrderForm;
import com.daijia.model.form.customer.SubmitOrderForm;
import com.daijia.model.form.map.CalculateDrivingLineForm;
import com.daijia.model.vo.customer.ExpectOrderVo;
import com.daijia.model.vo.map.DrivingLineVo;
import com.daijia.model.vo.order.CurrentOrderInfoVo;

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

}
