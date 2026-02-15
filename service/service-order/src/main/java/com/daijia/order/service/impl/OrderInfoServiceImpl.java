package com.daijia.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daijia.model.entity.order.OrderInfo;
import com.daijia.model.enums.OrderStatus;
import com.daijia.model.vo.order.CurrentOrderInfoVo;
import com.daijia.order.mapper.OrderInfoMapper;
import com.daijia.order.service.OrderInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
