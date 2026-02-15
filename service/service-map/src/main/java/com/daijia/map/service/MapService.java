package com.daijia.map.service;


import com.daijia.model.form.map.CalculateDrivingLineForm;
import com.daijia.model.vo.map.DrivingLineVo;

public interface MapService {

    //计算驾驶线路
    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);
}
