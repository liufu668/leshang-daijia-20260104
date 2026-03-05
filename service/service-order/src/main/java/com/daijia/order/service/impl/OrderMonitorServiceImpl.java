package com.daijia.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daijia.model.entity.order.OrderMonitor;
import com.daijia.model.entity.order.OrderMonitorRecord;
import com.daijia.order.mapper.OrderMonitorMapper;
import com.daijia.order.repository.OrderMonitorRecordRepository;
import com.daijia.order.service.OrderMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderMonitorServiceImpl extends ServiceImpl<OrderMonitorMapper, OrderMonitor> implements OrderMonitorService {

    private final OrderMonitorRecordRepository orderMonitorRecordRepository;
    private final OrderMonitorMapper orderMonitorMapper;


    @Override
    public Boolean saveOrderMonitorRecord(OrderMonitorRecord orderMonitorRecord) {
        orderMonitorRecordRepository.save(orderMonitorRecord);
        return true;
    }


    @Override
    public Long saveOrderMonitor(OrderMonitor orderMonitor) {
        orderMonitorMapper.insert(orderMonitor);
        return orderMonitor.getId();
    }

    @Override
    public OrderMonitor getOrderMonitor(Long orderId) {
        return this.getOne(new LambdaQueryWrapper<OrderMonitor>().eq(OrderMonitor::getOrderId, orderId));
    }

    @Override
    public Boolean updateOrderMonitor(OrderMonitor orderMonitor) {
        return this.updateById(orderMonitor);
    }

}
