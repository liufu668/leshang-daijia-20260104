package com.daijia.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daijia.common.constant.SystemConstant;
import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.driver.config.TencentCloudProperties;
import com.daijia.driver.mapper.*;
import com.daijia.model.entity.driver.*;
import com.daijia.model.form.driver.DriverFaceModelForm;
import com.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.daijia.model.vo.driver.DriverAuthInfoVo;
import com.daijia.model.vo.driver.DriverLoginVo;
import com.daijia.driver.service.CosService;
import com.daijia.driver.service.DriverService;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.iai.v20200303.IaiClient;
import com.tencentcloudapi.iai.v20200303.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.joda.time.DateTime;
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
    private final DriverAccountMapper driverAccountMapper;
    private final DriverSetMapper driverSetMapper;
    private final WxMaService wxMaService;
    private final CosService cosService;
    private final TencentCloudProperties tencentCloudProperties;
    private final DriverFaceRecognitionMapper driverFaceRecognitionMapper;


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

    //创建司机人脸模型
    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        //根据司机id获取司机信息
        DriverInfo driverInfo =
                driverInfoMapper.selectById(driverFaceModelForm.getDriverId());
        try{

            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(tencentCloudProperties.getSecretId(),
                    tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(),
                    clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            CreatePersonRequest req = new CreatePersonRequest();
            //设置相关值
            req.setGroupId(tencentCloudProperties.getPersonGroupId());
            //基本信息
            req.setPersonId(String.valueOf(driverInfo.getId()));
            req.setGender(Long.parseLong(driverInfo.getGender()));
            req.setQualityControl(4L);
            req.setUniquePersonControl(4L);
            req.setPersonName(driverInfo.getName());
            req.setImage(driverFaceModelForm.getImageBase64());

            // 返回的resp是一个CreatePersonResponse的实例，与请求对象对应
            CreatePersonResponse resp = client.CreatePerson(req);
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
            String faceId = resp.getFaceId();
            if(StringUtils.hasText(faceId)) {
                driverInfo.setFaceModelId(faceId);
                driverInfoMapper.updateById(driverInfo);
            }
        } catch (TencentCloudSDKException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //获取司机设置信息
    @Override
    public DriverSet getDriverSet(Long driverId) {
        LambdaQueryWrapper<DriverSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverSet::getDriverId,driverId);
        DriverSet driverSet = driverSetMapper.selectOne(wrapper);
        return driverSet;
    }


    ////判断司机当日是否进行过人脸识别
    @Override
    public Boolean isFaceRecognition(Long driverId) {
        //根据司机id + 当日日期进行查询
        LambdaQueryWrapper<DriverFaceRecognition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverFaceRecognition::getDriverId,driverId);
        // 年-月-日 格式
        wrapper.eq(DriverFaceRecognition::getFaceDate,new DateTime().toString("yyyy-MM-dd"));
        //调用mapper方法
        Long count = driverFaceRecognitionMapper.selectCount(wrapper);

        return count != 0;
    }

    //人脸识别
    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        //1 照片比对
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(tencentCloudProperties.getSecretId(),
                    tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred,
                    tencentCloudProperties.getRegion(),
                    clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            VerifyFaceRequest req = new VerifyFaceRequest();
            //设置相关参数
            req.setImage(driverFaceModelForm.getImageBase64());
            req.setPersonId(String.valueOf(driverFaceModelForm.getDriverId()));

            // 返回的resp是一个VerifyFaceResponse的实例，与请求对象对应
            VerifyFaceResponse resp = client.VerifyFace(req);
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
            if(resp.getIsMatch()) { //照片比对成功
                //2 如果照片比对成功，静态活体检测
                Boolean isSuccess = this.
                        detectLiveFace(driverFaceModelForm.getImageBase64());
                if(isSuccess) {//3 如果静态活体检测通过，添加数据到认证表里面
                    DriverFaceRecognition driverFaceRecognition = new DriverFaceRecognition();
                    driverFaceRecognition.setDriverId(driverFaceModelForm.getDriverId());
                      driverFaceRecognition.setFaceDate(new Date());
                    driverFaceRecognitionMapper.insert(driverFaceRecognition);
                    return true;
                }
            }
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }

        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }

    //人脸静态活体检测
    private Boolean detectLiveFace(String imageBase64) {
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(tencentCloudProperties.getSecretId(),
                    tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(),
                    clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DetectLiveFaceRequest req = new DetectLiveFaceRequest();
            req.setImage(imageBase64);
            // 返回的resp是一个DetectLiveFaceResponse的实例，与请求对象对应
            DetectLiveFaceResponse resp = client.DetectLiveFace(req);
            // 输出json格式的字符串回包
            System.out.println(DetectLiveFaceResponse.toJsonString(resp));
            if(resp.getIsLiveness()) {
                return true;
            }
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }
        return false;
    }

    //更新接单状态
    // update driver_set set status=? where driver_id=?
    @Override
    public Boolean updateServiceStatus(Long driverId, Integer status) {
        LambdaQueryWrapper<DriverSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverSet::getDriverId,driverId);
        DriverSet driverSet = new DriverSet();
        driverSet.setServiceStatus(status);
        driverSetMapper.update(driverSet,wrapper);
        return true;
    }


}
