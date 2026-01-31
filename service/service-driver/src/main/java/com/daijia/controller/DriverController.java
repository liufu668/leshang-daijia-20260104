package com.daijia.controller;

import com.daijia.common.result.Result;
import com.daijia.model.vo.driver.DriverLoginVo;
import com.daijia.service.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/driver/info")
@Slf4j
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping("/login/{code}")
    public Result<String> login(@PathVariable String code){
        return Result.ok(driverService.login(code));
    }

    @GetMapping("/getDriverLoginInfo/{driverId}")
    public Result<DriverLoginVo> getDriverLoginInfo(Long driverId){
        DriverLoginVo driverLoginVo = driverService.getDriverLoginInfo(driverId);
        return Result.ok(driverLoginVo);
    }

    @GetMapping("/getDriverIdByWxOpenId/{wxOpenId}")
    public Result<Long> getDriverIdByWxOpenId(String wxOpenId){
        return Result.ok(driverService.getDriverIdByWxOpenId(wxOpenId));
    }

}
