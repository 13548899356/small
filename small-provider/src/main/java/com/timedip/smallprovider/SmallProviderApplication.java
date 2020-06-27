package com.timedip.smallprovider;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableDiscoveryClient
//开启Mapper自动扫描
@MapperScan("com.zking.nacosprovider.mapper")
//开启事务管理器
@EnableTransactionManagement
//开启动态代理
@EnableAspectJAutoProxy
public class SmallProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmallProviderApplication.class, args);
    }

}
