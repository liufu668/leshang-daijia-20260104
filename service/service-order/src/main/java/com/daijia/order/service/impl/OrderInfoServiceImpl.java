package com.daijia.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daijia.common.constant.RedisConstant;
import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.model.entity.order.OrderInfo;
import com.daijia.model.entity.order.OrderStatusLog;
import com.daijia.model.enums.OrderStatus;
import com.daijia.model.form.order.OrderInfoForm;
import com.daijia.model.vo.order.CurrentOrderInfoVo;
import com.daijia.order.mapper.OrderInfoMapper;
import com.daijia.order.mapper.OrderStatusLogMapper;
import com.daijia.order.service.OrderInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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

    //乘客端查找当前订单
    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        //封装条件
        //乘客id
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

        //调用方法
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

    //乘客下单
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

    //生成订单之后，发送延迟消息
    private void sendDelayMessage(Long orderId) {
        try{
            //1 创建队列
            RBlockingQueue<Object> blockingDueue = redissonClient.getBlockingQueue("queue_cancel");

            //2 把创建队列放到延迟队列里面
            RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingDueue);

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
    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        //判断订单是否存在，通过Redis，减少数据库压力
        if(!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }

        //创建锁
        RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);

        try {
            //获取锁
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

}
