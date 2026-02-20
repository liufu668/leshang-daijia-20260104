package com.daijia.order.client;

import com.daijia.common.result.Result;
import com.daijia.model.form.order.OrderInfoForm;
import com.daijia.model.vo.order.CurrentOrderInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-order")
public interface OrderInfoFeignClient {

    @GetMapping("/order/info/searchCustomerCurrentOrder/{customerId}")
    Result<CurrentOrderInfoVo> searchCustomerCurrentOrder(@PathVariable("customerId") Long customerId);

    /**
     * 保存订单信息
     * @param orderInfoForm
     * @return
     */
    @PostMapping("/order/info/saveOrderInfo")
    Result<Long> saveOrderInfo(@RequestBody OrderInfoForm orderInfoForm);

    /**
     * 根据订单id获取订单状态
     * @param orderId
     * @return
     */
    @GetMapping("/order/info/getOrderStatus/{orderId}")
    Result<Integer> getOrderStatus(@PathVariable("orderId") Long orderId);

    /**
     * 司机端查找当前订单
     * @param driverId
     * @return
     */
    @GetMapping("/order/info/searchDriverCurrentOrder/{driverId}")
    Result<CurrentOrderInfoVo> searchDriverCurrentOrder(@PathVariable("driverId") Long driverId);

    /**
     * 司机抢单
     * @param driverId
     * @param orderId
     * @return
     */
    @GetMapping("/order/info/robNewOrder/{driverId}/{orderId}")
    Result<Boolean> robNewOrder(@PathVariable("driverId") Long driverId, @PathVariable("orderId") Long orderId);

}
