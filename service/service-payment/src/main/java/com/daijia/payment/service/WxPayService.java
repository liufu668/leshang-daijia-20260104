package com.daijia.payment.service;

import com.daijia.model.form.payment.PaymentInfoForm;
import com.daijia.model.vo.payment.WxPrepayVo;
import jakarta.servlet.http.HttpServletRequest;

public interface WxPayService {

    WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm);

    Boolean queryPayStatus(String orderNo);

    void wxnotify(HttpServletRequest request);

    //支付成功后续处理
    void handleOrder(String orderNo);
}
