package com.daijia.driver.service.impl;


import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.Result;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.driver.client.DriverInfoFeignClient;
import com.daijia.driver.service.DriverService;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.driver.DriverLoginVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverInfoFeignClient driverInfoFeignClient;

    @Override
    public String login(String code) {
        Result<String> loginResult = driverInfoFeignClient.login(code);

        Integer codeResult = loginResult.getCode();
        if(codeResult != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        String token = loginResult.getData();

        return token;
    }

    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        // 根据用户ID进行远程调用,返回用户信息
        Result<DriverLoginVo> driverLoginVoResult = driverInfoFeignClient.getDriverLoginInfo(driverId);

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
}
