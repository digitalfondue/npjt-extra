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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;

public class QueryFactory2<T> implements FactoryBean<T>, BeanFactoryAware, InitializingBean {

    private final Class<T> targetInterface;
    private final String activeDB;

    private DataSource dataSource;

    public QueryFactory2(Class<T> targetInterface, String activeDB) {
        this.targetInterface = targetInterface;
        this.activeDB = activeDB;
    }

    @Override
    public T getObject() {
        return new QueryFactory(activeDB, dataSource).from(targetInterface);
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


    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        System.err.println("set factory bean");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.err.println("after property set");
        System.err.println("Data source is" + dataSource);
    }
}
