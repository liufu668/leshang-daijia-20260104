package com.daijia.driver.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;


/**
 * 全局自定义线程池配置
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {

        //动态获取服务器核数
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                processors+1, // 核心线程个数 io:2n ,cpu: n+1  n:内核数据
                processors+1,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(3),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        ) {
            @Override
            public void execute(Runnable command) {
                // 提交前记录状态
                log.info("【线程池监控】提交任务前 | 活跃线程:{}/{} | 队列大小:{}/{} | 完成任务:{}",
                        this.getActiveCount(),
                        this.getMaximumPoolSize(),
                        this.getQueue().size(),
                        this.getQueue().remainingCapacity() + this.getQueue().size(),
                        this.getCompletedTaskCount());
                
                try {
                    super.execute(command);
                    // 提交成功后记录
                    log.debug("【线程池监控】任务提交成功 | 当前线程池大小:{}", this.getPoolSize());
                } catch (RejectedExecutionException e) {
                    // 拒绝时记录告警
                    log.error("【线程池监控】任务被拒绝！| 活跃线程:{}/{} | 队列已满:{}/{} | 拒绝策略:AbortPolicy",
                            this.getActiveCount(),
                            this.getMaximumPoolSize(),
                            this.getQueue().size(),
                            this.getQueue().remainingCapacity() + this.getQueue().size());
                    throw e;
                }
            }

            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                // 任务执行前回调（在工作线程中执行）
                log.debug("【线程池监控】开始执行任务 | 线程名:{} | 活跃线程:{}", 
                        t.getName(), 
                        this.getActiveCount());
                super.beforeExecute(t, r);
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                // 任务执行后回调
                if (t != null) {
                    log.error("【线程池监控】任务执行异常 | 活跃线程:{}", 
                            this.getActiveCount(), t);
                } else {
                    log.debug("【线程池监控】任务执行完成 | 活跃线程:{} | 完成任务总数:{}", 
                            this.getActiveCount(),
                            this.getCompletedTaskCount());
                }
                super.afterExecute(r, t);
            }
        };
        
        // 注册关闭钩子，打印最终统计
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("【线程池统计】服务关闭 | 总提交任务:{} | 完成任务:{} | 峰值线程:{}",
                    threadPoolExecutor.getTaskCount(),
                    threadPoolExecutor.getCompletedTaskCount(),
                    threadPoolExecutor.getLargestPoolSize());
        }));
        
        return threadPoolExecutor;
    }
}
