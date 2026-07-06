package com.daijia.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daijia.common.constant.RedisConstant;
import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.Result;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.coupon.client.CouponFeignClient;
import com.daijia.customer.client.CustomerInfoFeignClient;
import com.daijia.customer.service.OrderService;
import com.daijia.dispatch.client.NewOrderFeignClient;
import com.daijia.driver.client.DriverInfoFeignClient;
import com.daijia.map.client.LocationFeignClient;
import com.daijia.map.client.MapFeignClient;
import com.daijia.model.entity.order.OrderInfo;
import com.daijia.model.entity.order.OrderMonitor;
import com.daijia.model.enums.OrderStatus;
import com.daijia.model.form.coupon.UseCouponForm;
import com.daijia.model.form.customer.ExpectOrderForm;
import com.daijia.model.form.customer.SubmitOrderForm;
import com.daijia.model.form.map.CalculateDrivingLineForm;
import com.daijia.model.form.order.OrderInfoForm;
import com.daijia.model.form.payment.CreateWxPaymentForm;
import com.daijia.model.form.payment.PaymentInfoForm;
import com.daijia.model.form.rules.FeeRuleRequestForm;
import com.daijia.model.vo.base.PageVo;
import com.daijia.model.vo.customer.ExpectOrderVo;
import com.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.daijia.model.vo.driver.DriverInfoVo;
import com.daijia.model.vo.map.DrivingLineVo;
import com.daijia.model.vo.map.OrderLocationVo;
import com.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.daijia.model.vo.order.CurrentOrderInfoVo;
import com.daijia.model.vo.order.OrderBillVo;
import com.daijia.model.vo.order.OrderInfoVo;
import com.daijia.model.vo.order.OrderPayVo;
import com.daijia.model.vo.payment.WxPrepayVo;
import com.daijia.model.vo.rules.FeeRuleResponseVo;
import com.daijia.order.client.OrderInfoFeignClient;
import com.daijia.payment.client.WxPayFeignClient;
import com.daijia.rules.client.FeeRuleFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class OrderServiceImpl implements OrderService {

    private final OrderInfoFeignClient orderInfoFeignClient;
    private final MapFeignClient mapFeignClient;
    private final FeeRuleFeignClient feeRuleFeignClient;
    private final NewOrderFeignClient newOrderFeignClient;
    private final DriverInfoFeignClient driverInfoFeignClient;
    private final LocationFeignClient locationFeignClient;
    private final WxPayFeignClient wxPayFeignClient;
    private final CouponFeignClient couponFeignClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Qualifier("applicationTaskExecutor")
    @Autowired
    private Executor orderExecutor;

    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        return orderInfoFeignClient.searchCustomerCurrentOrder(customerId).getData();
    }

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        return mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();
    }

    ////预估订单数据(优化前)
    //@Override
    //public ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm) {
    //    //获取驾驶线路
    //    CalculateDrivingLineForm calculateDrivingLineForm = new CalculateDrivingLineForm();
    //    BeanUtils.copyProperties(expectOrderForm,calculateDrivingLineForm);
    //    Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
    //    DrivingLineVo drivingLineVo = drivingLineVoResult.getData();
    //
    //    //获取订单费用
    //    FeeRuleRequestForm calculateOrderFeeForm = new FeeRuleRequestForm();
    //    calculateOrderFeeForm.setDistance(drivingLineVo.getDistance());
    //    calculateOrderFeeForm.setStartTime(new Date());
    //    calculateOrderFeeForm.setWaitMinute(0);
    //    Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(calculateOrderFeeForm);
    //    FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();
    //
    //    //封装ExpectOrderVo
    //    ExpectOrderVo expectOrderVo = new ExpectOrderVo();
    //    expectOrderVo.setDrivingLineVo(drivingLineVo);
    //    expectOrderVo.setFeeRuleResponseVo(feeRuleResponseVo);
    //    return expectOrderVo;
    //}

    //// 预估订单数据(优化后)
    //@Override
    //public ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm) {
    //    // 先从缓存取
    //    String cacheKey = RedisConstant.EXPECT_ORDER_CACHE_PREFIX
    //            + expectOrderForm.getStartPointLongitude()
    //            + expectOrderForm.getStartPointLatitude()
    //            + expectOrderForm.getEndPointLongitude()
    //            + expectOrderForm.getEndPointLatitude();
    //
    //    ExpectOrderVo cacheVo = (ExpectOrderVo) redisTemplate.opsForValue().get(cacheKey);
    //    if (cacheVo != null) {
    //        return cacheVo;
    //    }
    //
    //    // 缓存没有，远程计算
    //    CalculateDrivingLineForm calculateDrivingLineForm = new CalculateDrivingLineForm();
    //    BeanUtils.copyProperties(expectOrderForm, calculateDrivingLineForm);
    //    DrivingLineVo drivingLineVo = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();
    //
    //    FeeRuleRequestForm feeForm = new FeeRuleRequestForm();
    //    feeForm.setDistance(drivingLineVo.getDistance());
    //    feeForm.setStartTime(new Date());
    //    feeForm.setWaitMinute(0);
    //    FeeRuleResponseVo feeRuleResponseVo = feeRuleFeignClient.calculateOrderFee(feeForm).getData();
    //
    //    ExpectOrderVo expectOrderVo = new ExpectOrderVo();
    //    expectOrderVo.setDrivingLineVo(drivingLineVo);
    //    expectOrderVo.setFeeRuleResponseVo(feeRuleResponseVo);
    //
    //    // 放入缓存，15分钟有效
    //    redisTemplate.opsForValue().set(cacheKey, expectOrderVo, RedisConstant.CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
    //    return expectOrderVo;
    //}


    // 预估订单数据(优化后)
    @Override
    public ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm) {
        String cacheKey = RedisConstant.EXPECT_ORDER_CACHE_PREFIX
                + expectOrderForm.getStartPointLongitude()
                + expectOrderForm.getStartPointLatitude()
                + expectOrderForm.getEndPointLongitude()
                + expectOrderForm.getEndPointLatitude();

        // 1. 查缓存
        ExpectOrderVo cacheVo = (ExpectOrderVo) redisTemplate.opsForValue().get(cacheKey);
        if (cacheVo != null) {
            // ==============================
            // 真正判断：是不是【空值缓存】
            // ==============================
            if (cacheVo.getDrivingLineVo() == null) {
                throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            }
            return cacheVo;
        }

        // 2. 轻锁防击穿
        String lockKey = "lock:expect:" + cacheKey;
        Boolean lockSuccess = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 3, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(lockSuccess)) {
            throw new GuiguException(ResultCodeEnum.SERVICE_BUSY);
        }

        try {
            // 3. 双检
            cacheVo = (ExpectOrderVo) redisTemplate.opsForValue().get(cacheKey);
            if (cacheVo != null) {
                if (cacheVo.getDrivingLineVo() == null) {
                    throw new GuiguException(ResultCodeEnum.DATA_ERROR);
                }
                return cacheVo;
            }

            // 4. 真实调用
            CalculateDrivingLineForm lineForm = new CalculateDrivingLineForm();
            BeanUtils.copyProperties(expectOrderForm, lineForm);
            DrivingLineVo drivingLineVo = mapFeignClient.calculateDrivingLine(lineForm).getData();

            FeeRuleResponseVo feeRuleVo = null;
            if (drivingLineVo != null) {
                FeeRuleRequestForm feeForm = new FeeRuleRequestForm();
                feeForm.setDistance(drivingLineVo.getDistance());
                feeForm.setStartTime(new Date());
                feeForm.setWaitMinute(0);
                feeRuleVo = feeRuleFeignClient.calculateOrderFee(feeForm).getData();
            }

            ExpectOrderVo resultVo = new ExpectOrderVo();
            resultVo.setDrivingLineVo(drivingLineVo);
            resultVo.setFeeRuleResponseVo(feeRuleVo);

            // ==========================================
            // 关键：查不到 → 缓存空对象（真正防穿透）
            // ==========================================
            if (drivingLineVo == null || feeRuleVo == null) {
                redisTemplate.opsForValue().set(cacheKey, resultVo, 2, TimeUnit.MINUTES);
                throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            }

            // 正常数据：随机过期，防雪崩
            long baseExpire = RedisConstant.CACHE_EXPIRE_MINUTES;
            long randomOffset = ThreadLocalRandom.current().nextInt(-1, 4);
            redisTemplate.opsForValue().set(cacheKey, resultVo, baseExpire + randomOffset, TimeUnit.MINUTES);

            return resultVo;

        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    ////乘客下单(优化前)
    //@Override
    //public Long submitOrder(SubmitOrderForm submitOrderForm) {
    //    //1 重新计算驾驶线路
    //    CalculateDrivingLineForm calculateDrivingLineForm = new CalculateDrivingLineForm();
    //    BeanUtils.copyProperties(submitOrderForm,calculateDrivingLineForm);
    //    Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
    //    DrivingLineVo drivingLineVo = drivingLineVoResult.getData();
    //
    //    // 只要是远程调用、数据库查询返回的对象，全部都要判空！
    //    if(drivingLineVo == null) {
    //        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    //    }
    //
    //    //2 重新计算订单费用
    //    FeeRuleRequestForm calculateOrderFeeForm = new FeeRuleRequestForm();
    //    calculateOrderFeeForm.setDistance(drivingLineVo.getDistance());
    //    calculateOrderFeeForm.setStartTime(new Date());
    //    calculateOrderFeeForm.setWaitMinute(0);
    //    Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(calculateOrderFeeForm);
    //    FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();
    //
    //    if(feeRuleResponseVo == null) {
    //        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    //    }
    //
    //    //封装数据
    //    OrderInfoForm orderInfoForm = new OrderInfoForm();
    //    BeanUtils.copyProperties(submitOrderForm,orderInfoForm);
    //    orderInfoForm.setExpectDistance(drivingLineVo.getDistance());
    //    orderInfoForm.setExpectAmount(feeRuleResponseVo.getTotalAmount());
    //    Result<Long> orderInfoResult = orderInfoFeignClient.saveOrderInfo(orderInfoForm);
    //    Long orderId = orderInfoResult.getData();
    //
    //    if(orderId == null) {
    //        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    //    }
    //
    //    //任务调度：查询附近可以接单司机
    //    NewOrderTaskVo newOrderDispatchVo = new NewOrderTaskVo();
    //    newOrderDispatchVo.setOrderId(orderId);
    //    newOrderDispatchVo.setStartLocation(orderInfoForm.getStartLocation());
    //    newOrderDispatchVo.setStartPointLongitude(orderInfoForm.getStartPointLongitude());
    //    newOrderDispatchVo.setStartPointLatitude(orderInfoForm.getStartPointLatitude());
    //    newOrderDispatchVo.setEndLocation(orderInfoForm.getEndLocation());
    //    newOrderDispatchVo.setEndPointLongitude(orderInfoForm.getEndPointLongitude());
    //    newOrderDispatchVo.setEndPointLatitude(orderInfoForm.getEndPointLatitude());
    //    newOrderDispatchVo.setExpectAmount(orderInfoForm.getExpectAmount());
    //    newOrderDispatchVo.setExpectDistance(orderInfoForm.getExpectDistance());
    //    newOrderDispatchVo.setExpectTime(drivingLineVo.getDuration());
    //    newOrderDispatchVo.setFavourFee(orderInfoForm.getFavourFee());
    //    newOrderDispatchVo.setCreateTime(new Date());
    //    //远程调用
    //    Long jobId = newOrderFeignClient.addAndStartTask(newOrderDispatchVo).getData();
    //
    //    if(jobId == null) {
    //        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    //    }
    //
    //    log.info("订单id为： {}，绑定任务id为：{}", orderId, jobId);
    //    //返回订单id
    //    return orderId;
    //}

    //// 乘客下单接口优化后
    //@Override
    //public Long submitOrder(SubmitOrderForm submitOrderForm) {
    //    String cacheKey = RedisConstant.EXPECT_ORDER_CACHE_PREFIX
    //            + submitOrderForm.getStartPointLongitude()
    //            + submitOrderForm.getStartPointLatitude()
    //            + submitOrderForm.getEndPointLongitude()
    //            + submitOrderForm.getEndPointLatitude();
    //
    //    ExpectOrderVo expectOrderVo = (ExpectOrderVo) redisTemplate.opsForValue().get(cacheKey);
    //
    //    DrivingLineVo drivingLineVo;
    //    FeeRuleResponseVo feeRuleResponseVo;
    //
    //    // 缓存过期就重新计算
    //    if (expectOrderVo == null) {
    //        log.info("订单预估缓存已过期，实时重新计算路线和费用");
    //
    //        CalculateDrivingLineForm lineForm = new CalculateDrivingLineForm();
    //        BeanUtils.copyProperties(submitOrderForm, lineForm);
    //        drivingLineVo = mapFeignClient.calculateDrivingLine(lineForm).getData();
    //
    //        FeeRuleRequestForm feeForm = new FeeRuleRequestForm();
    //        feeForm.setDistance(drivingLineVo.getDistance());
    //        feeForm.setStartTime(new Date());
    //        feeForm.setWaitMinute(0);
    //        feeRuleResponseVo = feeRuleFeignClient.calculateOrderFee(feeForm).getData();
    //
    //        // 重新存入缓存
    //        ExpectOrderVo newVo = new ExpectOrderVo();
    //        newVo.setDrivingLineVo(drivingLineVo);
    //        newVo.setFeeRuleResponseVo(feeRuleResponseVo);
    //        redisTemplate.opsForValue().set(cacheKey, newVo, RedisConstant.CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
    //    } else {
    //        drivingLineVo = expectOrderVo.getDrivingLineVo();
    //        feeRuleResponseVo = expectOrderVo.getFeeRuleResponseVo();
    //    }
    //
    //    // ==========================================
    //    // 核心：完全异步，不join、不阻塞，立即返回订单ID
    //    // ==========================================
    //    CompletableFuture.supplyAsync(() -> {
    //        OrderInfoForm orderInfoForm = new OrderInfoForm();
    //        BeanUtils.copyProperties(submitOrderForm, orderInfoForm);
    //        orderInfoForm.setExpectDistance(drivingLineVo.getDistance());
    //        orderInfoForm.setExpectAmount(feeRuleResponseVo.getTotalAmount());
    //        orderInfoForm.setDispatchStatus(RedisConstant.DISPATCH_NOT_START);
    //
    //        Long orderId = orderInfoFeignClient.saveOrderInfo(orderInfoForm).getData();
    //        if (orderId == null) {
    //            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    //        }
    //        return orderId;
    //    }, orderExecutor).thenAccept(orderId -> {
    //        // 订单保存成功 → 异步派单
    //        NewOrderTaskVo taskVo = buildNewOrderTaskVo(orderId, submitOrderForm, drivingLineVo, feeRuleResponseVo);
    //        asyncStartDispatchTask(orderId, taskVo);
    //
    //        // 下单成功 → 删除缓存
    //        redisTemplate.delete(cacheKey);
    //    });
    //
    //    // 立即返回一个临时订单ID（前端轮询状态）
    //    // 这里必须返回，不能没有return！
    //    return -1L;
    //}


    // 乘客下单接口优化后
    @Override
    public Long submitOrder(SubmitOrderForm submitOrderForm) {

        // 1. 构建缓存key（和expectOrder保持一致）
        String cacheKey = RedisConstant.EXPECT_ORDER_CACHE_PREFIX
                + submitOrderForm.getStartPointLongitude()
                + submitOrderForm.getStartPointLatitude()
                + submitOrderForm.getEndPointLongitude()
                + submitOrderForm.getEndPointLatitude();

        // 缓存查询
        ExpectOrderVo expectOrderVo = (ExpectOrderVo) redisTemplate.opsForValue().get(cacheKey);

        // ===================== 解决 Lambda 有效 final 问题 =====================
        // 用一个临时对象包装，避免直接修改基础变量
        class VoHolder {
            DrivingLineVo drivingLine;
            FeeRuleResponseVo feeRule;
        }
        VoHolder holder = new VoHolder();
        // =====================================================================

        // ==========================================
        // 2. 缓存命中：直接赋值
        // ==========================================
        if (expectOrderVo != null) {
            // 判断是否是空值缓存（防穿透）
            if (expectOrderVo.getDrivingLineVo() == null) {
                throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            }
            holder.drivingLine = expectOrderVo.getDrivingLineVo();
            holder.feeRule = expectOrderVo.getFeeRuleResponseVo();
        }

        // ==========================================
        // 3. 缓存未命中：加锁回源
        // ==========================================
        else {
            String lockKey = "lock:submit:" + cacheKey;
            Boolean lockSuccess = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 3, TimeUnit.SECONDS);

            // 抢锁失败直接降级
            if (Boolean.FALSE.equals(lockSuccess)) {
                throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
            }

            try {
                // 双检缓存
                expectOrderVo = (ExpectOrderVo) redisTemplate.opsForValue().get(cacheKey);
                if (expectOrderVo != null) {
                    if (expectOrderVo.getDrivingLineVo() == null) {
                        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
                    }
                    holder.drivingLine = expectOrderVo.getDrivingLineVo();
                    holder.feeRule = expectOrderVo.getFeeRuleResponseVo();
                } else {
                    // 真实调用地图接口
                    CalculateDrivingLineForm lineForm = new CalculateDrivingLineForm();
                    BeanUtils.copyProperties(submitOrderForm, lineForm);
                    DrivingLineVo drivingLineVo = mapFeignClient.calculateDrivingLine(lineForm).getData();

                    FeeRuleResponseVo feeRuleResponseVo = null;
                    if (drivingLineVo != null) {
                        FeeRuleRequestForm feeForm = new FeeRuleRequestForm();
                        feeForm.setDistance(drivingLineVo.getDistance());
                        feeForm.setStartTime(new Date());
                        feeForm.setWaitMinute(0);
                        feeRuleResponseVo = feeRuleFeignClient.calculateOrderFee(feeForm).getData();
                    }

                    // 回源失败 → 缓存空值（防穿透）
                    if (drivingLineVo == null || feeRuleResponseVo == null) {
                        ExpectOrderVo emptyVo = new ExpectOrderVo();
                        emptyVo.setDrivingLineVo(null);
                        emptyVo.setFeeRuleResponseVo(null);
                        redisTemplate.opsForValue().set(cacheKey, emptyVo, 2, TimeUnit.MINUTES);
                        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
                    }

                    // 回源成功 → 写缓存（随机过期防雪崩）
                    ExpectOrderVo newVo = new ExpectOrderVo();
                    newVo.setDrivingLineVo(drivingLineVo);
                    newVo.setFeeRuleResponseVo(feeRuleResponseVo);

                    long baseExpire = RedisConstant.CACHE_EXPIRE_MINUTES;
                    long randomOffset = ThreadLocalRandom.current().nextInt(-1, 4);
                    long finalExpire = baseExpire + randomOffset;

                    redisTemplate.opsForValue().set(cacheKey, newVo, finalExpire, TimeUnit.MINUTES);

                    // 赋值给 holder
                    holder.drivingLine = drivingLineVo;
                    holder.feeRule = feeRuleResponseVo;
                }
            } finally {
                redisTemplate.delete(lockKey);
            }
        }

        // 最终判空
        if (holder.drivingLine == null || holder.feeRule == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        OrderInfoForm orderInfoForm = new OrderInfoForm();
        BeanUtils.copyProperties(submitOrderForm, orderInfoForm);
        orderInfoForm.setExpectDistance(holder.drivingLine.getDistance());
        orderInfoForm.setExpectAmount(holder.feeRule.getTotalAmount());
        orderInfoForm.setDispatchStatus(RedisConstant.DISPATCH_NOT_START);

        // 主线程直接保存订单，返回真实 orderId
        Long orderId = orderInfoFeignClient.saveOrderInfo(orderInfoForm).getData();
        if (orderId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // ===================== 【只异步派单】 =====================
        CompletableFuture.runAsync(() -> {
            try {
                NewOrderTaskVo taskVo = buildNewOrderTaskVo(orderId, submitOrderForm, holder.drivingLine, holder.feeRule);
                asyncStartDispatchTask(orderId, taskVo);
                redisTemplate.delete(cacheKey);
            } catch (Exception e) {
                log.error("异步派单失败 orderId:{}", orderId, e);
            }
        }, orderExecutor);

        // 直接返回真实订单ID，前端不用改
        return orderId;
    }


    // 异步派单
    public void asyncStartDispatchTask(Long orderId, NewOrderTaskVo taskVo) {
        try {
            log.info("异步开启任务调度，orderId:{}", orderId);
            Long jobId = newOrderFeignClient.addAndStartTask(taskVo).getData();

            if (jobId != null) {
                // 调度成功，更新状态
                orderInfoFeignClient.updateDispatchStatus(orderId, RedisConstant.DISPATCH_STARTED);
                log.info("调度成功，orderId:{}, jobId:{}", orderId, jobId);
            }else{
                // 首次派单失败,让补偿任务能立即兜底，而不是等到10秒后
                orderInfoFeignClient.updateDispatchStatus(orderId, RedisConstant.DISPATCH_FAILED);
            }
        } catch (Exception e) {
            orderInfoFeignClient.updateDispatchStatus(orderId, RedisConstant.DISPATCH_FAILED);
            log.error("异步调度任务异常，orderId:{}", orderId, e);
        }
    }

    // ====================== 3. 定时补偿：兜底未调度成功的订单 ======================
    //@Scheduled(fixedRate = 30000) // 每30秒
    //public void compensateUnDispatchOrders() {
    //    log.info("定时补偿任务：开始处理未调度订单");
    //    // 1. 查询 未调度、创建超过10秒、未取消的订单
    //    Result<List<OrderInfo>> result = orderInfoFeignClient.listUnDispatchOrders(10);
    //    List<OrderInfo> unDispatchList = result != null ? result.getData() : null;
    //    if (unDispatchList == null || unDispatchList.isEmpty()) {
    //        log.info("暂无需要补偿的订单");
    //        return;  // ✅ 直接返回，不抛异常
    //    }
    //    for (OrderInfo order : unDispatchList) {
    //        try {
    //            NewOrderTaskVo taskVo = buildTaskFromOrder(order);
    //            Long jobId = newOrderFeignClient.addAndStartTask(taskVo).getData();
    //            if (jobId != null) {
    //                orderInfoFeignClient.updateDispatchStatus(order.getId(), RedisConstant.DISPATCH_STARTED);
    //            }
    //        } catch (Exception e) {
    //            // 记录日志
    //            log.error("补偿调度失败 orderId:{}", order.getId(), e);
    //        }
    //    }
    //}

    // ====================== 3. 定时补偿：兜底未调度成功的订单 ======================
    @Scheduled(fixedRate = 30000) // 每30秒
    public void compensateUnDispatchOrders() {
        log.info("定时补偿任务：开始处理未调度订单");

        int successCount = 0;
        int failCount = 0;
        List<Long> failedOrderIds = new ArrayList<>();

        // 1. 查询 未调度(dispatchStatus=0) 或 调度失败(dispatchStatus=3)、创建超过10秒、未取消的订单
        Result<List<OrderInfo>> result = orderInfoFeignClient.listUnDispatchOrders(10);
        List<OrderInfo> unDispatchList = result != null ? result.getData() : null;
        if (unDispatchList == null || unDispatchList.isEmpty()) {
            log.info("暂无需要补偿的订单");
            return;
        }

        log.info("本次需要补偿的订单数: {}", unDispatchList.size());

        for (OrderInfo order : unDispatchList) {
            try {
                NewOrderTaskVo taskVo = buildTaskFromOrder(order);
                Long jobId = newOrderFeignClient.addAndStartTask(taskVo).getData();
                if (jobId != null) {
                    orderInfoFeignClient.updateDispatchStatus(order.getId(), RedisConstant.DISPATCH_STARTED);
                    successCount++;
                    log.info("补偿调度成功 orderId:{}, jobId:{}", order.getId(), jobId);
                } else {
                    failCount++;
                    failedOrderIds.add(order.getId());
                    orderInfoFeignClient.updateDispatchStatus(order.getId(), RedisConstant.DISPATCH_FAILED);
                    log.warn("补偿调度返回null orderId:{}", order.getId());
                }
            } catch (Exception e) {
                failCount++;
                failedOrderIds.add(order.getId());
                orderInfoFeignClient.updateDispatchStatus(order.getId(), RedisConstant.DISPATCH_FAILED);
                log.error("补偿调度异常 | orderId:{} | customerId:{} | error:{}",
                    order.getId(), order.getCustomerId(), e.getMessage(), e);
            }
        }

        log.info("定时补偿任务完成 | 总数:{} | 成功:{} | 失败:{} | 失败订单ID:{}",
            unDispatchList.size(), successCount, failCount, failedOrderIds);

        if (failCount >= 3) {
            double failRate = (double) failCount / unDispatchList.size();
            if (failRate > 0.5) {
                log.error("【严重告警】补偿任务失败率过高 | 失败率:{}/{} | 失败订单:{}",
                    failCount, unDispatchList.size(), failedOrderIds);
            }
        }
    }

    // 补偿任务调度
    private NewOrderTaskVo buildTaskFromOrder(OrderInfo order) {
        NewOrderTaskVo taskVo = new NewOrderTaskVo();
        taskVo.setOrderId(order.getId());
        taskVo.setStartLocation(order.getStartLocation());
        taskVo.setStartPointLongitude(order.getStartPointLongitude());
        taskVo.setStartPointLatitude(order.getStartPointLatitude());
        taskVo.setEndLocation(order.getEndLocation());
        taskVo.setEndPointLongitude(order.getEndPointLongitude());
        taskVo.setEndPointLatitude(order.getEndPointLatitude());
        taskVo.setExpectAmount(order.getExpectAmount());
        taskVo.setExpectDistance(order.getExpectDistance());
        taskVo.setFavourFee(order.getFavourFee());
        taskVo.setCreateTime(order.getCreateTime());
        return taskVo;
    }

    // ====================== 4. CompletableFuture 并行工具方法 ======================
    private NewOrderTaskVo buildNewOrderTaskVo(Long orderId, SubmitOrderForm form, DrivingLineVo drivingLineVo, FeeRuleResponseVo feeRuleResponseVo) {
        NewOrderTaskVo newOrderDispatchVo = new NewOrderTaskVo();
        newOrderDispatchVo.setOrderId(orderId);
        newOrderDispatchVo.setStartLocation(form.getStartLocation());
        newOrderDispatchVo.setStartPointLongitude(form.getStartPointLongitude());
        newOrderDispatchVo.setStartPointLatitude(form.getStartPointLatitude());
        newOrderDispatchVo.setEndLocation(form.getEndLocation());
        newOrderDispatchVo.setEndPointLongitude(form.getEndPointLongitude());
        newOrderDispatchVo.setEndPointLatitude(form.getEndPointLatitude());
        newOrderDispatchVo.setExpectAmount(feeRuleResponseVo.getTotalAmount());
        newOrderDispatchVo.setExpectDistance(drivingLineVo.getDistance());
        newOrderDispatchVo.setExpectTime(drivingLineVo.getDuration());
        newOrderDispatchVo.setFavourFee(form.getFavourFee());
        newOrderDispatchVo.setCreateTime(new Date());
        return newOrderDispatchVo;
    }

    //查询订单状态
    @Override
    public Integer getOrderStatus(Long orderId) {
        Result<Integer> integerResult = orderInfoFeignClient.getOrderStatus(orderId);
        return integerResult.getData();
    }

    // 查询订单信息
    @Override
    public OrderInfoVo getOrderInfo(Long orderId, Long customerId) {
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
        //判断
        if(orderInfo.getCustomerId() != customerId) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        //获取司机信息
        DriverInfoVo driverInfoVo = null;
        Long driverId = orderInfo.getDriverId();
        if(driverId != null) {
            driverInfoVo = driverInfoFeignClient.getDriverInfo(driverId).getData();
        }

        //获取账单信息
        OrderBillVo orderBillVo = null;
        if(orderInfo.getStatus() >= OrderStatus.UNPAID.getStatus()) {
            orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();
        }

        OrderInfoVo orderInfoVo = new OrderInfoVo();
        orderInfoVo.setOrderId(orderId);
        BeanUtils.copyProperties(orderInfo,orderInfoVo);
        orderInfoVo.setOrderBillVo(orderBillVo);
        orderInfoVo.setDriverInfoVo(driverInfoVo);
        return orderInfoVo;
    }

    // 获取司机信息
    @Override
    public DriverInfoVo getDriverInfo(Long orderId, Long customerId) {
        //根据订单id获取订单信息
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
        if(orderInfo.getCustomerId() != customerId) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        return driverInfoFeignClient.getDriverInfo(orderInfo.getDriverId()).getData();
    }

    // 实时查询司机位置信息(司乘同显)
    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        return locationFeignClient.getCacheOrderLocation(orderId).getData();
    }

    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
        return locationFeignClient.getOrderServiceLastLocation(orderId).getData();
    }

    @Override
    public PageVo findCustomerOrderPage(Long customerId, Long page, Long limit) {
        return orderInfoFeignClient.findCustomerOrderPage(customerId,page,limit).getData();
    }


    //优化前
    //@Override
    //public WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm) {
    //    //获取订单支付信息
    //    OrderPayVo orderPayVo = orderInfoFeignClient.getOrderPayVo(createWxPaymentForm.getOrderNo(),
    //            createWxPaymentForm.getCustomerId()).getData();
    //    //判断
    //    if(orderPayVo.getStatus() != OrderStatus.UNPAID.getStatus()) {
    //        throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
    //    }
    //
    //    //获取乘客和司机openid
    //    String customerOpenId = customerInfoFeignClient.getCustomerOpenId(orderPayVo.getCustomerId()).getData();
    //
    //    String driverOpenId = driverInfoFeignClient.getDriverOpenId(orderPayVo.getDriverId()).getData();
    //
    //    //处理优惠卷
    //    BigDecimal couponAmount = null;
    //    //判断
    //    if (null == orderPayVo.getCouponAmount()
    //            && null != createWxPaymentForm.getCustomerCouponId()
    //            && createWxPaymentForm.getCustomerCouponId() != 0) {
    //        UseCouponForm useCouponForm = new UseCouponForm();
    //        useCouponForm.setOrderId(orderPayVo.getOrderId());
    //        useCouponForm.setCustomerCouponId(createWxPaymentForm.getCustomerCouponId());
    //        useCouponForm.setOrderAmount(orderPayVo.getPayAmount());
    //        useCouponForm.setCustomerId(createWxPaymentForm.getCustomerId());
    //        couponAmount = couponFeignClient.useCoupon(useCouponForm).getData();
    //    }
    //
    //    //更新订单支付金额
    //    //获取支付金额
    //    BigDecimal payAmount = orderPayVo.getPayAmount();
    //    if(couponAmount != null) {
    //        orderInfoFeignClient.updateCouponAmount(orderPayVo.getOrderId(),couponAmount).getData();
    //
    //        //当前支付金额
    //        payAmount = payAmount.subtract(couponAmount);
    //    }
    //
    //    //封装需要数据到实体类，远程调用发起微信支付
    //    PaymentInfoForm paymentInfoForm = new PaymentInfoForm();
    //    paymentInfoForm.setCustomerOpenId(customerOpenId);
    //    paymentInfoForm.setDriverOpenId(driverOpenId);
    //    paymentInfoForm.setOrderNo(orderPayVo.getOrderNo());
    //
    //    paymentInfoForm.setAmount(payAmount);
    //
    //    paymentInfoForm.setContent(orderPayVo.getContent());
    //    paymentInfoForm.setPayWay(1);
    //
    //    WxPrepayVo wxPrepayVo = wxPayFeignClient.createWxPayment(paymentInfoForm).getData();
    //    return wxPrepayVo;
    //}

    // 优化后
    @Override
    public WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm) {
        //获取订单支付信息
        OrderPayVo orderPayVo = orderInfoFeignClient.getOrderPayVo(createWxPaymentForm.getOrderNo(),
                createWxPaymentForm.getCustomerId()).getData();

        if(orderPayVo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        //判断
        if(orderPayVo.getStatus() != OrderStatus.UNPAID.getStatus()) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        //获取乘客和司机openid(个人测试跳过微信支付,而且用的乘客,司机信息查不到对应的openid)
        //String customerOpenId = customerInfoFeignClient.getCustomerOpenId(orderPayVo.getCustomerId()).getData();
        //String driverOpenId = driverInfoFeignClient.getDriverOpenId(orderPayVo.getDriverId()).getData();

        //处理优惠卷
        BigDecimal couponAmount = null;
        //判断
        if (null == orderPayVo.getCouponAmount()
                && null != createWxPaymentForm.getCustomerCouponId()
                && createWxPaymentForm.getCustomerCouponId() != 0) {
            UseCouponForm useCouponForm = new UseCouponForm();
            useCouponForm.setOrderId(orderPayVo.getOrderId());
            useCouponForm.setCustomerCouponId(createWxPaymentForm.getCustomerCouponId());
            useCouponForm.setOrderAmount(orderPayVo.getPayAmount());
            useCouponForm.setCustomerId(createWxPaymentForm.getCustomerId());

            // 预占优惠券
            couponAmount = couponFeignClient.preOccupyCoupon(useCouponForm).getData();

            log.info("使用优惠券后可减免的金额:{}", couponAmount);
        }

        //获取支付金额
        BigDecimal payAmount = orderPayVo.getPayAmount();
        if(couponAmount != null) {
            //当前支付金额(减去优惠的金额)
            payAmount = payAmount.subtract(couponAmount);
            log.info("当前需要支付的金额: {}", payAmount);
        }

        //封装需要数据到实体类，远程调用发起微信支付
        PaymentInfoForm paymentInfoForm = new PaymentInfoForm();
        //paymentInfoForm.setCustomerOpenId(customerOpenId);
        //paymentInfoForm.setDriverOpenId(driverOpenId);
        paymentInfoForm.setOrderNo(orderPayVo.getOrderNo());

        paymentInfoForm.setAmount(payAmount);

        paymentInfoForm.setContent(orderPayVo.getContent());
        paymentInfoForm.setPayWay(1);

        WxPrepayVo wxPrepayVo = wxPayFeignClient.createWxPayment(paymentInfoForm).getData();
        return wxPrepayVo;
    }

    // 优化前
    //// 查询订单支付状态
    //@Override
    //public Boolean queryPayStatus(String orderNo) {
    //
    //    return wxPayFeignClient.queryPayStatus(orderNo).getData();
    //}

    // 优化后
    // 查询订单支付状态
    @Override
    public Boolean queryPayStatus(String orderNo) {
        // 1. 查询微信支付状态
        Boolean paySuccess = wxPayFeignClient.queryPayStatus(orderNo).getData();

        // 只有支付成功，才做后续业务
        if (Boolean.TRUE.equals(paySuccess)) {
            // 2. 根据订单号查询订单信息
            OrderPayVo orderPayVo = orderInfoFeignClient.getOrderInfoByOrderNo(orderNo).getData();
            if (orderPayVo == null) {
                return true;
            }

            // 3. 已经核销过优惠券，不再处理
            if (orderPayVo.getCouponAmount() != null) {
                return true;
            }

            Long customerCouponId = orderPayVo.getCustomerCouponId();
            if (customerCouponId == null || customerCouponId == 0) {
                return true;
            }

            // 4. 组装核销参数
            UseCouponForm useCouponForm = new UseCouponForm();
            useCouponForm.setOrderId(orderPayVo.getOrderId());
            useCouponForm.setCustomerCouponId(customerCouponId);
            useCouponForm.setOrderAmount(orderPayVo.getPayAmount());
            useCouponForm.setCustomerId(orderPayVo.getCustomerId());

            // 5. 真正核销优惠券（只调用一次）
            BigDecimal couponAmount = couponFeignClient.useCoupon(useCouponForm).getData();

            // 6. 更新订单里的优惠券金额
            orderInfoFeignClient.updateCouponAmount(orderPayVo.getOrderId(), couponAmount);
        } else {
            try {
                OrderPayVo orderPayVo = orderInfoFeignClient.getOrderInfoByOrderNo(orderNo).getData();
                if (orderPayVo == null) return false;

                Long customerCouponId = orderPayVo.getCustomerCouponId();
                if (customerCouponId == null || customerCouponId == 0) return false;

                // 支付未成功 → 释放预占优惠券
                log.info("支付失败/超时，释放预占优惠券 orderNo:{}, couponId:{}",
                        orderNo, customerCouponId);

                // 调用释放接口
                couponFeignClient.releasePreOccupyCoupon(
                        orderPayVo.getCustomerId(),
                        orderPayVo.getOrderId()
                );
            } catch (Exception e) {
                log.error("支付失败，释放优惠券异常 orderNo:{}", orderNo, e);
            }
        }


        return paySuccess;
    }

    /**
     * 定时任务：释放【超时未支付订单】的预占优惠券
     * 每 1 分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void releaseTimeoutCouponTask() {
        log.info("=== 定时任务：开始释放超时未支付订单的预占优惠券 ===");

        try {
            // 1. 查询超过15分钟未支付的订单
            Result<List<OrderInfo>> result = orderInfoFeignClient.listTimeoutUnpaidOrder(15);
            List<OrderInfo> orderList = result.getData();
            if (orderList == null || orderList.isEmpty()) {
                log.info("暂无需要释放优惠券的超时订单");
                return;
            }

            // 2. 遍历关单 + 释放优惠券
            for (OrderInfo order : orderList) {
                try {
                    Long orderId = order.getId();
                    Long customerId = order.getCustomerId();

                    log.info("超时未支付订单，自动关单并释放优惠券 orderId:{}", orderId);

                    // ==========================
                    // 关键：订单关单（改成取消状态）
                    // ==========================
                    orderInfoFeignClient.cancelOrder(orderId, "超时未支付，系统自动关单");

                    // ==========================
                    // 优惠券不需要在OrderInfo里存ID！
                    // 我们直接根据 customerId + orderId 去优惠券服务查并释放
                    // ==========================
                    couponFeignClient.releasePreOccupyCoupon(customerId, orderId);

                } catch (Exception e) {
                    log.error("处理超时订单失败 orderId:{}", order.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("定时释放优惠券任务异常", e);
        }
    }
}
