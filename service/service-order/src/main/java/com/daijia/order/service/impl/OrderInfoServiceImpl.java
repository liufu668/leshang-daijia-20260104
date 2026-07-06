package com.daijia.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daijia.common.config.rabbitmq.RabbitConfig;
import com.daijia.common.constant.RedisConstant;
import com.daijia.common.exception.GuiguException;
import com.daijia.common.model.OrderRobMsg;
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
import org.redisson.api.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * extends ServiceImpl<OrderInfoMapper, OrderInfo> 继承 MyBatis-Plus 提供的基础实现类
 *
 * 继承后获得什么：(无需自己写这些基础代码！)
 *      基础的 CRUD 方法（增删改查）
 *      分页查询方法
 *      批量操作等方法
 */

@Slf4j
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
    private final RabbitTemplate rabbitTemplate;

    public List<OrderInfo> listUnDispatchOrders(Integer seconds) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();

        // 1. 只查 等待接单 的订单
        wrapper.eq(OrderInfo::getStatus, OrderStatus.WAITING_ACCEPT.getStatus());

        // 2. 调度状态 = 0（未调度）或 3（调度失败，等待重试）
        wrapper.in(OrderInfo::getDispatchStatus,
                RedisConstant.DISPATCH_NOT_START,
                RedisConstant.DISPATCH_FAILED);

        // 3. 创建时间超过 N 秒
        wrapper.le(OrderInfo::getCreateTime, new Date(System.currentTimeMillis() - seconds * 1000L));
        // 4. 查询
        return orderInfoMapper.selectList(wrapper);
    }

    @Override
    public Boolean updateDispatchStatus(Long orderId, Integer status) {
        // 1. 构建更新条件
        LambdaUpdateWrapper<OrderInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderInfo::getId, orderId);  // 按订单ID更新

        // 2. 设置要更新的字段：dispatchStatus
        updateWrapper.set(OrderInfo::getDispatchStatus, status);

        // 3. 执行更新
        int rows = orderInfoMapper.update(null, updateWrapper);

        // 4. 返回是否成功
        return rows > 0;
    }


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

        String orderAcceptMark = RedisConstant.ORDER_ACCEPT_MARK + ":" + orderInfo.getId();

        redisTemplate.opsForValue().set(orderAcceptMark,
                "0", RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);


        //向redis添加标识
        //接单标识，标识不存在了说明不在等待接单状态了
        //redisTemplate.opsForValue().set(RedisConstant.ORDER_ACCEPT_MARK,
        //                "0", RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);

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

    //调用方法取消订单
    @Override
    public void orderCancel(long orderId) {
        //orderId查询订单信息
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        //判断
        if(orderInfo.getStatus()==OrderStatus.WAITING_ACCEPT.getStatus()) {
            //修改订单状态：取消状态
            orderInfo.setStatus(OrderStatus.CANCEL_ORDER.getStatus());
            int rows = orderInfoMapper.updateById(orderInfo);
            if(rows == 1) {
                //删除接单标识

                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
            }
        }
    }

    @Override
    public List<OrderInfo> listTimeoutUnpaidOrder(Integer minutes) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        // 只查询【待支付】状态
        wrapper.eq(OrderInfo::getStatus, OrderStatus.UNPAID.getStatus());
        // 超过指定分钟未支付（结束服务时间 - 当前时间）
        wrapper.le(OrderInfo::getEndServiceTime, new Date(System.currentTimeMillis() - minutes * 60 * 1000L));
        return orderInfoMapper.selectList(wrapper);
    }

    @Override
    public Boolean cancelOrder(Long orderId, String remark) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (orderInfo == null) {
            return false;
        }

        // 只有两种状态可以取消：
        // 1. 等待接单
        // 2. 待支付（超时未支付）
        if (!(orderInfo.getStatus().equals(OrderStatus.WAITING_ACCEPT.getStatus())
                || orderInfo.getStatus().equals(OrderStatus.UNPAID.getStatus()))) {
            return true;
        }

        // 更新为取消状态
        OrderInfo update = new OrderInfo();
        update.setId(orderId);
        update.setStatus(OrderStatus.CANCEL_ORDER.getStatus());
        int rows = orderInfoMapper.updateById(update);

        // 记录日志
        if (rows == 1) {
            log(orderId, OrderStatus.CANCEL_ORDER.getStatus());
        }

        return rows == 1;
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


    //司机抢单(优化前)
    //@Override
    //public Boolean robNewOrder(Long driverId, Long orderId) {
    //    //判断订单是否存在，通过Redis，减少数据库压力
    //    if(!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
    //        //抢单失败
    //        throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
    //    }
    //
    //    //创建锁
    //    RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);
    //
    //    try {
    //        //获取锁
    //        boolean flag = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME,RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);
    //        if(flag) {
    //            if(!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
    //                //抢单失败
    //                throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
    //            }
    //            //司机抢单
    //            //修改order_info表订单状态值2：已经接单 + 司机id + 司机接单时间
    //            //修改条件：根据订单id
    //            LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
    //            wrapper.eq(OrderInfo::getId,orderId);
    //            OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
    //            //设置
    //            orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
    //            orderInfo.setDriverId(driverId);
    //            orderInfo.setAcceptTime(new Date());
    //            //调用方法修改
    //            int rows = orderInfoMapper.updateById(orderInfo);
    //            if(rows != 1) {
    //                //抢单失败
    //                throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
    //            }
    //
    //            //删除抢单标识
    //            redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
    //        }
    //    }catch (Exception e) {
    //        //抢单失败
    //        throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
    //    }finally {
    //        //释放
    //        if(lock.isLocked()) {
    //            lock.unlock();
    //        }
    //    }
    //    return true;
    //}
    //
    //// 司机抢单(优化后)
    //@Override
    //public Boolean robNewOrder(Long driverId, Long orderId) {
    //
    //    log.info("开始抢单,司机ID: {}, 订单ID: {}", driverId, orderId);
    //
    //
    //    // 优化1：Redis 标识拼接 orderId, 减小锁的粒度
    //    String orderAcceptMark = RedisConstant.ORDER_ACCEPT_MARK + ":" + orderId;
    //
    //    //判断订单是否存在，通过Redis，减少数据库压力
    //    // Redis hasKey 非原子操作（高并发下有数据一致性风险）
    //    // 高并发下，线程 A 执行 hasKey 为 true 后，线程 B 立刻删除该 Key，线程 A 仍会继续执行，导致无效抢单。
    //    // 所以改用 Lua 原子脚本（处理 Object 返回值），避免并发误判：
    //    RedisScript<Long> existsScript = new DefaultRedisScript<>(
    //            "if redis.call('exists', KEYS[1]) == 1 then return 1 else return 0 end",
    //            Long.class
    //    );
    //    // 处理Object返回值，避免类型转换异常
    //    Object existsObj = redisTemplate.execute(existsScript, Collections.singletonList(orderAcceptMark));
    //    Long exists = (existsObj == null) ? 0 : (Long) existsObj;
    //    if (exists != 1) {
    //        throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
    //    }
    //
    //    // 用分布式锁防止同时抢，用订单状态防止重复抢，双重保证不会一个单被多个司机接到。
    //    RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);
    //
    //    try {
    //        boolean flag = lock.tryLock(
    //                // 一起抢,失败就不等了
    //                0,
    //                RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME,
    //                TimeUnit.MILLISECONDS
    //        );
    //
    //        if(flag) {
    //            log.info("获取锁成功,开始处理抢单, driverId={}, orderId={}", driverId, orderId);
    //
    //            // 双重校验
    //            Object innerExistsObj = redisTemplate.execute(existsScript, Collections.singletonList(orderAcceptMark));
    //            Long innerExists = (innerExistsObj == null) ? 0 : (Long) innerExistsObj;
    //            if (innerExists != 1) {
    //                log.warn("订单已被抢, orderId={}", orderId);
    //                throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
    //            }
    //
    //            // 核心逻辑：只更新状态为WAITING_ACCEPT的订单，同时用version做乐观锁（需给order_info加version字段）
    //            // 性能提升要点: 用乐观锁替代了 db 默认的悲观锁
    //            LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
    //            queryWrapper.select(OrderInfo::getId, OrderInfo::getVersion)
    //                    .eq(OrderInfo::getId, orderId);
    //            OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
    //            if (orderInfo == null) {
    //                log.warn("订单不存在, orderId={}", orderId);
    //                throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
    //            }
    //
    //            // 6. 数据库条件更新+乐观锁
    //            LambdaUpdateWrapper<OrderInfo> updateWrapper = new LambdaUpdateWrapper<>();
    //            updateWrapper.eq(OrderInfo::getId, orderId)
    //                    .eq(OrderInfo::getStatus, OrderStatus.WAITING_ACCEPT.getStatus())
    //                    .eq(OrderInfo::getVersion, orderInfo.getVersion())
    //                    .set(OrderInfo::getVersion, orderInfo.getVersion() + 1)
    //                    .set(OrderInfo::getStatus, OrderStatus.ACCEPTED.getStatus())
    //                    .set(OrderInfo::getDriverId, driverId)
    //                    .set(OrderInfo::getAcceptTime, new Date());
    //            int rows = orderInfoMapper.update(null, updateWrapper);
    //
    //            if(rows != 1) {
    //                //抢单失败
    //                throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
    //            }
    //
    //            String deleteScript = "if redis.call('exists', KEYS[1]) == 1 then redis.call('del', KEYS[1]); return 1 else return 0 end";
    //            Object deleteObj = redisTemplate.execute(
    //                    new DefaultRedisScript<>(deleteScript, Integer.class),
    //                    Collections.singletonList(orderAcceptMark)
    //            );
    //            Integer deleteResult = (deleteObj == null) ? 0 : (Integer) deleteObj;
    //            log.info("删除Redis订单标识, orderId={}, 结果:{}", orderId, deleteResult == 1 ? "成功" : "失败");
    //
    //        }else {
    //            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
    //        }
    //    }catch (GuiguException e) {
    //        // 业务异常直接抛出，保留原始信息
    //        throw e;
    //    } catch (Exception e) {
    //        log.error("抢单异常, driverId={}, orderId={}", driverId, orderId, e);
    //        throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
    //    } finally {
    //        // 优化解锁逻辑：用Redisson的safeUnlock，避免IllegalMonitorStateException
    //        if (lock.isHeldByCurrentThread()) {
    //            try {
    //                lock.unlock();
    //            } catch (Exception e) {
    //                log.error("解锁异常, orderId={}", orderId, e);
    //            }
    //        }
    //    }
    //
    //    return true;
    //}

    //给核心方法添加 JavaDoc 注释
    /**
     * 司机抢单核心逻辑 - 高并发场景下的三重保障机制
     * <p>
     * 解决方案：
     * 1. Redis Lua 脚本保证订单状态判断的原子性
     * 2. Redisson 分布式锁控制并发访问
     * 3. RabbitMQ 异步处理 + 发布确认保证消息可靠性
     * 4. 数据库乐观锁作为最终兜底
     * </p>
     *
     * @param driverId 司机ID
     * @param orderId  订单ID
     * @return 抢单成功返回 true
     * @throws GuiguException 抢单失败时抛出异常（订单已被抢、网络异常等）
     */
    // 司机抢单(优化后)
    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {

        log.info("开始抢单,司机ID: {}, 订单ID: {}", driverId, orderId);


        // 优化1：Redis 标识拼接 orderId, 减小锁的粒度
        String orderAcceptMark = RedisConstant.ORDER_ACCEPT_MARK + ":" + orderId;

        //判断订单是否存在，通过Redis，减少数据库压力
        RedisScript<Long> existsScript = new DefaultRedisScript<>(
                "if redis.call('exists', KEYS[1]) == 1 then return 1 else return 0 end",
                Long.class
        );
        // 处理Object返回值，避免类型转换异常
        Object existsObj = redisTemplate.execute(existsScript, Collections.singletonList(orderAcceptMark));
        Long exists = (existsObj == null) ? 0 : (Long) existsObj;
        if (exists != 1) {
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }

        // 用分布式锁防止同时抢，用订单状态防止重复抢，双重保证不会一个单被多个司机接到。
        RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);

        try {
            boolean flag = lock.tryLock(
                    // 一起抢,失败就不等了
                    0,
                    RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME,
                    TimeUnit.MILLISECONDS
            );

            if(flag) {
                log.info("获取锁成功,开始处理抢单, driverId={}, orderId={}", driverId, orderId);

                // 双重校验
                Object innerExistsObj = redisTemplate.execute(existsScript, Collections.singletonList(orderAcceptMark));
                Long innerExists = (innerExistsObj == null) ? 0 : (Long) innerExistsObj;
                if (innerExists != 1) {
                    log.warn("订单已被抢, orderId={}", orderId);
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }

                // 只发消息，不更新DB
                OrderRobMsg msg = new OrderRobMsg();
                msg.setOrderId(orderId);
                msg.setDriverId(driverId);

                // 1. 创建消息唯一ID（用于确认）
                CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());

                // 2. 发送消息，并等待 MQ 服务器确认
                rabbitTemplate.convertAndSend(
                        RabbitConfig.EXCHANGE,
                        RabbitConfig.ROUTING_KEY,
                        msg,
                        correlationData
                );

                // 3. 等待 MQ 返回 ACK（5秒超时）
                try {
                    CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
                    // 如果 MQ 返回失败，直接抛异常，不删标识
                    if (!confirm.isAck()) {
                        log.error("MQ发送失败，Broker返回NACK，orderId={}", orderId);
                        throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                    }
                } catch (Exception e) {
                    log.error("等待MQ确认超时或异常，orderId={}", orderId, e);
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }

                // 只有 MQ 确认成功，才会走到这里删标识
                String deleteScript = "if redis.call('exists', KEYS[1]) == 1 then redis.call('del', KEYS[1]); return 1 else return 0 end";
                Object deleteObj = redisTemplate.execute(
                        new DefaultRedisScript<>(deleteScript, Integer.class),
                        Collections.singletonList(orderAcceptMark)
                );
                Integer deleteResult = (deleteObj == null) ? 0 : (Integer) deleteObj;
                log.info("删除Redis订单标识, orderId={}, 结果:{}", orderId, deleteResult == 1 ? "成功" : "失败");

            }else {
                throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
            }
        }catch (GuiguException e) {
            // 业务异常直接抛出，保留原始信息
            throw e;
        } catch (Exception e) {
            log.error("抢单异常, driverId={}, orderId={}", driverId, orderId, e);
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        } finally {
            // 优化解锁逻辑：用Redisson的safeUnlock，避免IllegalMonitorStateException
            if (lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.error("解锁异常, orderId={}", orderId, e);
                }
            }
        }

        return true;
    }

    // ====================== 新增：抢单消费失败兜底定时任务 ======================
    /**
     * 每分钟执行：拉取抢单死信队列消息重试
     */
    @Scheduled(fixedRate = 60 * 1000)
    public void handleRobDlqMessage() {
        String dlqQueue = RabbitConfig.ROB_DLQ_QUEUE;
        int handleCount = 0;
        List<OrderRobMsg> failedMessages = new ArrayList<>();

        while (true) {
            Message message = rabbitTemplate.receive(dlqQueue);
            if (message == null) {
                break;
            }
            handleCount++;
            OrderRobMsg robMsg = null;
            try {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                robMsg = JSON.parseObject(body, OrderRobMsg.class);
                Long orderId = robMsg.getOrderId();
                Long driverId = robMsg.getDriverId();

                int retryCount = robMsg.getRetryCount() == null ? 1 : robMsg.getRetryCount() + 1;
                robMsg.setRetryCount(retryCount);

                if (retryCount > 3) {
                    log.error("【需人工介入】死信消息超过最大重试次数，已丢弃！" +
                                    "orderId={}, driverId={}, retryCount={}, orderStatus=WAITING_ACCEPT",
                            orderId, driverId, retryCount);
                    continue;
                }

                log.info("定时任务重试死信消息，orderId={}, driverId={}, retryCount={}",
                        orderId, driverId, retryCount);

                LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.select(OrderInfo::getId, OrderInfo::getVersion, OrderInfo::getStatus)
                        .eq(OrderInfo::getId, orderId);
                OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);

                if (orderInfo == null || !OrderStatus.WAITING_ACCEPT.getStatus().equals(orderInfo.getStatus())) {
                    log.warn("死信消息无效，订单已处理或不存在 orderId={}", orderId);
                    continue;
                }

                LambdaUpdateWrapper<OrderInfo> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(OrderInfo::getId, orderId)
                        .eq(OrderInfo::getStatus, OrderStatus.WAITING_ACCEPT.getStatus())
                        .eq(OrderInfo::getVersion, orderInfo.getVersion())
                        .set(OrderInfo::getVersion, orderInfo.getVersion() + 1)
                        .set(OrderInfo::getStatus, OrderStatus.ACCEPTED.getStatus())
                        .set(OrderInfo::getDriverId, driverId)
                        .set(OrderInfo::getAcceptTime, new Date());

                int rows = orderInfoMapper.update(null, updateWrapper);
                if (rows == 1) {
                    log.info("死信消息重试消费成功 orderId={}", orderId);
                } else if (orderInfo != null
                        && OrderStatus.WAITING_ACCEPT.getStatus().equals(orderInfo.getStatus())) {
                    failedMessages.add(robMsg);
                }
            } catch (Exception e) {
                if (robMsg != null && robMsg.getRetryCount() != null && robMsg.getRetryCount() <= 3) {
                    failedMessages.add(robMsg);
                } else {
                    log.error("【需人工介入】死信消息异常且超过最大重试次数，已丢弃！orderId={}",
                            robMsg != null ? robMsg.getOrderId() : "unknown");
                }
                log.error("处理死信消息异常", e);
            }
        }

        for (OrderRobMsg msg : failedMessages) {
            rabbitTemplate.convertAndSend("", dlqQueue, msg);
        }

        if (handleCount > 0) {
            log.info("死信处理完成 | 总数:{} | 成功:{} | 重入队:{}",
                    handleCount, handleCount - failedMessages.size(), failedMessages.size());
        }
    }


    // 去掉分布式锁(进一步优化司机抢单)
    //@Override
    //public Boolean robNewOrder(Long driverId, Long orderId) {
    //    String key = RedisConstant.ORDER_ACCEPT_MARK + ":" + orderId;
    //
    //    // Lua 原子抢单：1000并发只有1人成功
    //    String script = "if redis.call('get', KEYS[1]) == '1' then "
    //            + "redis.call('set', KEYS[1], '0');"
    //            + "redis.call('hset', 'order:winner', KEYS[1], ARGV[1]);"
    //            + "return 1;"
    //            + "else "
    //            + "return 0;"
    //            + "end";
    //
    //    RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
    //    Long result = redisTemplate.execute(redisScript, Lists.newArrayList(key), driverId.toString());
    //
    //    if (result == null || result == 0) {
    //        throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
    //    }
    //
    //    // 抢单成功 → 发送 MQ 异步落库
    //    OrderRobMsg msg = new OrderRobMsg();
    //    msg.setOrderId(orderId);
    //    msg.setDriverId(driverId);
    //    rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, msg);
    //
    //    return true;
    //}
    //
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

    @Override
    public OrderPayVo getOrderInfoByOrderNo(String orderNo) {
        OrderPayVo orderPayVo = orderInfoMapper.selectOrderInfo(orderNo);
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

    // 更新 order_bill 表里的优惠券金额
    @Override
    public Boolean updateCouponAmount(Long orderId, BigDecimal couponAmount) {
        orderBillMapper.updateCouponAmount(orderId,couponAmount);
        return true;
    }

}
