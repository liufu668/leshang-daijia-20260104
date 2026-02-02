package com.daijia.service;

public interface SysLoginService {

    Long getDriverIdByWxOpenId(String wxOpenId);

    Long getCustomerIdByWxOpenId(String wxOpenId);

}
