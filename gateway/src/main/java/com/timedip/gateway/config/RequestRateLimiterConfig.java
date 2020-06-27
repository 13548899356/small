package com.timedip.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * 请求限流配置
 */
@Configuration
public class RequestRateLimiterConfig {

    /**
     * 按IP来限流
     */
    @Bean
    public KeyResolver ipAddrKeyResolver() {//JDK8的新特性——Lambda表达式
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }

    ///**
    // * 按用户限流
    // */
    //@Bean
    //KeyResolver userKeyResolver() {
    //    return exchange -> Mono.just(exchange.getRequest().getQueryParams().getFirst("user"));
    //}

    ///**
    // * 按URL限流,即以每秒内请求数按URL分组统计，超出限流的url请求都将返回429状态
    // *
    // * @return
    // */
    //@Bean
    //@Primary
    //KeyResolver apiKeyResolver() {
    //    return exchange -> Mono.just(exchange.getRequest().getPath().toString());
    //}
}
