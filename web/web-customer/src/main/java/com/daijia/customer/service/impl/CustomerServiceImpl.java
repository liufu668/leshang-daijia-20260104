package com.daijia.customer.service.impl;

import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.Result;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.customer.client.CustomerInfoFeignClient;
import com.daijia.customer.service.CustomerService;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.customer.UpdateWxPhoneVo;
import com.daijia.security.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerInfoFeignClient client;
    private final RedisTemplate redisTemplate;
    private final CustomerInfoFeignClient customerInfoFeignClient;
    private final TokenService tokenService;

    @Override
    public String login(String code) {

        Result<String> loginResult = client.login(code);

        Integer codeResult = loginResult.getCode();
        if(codeResult != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        String token = loginResult.getData();

        return token;
    }

    @Override
    public CustomerLoginVo getCustomerLoginInfo(String token) {

        return null;
    }

    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {

        Result<CustomerLoginVo> customerLoginVoResult = customerInfoFeignClient.getCustomerLoginInfo(customerId);

        Integer code = customerLoginVoResult.getCode();
        if(code != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        CustomerLoginVo customerLoginVo = customerLoginVoResult.getData();
        if(customerLoginVo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return customerLoginVo;
    }

    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneVo updateWxPhoneVo) {
        Result<Boolean> booleanResult = customerInfoFeignClient.updateWxPhoneNumber(updateWxPhoneVo);
        return true;
    }
}
