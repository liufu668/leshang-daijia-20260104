package com.daijia.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.Result;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.model.entity.customer.CustomerInfo;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.customer.UpdateWxPhoneVo;
import com.daijia.service.CustomerInfoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/customer/info")
@Slf4j
@RequiredArgsConstructor // 构造器注入
public class CustomerController {

    private final CustomerInfoService customerInfoService;

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> login(@PathVariable String code){
        return Result.ok(customerInfoService.login(code));
    }

    @Operation(summary = "获取客户登录信息")
    @GetMapping("/getCustomerLoginInfo/{customerId}")
    public Result<CustomerLoginVo> getCustomerLoginInfo(@PathVariable Long customerId) {
        CustomerLoginVo customerLoginVo = customerInfoService.getCustomerInfo(customerId);
        return Result.ok(customerLoginVo);
    }

    @Operation(summary = "更新客户微信手机号码")
    @PostMapping("/updateWxPhoneNumber")
    public Result<Boolean> updateWxPhone(@RequestBody UpdateWxPhoneVo updateWxPhoneVo) {
        return Result.ok(customerInfoService.updateWxPhoneNumber(updateWxPhoneVo));
    }

    @Operation(summary = "获取客户OpenId")
    @GetMapping("/getCustomerOpenId/{customerId}")
    public Result<String> getCustomerOpenId(@PathVariable Long customerId) {
        return Result.ok(customerInfoService.getCustomerWxOpenId(customerId));
    }

    @Operation(summary = "获取客户ID")
    @GetMapping("/getCustomerIdByWxOpenId/{wxOpenId}")
    public Result<Long> getCustomerIdByWxOpenId(@PathVariable  String wxOpenId) {
        return Result.ok(customerInfoService.getCustomerIdByWxOpenId(wxOpenId));
    }
}
