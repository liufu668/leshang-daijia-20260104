package com.daijia.coupon.client;

import com.daijia.common.result.Result;
import com.daijia.model.form.coupon.UseCouponForm;
import com.daijia.model.vo.base.PageVo;
import com.daijia.model.vo.coupon.AvailableCouponVo;
import com.daijia.model.vo.coupon.NoReceiveCouponVo;
import com.daijia.model.vo.coupon.NoUseCouponVo;
import com.daijia.model.vo.coupon.UsedCouponVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
public class CouponFeignFallback implements CouponFeignClient {

    @Override
    public Result<PageVo<NoReceiveCouponVo>> findNoReceivePage(Long customerId, Long page, Long limit) {
        log.error("【降级】优惠券服务不可用，findNoReceivePage 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<PageVo<NoUseCouponVo>> findNoUsePage(Long customerId, Long page, Long limit) {
        log.error("【降级】优惠券服务不可用，findNoUsePage 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<Boolean> receive(Long customerId, Long couponId) {
        log.error("【降级】优惠券服务不可用，receive 调用失败");
        return Result.fail(false);
    }

    @Override
    public Result<List<AvailableCouponVo>> findAvailableCoupon(Long customerId, BigDecimal orderAmount) {
        log.error("【降级】优惠券服务不可用，findAvailableCoupon 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<BigDecimal> useCoupon(UseCouponForm useCouponForm) {
        log.error("【降级】优惠券服务不可用，useCoupon 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<BigDecimal> preOccupyCoupon(UseCouponForm useCouponForm) {
        log.error("【降级】优惠券服务不可用，preOccupyCoupon 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<Boolean> releasePreOccupyCoupon(Long customerId, Long orderId) {
        log.error("【降级】优惠券服务不可用，releasePreOccupyCoupon 调用失败");
        return Result.fail(false);
    }

    @Override
    public Result<PageVo<UsedCouponVo>> findUsedPage(Long customerId, Long page, Long limit) {
        log.error("【降级】优惠券服务不可用，findUsedPage 调用失败");
        return Result.fail(null);
    }
}
