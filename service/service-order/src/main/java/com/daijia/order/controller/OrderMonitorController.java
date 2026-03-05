package com.daijia.order.controller;

import com.daijia.common.result.Result;
import com.daijia.model.entity.order.OrderMonitor;
import com.daijia.model.entity.order.OrderMonitorRecord;
import com.daijia.order.service.OrderMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order/monitor")
@RequiredArgsConstructor
public class OrderMonitorController {

    private final OrderMonitorService orderMonitorService;

    @Operation(summary = "保存订单监控记录数据")
    @PostMapping("/saveOrderMonitorRecord")
    public Result<Boolean> saveMonitorRecord(@RequestBody OrderMonitorRecord orderMonitorRecord) {
        return Result.ok(orderMonitorService.saveOrderMonitorRecord(orderMonitorRecord));
    }

    @Operation(summary = "根据订单id获取订单监控信息")
    @GetMapping("/getOrderMonitor/{orderId}")
    public Result<OrderMonitor> getOrderMonitor(@PathVariable Long orderId) {
        return Result.ok(orderMonitorService.getOrderMonitor(orderId));
    }

    @Operation(summary = "更新订单监控信息")
    @PostMapping("/updateOrderMonitor")
    public Result<Boolean> updateOrderMonitor(@RequestBody OrderMonitor OrderMonitor) {
        return Result.ok(orderMonitorService.updateOrderMonitor(OrderMonitor));
    }


}

