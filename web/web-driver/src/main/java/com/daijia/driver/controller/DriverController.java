package com.daijia.driver.controller;

import com.daijia.common.result.Result;
import com.daijia.driver.service.DriverService;
import com.daijia.model.form.driver.DriverFaceModelForm;
import com.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.daijia.model.vo.driver.DriverAuthInfoVo;
import com.daijia.model.vo.driver.DriverLoginVo;
import com.daijia.security.config.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


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

    @Operation(summary = "获取司机登录信息")
    @GetMapping("/getDriverLoginInfo")
    public Result<DriverLoginVo> getDriverLoginInfo() {
        Long id = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        log.info("从上下文获取到的司机ID: {}", id);
        DriverLoginVo driverLoginVo = driverService.getDriverLoginInfo(id);
        return Result.ok(driverLoginVo);
    }

    @Operation(summary = "获取司机认证信息")
    @GetMapping("/getDriverAuthInfo")
    public Result<DriverAuthInfoVo> getDriverAuthInfo() {
        Long id = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        return Result.ok(driverService.getDriverAuthInfo(id));
    }

    @Operation(summary = "更新司机认证信息")
    @PostMapping("/updateDriverAuthInfo")
    public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        updateDriverAuthInfoForm.setDriverId(Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName()));
        return Result.ok(driverService.updateDriverAuthInfo(updateDriverAuthInfoForm));
    }
    @Operation(summary = "创建司机人脸模型")
    @PostMapping("/creatDriverFaceModel")
    public Result<Boolean> creatDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        driverFaceModelForm.setDriverId(Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName()));
        return Result.ok(driverService.creatDriverFaceModel(driverFaceModelForm));
    }

    @Operation(summary = "判断司机当日是否进行过人脸识别")
    @GetMapping("/isFaceRecognition")
    Result<Boolean> isFaceRecognition() {
        Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        return Result.ok(driverService.isFaceRecognition(driverId));
    }

    @Operation(summary = "验证司机人脸")
    @PostMapping("/verifyDriverFace")
    public Result<Boolean> verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        driverFaceModelForm.setDriverId(Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName()));
        return Result.ok(driverService.verifyDriverFace(driverFaceModelForm));
    }

    @Operation(summary = "开始接单服务")
    @GetMapping("/startService")
    public Result<Boolean> startService() {
        Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        return Result.ok(driverService.startService(driverId));
    }

    @Operation(summary = "停止接单服务")
    @GetMapping("/stopService")
    public Result<Boolean> stopService() {
        Long driverId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        return Result.ok(driverService.stopService(driverId));
    }

}
