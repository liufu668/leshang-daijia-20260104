package com.daijia.customer.controller;

import com.daijia.common.result.Result;
import com.daijia.customer.service.OrderService;
import com.daijia.model.form.customer.ExpectOrderForm;
import com.daijia.model.form.customer.SubmitOrderForm;
import com.daijia.model.form.map.CalculateDrivingLineForm;
import com.daijia.model.vo.customer.ExpectOrderVo;
import com.daijia.model.vo.map.DrivingLineVo;
import com.daijia.model.vo.order.CurrentOrderInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "乘客端查找当前订单")
    @GetMapping("/searchCustomerCurrentOrder")
    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long customerId = Long.valueOf(authentication.getName());
        return Result.ok(orderService.searchCustomerCurrentOrder(customerId));
    }

    @Operation(summary = "计算最佳驾驶线路")
    @PostMapping("/calculateDrivingLine")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
        return Result.ok(orderService.calculateDrivingLine(calculateDrivingLineForm));
    }

    @Operation(summary = "预估订单数据")
    @PostMapping("/expectOrder")
    public Result<ExpectOrderVo> expectOrder(@RequestBody ExpectOrderForm expectOrderForm) {
        return Result.ok(orderService.expectOrder(expectOrderForm));
    }

    @Operation(summary = "乘客下单")
    @PostMapping("/submitOrder")
    public Result<Long> submitOrder(@RequestBody SubmitOrderForm submitOrderForm) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long customerId = Long.valueOf(authentication.getName());
        submitOrderForm.setCustomerId(customerId);
        return Result.ok(orderService.submitOrder(submitOrderForm));
    }

    @Operation(summary = "查询订单状态")
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<Integer> getOrderStatus(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderStatus(orderId));
    }
}
