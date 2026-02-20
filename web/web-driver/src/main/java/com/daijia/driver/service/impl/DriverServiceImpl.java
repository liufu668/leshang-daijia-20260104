package com.daijia.driver.service.impl;


import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.Result;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.dispatch.client.NewOrderFeignClient;
import com.daijia.driver.client.DriverInfoFeignClient;
import com.daijia.driver.service.DriverService;
import com.daijia.map.client.LocationFeignClient;
import com.daijia.model.form.driver.DriverFaceModelForm;
import com.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.daijia.model.vo.driver.DriverAuthInfoVo;
import com.daijia.model.vo.driver.DriverLoginVo;
import com.daijia.security.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverInfoFeignClient driverInfoFeignClient;
    private final TokenService tokenService;
    private final LocationFeignClient locationFeignClient;
    private final NewOrderFeignClient newOrderFeignClient;

    @Override
    public String login(String code) {
        Result<Long> loginResult = driverInfoFeignClient.login(code);

        Long id = loginResult.getData();

        String token = tokenService.createToken(id);

        return token;
    }

    @Override
    public DriverLoginVo getDriverLoginInfo(Long id) {

        // 根据用户ID进行远程调用,返回用户信息
        Result<DriverLoginVo> driverLoginVoResult = driverInfoFeignClient.getDriverLoginInfo(id);

        Integer code = driverLoginVoResult.getCode();
        if(code != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        DriverLoginVo driverLoginVo = driverLoginVoResult.getData();
        if(driverLoginVo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 返回用户信息
        return driverLoginVo;
    }

    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long id) {
        Result<DriverAuthInfoVo> driverAuthInfoVoResult = driverInfoFeignClient.getDriverAuthInfo(id);
        DriverAuthInfoVo driverAuthInfoVo = driverAuthInfoVoResult.getData();
        return driverAuthInfoVo;
    }

    //更新司机认证信息
    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        Result<Boolean> booleanResult = driverInfoFeignClient.UpdateDriverAuthInfo(updateDriverAuthInfoForm);
        Boolean data = booleanResult.getData();
        return data;
    }

    //创建司机人脸模型
    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        Result<Boolean> booleanResult = driverInfoFeignClient.creatDriverFaceModel(driverFaceModelForm);
        return booleanResult.getData();
    }

    //判断是否进行人脸识别
    @Override
    public Boolean isFaceRecognition(Long driverId) {
        return driverInfoFeignClient.isFaceRecognition(driverId).getData();
    }

    //人脸识别
    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        return driverInfoFeignClient.verifyDriverFace(driverFaceModelForm).getData();
    }

    //开始接单服务
    @Override
    public Boolean startService(Long driverId) {
        //1 判断完成认证
        DriverLoginVo driverLoginVo = driverInfoFeignClient.getDriverLoginInfo(driverId).getData();
        if(driverLoginVo.getAuthStatus()!=2) {
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }

        //2 判断当日是否人脸识别
        Boolean isFace = driverInfoFeignClient.isFaceRecognition(driverId).getData();
        if(!isFace) {
            throw new GuiguException(ResultCodeEnum.FACE_ERROR);
        }

        //3 更新订单状态 1 开始接单
        driverInfoFeignClient.updateServiceStatus(driverId,1);

        //4 删除redis司机位置信息
        locationFeignClient.removeDriverLocation(driverId);

        //5 清空司机临时队列数据
        newOrderFeignClient.clearNewOrderQueueData(driverId);
        return true;
    }

    //停止接单服务
    @Override
    public Boolean stopService(Long driverId) {
        //更新司机的接单状态 0
        driverInfoFeignClient.updateServiceStatus(driverId,0);

        //删除司机位置信息
        locationFeignClient.removeDriverLocation(driverId);

        //清空司机临时队列
        newOrderFeignClient.clearNewOrderQueueData(driverId);
        return true;
    }

}
