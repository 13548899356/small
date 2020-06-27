package com.timedip.gateway.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 1. 保存Gateway(网关)中与nacos相关的属性
 * 2. 这些信息是自定义配置属性，它们保存在配置文件application.yml中
 * 3. 通过@ConfigurationProperties注解读取
 */
//@ConfigurationProperties(prefix = "gateway.nacos", ignoreUnknownFields = true)
@Configuration
@Data
public class GatewayNacosProperties {

    @Value("${gateway.nacos.server-addr}")
    private String serverAddr;

//    @Value("${gateway.nacos.namespace}")
//    private String namespace;

    @Value("${gateway.nacos.data-id}")
    private String dataId;

    @Value("${gateway.nacos.group}")
    private String group;

}
