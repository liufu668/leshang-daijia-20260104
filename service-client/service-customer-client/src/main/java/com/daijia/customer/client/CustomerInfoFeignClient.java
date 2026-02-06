package com.daijia.customer.client;

import com.daijia.common.result.Result;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.customer.UpdateWxPhoneVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "service-customer")
public interface CustomerInfoFeignClient {

    @GetMapping("/customer/info/login/{code}")
    public Result<Long> login(@PathVariable String code);

    @GetMapping("/customer/info/getCustomerLoginInfo/{id}")
    Result<CustomerLoginVo> getCustomerLoginInfo(@PathVariable Long id);

    @PostMapping("/customer/info/updateWxPhoneNumber")
    Result<Boolean> updateWxPhoneNumber(@RequestBody UpdateWxPhoneVo updateWxPhoneVo);
}