package com.daijia.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daijia.common.constant.RedisConstant;
import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.model.entity.order.*;
import com.daijia.model.enums.OrderStatus;
import com.daijia.model.form.order.OrderInfoForm;
import com.daijia.model.form.order.StartDriveForm;
import com.daijia.model.form.order.UpdateOrderBillForm;
import com.daijia.model.form.order.UpdateOrderCartForm;
import com.daijia.model.vo.base.PageVo;
import com.daijia.model.vo.order.*;
import com.daijia.order.mapper.OrderBillMapper;
import com.daijia.order.mapper.OrderInfoMapper;
import com.daijia.order.mapper.OrderProfitsharingMapper;
import com.daijia.order.mapper.OrderStatusLogMapper;
import com.daijia.order.service.OrderInfoService;
import com.daijia.order.service.OrderMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * extends ServiceImpl<OrderInfoMapper, OrderInfo> 继承 MyBatis-Plus 提供的基础实现类
 *
 * 继承后获得什么：(无需自己写这些基础代码！)
 *      基础的 CRUD 方法（增删改查）
 *      分页查询方法
 *      批量操作等方法
 */

@Service
@RequiredArgsConstructor
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    private final OrderInfoMapper orderInfoMapper;
    private final RedisTemplate redisTemplate;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final RedissonClient redissonClient;
    private final OrderBillMapper orderBillMapper;
    private final OrderProfitsharingMapper orderProfitsharingMapper;
    private final OrderMonitorService orderMonitorService;

    //乘客端查找当前是否有未完成的订单
    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        //封装条件
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getCustomerId,customerId);

        // 未完成订单状态数组
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),        // 已接单
                OrderStatus.DRIVER_ARRIVED.getStatus(),  // 司机已到达
                OrderStatus.UPDATE_CART_INFO.getStatus(), // 更新代驾车辆信息
                OrderStatus.START_SERVICE.getStatus(),   // 开始服务
                OrderStatus.END_SERVICE.getStatus(),     // 结束服务
                OrderStatus.UNPAID.getStatus()           // 待付款
        };
        // 选出在数组中的状态的订单
        wrapper.in(OrderInfo::getStatus,statusArray);

        //获取最新一条记录
        wrapper.orderByDesc(OrderInfo::getId);
        wrapper.last(" limit 1");

        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);

        //封装到CurrentOrderInfoVo
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if(orderInfo != null) {
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }

    //乘客下单,保存订单数据到数据库
    @Override
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        //order_info添加订单数据
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoForm,orderInfo);
        //订单号
        String orderNo = UUID.randomUUID().toString().replaceAll("-","");
        orderInfo.setOrderNo(orderNo);
        //订单状态
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
        orderInfoMapper.insert(orderInfo);

        //生成订单之后，发送延迟消息
        this.sendDelayMessage(orderInfo.getId());

        //记录日志
        this.log(orderInfo.getId(),orderInfo.getStatus());

        //向redis添加标识
        //接单标识，标识不存在了说明不在等待接单状态了
        redisTemplate.opsForValue().set(RedisConstant.ORDER_ACCEPT_MARK,
                "0", RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);


        return orderInfo.getId();

    }

    //生成订单之后，发送延迟消息,到过期时间后自动取消订单
    private void sendDelayMessage(Long orderId) {
        try{
            /**
             * 1 创建队列
             *              RBlockingQueue：基于Redis的List数据结构实现的阻塞队列
             *             底层Redis命令：对应 LPUSH、RPOP、BLPOP 等操作
             *             提供了线程阻塞的获取能力
             */
            RBlockingQueue<Object> blockingQueue = redissonClient.getBlockingQueue("queue_cancel");

            /**
             * 2 把创建队列放到延迟队列里面
             *             RDelayedQueue：是Redisson对普通队列的增强包装
             *             底层原理：使用Redis的Sorted Set（有序集合） + Pub/Sub机制实现
             *             Sorted Set的score存储消息的到期时间戳
             *             Pub/Sub用于通知延迟消息已到期
             *
             */
            RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);

            //3 发送消息到延迟队列里面
            //设置过期时间
            delayedQueue.offer(orderId.toString(),15,TimeUnit.MINUTES);

        }catch (Exception e) {
            e.printStackTrace();
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

    public void log(Long orderId, Integer status) {
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderId);
        orderStatusLog.setOrderStatus(status);
        orderStatusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(orderStatusLog);
    }

    // 查询订单状态
    @Override
    public Integer getOrderStatus(Long orderId) {
        //sql语句： select status from order_info where id=?
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,orderId);
        wrapper.select(OrderInfo::getStatus);
        //调用mapper方法
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        //订单不存在
        if(orderInfo == null) {
            return OrderStatus.NULL_ORDER.getStatus();
        }

        return orderInfo.getStatus();
    }

    //司机端查找当前订单
    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        //封装条件
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getDriverId,driverId);
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CART_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus()
        };
        wrapper.in(OrderInfo::getStatus,statusArray);
        wrapper.orderByDesc(OrderInfo::getId);
        wrapper.last(" limit 1");
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        //封装到vo
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if(null != orderInfo) {
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }


    //Redisson分布式锁
    //司机抢单
    @Transactional(rollbackFor = Exception.class) // 保证单数据库的数据一致性
    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        //判断订单是否存在，通过Redis，减少数据库压力
        if(!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }

        //创建分布式锁
        RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);

        try {
            //获取锁
            /**
             * TryLock是一种非阻塞式的分布式锁，实现原理：Redis的SETNX命令
             * 参数：
             *     waitTime：等待获取锁的时间
             *     leaseTime：加锁的时间
             */
            boolean flag = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME,RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if(flag) {
                if(!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
                    //抢单失败
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }
                //司机抢单
                //修改order_info表订单状态值2：已经接单 + 司机id + 司机接单时间
                //修改条件：根据订单id
                LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(OrderInfo::getId,orderId);
                OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
                //设置
                orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
                orderInfo.setDriverId(driverId);
                orderInfo.setAcceptTime(new Date());
                //调用方法修改
                int rows = orderInfoMapper.updateById(orderInfo);
                if(rows != 1) {
                    //抢单失败
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }

                //删除抢单标识
                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
            }
        }catch (Exception e) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }finally {
            //释放
            if(lock.isLocked()) {
                lock.unlock();
            }
        }
        return true;
    }

    //乘客端获取订单账单信息,准备支付
    @Override
    public OrderBillVo getOrderBillInfo(Long orderId) {
        LambdaQueryWrapper<OrderBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderBill::getOrderId,orderId);
        OrderBill orderBill = orderBillMapper.selectOne(wrapper);

        OrderBillVo orderBillVo = new OrderBillVo();
        BeanUtils.copyProperties(orderBill,orderBillVo);
        return orderBillVo;
    }

    // 获取订单分账信息
    @Override
    public OrderProfitsharingVo getOrderProfitsharing(Long orderId) {
        LambdaQueryWrapper<OrderProfitsharing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderProfitsharing::getOrderId,orderId);
        OrderProfitsharing orderProfitsharing = orderProfitsharingMapper.selectOne(wrapper);

        OrderProfitsharingVo orderProfitsharingVo = new OrderProfitsharingVo();
        BeanUtils.copyProperties(orderProfitsharing,orderProfitsharingVo);
        return orderProfitsharingVo;
    }

    //司机到达起始点
    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        // 更新订单状态和到达时间，条件：orderId + driverId
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,orderId);
        wrapper.eq(OrderInfo::getDriverId,driverId);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.DRIVER_ARRIVED.getStatus());
        orderInfo.setArriveTime(new Date());

        int rows = orderInfoMapper.update(orderInfo, wrapper);

        if(rows == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    /**
     * 录入代驾车辆信息
     * @param updateOrderCartForm
     * @return
     */
    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,updateOrderCartForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId,updateOrderCartForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(updateOrderCartForm,orderInfo);
        orderInfo.setStatus(OrderStatus.UPDATE_CART_INFO.getStatus());

        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if(rows == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    //开始代驾服务
    @Override
    public Boolean startDriver(StartDriveForm startDriveForm) {
        //根据订单id  +  司机id  更新订单状态  和 开始代驾时间
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, startDriveForm.getOrderId());
        queryWrapper.eq(OrderInfo::getDriverId, startDriveForm.getDriverId());

        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.START_SERVICE.getStatus());
        updateOrderInfo.setStartServiceTime(new Date());
        //只能更新自己的订单
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if(row == 1) {
            //记录日志
            this.log(startDriveForm.getOrderId(), OrderStatus.START_SERVICE.getStatus());
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }

        //初始化订单监控统计数据
        OrderMonitor orderMonitor = new OrderMonitor();
        orderMonitor.setOrderId(startDriveForm.getOrderId());
        orderMonitorService.saveOrderMonitor(orderMonitor);
        return true;
    }

    // 获取规定时间内司机完成的订单数
    @Override
    public Long getOrderNumByTime(String startTime, String endTime) {
        // 09 <= time < 10   <= time1  <    11
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(OrderInfo::getStartServiceTime,startTime);
        wrapper.lt(OrderInfo::getStartServiceTime,endTime);
        Long count = orderInfoMapper.selectCount(wrapper);
        return count;
    }

    //结束代驾
    @Override
    public Boolean endDrive(UpdateOrderBillForm updateOrderBillForm) {
        //1 更新订单信息
        // update order_info set ..... where id=? and driver_id=?
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,updateOrderBillForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId,updateOrderBillForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.END_SERVICE.getStatus());
        orderInfo.setRealAmount(updateOrderBillForm.getTotalAmount());
        orderInfo.setFavourFee(updateOrderBillForm.getFavourFee());
        orderInfo.setRealDistance(updateOrderBillForm.getRealDistance());
        orderInfo.setEndServiceTime(new Date());

        int rows = orderInfoMapper.update(orderInfo, wrapper);

        if(rows == 1) {
            //添加账单数据
            OrderBill orderBill = new OrderBill();
            BeanUtils.copyProperties(updateOrderBillForm,orderBill);
            orderBill.setOrderId(updateOrderBillForm.getOrderId());
            orderBill.setPayAmount(updateOrderBillForm.getTotalAmount());
            orderBillMapper.insert(orderBill);

            //添加分账信息
            OrderProfitsharing orderProfitsharing = new OrderProfitsharing();
            BeanUtils.copyProperties(updateOrderBillForm, orderProfitsharing);
            orderProfitsharing.setOrderId(updateOrderBillForm.getOrderId());
            //TODO
            orderProfitsharing.setRuleId(new Date().getTime());
            orderProfitsharing.setStatus(1);
            orderProfitsharingMapper.insert(orderProfitsharing);

        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }


    //获取乘客订单分页列表
    @Override
    public PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId) {
        IPage<OrderListVo> pageInfo =  orderInfoMapper.selectCustomerOrderPage(pageParam,customerId);
        return new PageVo<>(pageInfo.getRecords(),pageInfo.getPages(),pageInfo.getTotal());
    }

    @Override
    public PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId) {
        IPage<OrderListVo> pageInfo = orderInfoMapper.selectDriverOrderPage(pageParam, driverId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        //更新订单信息
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.eq(OrderInfo::getDriverId, driverId);
        //更新字段
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.UNPAID.getStatus());
        //只能更新自己的订单
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if(row == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public OrderPayVo getOrderPayVo(String orderNo, Long customerId) {
        OrderPayVo orderPayVo = orderInfoMapper.selectOrderPayVo(orderNo,customerId);
        if(orderPayVo != null) {
            String content = orderPayVo.getStartLocation() + " 到 "+orderPayVo.getEndLocation();
            orderPayVo.setContent(content);
        }
        return orderPayVo;
    }

    //更新订单支付状态
    @Override
    public Boolean updateOrderPayStatus(String orderNo) {
        //查询订单，判断订单状态，如果已更新支付状态，直接返回
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        queryWrapper.select(OrderInfo::getId, OrderInfo::getDriverId, OrderInfo::getStatus);
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        if(null == orderInfo || orderInfo.getStatus().intValue() == OrderStatus.PAID.getStatus().intValue()) return true;

        //更新订单状态
        LambdaQueryWrapper<OrderInfo> updateQueryWrapper = new LambdaQueryWrapper<>();
        updateQueryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        //更新字段
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.PAID.getStatus());
        updateOrderInfo.setPayTime(new Date());
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if(row == 1) {
            //记录日志
            this.log(orderInfo.getId(), OrderStatus.PAID.getStatus());
        } else {
            log.error("订单支付回调更新订单状态失败，订单号为：" + orderNo);
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

    //获取订单的系统奖励信息
    @Override
    public OrderRewardVo getOrderRewardFee(String orderNo) {
        //根据订单编号查询订单表
        OrderInfo orderInfo =
                orderInfoMapper.selectOne(
                        new LambdaQueryWrapper<OrderInfo>()
                                .eq(OrderInfo::getOrderNo, orderNo)
                                .select(OrderInfo::getId,OrderInfo::getDriverId));

        //根据订单id查询系统奖励表
        OrderBill orderBill =
                orderBillMapper.selectOne(new LambdaQueryWrapper<OrderBill>()
                        .eq(OrderBill::getOrderId, orderInfo.getId())
                        .select(OrderBill::getRewardFee));

        //封装到vo里面
        OrderRewardVo orderRewardVo = new OrderRewardVo();
        orderRewardVo.setOrderId(orderInfo.getId());
        orderRewardVo.setDriverId(orderInfo.getDriverId());
        orderRewardVo.setRewardFee(orderBill.getRewardFee());
        return orderRewardVo;
    }

    @Override
    public Boolean updateCouponAmount(Long orderId, BigDecimal couponAmount) {
        orderBillMapper.updateCouponAmount(orderId,couponAmount);
        return true;
    }

}
