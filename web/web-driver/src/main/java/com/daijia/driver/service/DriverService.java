package com.daijia.driver.service;

import com.daijia.model.entity.form.driver.DriverFaceModelForm;
import com.daijia.model.entity.form.driver.UpdateDriverAuthInfoForm;
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

}
