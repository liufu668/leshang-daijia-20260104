package com.daijia.driver.service;

import com.daijia.model.form.driver.DriverFaceModelForm;
import com.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.daijia.model.vo.driver.DriverAuthInfoVo;
import com.daijia.model.vo.driver.DriverLoginVo;

public interface DriverService {

    String login(String code);

    DriverLoginVo getDriverLoginInfo(Long id);

    DriverAuthInfoVo getDriverAuthInfo(Long id);

    //更新司机认证信息
    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    //创建司机人脸模型
    Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm);

    //判断司机人脸识别
    Boolean isFaceRecognition(Long driverId);

    //人脸识别
    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);

    //开始接单服务
    Boolean startService(Long driverId);

    //停止接单服务
    Boolean stopService(Long driverId);

}
