package com.daijia.driver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daijia.driver.mapper.DriverAccountDetailMapper;
import com.daijia.driver.mapper.DriverAccountMapper;
import com.daijia.driver.service.DriverAccountService;
import com.daijia.model.entity.driver.DriverAccount;
import com.daijia.model.entity.driver.DriverAccountDetail;
import com.daijia.model.form.driver.TransferForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverAccountServiceImpl extends ServiceImpl<DriverAccountMapper, DriverAccount> implements DriverAccountService {

    private final DriverAccountMapper driverAccountMapper;
    private final DriverAccountDetailMapper driverAccountDetailMapper;

    @Override
    public Boolean transfer(TransferForm transferForm) {
        //1 去重
        LambdaQueryWrapper<DriverAccountDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverAccountDetail::getTradeNo,transferForm.getTradeNo());
        Long count = driverAccountDetailMapper.selectCount(wrapper);
        if(count > 0) {
            return true;
        }

        //2 添加奖励到司机账户表
        //driverAccountMapper.add(transferForm.getDriverId(),transferForm.getAmount());

        //3 添加交易记录
        DriverAccountDetail driverAccountDetail = new DriverAccountDetail();
        BeanUtils.copyProperties(transferForm,driverAccountDetail);
        driverAccountDetailMapper.insert(driverAccountDetail);

        return true;
    }
}
