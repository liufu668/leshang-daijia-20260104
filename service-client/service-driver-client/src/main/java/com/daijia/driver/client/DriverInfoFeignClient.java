package com.daijia.driver.client;

import com.daijia.common.result.Result;
import com.daijia.model.vo.driver.DriverLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-driver")
public interface DriverInfoFeignClient {

    @GetMapping("/driver/info/login/{code}")
    public Result<String> login(@PathVariable String code);

    @GetMapping("/driver/info/getDriverLoginInfo/{wxOpenId}")
    public Result<DriverLoginVo> getDriverLoginInfo(@PathVariable String wxOpenId);
}
