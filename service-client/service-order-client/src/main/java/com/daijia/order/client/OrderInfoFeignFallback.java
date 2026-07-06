package com.daijia.order.client;

import com.daijia.common.result.Result;
import com.daijia.model.entity.order.OrderInfo;
import com.daijia.model.form.order.OrderInfoForm;
import com.daijia.model.form.order.StartDriveForm;
import com.daijia.model.form.order.UpdateOrderBillForm;
import com.daijia.model.form.order.UpdateOrderCartForm;
import com.daijia.model.vo.base.PageVo;
import com.daijia.model.vo.order.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
public class OrderInfoFeignFallback implements OrderInfoFeignClient {

    @Override
    public Result<List<OrderInfo>> listUnDispatchOrders(Integer seconds) {
        log.error("【降级】listUnDispatchOrders 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<Boolean> updateDispatchStatus(Long orderId, Integer status) {
        log.error("【降级】updateDispatchStatus 调用失败, orderId:{}", orderId);
        return Result.fail(false);
    }

    @Override
    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder(Long customerId) {
        log.error("【降级】searchCustomerCurrentOrder 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<Long> saveOrderInfo(OrderInfoForm orderInfoForm) {
        log.error("【降级】saveOrderInfo 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<Integer> getOrderStatus(Long orderId) {
        log.error("【降级】getOrderStatus 调用失败, orderId:{}", orderId);
        return Result.fail(null);
    }

    @Override
    public Result<CurrentOrderInfoVo> searchDriverCurrentOrder(Long driverId) {
        log.error("【降级】searchDriverCurrentOrder 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<Boolean> robNewOrder(Long driverId, Long orderId) {
        log.error("【降级】robNewOrder 调用失败, driverId:{}, orderId:{}", driverId, orderId);
        return Result.fail(false);
    }

    @Override
    public Result<OrderInfo> getOrderInfo(Long orderId) {
        log.error("【降级】getOrderInfo 调用失败, orderId:{}", orderId);
        return Result.fail(null);
    }

    @Override
    public Result<OrderBillVo> getOrderBillInfo(Long orderId) {
        log.error("【降级】getOrderBillInfo 调用失败, orderId:{}", orderId);
        return Result.fail(null);
    }

    @Override
    public Result<OrderProfitsharingVo> getOrderProfitsharing(Long orderId) {
        log.error("【降级】getOrderProfitsharing 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<Boolean> driverArriveStartLocation(Long orderId, Long driverId) {
        log.error("【降级】driverArriveStartLocation 调用失败");
        return Result.fail(false);
    }

    @Override
    public Result<Boolean> updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        log.error("【降级】updateOrderCart 调用失败");
        return Result.fail(false);
    }

    @Override
    public Result<Boolean> startDrive(StartDriveForm startDriveForm) {
        log.error("【降级】startDrive 调用失败");
        return Result.fail(false);
    }

    @Override
    public Result<Long> getOrderNumByTime(String startTime, String endTime) {
        log.error("【降级】getOrderNumByTime 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<Boolean> endDrive(UpdateOrderBillForm updateOrderBillForm) {
        log.error("【降级】endDrive 调用失败");
        return Result.fail(false);
    }

    @Override
    public Result<PageVo> findCustomerOrderPage(Long customerId, Long page, Long limit) {
        log.error("【降级】findCustomerOrderPage 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<PageVo> findDriverOrderPage(Long driverId, Long page, Long limit) {
        log.error("【降级】findDriverOrderPage 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<Boolean> sendOrderBillInfo(Long orderId, Long driverId) {
        log.error("【降级】sendOrderBillInfo 调用失败");
        return Result.fail(false);
    }

    @Override
    public Result<OrderPayVo> getOrderPayVo(String orderNo, Long customerId) {
        log.error("【降级】getOrderPayVo 调用失败, orderNo:{}", orderNo);
        return Result.fail(null);
    }

    @Override
    public Result<OrderPayVo> getOrderInfoByOrderNo(String orderNo) {
        log.error("【降级】getOrderInfoByOrderNo 调用失败, orderNo:{}", orderNo);
        return Result.fail(null);
    }

    @Override
    public Result<Boolean> updateOrderPayStatus(String orderNo) {
        log.error("【降级】updateOrderPayStatus 调用失败, orderNo:{}", orderNo);
        return Result.fail(false);
    }

    @Override
    public Result<OrderRewardVo> getOrderRewardFee(String orderNo) {
        log.error("【降级】getOrderRewardFee 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<Boolean> updateCouponAmount(Long orderId, BigDecimal couponAmount) {
        log.error("【降级】updateCouponAmount 调用失败, orderId:{}", orderId);
        return Result.fail(false);
    }

    @Override
    public Result<List<OrderInfo>> listTimeoutUnpaidOrder(Integer minutes) {
        log.error("【降级】listTimeoutUnpaidOrder 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<Boolean> cancelOrder(Long orderId, String remark) {
        log.error("【降级】cancelOrder 调用失败, orderId:{}", orderId);
        return Result.fail(false);
    }
}
