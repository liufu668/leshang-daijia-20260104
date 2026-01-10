package com.daijia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.daijia.model.entity.CustomerInfo;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.customer.UpdateWxPhoneVo;
import org.springframework.stereotype.Service;

@Service
public interface CustomerInfoService extends IService<CustomerInfo> {

    // 微信小程序登录接口
    Long login(String code);

    // 获取客户登录信息
    CustomerLoginVo getCustomerInfo(Long customerId);

    // 更新客户微信手机号码
    Boolean updateWxPhoneNumber(UpdateWxPhoneVo updateWxPhoneVo);

    String getCustomerOpenId(Long customerId);
}
