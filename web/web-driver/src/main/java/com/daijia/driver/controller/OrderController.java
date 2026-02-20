package com.daijia.driver.controller;

import com.daijia.common.result.Result;
import com.daijia.driver.service.OrderService;
import com.daijia.model.vo.order.CurrentOrderInfoVo;
import com.daijia.model.vo.order.NewOrderDataVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "查询订单状态")
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<Integer> getOrderStatus(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderStatus(orderId));
    }

    @Operation(summary = "查询司机新订单数据")
    @GetMapping("/findNewOrderQueueData")
    public Result<List<NewOrderDataVo>> findNewOrderQueueData() {
        Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        return Result.ok(orderService.findNewOrderQueueData(driverId));
    }


    @Operation(summary = "司机端查找当前订单")
    @GetMapping("/searchDriverCurrentOrder")
    public Result<CurrentOrderInfoVo> searchDriverCurrentOrder() {
        Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        return Result.ok(orderService.searchDriverCurrentOrder(driverId));
    }

    @Operation(summary = "司机抢单")
    @GetMapping("/robNewOrder/{orderId}")
    public Result<Boolean> robNewOrder(@PathVariable Long orderId) {
        Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        return Result.ok(orderService.robNewOrder(driverId, orderId));
    }

}
