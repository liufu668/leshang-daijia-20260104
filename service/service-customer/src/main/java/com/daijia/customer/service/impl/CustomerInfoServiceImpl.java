package com.daijia.customer.service.impl;


import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daijia.customer.mapper.CustomerInfoMapper;
import com.daijia.customer.mapper.CustomerLoginLogMapper;
import com.daijia.model.entity.customer.CustomerInfo;
import com.daijia.model.entity.customer.CustomerLoginLog;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.customer.UpdateWxPhoneVo;
import com.daijia.customer.service.CustomerInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Random;


@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {

    private final CustomerLoginLogMapper customerLoginLogMapper;
    private final WxMaService wxMaService;
    private final CustomerInfoMapper customerInfoMapper;

    // 根据openid到数据库查找用户
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public Long login(String code){
        // 使用微信工具包对象,通过 code 获取微信唯一标识 openId
        String openId = null;
        try {
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            openId = sessionInfo.getOpenid();
            log.info("[小程序授权] openId={}", openId);
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }

        // 根据 openId 查询数据库表
        // 如果 openId 不存在则返回 null, 如果存在则返回一条记录
        CustomerInfo customerInfo = this.getOne(new LambdaQueryWrapper<CustomerInfo>().eq(CustomerInfo::getWxOpenId, openId));

        Date now = new Date();

        // 用户不存在,就创建新用户
        if(null == customerInfo){
            customerInfo = new CustomerInfo();
            customerInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            customerInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            customerInfo.setWxOpenId(openId);

            customerInfo.setCreateTime(now);
            customerInfo.setUpdateTime(now);
            this.save(customerInfo);
        }
        // 登录日志
        CustomerLoginLog customerLoginLog = new CustomerLoginLog();
        customerLoginLog.setCustomerId(customerInfo.getId());
        customerLoginLog.setMsg("小程序登录");
        // 登录日志一旦创建后不会再修改,所以只设置 createTime,不设置 updateTime
        customerLoginLog.setCreateTime(now);
        customerLoginLogMapper.insert(customerLoginLog);

        return customerInfo.getId();
    }

    @Override
    public CustomerLoginVo getCustomerInfo(Long id) {
        CustomerInfo customerInfo = customerInfoMapper.selectById(id);
        // 封装到 CustomerLoginVo
        CustomerLoginVo customerLoginVo = new CustomerLoginVo();
        // 复制属性
        BeanUtils.copyProperties(customerInfo, customerLoginVo);

        // 判断是否绑定手机号码
        String phone = customerInfo.getPhone();
        boolean isBindPhone = StringUtils.hasText(phone); // 检查手机号是否为空
        customerLoginVo.setIsBindPhone(isBindPhone); // 设置是否绑定手机号
        return customerLoginVo;
    }

    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneVo updateWxPhoneVo) {
        // 根据code值获取微信绑定手机号码
        //WxMaPhoneNumberInfo phoneNoInfo = wxMaService.getUserService().getPhoneNoInfo(updateWxPhoneVo.getCode());
        //String phone = phoneNoInfo.getPhoneNumber(); // 获取手机号

        // 生成模拟手机号: 138 + 随机 8 位数字
        Random random = new Random();
        String randomNum = String.format("%08d", random.nextInt(10000000, 99999999));
        String phone = "138" + randomNum;

        // 更新数据库
        CustomerInfo customerInfo = customerInfoMapper.selectById(updateWxPhoneVo.getCustomerId());
        customerInfo.setPhone(phone);
        customerInfo.setUpdateTime(new Date());
        customerInfoMapper.updateById(customerInfo);

        log.info("绑定手机号成功,phone: {}", phone);

        return true; // 返回更新成功
    }

    // 调用微信支付的API的时候需要分别提供司机和乘客的OpenId
    @Override
    public String getCustomerOpenId(Long customerId) {
        LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerInfo::getId,customerId);
        CustomerInfo customerInfo = customerInfoMapper.selectOne(wrapper);
        return customerInfo.getWxOpenId();
    }

}
