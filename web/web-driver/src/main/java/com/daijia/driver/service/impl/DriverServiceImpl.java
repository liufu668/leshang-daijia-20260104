package com.daijia.driver.service.impl;


import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.Result;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.driver.client.DriverInfoFeignClient;
import com.daijia.driver.service.DriverService;
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
}
