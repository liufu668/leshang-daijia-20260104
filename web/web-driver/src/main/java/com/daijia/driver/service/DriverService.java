package com.daijia.driver.service;

import com.daijia.model.vo.driver.DriverLoginVo;

public interface DriverService {

    String login(String code);

    DriverLoginVo getDriverLoginInfo(Long driverId);
}
