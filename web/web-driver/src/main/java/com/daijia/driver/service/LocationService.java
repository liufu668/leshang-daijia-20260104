package com.daijia.driver.service;

import com.daijia.model.form.map.OrderServiceLocationForm;
import com.daijia.model.form.map.UpdateDriverLocationForm;
import com.daijia.model.form.map.UpdateOrderLocationForm;

import java.util.List;

public interface LocationService {

    //更新司机位置
    Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);

    Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm);

    Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList);

}
