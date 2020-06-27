package com.timedip.gateway.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.timedip.gateway.entity.FilterEntity;
import com.timedip.gateway.entity.PredicateEntity;
import com.timedip.gateway.entity.RouteEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * 此类实现了Spring Cloud Gateway + nacos 的动态路由，它实现一个Spring提供的事件推送接口ApplicationEventPublisherAware
 */
@Component
public class DynamicRoutingConfig implements ApplicationEventPublisherAware {

    private final Logger logger = LoggerFactory.getLogger(DynamicRoutingConfig.class);

    //private static final String DATA_ID = "zuul-refresh-dev.json";
    //private static final String Group = "DEFAULT_GROUP";

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;

    @Autowired
    private GatewayNacosProperties gatewayNacosProperties;

    private ApplicationEventPublisher applicationEventPublisher;


    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * 这个方法主要负责监听Nacos的配置变化，这里先使用参数构建一个ConfigService，再使用ConfigService开启一个监听，
     * 并且在监听的方法中刷新路由信息。
     *
     * @throws NacosException
     */
    @Bean
    public void refreshRouting() throws NacosException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, gatewayNacosProperties.getServerAddr());
//        properties.put(PropertyKeyConst.NAMESPACE, gatewayNacosProperties.getNamespace());
        ConfigService configService = NacosFactory.createConfigService(properties);

        //解决无法读取nacos原有配置而需要更新才能读取配置BUG  5000:等待的最长时间
        String json=configService.getConfig(gatewayNacosProperties.getDataId(),gatewayNacosProperties.getGroup(),5000);
        this.parsejson(json);
        System.out.println("json:"+json);


        configService.addListener(gatewayNacosProperties.getDataId(), gatewayNacosProperties.getGroup(), new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
//                parsejson(configInfo);
                logger.info(configInfo);
                boolean refreshGatewayRoute = JSONObject.parseObject(configInfo).getBoolean("refreshGatewayRoute");

                if (refreshGatewayRoute) {
                    List<RouteEntity> list = JSON.parseArray(JSONObject.parseObject(configInfo).getString("routeList")).toJavaList(RouteEntity.class);

                    for (RouteEntity route : list) {
                        update(assembleRouteDefinition(route));
                    }
                } else {
                    logger.info("路由未发生变更");
                }

            }
        });
    }

    /**
     *解析json获取路由信息
     * @param json
     */
    public void parsejson(String json){
        logger.info(json);
        boolean refreshGatewayRoute = JSONObject.parseObject(json).getBoolean("refreshGatewayRoute");

        if (refreshGatewayRoute) {
            List<RouteEntity> list = JSON.parseArray(JSONObject.parseObject(json).getString("routeList")).toJavaList(RouteEntity.class);

            for (RouteEntity route : list) {
                update(assembleRouteDefinition(route));
            }
        } else {
            logger.info("路由未发生变更");
        }

    }


    /**
     * 路由更新
     *
     * @param routeDefinition
     * @return
     */
    public void update(RouteDefinition routeDefinition) {

        try {
            this.routeDefinitionWriter.delete(Mono.just(routeDefinition.getId()));
            logger.info("路由更新成功");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        try {
            routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
            this.applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
            logger.info("路由更新成功");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public RouteDefinition assembleRouteDefinition(RouteEntity routeEntity) {

        RouteDefinition definition = new RouteDefinition();

        // ID
        definition.setId(routeEntity.getId());

        // Predicates
        List<PredicateDefinition> pdList = new ArrayList<>();
        for (PredicateEntity predicateEntity : routeEntity.getPredicates()) {
            PredicateDefinition predicateDefinition = new PredicateDefinition();
            predicateDefinition.setArgs(predicateEntity.getArgs());
            predicateDefinition.setName(predicateEntity.getName());
            pdList.add(predicateDefinition);
        }
        definition.setPredicates(pdList);

        // Filters
        List<FilterDefinition> fdList = new ArrayList<>();
        for (FilterEntity filterEntity : routeEntity.getFilters()) {
            FilterDefinition filterDefinition = new FilterDefinition();
            filterDefinition.setArgs(filterEntity.getArgs());
            filterDefinition.setName(filterEntity.getName());
            fdList.add(filterDefinition);
        }
        definition.setFilters(fdList);

        // URI
        URI uri = UriComponentsBuilder.fromUriString(routeEntity.getUri()).build().toUri();
        definition.setUri(uri);

        return definition;
    }
}