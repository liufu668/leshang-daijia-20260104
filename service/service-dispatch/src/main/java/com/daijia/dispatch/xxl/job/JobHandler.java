package com.daijia.dispatch.xxl.job;

import com.daijia.dispatch.mapper.XxlJobLogMapper;
import com.daijia.dispatch.service.NewOrderService;
import com.daijia.model.entity.dispatch.XxlJobLog;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 任务类(存放业务逻辑代码)
 */
@Component
@RequiredArgsConstructor
public class JobHandler {

    private final XxlJobLogMapper xxlJobLogMapper;
    private final NewOrderService newOrderService;

    @XxlJob("newOrderTaskHandler")
    public void newOrderTaskHandler() {

        //记录任务调度日志
        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        long startTime = System.currentTimeMillis();

        try {
            //执行任务：搜索附近代驾司机
            newOrderService.executeTask(XxlJobHelper.getJobId());

            //成功状态
            xxlJobLog.setStatus(1);
        } catch (Exception e) {
            //失败状态
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
            e.printStackTrace();
        } finally {
            long times = System.currentTimeMillis()- startTime;
            //TODO 完善long
            xxlJobLog.setTimes((int)times);
            xxlJobLogMapper.insert(xxlJobLog);
        }
    }
}
