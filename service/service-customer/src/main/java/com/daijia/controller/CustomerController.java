package com.daijia.controller;


import com.daijia.common.result.Result;
import com.daijia.model.entity.CustomerInfo;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.customer.UpdateWxPhoneVo;
import com.daijia.security.service.TokenService;
import com.daijia.service.CustomerInfoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Random;

// origins = "*": 允许所有域名/来源访问该接口
// 浏览器会缓存 OPTIONS 预检请求的结果，在 3600 秒（1小时）内不再重复发送预检请求
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/customer-api/customer")
@Slf4j
@RequiredArgsConstructor // 构造器注入
public class CustomerController {

    private final CustomerInfoService customerInfoService;
    private final TokenService tokenService;

    private static final String AUTH_URL = "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={js_code}&grant_type={grant_type}";

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<Long> login(@PathVariable String code) throws IOException {
        return Result.ok(customerInfoService.login(code));
    }

    @Operation(summary = "获取客户登录信息")
    @GetMapping("/getCustomerLoginInfo/{customerId}")
    public Result<CustomerLoginVo> getCustomerLoginInfo(@PathVariable Long customerId) {
        CustomerLoginVo customerLoginVo = customerInfoService.getCustomerInfo(customerId);
        return Result.ok(customerLoginVo);
    }

    @Operation(summary = "更新客户微信手机号码")
    @PostMapping("/updateWxPhone")
    public Result<Boolean> updateWxPhone(@RequestBody UpdateWxPhoneVo updateWxPhoneVo) {
        return Result.ok(customerInfoService.updateWxPhoneNumber(updateWxPhoneVo));
    }

    @Operation(summary = "获取客户OpenId")
    @GetMapping("/getCustomerOpenId/{customerId}")
    public Result<String> getCustomerOpenId(@PathVariable Long customerId) {
        return Result.ok(customerInfoService.getCustomerOpenId(customerId));
    }
}
