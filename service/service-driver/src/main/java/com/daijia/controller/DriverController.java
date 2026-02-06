package com.daijia.controller;

import com.daijia.common.result.Result;
import com.daijia.model.entity.form.driver.UpdateDriverAuthInfoForm;
import com.daijia.model.vo.driver.DriverAuthInfoVo;
import com.daijia.model.vo.driver.DriverLoginVo;
import com.daijia.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/driver/info")
@Slf4j
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping("/login/{code}")
    public Result<Long> login(@PathVariable String code){
        return Result.ok(driverService.login(code));
    }

    @GetMapping("/getDriverLoginInfo/{id}")
    public Result<DriverLoginVo> getDriverLoginInfo(@PathVariable Long id){
        DriverLoginVo driverLoginVo = driverService.getDriverLoginInfo(id);
        return Result.ok(driverLoginVo);
    }

    @GetMapping("/getDriverAuthInfo/{id}")
    public Result<DriverAuthInfoVo> getDriverAuthInfo(@PathVariable Long id){
        DriverAuthInfoVo driverAuthInfoVo = driverService.getDriverAuthInfo(id);
        return Result.ok(driverAuthInfoVo);
    }

    //更新司机认证信息
    @Operation(summary = "更新司机认证信息")
    @PostMapping("/updateDriverAuthInfo")
    public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        Boolean isSuccess = driverService.updateDriverAuthInfo(updateDriverAuthInfoForm);
        return Result.ok(isSuccess);
    }
}
