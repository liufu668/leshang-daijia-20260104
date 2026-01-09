package com.atguigu.daijia.controller;

import com.daijia.daijia.common.Result;
import com.daijia.daijia.dto.UpdateWxPhoneDTO;
import com.daijia.daijia.model.CustomerInfo;
import com.daijia.daijia.dto.UpdateCustomerInfoDTO;
import com.daijia.daijia.repository.CustomerLoginLogRepository;
import com.daijia.daijia.repository.CustomerRepository;
import com.daijia.daijia.service.CustomerInfoService;
import com.daijia.daijia.service.TokenService;
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

    /**
     * 获取用户信息
     */
    @GetMapping("/getCustomerLoginInfo")
    public Result getCustomerLoginInfo(@RequestHeader("token") String token) {
        CustomerInfo customerInfo = tokenService.getCustomerByToken(token);
        return new Result(200, "success", customerInfo);
    }

    /**
     * 更新用户信息
     */
    @PostMapping("/updateCustomerInfo")
    public Result updateCustomerInfo(@RequestHeader("token") String token, @RequestBody UpdateCustomerInfoDTO request) {
        CustomerInfo customerInfo = tokenService.getCustomerByToken(token);
        customerInfo.setNickname(request.getNickname());
        customerInfo.setAvatarUrl(request.getAvatarUrl());
        customerRepository.save(customerInfo);
        return new Result(200, "success", customerInfo);
    }

    /**
     * 更新微信手机号
     */
    @PostMapping("/updateWxPhone")
    public Result updateWxPhone(@RequestHeader("token") String token, @RequestBody UpdateWxPhoneDTO updateWxPhoneDTO) {

        // 生成模拟手机号: 138 + 随机 8 位数字
        Random random = new Random();
        String randomNum = String.format("%08d", random.nextInt(10000000, 99999999));
        String phone = "138" + randomNum;
        // 更新数据库
        CustomerInfo customerInfo = tokenService.getCustomerByToken(token);
        customerInfo.setPhone(phone);
        customerRepository.save(customerInfo);
        return new Result(200, "success", phone);
    }
}
