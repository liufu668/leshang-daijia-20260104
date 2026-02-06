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
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerInfoFeignClient customerInfoFeignClient;
    private final TokenService tokenService;

    @Override
    public String login(String code) {
        Result<Long> loginResult = customerInfoFeignClient.login(code);

        Long id = loginResult.getData();

        String token = tokenService.createToken(id);

        return token;
    }

    @Override
    public CustomerLoginVo getCustomerLoginInfo(Long id) {

        // 根据用户ID进行远程调用,返回用户信息
        Result<CustomerLoginVo> customerLoginVoResult = customerInfoFeignClient.getCustomerLoginInfo(id);

        Integer code = customerLoginVoResult.getCode();
        if(code != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        CustomerLoginVo customerLoginVo = customerLoginVoResult.getData();
        if(customerLoginVo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 返回用户信息
        return customerLoginVo;
    }

    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneVo updateWxPhoneVo) {
        Result<Boolean> updateWxPhoneNumberResult = customerInfoFeignClient.updateWxPhoneNumber(updateWxPhoneVo);
        return true;
    }
}
