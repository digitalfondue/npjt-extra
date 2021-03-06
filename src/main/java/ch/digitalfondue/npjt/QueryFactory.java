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

import ch.digitalfondue.npjt.mapper.*;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class QueryFactory<T> implements FactoryBean<T> {

    private final Class<T> targetInterface;
    private final String activeDB;

    private DataSource dataSource;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private List<ColumnMapperFactory> additionalColumnMapperFactories;
    private List<ParameterConverter> additionalParameterConverters;

    public QueryFactory(Class<T> targetInterface, String activeDB) {
        this.targetInterface = targetInterface;
        this.activeDB = activeDB;
    }

    public List<ColumnMapperFactory> getDefaultFactories() {
        return new ArrayList<>(Arrays.asList(
                new DefaultMapper.Factory(),
                new EnumMapper.Factory(),
                new LocalDateMapper.Factory(),
                new LocalDateTimeMapper.Factory(),
                new InstantMapper.Factory(),
                new ZonedDateTimeMapper.Factory())
        );
    }

    public List<ParameterConverter> getDefaultParameterConverters() {
        return new ArrayList<>(Arrays.asList(
                new DefaultMapper.Converter(),
                new EnumMapper.Converter(),
                new LocalDateMapper.Converter(),
                new LocalDateTimeMapper.Converter(),
                new InstantMapper.Converter(),
                new ZonedDateTimeMapper.Converter())
        );
    }

    public static <T> T from(Class<T> clazz, String activeDB, DataSource dataSource) {
        return from(clazz, activeDB, dataSource, null, null);
    }

    public static <T> T from(Class<T> clazz, String activeDB,
                             DataSource dataSource,
                             List<ColumnMapperFactory> additionalColumnMappers, List<ParameterConverter> additionalParameterConverters) {
        QueryFactory<T> qf = new QueryFactory<>(clazz, activeDB);
        qf.setAdditionalColumnMapperFactories(additionalColumnMappers);
        qf.setAdditionalParameterConverters(additionalParameterConverters);
        qf.setDataSource(dataSource);
        return qf.getObject();
    }

    @Override
    public T getObject() {
        return from(targetInterface);
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
    public void setJdbc(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Autowired(required = false)
    public void setAdditionalColumnMapperFactories(List<ColumnMapperFactory> additionalColumnMapperFactories) {
        this.additionalColumnMapperFactories = additionalColumnMapperFactories;
    }

    @Autowired(required = false)
    public void setAdditionalParameterConverters(List<ParameterConverter> additionalParameterConverters) {
        this.additionalParameterConverters = additionalParameterConverters;
    }
    //

    static class QueryTypeAndQuery {
        final QueryType type;
        final String query;
        final Class<?> rowMapperClass;

        QueryTypeAndQuery(QueryType type, String query, Class<?> rowMapperClass) {
            this.type = type;
            this.query = query;
            this.rowMapperClass = rowMapperClass;
        }
    }


    private QueryTypeAndQuery extractQueryAnnotation(Class<?> clazz, Method method) {

        Query q = method.getAnnotation(Query.class);
        QueriesOverride qs = method.getAnnotation(QueriesOverride.class);

        // only one @Query annotation, thus we return the value without checking the database
        if (qs == null) {
            return new QueryTypeAndQuery(q.type(), q.value(), q.mapper());
        }

        for (QueryOverride query : qs.value()) {
            if (query.db().equals(activeDB)) {
                return new QueryTypeAndQuery(q.type(), query.value(), query.mapper());
            }
        }

        return new QueryTypeAndQuery(q.type(), q.value(), q.mapper());
    }

    //from https://rmannibucau.wordpress.com/2014/03/27/java-8-default-interface-methods-and-jdk-dynamic-proxies/
    private static final Constructor<MethodHandles.Lookup> LOOKUP_CONSTRUCTOR;
    private static final Method PRIVATE_LOOKUP_IN = ReflectionUtils.findMethod(MethodHandles.class, "privateLookupIn", Class.class, MethodHandles.Lookup.class);

    static {
        try {
            if(PRIVATE_LOOKUP_IN == null) {
                LOOKUP_CONSTRUCTOR = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                if (!LOOKUP_CONSTRUCTOR.isAccessible()) { //TODO: is deprecated
                    LOOKUP_CONSTRUCTOR.setAccessible(true);
                }
            } else {
                LOOKUP_CONSTRUCTOR = null;
            }
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(e);
        }
    }


    @SuppressWarnings("unchecked")
    private <T> T from(final Class<T> clazz) {

        SortedSet<ColumnMapperFactory> columnMapperFactories = new TreeSet<>(Comparator.comparingInt(ColumnMapperFactory::order).thenComparing(Objects::hashCode));
        columnMapperFactories.addAll(getDefaultFactories());
        if (additionalColumnMapperFactories != null) {
            columnMapperFactories.addAll(additionalColumnMapperFactories);
        }

        SortedSet<ParameterConverter> parameterConverters = new TreeSet<>(Comparator.comparingInt(ParameterConverter::order).thenComparing(Objects::hashCode));
        parameterConverters.addAll(getDefaultParameterConverters());
        if (additionalParameterConverters != null) {
            parameterConverters.addAll(additionalParameterConverters);
        }

        NamedParameterJdbcTemplate jdbc = namedParameterJdbcTemplate == null ? new NamedParameterJdbcTemplate(dataSource) : namedParameterJdbcTemplate;

        return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
                new Class[] { clazz }, (proxy, method, args) -> {
                    boolean hasAnnotation = method.getAnnotation(Query.class) != null;
                    if(hasAnnotation) {
                        QueryTypeAndQuery qs = extractQueryAnnotation(clazz, method);
                        return qs.type.apply(qs, jdbc, method, args, columnMapperFactories, parameterConverters);
                    } else if(method.getReturnType().equals(NamedParameterJdbcTemplate.class) && args == null) {
                        return jdbc;
                    } else if(method.isDefault()) {
                        final Class<?> declaringClass = method.getDeclaringClass();
                        final MethodHandle handle;
                        if(PRIVATE_LOOKUP_IN != null) {
                            MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
                            handle = MethodHandles.lookup().findSpecial(declaringClass, method.getName(), methodType, declaringClass);
                        } else {
                            handle = LOOKUP_CONSTRUCTOR.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE).unreflectSpecial(method, declaringClass);
                        }
                        return handle.bindTo(proxy).invokeWithArguments(args);
                    } else if (method.getDeclaringClass().equals(Object.class)) {
                        String name = method.getName();
                        switch (name) {
                            case "equals": return proxy == args[0];
                            case "hashCode": return System.identityHashCode(proxy);
                            case "toString": return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                            default: throw new IllegalStateException(String.valueOf(method));
                        }
                    } else {
                        throw new IllegalArgumentException(String.format("missing @Query annotation for method %s in interface %s", method.getName(),	clazz.getSimpleName()));
                    }
                }
        );
    }

}
