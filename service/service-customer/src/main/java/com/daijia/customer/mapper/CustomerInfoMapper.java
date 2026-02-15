package com.daijia.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.daijia.model.entity.customer.CustomerInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerInfoMapper extends BaseMapper<CustomerInfo> {
}
