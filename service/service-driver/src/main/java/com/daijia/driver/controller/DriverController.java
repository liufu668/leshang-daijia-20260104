package com.daijia.driver.controller;

import com.daijia.common.result.Result;
import com.daijia.model.entity.driver.DriverSet;
import com.daijia.model.form.driver.DriverFaceModelForm;
import com.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.daijia.model.vo.driver.DriverAuthInfoVo;
import com.daijia.model.vo.driver.DriverInfoVo;
import com.daijia.model.vo.driver.DriverLoginVo;
import com.daijia.driver.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    //创建司机人脸模型
    @Operation(summary = "创建司机人脸模型")
    @PostMapping("/creatDriverFaceModel")
    public Result<Boolean> creatDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        Boolean isSuccess = driverService.creatDriverFaceModel(driverFaceModelForm);
        return Result.ok(isSuccess);
    }

    @Operation(summary = "获取司机设置信息")
    @GetMapping("/getDriverSet/{driverId}")
    public Result<DriverSet> getDriverSet(@PathVariable Long driverId) {
        return Result.ok(driverService.getDriverSet(driverId));
    }

    @Operation(summary = "批量获取司机设置信息")
    @GetMapping("/batchGetDriverSet/{driverIds}")
    public Result<Map<Long, DriverSet>> batchGetDriverSet(
            @PathVariable List<Long> driverIds) {
        return Result.ok(driverService.batchGetDriverSet(driverIds));
    }
    @Operation(summary = "判断司机当日是否进行过人脸识别")
    @GetMapping("/isFaceRecognition/{driverId}")
    Result<Boolean> isFaceRecognition(@PathVariable("driverId") Long driverId) {
        return Result.ok(driverService.isFaceRecognition(driverId));
    }

    @Operation(summary = "验证司机人脸")
    @PostMapping("/verifyDriverFace")
    public Result<Boolean> verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        return Result.ok(driverService.verifyDriverFace(driverFaceModelForm));
    }

    //更新接单状态
    @Operation(summary = "更新接单状态")
    @GetMapping("/updateServiceStatus/{driverId}/{status}")
    public Result<Boolean> updateServiceStatus(@PathVariable Long driverId, @PathVariable Integer status) {
        return Result.ok(driverService.updateServiceStatus(driverId, status));
    }

    @Operation(summary = "获取司机基本信息")
    @GetMapping("/getDriverInfo/{driverId}")
    public Result<DriverInfoVo> getDriverInfoOrder(@PathVariable Long driverId) {
        return Result.ok(driverService.getDriverInfoOrder(driverId));
    }

    @Operation(summary = "获取司机OpenId")
    @GetMapping("/getDriverOpenId/{driverId}")
    public Result<String> getDriverOpenId(@PathVariable Long driverId) {
        return Result.ok(driverService.getDriverOpenId(driverId));
    }

}
