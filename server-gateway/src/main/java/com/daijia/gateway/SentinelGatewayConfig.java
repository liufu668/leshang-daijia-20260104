package com.daijia.gateway;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class SentinelGatewayConfig {

    @PostConstruct
    public void init() {
        initGatewayRules();
        initCustomBlockHandler();
    }

    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 乘客端路由：QPS 500（预估/查询多，流量大）
        rules.add(new GatewayFlowRule("web-customer")
                .setCount(200)
                .setIntervalSec(1));

        // 司机端路由：QPS 300（抢单写操作多，阈值低一些）
        rules.add(new GatewayFlowRule("web-driver")
                .setCount(200)
                .setIntervalSec(1));

        // 支付路由：QPS 200（涉及资金，最严格）
        rules.add(new GatewayFlowRule("service-payment")
                .setCount(200)
                .setIntervalSec(1));

        GatewayRuleManager.loadRules(rules);
    }

    private void initCustomBlockHandler() {
        BlockRequestHandler blockHandler = new BlockRequestHandler() {
            @Override
            public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable throwable) {
                return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{\"code\":505,\"message\":\"系统繁忙，请稍后再试\",\"data\":null}");
            }
        };
        GatewayCallbackManager.setBlockHandler(blockHandler);
    }
}
