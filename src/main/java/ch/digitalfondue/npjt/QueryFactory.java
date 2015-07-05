/**
 * Copyright (C) 2015 digitalfondue (info@digitalfondue.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.digitalfondue.npjt;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import ch.digitalfondue.npjt.columnmapper.BooleanColumnMapper;
import ch.digitalfondue.npjt.columnmapper.ColumnMapperFactory;
import ch.digitalfondue.npjt.columnmapper.DefaultColumnMapper;
import ch.digitalfondue.npjt.columnmapper.EnumColumnMapper;
import ch.digitalfondue.npjt.columnmapper.ZonedDateTimeColumnMapper;
import ch.digitalfondue.npjt.parameterconverter.DefaultParameterConverter;
import ch.digitalfondue.npjt.parameterconverter.EnumParameterConverter;
import ch.digitalfondue.npjt.parameterconverter.ParameterConverter;
import ch.digitalfondue.npjt.parameterconverter.ZonedDateTimeParameterConverter;

public class QueryFactory {
	
	private static final boolean zonedDateTimeAvailable = ClassUtils.isPresent("java.time.ZonedDateTime", QueryFactory.class.getClassLoader());
	

	private final String activeDb;
	private final NamedParameterJdbcTemplate jdbc;
	private final TreeSet<ColumnMapperFactory> columnMapperFactories = new TreeSet<>(new Comparator<ColumnMapperFactory>() {
		@Override
		public int compare(ColumnMapperFactory o1, ColumnMapperFactory o2) {
			return Integer.compare(o1.order(), o2.order());
		}
	});
	private final TreeSet<ParameterConverter> parameterConverters = new TreeSet<>(new Comparator<ParameterConverter>() {
		@Override
		public int compare(ParameterConverter o1, ParameterConverter o2) {
			return Integer.compare(o1.order(), o2.order());
		}
	});
	
	//default mappers and converters
	{
		columnMapperFactories.add(new BooleanColumnMapper.BooleanColumnMapperFactory());
		columnMapperFactories.add(new EnumColumnMapper.EnumColumnMapperFactory());
		columnMapperFactories.add(new DefaultColumnMapper.DefaultColumnMapperFactory());
		
		parameterConverters.add(new DefaultParameterConverter());
		parameterConverters.add(new EnumParameterConverter());
		
		if(zonedDateTimeAvailable) {
			columnMapperFactories.add(new ZonedDateTimeColumnMapper.ZonedDateTimeColumnMapperFactory());
			parameterConverters.add(new ZonedDateTimeParameterConverter());
		}
	}

	public QueryFactory(String activeDB, NamedParameterJdbcTemplate jdbc) {
		this.activeDb = activeDB;
		this.jdbc = jdbc;
	}
	
	public QueryFactory(String activeDB, DataSource dataSource) {
		this.activeDb = activeDB;
		this.jdbc = new NamedParameterJdbcTemplate(dataSource);
	}
	
	public QueryFactory(String activeDB, JdbcTemplate jdbcTemplate) {
		this.activeDb = activeDB;
		this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
	}

	private static class QueryTypeAndQuery {
		private final QueryType type;
		private final String query;

		QueryTypeAndQuery(QueryType type, String query) {
			this.type = type;
			this.query = query;
		}
	}
	
	public QueryFactory addColumnMapperFactory(ColumnMapperFactory columnMapperFactory) {
		columnMapperFactories.add(columnMapperFactory);
		return this;
	}
	
	public QueryFactory emptyColumnMapperFactories() {
		columnMapperFactories.clear();
		return this;
	}
	
	public QueryFactory addParameterConverters(ParameterConverter parameterConverter) {
		parameterConverters.add(parameterConverter);
		return this;
	}
	
	public QueryFactory emptyParameterConverters() {
		parameterConverters.clear();
		return this;
	}


	private QueryTypeAndQuery extractQueryAnnotation(Class<?> clazz, Method method) {
		
		Query q = method.getAnnotation(Query.class);
		QueriesOverride qs = method.getAnnotation(QueriesOverride.class);

		// only one @Query annotation, thus we return the value without checking the database
		if (qs == null) {
			return new QueryTypeAndQuery(q.type(), q.value());
		}

		for (QueryOverride query : qs.value()) {
			if (query.db().equals(activeDb)) {
				return new QueryTypeAndQuery(q.type(), query.value());
			}
		}

		return new QueryTypeAndQuery(q.type(), q.value());
	}
	
	//from https://rmannibucau.wordpress.com/2014/03/27/java-8-default-interface-methods-and-jdk-dynamic-proxies/
	
	private static final Method IS_DEFAULT_METHOD = ReflectionUtils.findMethod(Method.class, "isDefault");
	private static final Constructor<MethodHandles.Lookup> LOOKUP_CONSTRUCTOR;
	
	static {
		try {
			LOOKUP_CONSTRUCTOR = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
			if (!LOOKUP_CONSTRUCTOR.isAccessible()) {
				LOOKUP_CONSTRUCTOR.setAccessible(true);
			}
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException(e);
		} 
	}
	

	@SuppressWarnings("unchecked")
	public <T> T from(final Class<T> clazz) {
		
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
				new Class[] { clazz }, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						boolean hasAnnotation = method.getAnnotation(Query.class) != null;
						if(hasAnnotation) {
							QueryTypeAndQuery qs = extractQueryAnnotation(clazz, method);
							return qs.type.apply(qs.query, jdbc, method, args, columnMapperFactories, parameterConverters);
						} else if(method.getReturnType().equals(NamedParameterJdbcTemplate.class) && args == null) {
							return jdbc;
						} else if(IS_DEFAULT_METHOD != null && (boolean) IS_DEFAULT_METHOD.invoke(method)) {
							final Class<?> declaringClass = method.getDeclaringClass();
							return LOOKUP_CONSTRUCTOR.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE).unreflectSpecial(method, declaringClass).bindTo(proxy).invokeWithArguments(args);
						} else {
							throw new IllegalArgumentException(String.format("missing @Query annotation for method %s in interface %s", method.getName(),	clazz.getSimpleName()));
						}
						
					}
				}

		);
	}

}
