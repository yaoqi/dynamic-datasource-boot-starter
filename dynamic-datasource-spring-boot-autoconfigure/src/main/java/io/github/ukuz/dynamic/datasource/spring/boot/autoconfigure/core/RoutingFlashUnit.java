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

import io.github.ukuz.dynamic.datasource.spring.boot.autoconfigure.jdbc.CrudType;

/**
 * @author ukuz90
 * @since 2019-06-05
 */
public class RoutingFlashUnit {

    private final CrudType crudType;
    private final String tag;

    private RoutingFlashUnit(CrudType crudType, String tag) {
        this.crudType = crudType;
        this.tag = tag;
    }

    public static class Builder {

        private CrudType crudType;
        private String tag;

        public Builder crudType(CrudType crudType) {
            this.crudType = crudType;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public RoutingFlashUnit build() {
            return new RoutingFlashUnit(crudType, tag);
        }

    }

    public CrudType getCrudType() {
        return crudType;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return "RoutingFlashUnit{" +
                "crudType=" + crudType +
                ", tag='" + tag + '\'' +
                '}';
    }
}
