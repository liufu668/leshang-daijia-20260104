package com.daijia.driver.controller;

import com.daijia.common.result.Result;
import com.daijia.driver.service.OrderService;
import com.daijia.model.form.map.CalculateDrivingLineForm;
import com.daijia.model.form.order.OrderFeeForm;
import com.daijia.model.form.order.StartDriveForm;
import com.daijia.model.form.order.UpdateOrderCartForm;
import com.daijia.model.vo.base.PageVo;
import com.daijia.model.vo.map.DrivingLineVo;
import com.daijia.model.vo.order.CurrentOrderInfoVo;
import com.daijia.model.vo.order.NewOrderDataVo;
import com.daijia.model.vo.order.OrderInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
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

    //@Operation(summary = "司机抢单")
    //@GetMapping("/robNewOrder/{orderId}")
    //public Result<Boolean> robNewOrder(@PathVariable Long orderId) {
    //    Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
    //    return Result.ok(orderService.robNewOrder(driverId, orderId));
    //}

    // 进行压测时绕过登录
    @Operation(summary = "司机抢单")
    @GetMapping("/robNewOrder")
    public Result<Boolean> robNewOrder(@RequestParam Long driverId, @RequestParam Long orderId) {
        //log.info("开始抢单,司机ID: {}, 订单ID: {}", driverId, orderId);
        return Result.ok(orderService.robNewOrder(driverId, orderId));
    }

    @Operation(summary = "获取订单账单详细信息")
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfoVo> getOrderInfo(@PathVariable Long orderId) {
        Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        return Result.ok(orderService.getOrderInfo(orderId, driverId));
    }

    @Operation(summary = "计算最佳驾驶线路")
    @PostMapping("/calculateDrivingLine")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
        return Result.ok(orderService.calculateDrivingLine(calculateDrivingLineForm));
    }

    @Operation(summary = "司机到达代驾起始地点")
    @GetMapping("/driverArriveStartLocation/{orderId}")
    public Result<Boolean> driverArriveStartLocation(@PathVariable Long orderId) {
        Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        return Result.ok(orderService.driverArriveStartLocation(orderId, driverId));
    }

    @Operation(summary = "更新代驾车辆信息")
    @PostMapping("/updateOrderCart")
    public Result<Boolean> updateOrderCart(@RequestBody UpdateOrderCartForm updateOrderCartForm) {
        Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        updateOrderCartForm.setDriverId(driverId);
        return Result.ok(orderService.updateOrderCart(updateOrderCartForm));
    }

    @Operation(summary = "开始代驾服务")
    @PostMapping("/startDrive")
    public Result<Boolean> startDrive(@RequestBody StartDriveForm startDriveForm) {
        Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        startDriveForm.setDriverId(driverId);
        return Result.ok(orderService.startDrive(startDriveForm));
    }

    @Operation(summary = "结束代驾服务更新订单账单")
    @PostMapping("/endDrive")
    public Result<Boolean> endDrive(@RequestBody OrderFeeForm orderFeeForm) {
        Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        orderFeeForm.setDriverId(driverId);
        return Result.ok(orderService.endDrive(orderFeeForm));
    }

    @Operation(summary = "获取司机订单分页列表")
    @GetMapping("findDriverOrderPage/{page}/{limit}")
    public Result<PageVo> findDriverOrderPage(
            @Parameter(name = "page", description = "当前页码", required = true)
            @PathVariable Long page,

            @Parameter(name = "limit", description = "每页记录数", required = true)
            @PathVariable Long limit) {
        Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        PageVo pageVo = orderService.findDriverOrderPage(driverId, page, limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "司机发送账单信息")
    @GetMapping("/sendOrderBillInfo/{orderId}")
    public Result<Boolean> sendOrderBillInfo(@PathVariable Long orderId) {
        Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        return Result.ok(orderService.sendOrderBillInfo(orderId, driverId));
    }
}
