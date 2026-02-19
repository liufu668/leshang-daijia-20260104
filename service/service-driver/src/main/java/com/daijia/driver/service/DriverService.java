package com.daijia.driver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.daijia.model.entity.driver.DriverInfo;
import com.daijia.model.entity.driver.DriverSet;
import com.daijia.model.form.driver.DriverFaceModelForm;
import com.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.daijia.model.vo.driver.DriverAuthInfoVo;
import com.daijia.model.vo.driver.DriverLoginVo;

public interface DriverService extends IService<DriverInfo> {
    Long login(String code);

    //获取司机登录信息
    DriverLoginVo getDriverLoginInfo(Long id);

    //获取司机认证信息
    DriverAuthInfoVo getDriverAuthInfo(Long id);

    //更新司机认证信息
    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    //创建司机人脸模型
    Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm);

    //获取司机设置信息
    DriverSet getDriverSet(Long driverId);

}
