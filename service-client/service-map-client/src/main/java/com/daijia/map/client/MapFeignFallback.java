package com.daijia.map.client;

import com.daijia.common.result.Result;
import com.daijia.model.form.map.CalculateDrivingLineForm;
import com.daijia.model.vo.map.DrivingLineVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MapFeignFallback implements MapFeignClient {

    @Override
    public Result<DrivingLineVo> calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        log.error("【降级】地图服务不可用，calculateDrivingLine 调用失败");
        return Result.fail(null);
    }
}
