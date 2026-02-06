package com.daijia.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daijia.common.constant.SystemConstant;
import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.mapper.*;
import com.daijia.model.entity.customer.CustomerInfo;
import com.daijia.model.entity.customer.CustomerLoginLog;
import com.daijia.model.entity.driver.DriverAccount;
import com.daijia.model.entity.driver.DriverInfo;
import com.daijia.model.entity.driver.DriverLoginLog;
import com.daijia.model.entity.driver.DriverSet;
import com.daijia.model.entity.form.driver.UpdateDriverAuthInfoForm;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.driver.DriverAuthInfoVo;
import com.daijia.model.vo.driver.DriverLoginVo;
import com.daijia.service.CosService;
import com.daijia.service.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverServiceImpl extends ServiceImpl<DriverInfoMapper, DriverInfo> implements DriverService {

    private final DriverInfoMapper driverInfoMapper;
    private final DriverLoginLogMapper driverLoginLogMapper;
    private final DriverAccountMapper  driverAccountMapper;
    private final DriverFaceRecognitionMapper faceRecognitionMapper;
    private final DriverSetMapper driverSetMapper;
    private final WxMaService wxMaService;
    private final CosService cosService;


    @Override
    public Long login(String code) {
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
        if(driverInfo == null){
            // 添加司机基本信息
            driverInfo = new DriverInfo();
            driverInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            driverInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            driverInfo.setWxOpenId(openId);

            driverInfo.setCreateTime(now);
            driverInfo.setUpdateTime(now);
            driverInfoMapper.insert(driverInfo);

            // 初始化司机设置
            DriverSet driverSet = new DriverSet();
            driverSet.setDriverId(driverInfo.getId());
            driverSet.setOrderDistance(new BigDecimal(0));//0：无限制
            driverSet.setAcceptDistance(new BigDecimal(SystemConstant.ACCEPT_DISTANCE));//默认接单范围：5公里
            driverSet.setIsAutoAccept(0);//0：否 1：是
            driverSetMapper.insert(driverSet);

            //初始化司机账户信息
            DriverAccount driverAccount = new DriverAccount();
            driverAccount.setDriverId(driverInfo.getId());
            driverAccountMapper.insert(driverAccount);
        }

        // 登录日志
        DriverLoginLog driverLoginLog = new DriverLoginLog();
        driverLoginLog.setDriverId(driverInfo.getId());
        driverLoginLog.setMsg("小程序登录");
        // 登录日志一旦创建后不会再修改,所以只设置 createTime,不设置 updateTime
        driverLoginLog.setCreateTime(now);
        driverLoginLogMapper.insert(driverLoginLog);

        return driverInfo.getId();
    }

    @Override
    public DriverLoginVo getDriverLoginInfo(Long id) {
        DriverInfo driverInfo = driverInfoMapper.selectById(id);

        DriverLoginVo driverLoginVo = new DriverLoginVo();
        // 复制属性
        BeanUtils.copyProperties(driverInfo, driverLoginVo);

        // 是否建档人脸识别, 判断司机是否已录入人脸模型（用于后续人脸识别）。
        String faceModelId = driverInfo.getFaceModelId();
        boolean isFaceModel = StringUtils.hasText(faceModelId);
        driverLoginVo.setIsArchiveFace(isFaceModel);

        return driverLoginVo;
    }

    /**
     * 获取司机的认证信息
     *
     * 司机登录成功后，会判断司机是否认证，如果未认证调到认证页面，认证页面为修改与查看页面，未认证通过可反复提交认证，因此我们要先查看认证信息。
     * 进入认证页面我们要回显证件信息，因此要申请临时访问路径。
     */
    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long id) {
        DriverInfo driverInfo = driverInfoMapper.selectById(id);
        DriverAuthInfoVo driverAuthInfoVo = new DriverAuthInfoVo();
        BeanUtils.copyProperties(driverInfo, driverAuthInfoVo);

        driverAuthInfoVo.setIdcardBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardBackUrl()));
        driverAuthInfoVo.setIdcardFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardFrontUrl()));
        driverAuthInfoVo.setIdcardHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardHandUrl()));
        driverAuthInfoVo.setDriverLicenseFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseFrontUrl()));
        driverAuthInfoVo.setDriverLicenseBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseBackUrl()));
        driverAuthInfoVo.setDriverLicenseHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseHandUrl()));

        return driverAuthInfoVo;

    }

    //更新司机认证信息
    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        //获取司机id
        Long driverId = updateDriverAuthInfoForm.getDriverId();

        //修改操作
        DriverInfo driverInfo = new DriverInfo();
        driverInfo.setId(driverId);
        BeanUtils.copyProperties(updateDriverAuthInfoForm,driverInfo);

        //        int i = driverInfoMapper.updateById(driverInfo);
        boolean update = this.updateById(driverInfo);
        return update;
    }

}
