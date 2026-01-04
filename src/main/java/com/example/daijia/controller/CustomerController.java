package com.example.daijia.controller;

import com.example.daijia.common.Result;
import com.example.daijia.dto.UpdateWxPhoneDTO;
import com.example.daijia.model.Customer;
import com.example.daijia.dto.UpdateCustomerInfoDTO;
import com.example.daijia.model.CustomerLoginLog;
import com.example.daijia.repository.CustomerLoginLogRepository;
import com.example.daijia.repository.CustomerRepository;
import com.example.daijia.service.CustomerService;
import com.example.daijia.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// origins = "*": 允许所有域名/来源访问该接口
// 浏览器会缓存 OPTIONS 预检请求的结果，在 3600 秒（1小时）内不再重复发送预检请求
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/customer-api/customer")
@Slf4j
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerLoginLogRepository customerLoginLogRepository;

    @Autowired
    private TokenService tokenService;

    private static final String APPID = "wx663d1d8b0f350e5d";

    private static final String APP_SECRET = "242978d8734d7b325f0d3dae8611b655";

    private static final String AUTH_URL = "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={js_code}&grant_type={grant_type}";

    /**
     * 乘客微信登录
     *
     * 前端调用wx.login后生成微信登录凭证code(5分钟内有效),通过wx.request传回后台换取微信用户openId
     * 前端发送的请求: http://localhost:8600/customer-api/customer/login/0a3tRoml2648Tg4Tj0nl2flotq3tRomt
     */
    @GetMapping("/login/{code}")
    public Result login(@PathVariable String code) throws IOException {

        System.out.println("前端返回的微信登录code: " + code);

        // 装载请求参数,与微信服务器接口通信,获取openid,session_key
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("appid", APPID);
        paramsMap.put("secret", APP_SECRET);
        paramsMap.put("js_code", code);
        paramsMap.put("grant_type", "authorization_code");

        // 发送HTTP GET请求,通过RestTemplate调用微信API
        RestTemplate restTemplate = new RestTemplate();
        // 拿到HTTP请求的response的所有内容
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(AUTH_URL, String.class, paramsMap);
        // 拿到body的内容,里面有openid和session_key
        String jsonResult = responseEntity.getBody();
        // 使用jackson来完成类型转换
        ObjectMapper objectMapper = new ObjectMapper();
        // 获取map类型是Map<String, String>
        MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, String.class);
        // 将json格式的字符串转换为Map
        Map<String, String> resultMap = null;
        try {
            resultMap = objectMapper.readValue(jsonResult, mapType);
        } catch (IOException e) {
            log.error("验证openId的http返回的body内容json转Map转换异常 jsonResult={}", jsonResult);
            throw new IOException("内容json转Map转换异常");
        }

        System.out.println(resultMap);
        String openId = resultMap.get("openid");
        String sessionKey = resultMap.get("session_key");

        // 根据openid检测用户是否存在,如果不存在,就创建用户
        UserDetails userDetails = customerService.loadUserByWxOpenId(openId);

        // 记录登录日志到 customer_login_log 表
        CustomerLoginLog customerLoginLog = new CustomerLoginLog();
        customerLoginLog.setCreateTime(LocalDateTime.now());
        customerLoginLog.setUpdateTime(LocalDateTime.now());
        customerLoginLog.setIsDeleted(1);
        customerLoginLog.setCustomerId(userDetails.getUsername());
        customerLoginLogRepository.save(customerLoginLog);

        // 生成 Token, 返回给前端
        String token = tokenService.createToken(sessionKey);
        System.out.println("后端生成的Token: " + token);
        return new Result(200,"success", token);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/getCustomerLoginInfo")
    public Result getCustomerLoginInfo(@RequestHeader("token") String token) {

        System.out.println("前端返回后端进行认证的token: " + token);
        Customer customer = tokenService.getUsernameByToken(token);
        return new Result(200, "success", customer);
    }

    /**
     * 更新用户信息
     */
    @PostMapping("/updateCustomerInfo")
    public Result updateCustomerInfo(@RequestHeader("token") String token, @RequestBody UpdateCustomerInfoDTO request) {
        System.out.println("更新用户信息");
        Customer customer = tokenService.getUsernameByToken(token);
        customer.setNickname(request.getNickname());
        System.out.println("用户昵称: " + customer.getNickname());
        customer.setAvatarUrl(request.getAvatarUrl());
        System.out.println("用户头像: " + customer.getAvatarUrl());
        customerRepository.save(customer);
        return new Result(200, "success", customer);
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
        System.out.println("生成的模拟手机号:" + phone);
        // 更新数据库
        Customer customer = tokenService.getUsernameByToken(token);
        customer.setPhone(phone);
        customerRepository.save(customer);
        return new Result(200, "success", phone);
    }
}
