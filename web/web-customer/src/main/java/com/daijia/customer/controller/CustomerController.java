package com.daijia.customer.controller;

import com.daijia.common.result.Result;
import com.daijia.customer.service.CustomerService;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.customer.UpdateWxPhoneVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerInfoService;

    @Operation(summary = "获取客户登录信息")
    @GetMapping("getCustomerLoginInfo")
    public Result<CustomerLoginVo> getCustomerLoginInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long customerId = Long.valueOf(authentication.getName());
        CustomerLoginVo customerLoginVo = customerInfoService.getCustomerLoginInfo(customerId);
        return Result.ok(customerLoginVo);
    }

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> wxLogin(@PathVariable String code) {
        log.info("前端返回的登录码,code: {}" + code);
        return Result.ok(customerInfoService.login(code));
    }

    @Operation(summary = "更新用户微信手机号")
    @PostMapping("/updateWxPhone")
    public Result updateWxPhone(@RequestBody UpdateWxPhoneVo updateWxPhoneVo) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long customerId = Long.valueOf(authentication.getName());
        updateWxPhoneVo.setCustomerId(customerId);
        return Result.ok(customerInfoService.updateWxPhoneNumber(updateWxPhoneVo));
    }
}
