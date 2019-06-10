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
package io.github.ukuz.dynamic.datasource.spring.boot.autoconfigure;

import io.github.ukuz.dynamic.datasource.spring.boot.autoconfigure.core.MultipleDataSourceSchemaInitializer;
import io.github.ukuz.dynamic.datasource.spring.boot.autoconfigure.jdbc.mybatis.MybatisAutoConfiguration;
import io.github.ukuz.dynamic.datasource.spring.boot.autoconfigure.jdbc.springjpa.RepositoryAutoProxyCreator;
import io.github.ukuz.dynamic.datasource.spring.boot.autoconfigure.properties.DynamicDataSourceProperties;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * @author ukuz90
 * @since 2019-06-04
 */
@Configuration
@ConditionalOnProperty(name = "dynamic.datasource.enable", matchIfMissing = true)
@EnableConfigurationProperties(DynamicDataSourceProperties.class)
public class DynamicDataSourceAutoConfiguration {

    @Bean
    @ConditionalOnBean(SqlSessionFactory.class)
    @ConditionalOnProperty(name = "dynamic.datasource.routing-strategy", havingValue = "dboperation", matchIfMissing = true)
    public MybatisAutoConfiguration mybatisAutoConfiguration() {
        return new MybatisAutoConfiguration();
    }

    @Bean
    @ConditionalOnClass(RepositoryQuery.class)
    @ConditionalOnProperty(name = "dynamic.datasource.routing-strategy", havingValue = "dboperation", matchIfMissing = true)
    public RepositoryAutoProxyCreator autoProxyCreator() {
        return new RepositoryAutoProxyCreator();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.jpa.hibernate.ddl-auto")
    @ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
    public MultipleDataSourceSchemaInitializer multipleDataSourceSchemaInitializer() {
        return new MultipleDataSourceSchemaInitializer();
    }

}
