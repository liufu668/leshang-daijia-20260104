package com.daijia.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 全局认证过滤器
 * 用于实现基于IP黑名单的访问控制
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 过滤请求，检查客户端IP是否在黑名单中
     *
     * @param exchange 服务器web交换对象，包含请求和响应信息
     * @param chain    过滤器链，用于继续处理请求
     * @return Mono<Void> 响应式处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取客户端IP地址
        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        
        // 检查Redis中是否存在该IP的黑名单记录
        Boolean isBlacklisted = redisTemplate.opsForSet().isMember("gateway:blacklist", ip);
        
        // 如果IP在黑名单中，则拒绝访问并返回403状态码
        if (Boolean.TRUE.equals(isBlacklisted)) {
            log.warn("IP {} 在黑名单中，拒绝访问", ip);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        
        // IP不在黑名单中，继续执行过滤器链
        return chain.filter(exchange);
    }

    /**
     * 设置过滤器的执行顺序
     * 即使当前只有一个过滤器，实现 Ordered 接口并指定顺序也是最佳实践。
     * 这为未来添加其他全局过滤器（如日志、限流等）预留了扩展空间，
     * 确保认证逻辑可以在其他逻辑之前或之后按预期执行。
     * 返回值越小，优先级越高。
     *
     * @return 过滤器顺序值
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
