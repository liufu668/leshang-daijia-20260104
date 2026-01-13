package com.daijia.customer.controller;

import com.daijia.common.login.GuiguLogin;
import com.daijia.common.result.Result;
import com.daijia.common.util.AuthContextHolder;
import com.daijia.customer.service.CustomerService;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.customer.UpdateWxPhoneVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerInfoService;

    @Operation(summary = "获取客户登录信息")
    @GuiguLogin
    @GetMapping("/getCustomerLoginInfo")
    public Result<CustomerLoginVo> getCustomerLoginInfo() {
        // 从 ThreadLocal 获取用户id
        Long customerId = AuthContextHolder.getUserId();
        CustomerLoginVo customerLoginVo = customerInfoService.getCustomerInfo(customerId);
        return Result.ok(customerLoginVo);
    }

    @Operation(summary = "获取客户登录信息")
    @GetMapping("getCustomerLoginInfo")
    public Result<CustomerLoginVo> getCustomerLoginInfo(@RequestHeader(value = "token") String token) {
        CustomerLoginVo customerLoginVo = customerInfoService.getCustomerLoginInfo(token);

        return Result.ok(customerLoginVo);
    }

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> wxLogin(@PathVariable String code) {
        return Result.ok(customerInfoService.login(code));
    }

    @Operation(summary = "更新用户微信手机号")
    @GuiguLogin
    @PostMapping("/updateWxPhone")
    public Result updateWxPhone(@RequestBody UpdateWxPhoneVo updateWxPhoneVo) {
        updateWxPhoneVo.setCustomerId(AuthContextHolder.getUserId());
        return Result.ok(customerInfoService.updateWxPhoneNumber(updateWxPhoneVo));
    }
}
