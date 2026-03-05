package com.daijia.dispatch.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daijia.common.constant.RedisConstant;
import com.daijia.dispatch.mapper.OrderJobMapper;
import com.daijia.dispatch.service.NewOrderService;
import com.daijia.dispatch.xxl.client.XxlJobClient;
import com.daijia.map.client.LocationFeignClient;
import com.daijia.model.entity.dispatch.OrderJob;
import com.daijia.model.enums.OrderStatus;
import com.daijia.model.form.map.SearchNearByDriverForm;
import com.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.daijia.model.vo.map.NearByDriverVo;
import com.daijia.model.vo.order.NewOrderDataVo;
import com.daijia.order.client.OrderInfoFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class NewOrderServiceImpl implements NewOrderService {

    private final OrderJobMapper orderJobMapper;
    private final XxlJobClient xxlJobClient;
    private final OrderInfoFeignClient orderInfoFeignClient;
    private final LocationFeignClient locationFeignClient;
    private final RedisTemplate redisTemplate;


    //调用调度中心创建并启动任务
    @Override
    public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {
        //1 判断当前订单是否启动任务调度
        //根据订单id查询
        LambdaQueryWrapper<OrderJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderJob::getOrderId,newOrderTaskVo.getOrderId());
        OrderJob orderJob = orderJobMapper.selectOne(wrapper);

        //2 没有启动，进行操作
        if(orderJob == null) {
            //创建并启动任务调度
            //String executorHandler 执行任务job方法
            // String param
            // String corn 执行cron表达式
            // String desc 描述信息
            Long jobId = xxlJobClient.addAndStart("newOrderTaskHandler", "",
                    "0 0/1 * * * ?",
                    "新创建订单任务调度：" + newOrderTaskVo.getOrderId());

            //记录任务调度信息
            orderJob = new OrderJob();
            orderJob.setOrderId(newOrderTaskVo.getOrderId());
            orderJob.setJobId(jobId);
            orderJob.setParameter(JSONObject.toJSONString(newOrderTaskVo));
            orderJobMapper.insert(orderJob);
        }
        return orderJob.getJobId();
    }

    //执行任务：搜索附近代驾司机
    @Override
    public void executeTask(long jobId) {
        //1 根据jobId查询数据库，当前任务是否已经创建
        //如果没有创建，不往下执行了
        LambdaQueryWrapper<OrderJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderJob::getJobId,jobId);
        OrderJob orderJob = orderJobMapper.selectOne(wrapper);
        if(orderJob == null) {
            //不往下执行了
            return;
        }

        //2 查询订单状态，如果当前订单接单状态，继续执行。如果当前订单不是接单状态，停止任务调度
        //获取OrderJob里面对象
        String jsonString = orderJob.getParameter();
        NewOrderTaskVo newOrderTaskVo = JSONObject.parseObject(jsonString, NewOrderTaskVo.class);

        //获取orderId
        Long orderId = newOrderTaskVo.getOrderId();
        Integer status = orderInfoFeignClient.getOrderStatus(orderId).getData();
        if(status.intValue() != OrderStatus.WAITING_ACCEPT.getStatus().intValue()) {
            //停止任务调度
            xxlJobClient.stopJob(jobId);
            return;
        }

        //3 远程调用:搜索附近满足条件可以接单司机
        //4 远程调用之后，获取满足可以接单司机集合
        SearchNearByDriverForm searchNearByDriverForm = new SearchNearByDriverForm();
        searchNearByDriverForm.setLongitude(newOrderTaskVo.getStartPointLongitude());
        searchNearByDriverForm.setLatitude(newOrderTaskVo.getStartPointLatitude());
        searchNearByDriverForm.setMileageDistance(newOrderTaskVo.getExpectDistance());
        //远程调用
        List<NearByDriverVo> nearByDriverVoList =
                locationFeignClient.searchNearByDriver(searchNearByDriverForm).getData();

        //5 遍历司机集合，得到每个司机，为每个司机创建临时队列，存储新订单信息
        nearByDriverVoList.forEach(driver -> {
            //使用Redis的set类型
            //根据订单id生成key
            String repeatKey =
                    RedisConstant.DRIVER_ORDER_REPEAT_LIST+newOrderTaskVo.getOrderId();
            //记录司机id，防止重复推送
            /**
             * 用Redis Set而不是普通Set的原因是：
             *
             * 分布式环境：多个服务实例需要共享数据
             * 状态持久：防止服务重启导致数据丢失
             * 原子操作：避免并发推送冲突
             * 自动过期：简化代码，避免内存泄漏
             * 扩展性：Redis可以横向扩展，支持更大规模
             *
             * 所以分布式系统中，需要这种集中式的存储方案
             */
            Boolean isMember = redisTemplate.opsForSet().isMember(repeatKey, driver.getDriverId());
            if(!isMember) {
                //把订单信息推送给满足条件多个司机
                redisTemplate.opsForSet().add(repeatKey,driver.getDriverId());
                // 已推送过的司机ID队列过期时间：15分钟，超过15分钟没有接单自动取消
                redisTemplate.expire(repeatKey,
                        RedisConstant.DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME,
                        TimeUnit.MINUTES);

                NewOrderDataVo newOrderDataVo = new NewOrderDataVo();
                newOrderDataVo.setOrderId(newOrderTaskVo.getOrderId());
                newOrderDataVo.setStartLocation(newOrderTaskVo.getStartLocation());
                newOrderDataVo.setEndLocation(newOrderTaskVo.getEndLocation());
                newOrderDataVo.setExpectAmount(newOrderTaskVo.getExpectAmount());
                newOrderDataVo.setExpectDistance(newOrderTaskVo.getExpectDistance());
                newOrderDataVo.setExpectTime(newOrderTaskVo.getExpectTime());
                newOrderDataVo.setFavourFee(newOrderTaskVo.getFavourFee());
                newOrderDataVo.setDistance(driver.getDistance());
                newOrderDataVo.setCreateTime(newOrderTaskVo.getCreateTime());
                //新订单保存到司机的临时队列，Redis里面List集合
                String key = RedisConstant.DRIVER_ORDER_TEMP_LIST+driver.getDriverId();
                redisTemplate.opsForList().leftPush(key,JSONObject.toJSONString(newOrderDataVo));
                //过期时间：1分钟
                redisTemplate.expire(key,1, TimeUnit.MINUTES);
            }
        });
    }

    //获取最新订单
    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        List<NewOrderDataVo> list = new ArrayList<>();
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        long size = redisTemplate.opsForList().size(key);
        if(size > 0) {
            for(int i=0; i<size; i++) {
                String content = (String)redisTemplate.opsForList().leftPop(key);
                NewOrderDataVo newOrderDataVo = JSONObject.parseObject(content, NewOrderDataVo.class);
                list.add(newOrderDataVo);
            }
        }
        return list;
    }

    //清空队列数据
    @Override
    public Boolean clearNewOrderQueueData(Long driverId) {
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        redisTemplate.delete(key);
        return true;
    }
}
