package com.daijia.dispatch.client;

import com.daijia.common.result.Result;
import com.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.daijia.model.vo.order.NewOrderDataVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class NewOrderFeignFallback implements NewOrderFeignClient {

    @Override
    public Result<Long> addAndStartTask(NewOrderTaskVo newOrderDispatchVo) {
        log.error("【降级】调度服务不可用，addAndStartTask 调用失败");
        return Result.fail(null);
    }

    @Override
    public Result<List<NewOrderDataVo>> findNewOrderQueueData(Long driverId) {
        log.error("【降级】调度服务不可用，findNewOrderQueueData 调用失败, driverId:{}", driverId);
        return Result.fail(null);
    }

    @Override
    public Result<Boolean> clearNewOrderQueueData(Long driverId) {
        log.error("【降级】调度服务不可用，clearNewOrderQueueData 调用失败");
        return Result.fail(false);
    }
}
