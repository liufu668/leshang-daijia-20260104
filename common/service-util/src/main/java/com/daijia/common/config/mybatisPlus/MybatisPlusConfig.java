package com.daijia.common.config.mybatisPlus;



import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MybatisPlus配置类
 *
 */
@Configuration
@MapperScan("com.daijia.mapper")
public class MybatisPlusConfig {


    //@Bean
    //public MybatisPlusInterceptor optimisticLockerInnerInterceptor(){
    //    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    //    //向Mybatis过滤器链中添加分页拦截器
    //    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    //    return interceptor;
    //}

    /**
     * MybatisPlus拦截器配置
     * @return 配置好的拦截器
     */

}