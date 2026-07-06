package com.daijia.payment.client;

import com.daijia.common.result.Result;
import com.daijia.model.form.payment.PaymentInfoForm;
import com.daijia.model.vo.payment.WxPrepayVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WxPayFeignFallback implements WxPayFeignClient {

    @Override
    public Result<WxPrepayVo> createWxPayment(PaymentInfoForm paymentInfoForm) {
        log.error("【降级】支付服务不可用，createWxPayment 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<Boolean> queryPayStatus(String orderNo) {
        log.error("【降级】支付服务不可用，queryPayStatus 调用失败, orderNo:{}", orderNo);
        return Result.fail(false);
    }
}
