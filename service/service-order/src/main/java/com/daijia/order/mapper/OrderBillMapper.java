package com.daijia.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.daijia.model.entity.order.OrderBill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;


@Mapper
public interface OrderBillMapper extends BaseMapper<OrderBill> {

    void updateCouponAmount(@Param("orderId") Long orderId, @Param("couponAmount") BigDecimal couponAmount);
}
