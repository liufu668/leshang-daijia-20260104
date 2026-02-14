package com.daijia.driver.client;

import com.daijia.common.result.Result;
import com.daijia.model.entity.form.driver.DriverFaceModelForm;
import com.daijia.model.entity.form.driver.UpdateDriverAuthInfoForm;
import com.daijia.model.vo.driver.DriverAuthInfoVo;
import com.daijia.model.vo.driver.DriverLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-driver")
public interface DriverInfoFeignClient {

    @GetMapping("/driver/info/login/{code}")
    Result<Long> login(@PathVariable String code);

    @GetMapping("/driver/info/getDriverLoginInfo/{id}")
    Result<DriverLoginVo> getDriverLoginInfo(@PathVariable Long id);

    @GetMapping("/driver/info/getDriverAuthInfo/{id}")
    Result<DriverAuthInfoVo> getDriverAuthInfo(@PathVariable Long id);

    @PostMapping("/driver/info/updateDriverAuthInfo")
    Result<Boolean> UpdateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    @PostMapping("/driver/info/creatDriverFaceModel")
    Result<Boolean> creatDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm);

}
