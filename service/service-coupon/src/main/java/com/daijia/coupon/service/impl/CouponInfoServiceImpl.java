package com.daijia.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daijia.common.constant.RedisConstant;
import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.coupon.mapper.CouponInfoMapper;
import com.daijia.coupon.mapper.CustomerCouponMapper;
import com.daijia.coupon.service.CouponInfoService;
import com.daijia.model.entity.coupon.CouponInfo;
import com.daijia.model.entity.coupon.CustomerCoupon;
import com.daijia.model.form.coupon.UseCouponForm;
import com.daijia.model.vo.base.PageVo;
import com.daijia.model.vo.coupon.AvailableCouponVo;
import com.daijia.model.vo.coupon.NoReceiveCouponVo;
import com.daijia.model.vo.coupon.NoUseCouponVo;
import com.daijia.model.vo.coupon.UsedCouponVo;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {

    private final CouponInfoMapper couponInfoMapper;
    private final CustomerCouponMapper customerCouponMapper;
    private final RedissonClient redissonClient;


    // 查询已使用优惠券
    @Override
    public PageVo<UsedCouponVo> findUsedPage(Page<CouponInfo> pageParam, Long customerId) {
        // 调用Mapper的分页方法，传入Page对象
        IPage<UsedCouponVo> pageInfo = couponInfoMapper.findUsedPage(pageParam, customerId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    //查询未领取优惠券
    @Override
    public PageVo<NoReceiveCouponVo> findNoReceivePage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<NoReceiveCouponVo> pageInfo = couponInfoMapper.findNoReceivePage(pageParam, customerId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    //查询未使用优惠券
    @Override
    public PageVo<NoUseCouponVo> findNoUsePage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<NoUseCouponVo> pageInfo = couponInfoMapper.findNoUsePage(pageParam, customerId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    //领取优惠卷
    @Transactional(rollbackFor = Exception.class) //同一微服务、同一数据库内的多表操作, 必须加 @Transactional 本地事务
    @Override
    public Boolean receive(Long customerId, Long couponId) {
        //1 couponId查询优惠卷信息
        //判断如果优惠卷不存在
        CouponInfo couponInfo = couponInfoMapper.selectById(couponId);
        if(couponInfo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        //2 判断优惠卷是否过期
        if(couponInfo.getExpireTime().before(new Date())) {
            throw new GuiguException(ResultCodeEnum.COUPON_EXPIRE);
        }

        //3 检查库存，发行数量 和 领取数量
        if(couponInfo.getPublishCount() != 0 &&
                couponInfo.getReceiveCount() == couponInfo.getPublishCount()) {
            throw new GuiguException(ResultCodeEnum.COUPON_LESS);
        }
        RLock lock = null;
        try {
            lock = redissonClient.getLock(RedisConstant.COUPON_LOCK + customerId);
            boolean flag = lock.tryLock(RedisConstant.COUPON_LOCK_WAIT_TIME,
                    RedisConstant.COUPON_LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if(flag) {
                //4 检查每个人限制领取数量
                if(couponInfo.getPerLimit() > 0) {
                    //统计当前客户已经领取优惠卷数量
                    LambdaQueryWrapper<CustomerCoupon> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(CustomerCoupon::getCouponId,couponId);
                    wrapper.eq(CustomerCoupon::getCustomerId,customerId);
                    Long count = customerCouponMapper.selectCount(wrapper);
                    //判断
                    if(count >= couponInfo.getPerLimit()) {
                        throw new GuiguException(ResultCodeEnum.COUPON_USER_LIMIT);
                    }
                }

                // 修改了 customer_coupon 和 coupon_info 两张表,所以要用事务
                //5 领取优惠卷
                //5.1 更新领取数量
                int row = couponInfoMapper.updateReceiveCount(couponId);

                //5.2 添加领取记录
                this.saveCustomerCoupon(customerId,couponId,couponInfo.getExpireTime());

                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(lock != null) {
                lock.unlock();
            }
        }
        return true;
    }

    //获取未使用的最佳优惠卷信息
    @Override
    public List<AvailableCouponVo> findAvailableCoupon(Long customerId, BigDecimal orderAmount) {

        //1 创建list集合，存储最终返回数据
        List<AvailableCouponVo> availableCouponVoList = new ArrayList<>();

        //2 根据乘客id，获取乘客已经领取但是没有使用的优惠卷列表
        //返回list集合
        List<NoUseCouponVo> list = couponInfoMapper.findNoUseList(customerId);

        //3 遍历乘客未使用优惠卷列表，得到每个优惠卷
        //3.1 判断优惠卷类型：现金卷 和 折扣卷
        List<NoUseCouponVo> typeList =
                list.stream().filter(item -> item.getCouponType() == 1).collect(Collectors.toList());

        //3.2 是现金券
        //判断现金卷是否满足条件
        for(NoUseCouponVo noUseCouponVo:typeList) {
            //判断使用门槛
            //减免金额
            BigDecimal reduceAmount = noUseCouponVo.getAmount();
            //1 没有门槛  == 0，订单金额必须大于优惠减免金额
            if(noUseCouponVo.getConditionAmount().doubleValue()==0
                    && orderAmount.subtract(reduceAmount).doubleValue()>0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo,reduceAmount));
            }

            //2 有门槛  ，订单金额大于优惠门槛金额
            if(noUseCouponVo.getConditionAmount().doubleValue() > 0
                    && orderAmount.subtract(noUseCouponVo.getConditionAmount()).doubleValue()>0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo,reduceAmount));
            }
        }

        //3.3 折扣卷
        //判断折扣卷是否满足条件
        List<NoUseCouponVo> typeList2 =
                list.stream().filter(item -> item.getCouponType() == 2).collect(Collectors.toList());
        for (NoUseCouponVo noUseCouponVo : typeList2) {
            //折扣之后金额
            // 100 打8折  = 100 * 8 /10= 80
            BigDecimal discountAmount = orderAmount.multiply(noUseCouponVo.getDiscount())
                    .divide(new BigDecimal("10")).setScale(2, RoundingMode.HALF_UP);

            BigDecimal reduceAmount = orderAmount.subtract(discountAmount);
            //2.2.1.没门槛
            if (noUseCouponVo.getConditionAmount().doubleValue() == 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
            //2.2.2.有门槛，订单折扣后金额大于优惠券门槛金额
            if (noUseCouponVo.getConditionAmount().doubleValue() > 0
                    && discountAmount.subtract(noUseCouponVo.getConditionAmount()).doubleValue() > 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
        }

        //4 把满足条件优惠卷放到最终list集合
        //根据金额排序
        if (!CollectionUtils.isEmpty(availableCouponVoList)) {
            Collections.sort(availableCouponVoList, new Comparator<AvailableCouponVo>() {
                @Override
                public int compare(AvailableCouponVo o1, AvailableCouponVo o2) {
                    return o1.getReduceAmount().compareTo(o2.getReduceAmount());
                }
            });
        }

        return availableCouponVoList;
    }

    // 预占优惠券
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BigDecimal preOccupyCoupon(UseCouponForm useCouponForm) {
        Long customerCouponId = useCouponForm.getCustomerCouponId();
        Long customerId = useCouponForm.getCustomerId();
        BigDecimal orderAmount = useCouponForm.getOrderAmount();

        // 1. 查询用户优惠券
        CustomerCoupon customerCoupon = customerCouponMapper.selectById(customerCouponId);
        if (customerCoupon == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 2. 归属校验
        if (!customerId.equals(customerCoupon.getCustomerId())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_OPERATION);
        }

        // 3. 状态必须是 未使用(1)
        if (customerCoupon.getStatus() != 1) {
            throw new GuiguException(ResultCodeEnum.COUPON_UNAVAILABLE);
        }

        // 4. 是否过期
        if (customerCoupon.getExpireTime().before(new Date())) {
            throw new GuiguException(ResultCodeEnum.COUPON_EXPIRE);
        }

        // 5. 查询优惠券模板
        CouponInfo couponInfo = couponInfoMapper.selectById(customerCoupon.getCouponId());
        if (couponInfo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 6. 计算优惠金额
        BigDecimal reduceAmount = calculateReduceAmount(couponInfo, orderAmount);
        if (reduceAmount == null || reduceAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new GuiguException(ResultCodeEnum.COUPON_CONDITION_NOT_MATCH);
        }

        // 7. 预占：状态 1 → 2
        CustomerCoupon update = new CustomerCoupon();
        update.setId(customerCouponId);
        update.setStatus(2); // 预占中
        customerCouponMapper.updateById(update);

        return reduceAmount;
    }

    @Override
    public Boolean releasePreOccupyCoupon(Long customerId, Long orderId) {
        // 根据 乘客ID + 订单ID 查询那张被预占的券
        LambdaQueryWrapper<CustomerCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerCoupon::getCustomerId, customerId);
        wrapper.eq(CustomerCoupon::getOrderId, orderId);
        wrapper.eq(CustomerCoupon::getStatus, 2); // 只查预占状态

        CustomerCoupon customerCoupon = customerCouponMapper.selectOne(wrapper);
        if (customerCoupon == null) {
            return true;
        }

        // 释放：预占 → 未使用
        CustomerCoupon update = new CustomerCoupon();
        update.setId(customerCoupon.getId());
        update.setStatus(1);
        update.setOrderId(null);
        return customerCouponMapper.updateById(update) == 1;
    }

    // ==================== 修复：使用优惠券（必须是预占状态） ====================
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BigDecimal useCoupon(UseCouponForm useCouponForm) {
        Long customerCouponId = useCouponForm.getCustomerCouponId();
        Long customerId = useCouponForm.getCustomerId();
        BigDecimal orderAmount = useCouponForm.getOrderAmount();

        // 1. 查询
        CustomerCoupon customerCoupon = customerCouponMapper.selectById(customerCouponId);
        if (customerCoupon == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 2. 归属
        if (!customerCoupon.getCustomerId().equals(customerId)) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_OPERATION);
        }

        // 3. 必须是预占状态(2)
        if (customerCoupon.getStatus() != 2) {
            throw new GuiguException(ResultCodeEnum.COUPON_STATUS_ERROR);
        }

        // 4. 优惠券模板
        CouponInfo couponInfo = couponInfoMapper.selectById(customerCoupon.getCouponId());
        if (couponInfo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 5. 计算优惠金额
        BigDecimal reduceAmount = calculateReduceAmount(couponInfo, orderAmount);
        if (reduceAmount == null || reduceAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new GuiguException(ResultCodeEnum.COUPON_CONDITION_NOT_MATCH);
        }

        // 6. 更新使用次数
        couponInfo.setUseCount(couponInfo.getUseCount() + 1);
        couponInfoMapper.updateById(couponInfo);

        // 7. 更新用户券：已使用
        CustomerCoupon update = new CustomerCoupon();
        update.setId(customerCouponId);
        update.setStatus(3); // 已使用
        update.setUsedTime(new Date());
        update.setOrderId(useCouponForm.getOrderId());
        customerCouponMapper.updateById(update);

        return reduceAmount;
    }

    // ==================== 抽取公共方法：计算优惠金额 ====================
    private BigDecimal calculateReduceAmount(CouponInfo couponInfo, BigDecimal orderAmount) {
        if (couponInfo.getCouponType() == 1) {
            // 现金券
            if (couponInfo.getConditionAmount().compareTo(BigDecimal.ZERO) == 0) {
                return couponInfo.getAmount();
            } else {
                return orderAmount.compareTo(couponInfo.getConditionAmount()) >= 0
                        ? couponInfo.getAmount()
                        : null;
            }
        } else if (couponInfo.getCouponType() == 2) {
            // 折扣券
            BigDecimal discountOrderAmount = orderAmount.multiply(couponInfo.getDiscount())
                    .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
            BigDecimal reduce = orderAmount.subtract(discountOrderAmount);

            if (couponInfo.getConditionAmount().compareTo(BigDecimal.ZERO) == 0) {
                return reduce;
            } else {
                return discountOrderAmount.compareTo(couponInfo.getConditionAmount()) >= 0
                        ? reduce
                        : null;
            }
        }
        return null;
    }


    //// 优化前
    ////使用优惠卷
    //@Transactional(rollbackFor = Exception.class) //同一微服务、同一数据库内的多表操作, 必须加 @Transactional 本地事务
    //@Override
    //public BigDecimal useCoupon(UseCouponForm useCouponForm) {
    //    //1 根据乘客优惠券id获取乘客优惠卷信息
    //    CustomerCoupon customerCoupon =
    //            customerCouponMapper.selectById(useCouponForm.getCustomerCouponId());
    //    if(customerCoupon == null) {
    //        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    //    }
    //    //2 根据优惠卷id获取优惠卷信息
    //    CouponInfo couponInfo =
    //            couponInfoMapper.selectById(customerCoupon.getCouponId());
    //    if(couponInfo == null) {
    //        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    //    }
    //
    //    //3 判断优惠卷是否是当前乘客所持有的
    //    if(customerCoupon.getCustomerId() != useCouponForm.getCustomerId()) {
    //        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    //    }
    //
    //    //4 判断是否具备优惠卷使用条件
    //    //现金和折扣卷，根据使用门槛判断
    //    BigDecimal reduceAmount = null;
    //    //1 现金券
    //    if(couponInfo.getCouponType() == 1) {
    //        //没有门槛，订单金额大于优惠减免金额
    //        if(couponInfo.getConditionAmount().doubleValue()==0
    //                && useCouponForm.getOrderAmount().subtract(couponInfo.getAmount()).doubleValue()>0) {
    //            reduceAmount = couponInfo.getAmount();
    //        }
    //
    //        //有门槛，订单金额大于优惠卷门槛金额
    //        if(couponInfo.getConditionAmount().doubleValue()>0
    //                && useCouponForm.getOrderAmount().subtract(couponInfo.getConditionAmount()).doubleValue()>0) {
    //            reduceAmount = couponInfo.getAmount();
    //        }
    //    } else {//2 折扣
    //        //折扣后金额
    //        BigDecimal discountOrderAmount = useCouponForm.getOrderAmount().multiply(couponInfo.getDiscount())
    //                .divide(new BigDecimal("10")).setScale(2, RoundingMode.HALF_UP);
    //        //订单优惠金额
    //        //2.2.1.没门槛
    //        if (couponInfo.getConditionAmount().doubleValue() == 0) {
    //            //减免金额
    //            reduceAmount = useCouponForm.getOrderAmount().subtract(discountOrderAmount);
    //        }
    //        //2.2.2.有门槛，订单折扣后金额大于优惠券门槛金额
    //        if (couponInfo.getConditionAmount().doubleValue() > 0 && discountOrderAmount.subtract(couponInfo.getConditionAmount()).doubleValue() > 0) {
    //            //减免金额
    //            reduceAmount = useCouponForm.getOrderAmount().subtract(discountOrderAmount);
    //        }
    //    }
    //
    //    //5 如果满足条件，更新两张表数据
    //    if(reduceAmount.doubleValue()>0) {
    //        //更新coupon_info使用数量
    //        //根据id查询优惠卷对象
    //        Integer useCount_old = couponInfo.getUseCount();
    //        couponInfo.setUseCount(useCount_old+1);
    //        couponInfoMapper.updateById(couponInfo);
    //
    //        //更新customer_coupon
    //        CustomerCoupon updateCustomerCoupon = new CustomerCoupon();
    //        updateCustomerCoupon.setId(customerCoupon.getId());
    //        updateCustomerCoupon.setUsedTime(new Date());
    //        updateCustomerCoupon.setOrderId(useCouponForm.getOrderId());
    //        customerCouponMapper.updateById(updateCustomerCoupon);
    //
    //        return reduceAmount;
    //    }
    //    return null;
    //}



    private AvailableCouponVo buildBestNoUseCouponVo(NoUseCouponVo noUseCouponVo, BigDecimal reduceAmount) {
        AvailableCouponVo bestNoUseCouponVo = new AvailableCouponVo();
        BeanUtils.copyProperties(noUseCouponVo, bestNoUseCouponVo);
        bestNoUseCouponVo.setCouponId(noUseCouponVo.getId());
        bestNoUseCouponVo.setReduceAmount(reduceAmount);
        return bestNoUseCouponVo;
    }

    // 保存领取优惠券的记录
    private void saveCustomerCoupon(Long customerId, Long couponId, Date expireTime) {
        CustomerCoupon customerCoupon = new CustomerCoupon();
        customerCoupon.setCouponId(couponId);
        customerCoupon.setCustomerId(customerId);
        customerCoupon.setExpireTime(expireTime);
        customerCoupon.setReceiveTime(new Date());
        customerCoupon.setStatus(1);
        customerCouponMapper.insert(customerCoupon);
    }

}
