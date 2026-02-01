package com.daijia.driver.controller;

import com.daijia.common.result.Result;
import com.daijia.driver.service.DriverService;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.driver.DriverLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;


@Slf4j
@RestController
@RequestMapping("/driver")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Operation(summary = "微信小程序登录")
    @GetMapping("/login/{code}")
    public Result<String> login(@PathVariable String code) {
        log.info("前端返回的登录码,code: {}" + code);
        return Result.ok(driverService.login(code));
    }

    //@Operation(summary = "获取司机登录信息")
    //@GetMapping("/getDriverLoginInfo")
    //public Result<DriverLoginVo> getDriverLoginInfo() {
    //    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    //    Long driverId = Long.valueOf(authentication.getName());
    //    log.info("获取司机登录信息, driverId: {}", driverId);
    //    DriverLoginVo driverLoginVo = driverService.getDriverLoginInfo(driverId);
    //    return Result.ok(driverLoginVo);
    //}
    @Operation(summary = "获取司机登录信息")
    @GetMapping("/getDriverLoginInfo")
    public Result<DriverLoginVo> getDriverLoginInfo(@RequestHeader("token") String token) {
        log.info("获取司机登录信息, 需要验证的token: {}", token);
        DriverLoginVo driverLoginVo = driverService.getDriverLoginInfo(token);
        return Result.ok(driverLoginVo);
    }
    //
    //@PostMapping("/updateDriverAuthInfo")
    //public Result updateDriverAuthInfo(@RequestBody ) {
    //
    //}
    //
    //@GetMapping("/getDriverAuthInfo")
    //public Result<DriverLoginVo> getDriverAuthInfo() {
    //
    //}
    //
    //@PostMapping("/updateUserPhoneByWx")
    //public Result updateUserPhoneByWx(@RequestBody ) {
    //
    //}
    //
    //@PostMapping("/creatDriverFaceModel")
    //public Result creatDriverFaceModel(@RequestBody) {
    //
    //}
    //
    //@PostMapping("/verifyDriverFace")
    //public Result verifyDriverFace(@RequestBody) {
    //
    //}
    //
    //@GetMapping("/isFaceRecognition")
    //public Result<Boolean> isFaceRecognition() {
    //
    //}
}
