package com.daijia.driver.service.impl;

import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.ResultCodeEnum;
import com.daijia.driver.client.DriverInfoFeignClient;
import com.daijia.driver.service.LocationService;
import com.daijia.map.client.LocationFeignClient;
import com.daijia.model.entity.driver.DriverSet;
import com.daijia.model.form.map.UpdateDriverLocationForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        //根据司机id获取司机个性化设置信息
        //        Long driverId = updateDriverLocationForm.getDriverId();
        //        Result<DriverSet> driverSetResult = driverInfoFeignClient.getDriverSet(driverId);
        //        DriverSet driverSet = driverSetResult.getData();
        //
        //        //判断：如果司机开始接单，更新位置信息
        //        if(driverSet.getServiceStatus() == 1) {
        //            Result<Boolean> booleanResult = locationFeignClient.updateDriverLocation(updateDriverLocationForm);
        //            return booleanResult.getData();
        //        } else {
        //            //没有接单
        //            throw new GuiguException(ResultCodeEnum.NO_START_SERVICE);
        //        }
    }
}
