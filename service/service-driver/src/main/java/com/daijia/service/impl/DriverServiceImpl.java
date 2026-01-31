package com.daijia.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.mapper.DriverInfoMapper;
import com.daijia.mapper.DriverLoginLogMapper;
import com.daijia.model.entity.customer.CustomerInfo;
import com.daijia.model.entity.customer.CustomerLoginLog;
import com.daijia.model.entity.driver.DriverInfo;
import com.daijia.model.entity.driver.DriverLoginLog;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.driver.DriverLoginVo;
import com.daijia.security.service.TokenService;
import com.daijia.service.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverServiceImpl extends ServiceImpl<DriverInfoMapper, DriverInfo> implements DriverService {

    private final DriverInfoMapper driverInfoMapper;
    private final DriverLoginLogMapper driverLoginLogMapper;
    private final WxMaService wxMaService;
    private final TokenService tokenService;


    @Override
    public String login(String code) {
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
        DriverInfo driverInfo = this.getOne(new LambdaQueryWrapper<DriverInfo>().eq(DriverInfo::getWxOpenId, openId));

        Date now = new Date();

        // 用户不存在,就创建新用户
        if(null == driverInfo){
            driverInfo = new DriverInfo();
            driverInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            driverInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            driverInfo.setWxOpenId(openId);

            driverInfo.setCreateTime(now);
            driverInfo.setUpdateTime(now);
            this.save(driverInfo);
        }

        // 登录日志
        DriverLoginLog driverLoginLog = new DriverLoginLog();
        driverLoginLog.setDriverId(driverInfo.getId());
        driverLoginLog.setMsg("小程序登录");
        // 登录日志一旦创建后不会再修改,所以只设置 createTime,不设置 updateTime
        driverLoginLog.setCreateTime(now);
        driverLoginLogMapper.insert(driverLoginLog);

        String token = tokenService.createToken(openId);

        return token;
    }

    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        // 根据用户 ID 查询用户信息
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);

        DriverLoginVo driverLoginVo = new DriverLoginVo();
        // 复制属性
        BeanUtils.copyProperties(driverInfo, driverLoginVo);

        return driverLoginVo;
    }

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
}
