package com.daijia.driver.service;

import com.daijia.model.form.map.UpdateDriverLocationForm;

public interface LocationService {

    //更新司机位置
    Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);

}
