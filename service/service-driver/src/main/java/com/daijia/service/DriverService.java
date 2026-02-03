package com.daijia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.daijia.model.entity.driver.DriverInfo;
import com.daijia.model.vo.driver.DriverLoginVo;

public interface DriverService extends IService<DriverInfo> {
    String login(String code);

    DriverLoginVo getDriverLoginInfo(String wxOpenId);

}
