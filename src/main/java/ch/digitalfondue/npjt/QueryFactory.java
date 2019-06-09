/**
 * Copyright © 2015 digitalfondue (info@digitalfondue.ch)
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.sql.DataSource;

import ch.digitalfondue.npjt.mapper.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.ReflectionUtils;


public class QueryFactory {

	private final String activeDb;
	private final NamedParameterJdbcTemplate jdbc;
	private final ClassReferencedSortedSet<ColumnMapperFactory> columnMapperFactories = new ClassReferencedSortedSet<>(Comparator.comparingInt(ColumnMapperFactory::order));
	private final ClassReferencedSortedSet<ParameterConverter> parameterConverters = new ClassReferencedSortedSet<>(Comparator.comparingInt(ParameterConverter::order));

	//default mappers and converters
	{
		columnMapperFactories.add(new EnumMapper.Factory());
		parameterConverters.add(new EnumMapper.Converter());
		
		columnMapperFactories.add(new DefaultMapper.Factory());
		parameterConverters.add(new DefaultMapper.Converter());
		
		// add support for LocalDateTime, LocalDate and Instant
		columnMapperFactories.add(new LocalDateMapper.Factory());
		parameterConverters.add(new LocalDateMapper.Converter());
			
		columnMapperFactories.add(new LocalDateTimeMapper.Factory());
		parameterConverters.add(new LocalDateTimeMapper.Converter());
			
		columnMapperFactories.add(new InstantMapper.Factory());
		parameterConverters.add(new InstantMapper.Converter());

		columnMapperFactories.add(new ZonedDateTimeMapper.Factory());
		parameterConverters.add(new ZonedDateTimeMapper.Converter());
	}
	
	/* ugly solution, TODO: find a better one for handling the removal */
	private static class ClassReferencedSortedSet<T> {
		final TreeSet<T> set;
		final Map<Class<?>, T> mapping = new HashMap<>();
		
		ClassReferencedSortedSet(Comparator<T> comparator) {
			this.set = new TreeSet<>(comparator);
		}
		
		void add(T o) {
			set.add(o);
			mapping.put(o.getClass(), o);
		}
		
		void clear() {
			set.clear();
			mapping.clear();
		}
		
		void remove(Class<?> clazz) {
			T o = mapping.get(clazz);
			mapping.remove(clazz);
			if(o != null) {
				set.remove(o);
			}
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
		private final Class<?> rowMapperClass;

		QueryTypeAndQuery(QueryType type, String query, Class<?> rowMapperClass) {
			this.type = type;
			this.query = query;
			this.rowMapperClass = rowMapperClass;
		}
	}
	
	// -----
	
	public QueryFactory addColumnMapperFactory(ColumnMapperFactory columnMapperFactory) {
		columnMapperFactories.add(columnMapperFactory);
		return this;
	}
	
	public QueryFactory emptyColumnMapperFactories() {
		columnMapperFactories.clear();
		return this;
	}
	
	public QueryFactory removeColumnMapperFactory(Class<? extends ColumnMapperFactory> clazz) {
		columnMapperFactories.remove(clazz);
		return this;
	}
	
	//-----
	
	public QueryFactory addParameterConverters(ParameterConverter parameterConverter) {
		parameterConverters.add(parameterConverter);
		return this;
	}
	
	public QueryFactory emptyParameterConverters() {
		parameterConverters.clear();
		return this;
	}
	
	public QueryFactory removeParameterConverter(Class<? extends ParameterConverter> clazz) {
		parameterConverters.remove(clazz);
		return this;
	}
	
	// -----


	private QueryTypeAndQuery extractQueryAnnotation(Class<?> clazz, Method method) {
		
		Query q = method.getAnnotation(Query.class);
		QueriesOverride qs = method.getAnnotation(QueriesOverride.class);

		// only one @Query annotation, thus we return the value without checking the database
		if (qs == null) {
			return new QueryTypeAndQuery(q.type(), q.value(), q.mapper());
		}
		
		for (QueryOverride query : qs.value()) {
			if (query.db().equals(activeDb)) {
				return new QueryTypeAndQuery(q.type(), query.value(), query.mapper());
			}
		}

		return new QueryTypeAndQuery(q.type(), q.value(), q.mapper());
	}
	
	//from https://rmannibucau.wordpress.com/2014/03/27/java-8-default-interface-methods-and-jdk-dynamic-proxies/
	
	private static final Method IS_DEFAULT_METHOD = ReflectionUtils.findMethod(Method.class, "isDefault");
	private static final Constructor<MethodHandles.Lookup> LOOKUP_CONSTRUCTOR;
	private static final Method PRIVATE_LOOKUP_IN = ReflectionUtils.findMethod(MethodHandles.class, "privateLookupIn", Class.class, MethodHandles.Lookup.class);
	
	static {
		try {
			if(PRIVATE_LOOKUP_IN == null) {
				LOOKUP_CONSTRUCTOR = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
				if (!LOOKUP_CONSTRUCTOR.isAccessible()) {
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
	public <T> T from(final Class<T> clazz) {
		
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
				new Class[] { clazz }, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						boolean hasAnnotation = method.getAnnotation(Query.class) != null;
						if(hasAnnotation) {
							QueryTypeAndQuery qs = extractQueryAnnotation(clazz, method);
							return qs.type.apply(qs.query, qs.rowMapperClass, jdbc, method, args, columnMapperFactories.set, parameterConverters.set);
						} else if(method.getReturnType().equals(NamedParameterJdbcTemplate.class) && args == null) {
							return jdbc;
						} else if(IS_DEFAULT_METHOD != null && (boolean) IS_DEFAULT_METHOD.invoke(method)) {
							final Class<?> declaringClass = method.getDeclaringClass();
							final MethodHandle handle;
							if(PRIVATE_LOOKUP_IN != null) {
								MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
								handle = MethodHandles.lookup().findSpecial(declaringClass, method.getName(), methodType, declaringClass);
							} else {
								handle = LOOKUP_CONSTRUCTOR.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE).unreflectSpecial(method, declaringClass);
							}
							return handle.bindTo(proxy).invokeWithArguments(args);
						} else {
							throw new IllegalArgumentException(String.format("missing @Query annotation for method %s in interface %s", method.getName(),	clazz.getSimpleName()));
						}
						
					}
				}

		);
	}

}
