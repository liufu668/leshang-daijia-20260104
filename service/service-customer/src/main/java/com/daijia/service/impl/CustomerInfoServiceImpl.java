package com.daijia.service.impl;


import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.security.service.TokenService;
import com.daijia.mapper.CustomerInfoMapper;
import com.daijia.mapper.CustomerLoginLogMapper;
import com.daijia.model.entity.customer.CustomerInfo;
import com.daijia.model.entity.customer.CustomerLoginLog;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.customer.UpdateWxPhoneVo;
import com.daijia.service.CustomerInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {

    private final CustomerLoginLogMapper customerLoginLogMapper;
    private final WxMaService wxMaService;
    private final CustomerInfoMapper customerInfoMapper;
    private final TokenService tokenService;


    // 根据openid到数据库查找用户
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public String login(String code){
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
        // 用户不存在,就创建新用户
        if(null == customerInfo){
            customerInfo = new CustomerInfo();
            customerInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            customerInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            customerInfo.setWxOpenId(openId);
            this.save(customerInfo);
        }

        // 登录日志
        CustomerLoginLog customerLoginLog = new CustomerLoginLog();
        customerLoginLog.setCustomerId(customerInfo.getId());
        customerLoginLog.setMsg("小程序登录");
        customerLoginLogMapper.insert(customerLoginLog);

        String token = tokenService.createToken(openId);

        return token;
    }

    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {
        // 根据用户 ID 查询用户信息
        CustomerInfo customerInfo = customerInfoMapper.selectById(customerId);

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
        try {
            //// 生成模拟手机号: 138 + 随机 8 位数字
            //Random random = new Random();
            //String randomNum = String.format("%08d", random.nextInt(10000000, 99999999));
            //String phone = "138" + randomNum;

            // 根据code值获取微信绑定手机号码
            WxMaPhoneNumberInfo phoneNoInfo = wxMaService.getUserService().getPhoneNoInfo(updateWxPhoneVo.getCode());
            String phone = phoneNoInfo.getPhoneNumber(); // 获取手机号

            // 更新数据库
            CustomerInfo customerInfo = customerInfoMapper.selectById(updateWxPhoneVo.getCustomerId());
            customerInfo.setPhone(phone);
            customerInfoMapper.updateById(customerInfo);

            return true; // 返回更新成功
        } catch (WxErrorException e) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR); // 招聘数据错误异常
        }
    }

    @Override
    public String getCustomerWxOpenId(Long customerId) {
        // selectById() 默认会查询所有字段，如果只需要 OpenId，会造成不必要的数据库传输。
        // 而 Lambada 查询只查所需字段,性能更优
        LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerInfo::getId, customerId);
        CustomerInfo customerInfo = customerInfoMapper.selectOne(wrapper);
        if(customerInfo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return customerInfo.getWxOpenId();
    }
}
