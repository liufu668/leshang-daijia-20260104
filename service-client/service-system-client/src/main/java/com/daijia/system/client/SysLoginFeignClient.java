package com.daijia.system.client;

import com.daijia.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "service-system")
public interface SysLoginFeignClient {

    @GetMapping("/sysLogin/getIdByWxOpenId/{wxOpenId}")
    public Result<Long> getDriverIdByWxOpenId(String wxOpenId);

    @GetMapping("/sysLogin/getCustomerIdByWxOpenId/{wxOpenId}")
    public Result<Long> getCustomerIdByWxOpenId(String wxOpenId);
}
