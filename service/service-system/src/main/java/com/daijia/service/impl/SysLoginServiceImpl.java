package com.daijia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.mapper.CustomerInfoMapper;
import com.daijia.mapper.DriverInfoMapper;
import com.daijia.model.entity.customer.CustomerInfo;
import com.daijia.model.entity.driver.DriverInfo;
import com.daijia.service.SysLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SysLoginServiceImpl implements SysLoginService {

    private final DriverInfoMapper driverInfoMapper;
    private final CustomerInfoMapper customerInfoMapper;

    @Override
    public Long getDriverIdByWxOpenId(String wxOpenId) {
        LambdaQueryWrapper<DriverInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverInfo::getWxOpenId, wxOpenId);
        DriverInfo driverInfo = driverInfoMapper.selectOne(wrapper);
        if(driverInfo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return driverInfo.getId();
    }

    @Override
    public Long getCustomerIdByWxOpenId(String wxOpenId) {
        LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerInfo::getWxOpenId, wxOpenId);
        CustomerInfo customerInfo = customerInfoMapper.selectOne(wrapper);
        if(customerInfo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return customerInfo.getId();
    }
}
