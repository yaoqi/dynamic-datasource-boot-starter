/*
 * Copyright 2019 ukuz90
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.ukuz.dynamic.datasource.spring.boot.autoconfigure.core;


import com.zaxxer.hikari.HikariDataSource;
import io.github.ukuz.dynamic.datasource.spring.boot.autoconfigure.loadbalance.LoadBalance;
import io.github.ukuz.dynamic.datasource.spring.boot.autoconfigure.loadbalance.ServiceInfo;
import io.github.ukuz.dynamic.datasource.spring.boot.autoconfigure.properties.DynamicDataSourceProperties;
import io.github.ukuz.dynamic.datasource.spring.boot.autoconfigure.properties.EnhancerDataSourceProperties;
import io.github.ukuz.dynamic.datasource.spring.boot.autoconfigure.strategy.RoutingStrategy;
import io.github.ukuz.dynamic.datasource.spring.boot.autoconfigure.utils.Holder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.*;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ukuz90
 * @since 2019-06-04
 */
public class DynamicRoutingDataSource extends AbstractRoutingDataSource implements DisposableBean, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private DynamicDataSourceProperties dataSourceProperties;
    private RoutingStrategy routingStrategy;

    private ConcurrentHashMap<String, Holder<ServiceInfo>> serviceInfos = new ConcurrentHashMap<>();
    private Holder<String> determineDatasourceKey = new Holder<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicRoutingDataSource.class);

    @Override
    protected Object determineCurrentLookupKey() {
        if (this.dataSourceProperties.getProperties() == null) {
            return null;
        }
        if (this.dataSourceProperties.getProperties().length == 1) {
            return this.dataSourceProperties.getProperties()[0].getName();
        }

        //直接选出数据源
        if (!StringUtils.isEmpty(determineDatasourceKey.getVal())) {
            String determineDataSourceKey = determineDatasourceKey.getVal();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("DynamicRoutingDataSource determineDatasourceKey key:【{}】", determineDataSourceKey);
            }
            return determineDataSourceKey;
        }

        Set<String> dataSourceKeys = routingStrategy.selectDataSourceKey(this.dataSourceProperties.getProperties());
        if (dataSourceKeys == null || dataSourceKeys.size() < 1) {
            throw new IllegalArgumentException("No DataSource support");
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("DynamicRoutingDataSource candidate datasource keys:【{}】", dataSourceKeys);
        }

        List<ServiceInfo> candidate = dataSourceKeys
                .stream()
                .map(name -> serviceInfos.get(name).getVal())
                .collect(Collectors.toList());


        LoadBalance loadBalance = (LoadBalance) PluginLoader.getLoader(LoadBalance.class).getPlugin(dataSourceProperties.getLoadbalance());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("DynamicRoutingDataSource loadBalance class:【{}】", loadBalance.getClass().getName());
        }
        ServiceInfo selected = loadBalance.select(candidate);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("DynamicRoutingDataSource transfer datasource key:【{}】", selected.getKey());
        }
        return selected.getKey();
    }

    @Override
    public void afterPropertiesSet() {
        initDataSourceMap();
        super.afterPropertiesSet();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext)applicationContext).addApplicationListener(new DetermineDataSourceListener());
            ((ConfigurableApplicationContext)applicationContext).addApplicationListener(new ClearDetermineDataSourceListenr());
        }

    }

    @Override
    public void destroy() {

    }

    private void initDataSourceMap() {
        dataSourceProperties = applicationContext.getBean(DynamicDataSourceProperties.class);
        if (dataSourceProperties.getProperties() != null) {
            Map<Object, Object> targetDataSource = new HashMap<>(dataSourceProperties.getProperties().length);
            Stream.of(dataSourceProperties.getProperties())
                    .forEach(dsp -> {
                        if (dsp.getType() == null) {
                            dsp.setType(HikariDataSource.class);
                        }
                        DataSource ds = dsp.initializeDataSourceBuilder().build();
                        targetDataSource.put(dsp.getName(), ds);

                        Holder holder = this.serviceInfos.get(dsp.getName());
                        if (holder == null) {
                            this.serviceInfos.putIfAbsent(dsp.getName(), new Holder<>());
                            holder = this.serviceInfos.get(dsp.getName());
                        }
                        holder.setVal(new ServiceInfo(dsp.getName(), dsp.getWeight()));
                    });
            this.setTargetDataSources(targetDataSource);

        }
        routingStrategy = applicationContext.getBean(RoutingStrategy.class);
    }

    private boolean checkDataSourceKeyValid(String key) {
        DynamicDataSourceProperties properties = applicationContext.getBean(DynamicDataSourceProperties.class);
        if (properties.getProperties() == null) {
            return false;
        }
        for (EnhancerDataSourceProperties prop : properties.getProperties()) {
            if (prop.getName().equals(key)) {
                return true;
            }
        }
        return false;
    }

    static class DetermineDataSourceEvent extends ApplicationEvent {

        private final String datasourceKey;

        /**
         * Create a new ApplicationEvent.
         *
         * @param source the object on which the event initially occurred (never {@code null})
         */
        public DetermineDataSourceEvent(String source) {
            super(source);
            this.datasourceKey = source;
        }

        public String getDatasourceKey() {
            return datasourceKey;
        }

    }

    static class ClearDetermineDataSourceEvent {

    }

    class DetermineDataSourceListener implements ApplicationListener<DetermineDataSourceEvent> {

        @Override
        public void onApplicationEvent(DetermineDataSourceEvent event) {
            if (!checkDataSourceKeyValid(event.getDatasourceKey())) {
                LOGGER.error("DynamicRoutingDataSource received invalid datasourceKey:{}", event.getDatasourceKey());
                return;
            }
            determineDatasourceKey.setVal(event.getDatasourceKey());
        }
    }

    class ClearDetermineDataSourceListenr implements ApplicationListener<PayloadApplicationEvent> {

        @Override
        public void onApplicationEvent(PayloadApplicationEvent event) {
            if (!(event.getPayload() instanceof ClearDetermineDataSourceEvent)) {
                return;
            }
            determineDatasourceKey.setVal(null);
        }
    }
}
