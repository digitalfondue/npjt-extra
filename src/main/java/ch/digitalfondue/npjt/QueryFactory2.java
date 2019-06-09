/**
 * Copyright © 2019 digitalfondue (info@digitalfondue.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.digitalfondue.npjt;

import ch.digitalfondue.npjt.mapper.ColumnMapperFactory;
import ch.digitalfondue.npjt.mapper.ParameterConverter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.List;

public class QueryFactory2<T> implements FactoryBean<T> {

    private final Class<T> targetInterface;
    private final String activeDB;


    private DataSource dataSource;
    private List<ColumnMapperFactory> additionalColumnMapperFactories;
    private List<ParameterConverter> additionalParameterConverters;

    public QueryFactory2(Class<T> targetInterface, String activeDB) {
        this.targetInterface = targetInterface;
        this.activeDB = activeDB;
    }

    @Override
    public T getObject() {
        QueryFactory qf = new QueryFactory(activeDB, dataSource);

        if (additionalColumnMapperFactories != null) {
            additionalColumnMapperFactories.forEach(qf::addColumnMapperFactory);
        }

        if (additionalParameterConverters != null) {
            additionalParameterConverters.forEach(qf::addParameterConverters);
        }

        return qf.from(targetInterface);
    }

    @Override
    public Class<T> getObjectType() {
        return targetInterface;
    }


    //
    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Autowired(required = false)
    public void setAdditionalColumnMapperFactories(List<ColumnMapperFactory> additionalColumnMapperFactories) {
        this.additionalColumnMapperFactories = additionalColumnMapperFactories;
    }

    @Autowired(required = false)
    public void setAdditionalParameterConverters(List<ParameterConverter> additionalParameterConverters) {
        this.additionalParameterConverters = additionalParameterConverters;
    }
}
