package com.daijia.driver.service.impl;

import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.driver.client.DriverInfoFeignClient;
import com.daijia.driver.service.LocationService;
import com.daijia.map.client.LocationFeignClient;
import com.daijia.model.entity.driver.DriverSet;
import com.daijia.model.form.map.OrderServiceLocationForm;
import com.daijia.model.form.map.UpdateDriverLocationForm;
import com.daijia.model.form.map.UpdateOrderLocationForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationFeignClient locationFeignClient;
    private final DriverInfoFeignClient driverInfoFeignClient;

    //更新司机位置
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        //开启接单了才能更新司机接单位置
        DriverSet driverSet = driverInfoFeignClient.getDriverSet(updateDriverLocationForm.getDriverId()).getData();
        if(driverSet.getServiceStatus().intValue() == 1) {
            return locationFeignClient.updateDriverLocation(updateDriverLocationForm).getData();
        } else {
            throw new GuiguException(ResultCodeEnum.NO_START_SERVICE);
        }
    }

    // 司乘同显,更新订单位置到缓存中
    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        return locationFeignClient.updateOrderLocationToCache(updateOrderLocationForm).getData();
    }

    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList) {
        return locationFeignClient.saveOrderServiceLocation(orderLocationServiceFormList).getData();
    }

}
