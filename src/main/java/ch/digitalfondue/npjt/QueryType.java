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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

import ch.digitalfondue.npjt.QueryFactory.QueryTypeAndQuery;
import ch.digitalfondue.npjt.mapper.ColumnMapperFactory;
import ch.digitalfondue.npjt.mapper.ParameterConverter;

/**
 * Query Type:
 *
 * <ul>
 * <li>TEMPLATE : we receive the string defined in @Query/@QueryOverride
 * annotation.
 * <li>EXECUTE : the query will be executed. If it's a select, the result will
 * be mapped with a ConstructorAnnotationRowMapper if it has the correct form.
 * </ul>
 *
 */
public enum QueryType {
	
	/**
	 * Receive the string defined in @Query/@QueryOverride annotation.
	 */
	TEMPLATE {
		@Override
		String apply(QueryTypeAndQuery queryTypeAndQuery, NamedParameterJdbcTemplate jdbc,
				Method method, Object[] args,
				SortedSet<ColumnMapperFactory> columnMapperFactories, SortedSet<ParameterConverter> parameterConverters) {
			return queryTypeAndQuery.query;
		}
	},

	/**
	 */
	EXECUTE {

		/**
		 * Keep a mapping between a given class and a possible RowMapper.
		 *
		 * If the Class has the correct form, a ConstructorAnnotationRowMapper
		 * will be built and the boolean set to true in the pair. If the class
		 * has not the correct form, the boolean will be false and the class
		 * will be used as it is in the jdbc template.
		 */
		private final Map<Class<Object>, HasRowmapper> cachedClassToMapper = new ConcurrentHashMap<>();

		@Override
		Object apply(QueryTypeAndQuery queryTypeAndQuery, NamedParameterJdbcTemplate jdbc,
					 Method method, Object[] args,
					 SortedSet<ColumnMapperFactory> columnMapperFactories, SortedSet<ParameterConverter> parameterConverters) {
			JdbcAction action = actionFromContext(method, queryTypeAndQuery);
			SqlParameterSource parameters = extractParameters(method, args, parameterConverters, jdbc);
			switch (action) {
			case QUERY:
				return doQuery(queryTypeAndQuery.query, queryTypeAndQuery.rowMapperClass, jdbc, method, parameters, columnMapperFactories);
			case UPDATE:
				return jdbc.update(queryTypeAndQuery.query, parameters);
			case INSERT_W_AUTO_GENERATED_KEY:
				return executeUpdateAndKeepKeys(queryTypeAndQuery.query, method, jdbc, parameters);
			default:
				throw new IllegalArgumentException("unknown value for action: " + action);
			}
		}

		
		@SuppressWarnings("unchecked")
		private Object doQuery(String template, Class<?> rowMapper,
				NamedParameterJdbcTemplate jdbc, Method method,
				SqlParameterSource parameters, SortedSet<ColumnMapperFactory> columnMapperFactories) {
			boolean isReturnOptional = isReturnOptional(method);
			if (method.getReturnType().isAssignableFrom(List.class) || isReturnOptional) {
				Class<Object> c = extractGenericMethod(method);
				
				HasRowmapper r = getRowMapper(c, rowMapper, columnMapperFactories);
				
				List<Object> res = handleList(template, jdbc, parameters, columnMapperFactories, c, r, method);
				if(isReturnOptional) {
					 return buildOptional(res);
				} else {
					return res;
				}
			} else {
				Class<Object> c = (Class<Object>) method.getReturnType();
				HasRowmapper r = getRowMapper(c, rowMapper, columnMapperFactories);
				return handleSingleObject(template, jdbc, parameters, columnMapperFactories, c, r, method);
			}
		}

		@SuppressWarnings("unchecked")
		private HasRowmapper getRowMapper(Class<Object> c, Class<?> rowMapper, SortedSet<ColumnMapperFactory> columnMapperFactories) {
			
			if(rowMapper != ConstructorAnnotationRowMapper.class) {
				try {
					return new HasRowmapper(true, (RowMapper<Object>) rowMapper.getConstructor().newInstance());
				} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
					throw new IllegalArgumentException("Was not able to create a new instance of " + rowMapper + ". It require a 0 args constructor.", e);
				}
			} else if (!cachedClassToMapper.containsKey(c)) {
				cachedClassToMapper.put(c, handleClass(c, columnMapperFactories));
			}
			return cachedClassToMapper.get(c);
		}
	},
	/**
	 * Specialized EXECUTE, will bypass the heuristic to determine if a query is a insert/update/delete or a select, will always treat the query as a select.
	 */
	SELECT {
		@Override
		Object apply(QueryTypeAndQuery queryTypeAndQuery, NamedParameterJdbcTemplate jdbc, Method method, Object[] args, SortedSet<ColumnMapperFactory> columnMapperFactories, SortedSet<ParameterConverter> parameterConverters) {
			return EXECUTE.apply(queryTypeAndQuery, jdbc, method, args, columnMapperFactories, parameterConverters);
		}
	},
	/**
	 * Specialized EXECUTE, will bypass the heuristic to determine if a query is a insert/update/delete or a select, will always treat the query as a insert/update/delete.
	 */
	MODIFYING {
		@Override
		Object apply(QueryTypeAndQuery queryTypeAndQuery, NamedParameterJdbcTemplate jdbc, Method method, Object[] args, SortedSet<ColumnMapperFactory> columnMapperFactories, SortedSet<ParameterConverter> parameterConverters) {
			return EXECUTE.apply(queryTypeAndQuery, jdbc, method, args, columnMapperFactories, parameterConverters);
		}
	},
	/**
	 * Specialized EXECUTE, will bypass the heuristic to determine if a query is a insert/update/delete or a select, will always treat the query as a select.
	 */
	MODIFYING_WITH_RETURN {
		@Override
		Object apply(QueryTypeAndQuery queryTypeAndQuery, NamedParameterJdbcTemplate jdbc, Method method, Object[] args, SortedSet<ColumnMapperFactory> columnMapperFactories, SortedSet<ParameterConverter> parameterConverters) {
			return EXECUTE.apply(queryTypeAndQuery, jdbc, method, args, columnMapperFactories, parameterConverters);
		}
	};
	
	abstract Object apply(QueryTypeAndQuery queryTypeAndQuery, NamedParameterJdbcTemplate jdbc,
						  Method method, Object[] args,
						  SortedSet<ColumnMapperFactory> columnMapperFactories, SortedSet<ParameterConverter> parameterConverters);
	
	private static Object handleSingleObject(String template,
			NamedParameterJdbcTemplate jdbc, SqlParameterSource parameters,
			SortedSet<ColumnMapperFactory> columnMapperFactories,
			Class<Object> c, HasRowmapper r, Method method) {
		if (r.present) {
			return jdbc.queryForObject(template, parameters, r.rowMapper);
		} else {
			RowMapper<Object> rowMapper = matchToOutput(columnMapperFactories, c, method.getAnnotations());
			if(rowMapper != null) {
				return jdbc.queryForObject(template, parameters, rowMapper);
			} else {
				return jdbc.queryForObject(template, parameters, c);
			}
			
		}
	}
	
	@SuppressWarnings("unchecked")
	private static Class<Object> extractGenericMethod(Method method) {
		return (Class<Object>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
	}
	
	protected Object buildOptional(List<Object> res) {
		if (res.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1, res.size());
		}

		if(res.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.ofNullable(res.iterator().next());
		}
	}
	

	private static boolean isReturnOptional(Method method) {
		return method.getReturnType().isAssignableFrom(Optional.class);
	}

	private static RowMapper<Object> matchToOutput(SortedSet<ColumnMapperFactory> columnMapperFactories, Class<Object> o, Annotation[] annotations) {
		
		for(ColumnMapperFactory mapper : columnMapperFactories) {
			if(mapper.accept(o, annotations)) {
				return mapper.getSingleColumnRowMapper(o);
			}
		}		
		return null;
	}

	private static JdbcAction actionFromContext(Method method, QueryTypeAndQuery queryTypeAndQuery) {
		
		if (method.getReturnType().isAssignableFrom(AffectedRowCountAndKey.class)) {
			return JdbcAction.INSERT_W_AUTO_GENERATED_KEY;
		} else if (queryTypeAndQuery.type == SELECT || queryTypeAndQuery.type == MODIFYING_WITH_RETURN) {
			return JdbcAction.QUERY;
		} else if (queryTypeAndQuery.type == MODIFYING) {
			return JdbcAction.UPDATE;
		} else {
			return actionFromTemplate(queryTypeAndQuery.query);
		}		
	}

	private static JdbcAction actionFromTemplate(String template) {
		String tmpl = StringUtils.deleteAny(template.toLowerCase(Locale.ENGLISH), "() ").trim();
		return tmpl.indexOf("select") == 0 ? JdbcAction.QUERY : JdbcAction.UPDATE;
	}

	private enum JdbcAction {
		QUERY, UPDATE, INSERT_W_AUTO_GENERATED_KEY
	}
	
	private static class HasRowmapper {
		private final boolean present;
		private final RowMapper<Object> rowMapper;

		HasRowmapper(boolean present, RowMapper<Object> rowMapper) {
			this.present = present;
			this.rowMapper = rowMapper;
		}
	}

	

	private static HasRowmapper handleClass(Class<Object> c, SortedSet<ColumnMapperFactory> columnMapperFactories) {
		if (ConstructorAnnotationRowMapper.hasConstructorInTheCorrectForm(c)) {
			return new HasRowmapper(true, new ConstructorAnnotationRowMapper<>(c, columnMapperFactories));
		} else {
			return new HasRowmapper(false, null);
		}
	}

	private static SqlParameterSource extractParameters(Method m, Object[] args, SortedSet<ParameterConverter> parameterConverters, NamedParameterJdbcTemplate jdbc) {

		Annotation[][] parameterAnnotations = m.getParameterAnnotations();
		if (parameterAnnotations == null || parameterAnnotations.length == 0) {
			return new EmptySqlParameterSource();
		}

		MapSqlParameterSource ps = new MapSqlParameterSource();
		Class<?>[] parameterTypes = m.getParameterTypes();
		for (int i = 0; i < args.length; i++) {
			String name = parameterName(parameterAnnotations[i]);
			if (name != null) {
				Object arg = args[i];
				Class<?> parameterType = parameterTypes[i];
				
				boolean hasAccepted = false;
				for (ParameterConverter parameterConverter : parameterConverters) {
					if (parameterConverter.accept(parameterType, parameterAnnotations[i])) {
						hasAccepted = true;
						if (parameterConverter instanceof ParameterConverter.AdvancedParameterConverter) {
							((ParameterConverter.AdvancedParameterConverter) parameterConverter).processParameter(new ParameterConverter.ProcessParameterContext(jdbc, name, arg, parameterType, parameterAnnotations[i], ps));
						} else {
							parameterConverter.processParameter(name, arg, parameterType, ps);
						}

						break;
					}
				}
				
				if (!hasAccepted) {
					throw new IllegalStateException("Was not able to find a ParameterConverter able to process object: " + arg + " with class " + parameterType);
				}
			}
		}

		return ps;
	}

	private static String parameterName(Annotation[] annotation) {

		if (annotation == null) {
			return null;
		}

		for (Annotation a : annotation) {
			if (a instanceof Bind) {
				return ((Bind) a).value();
			}
		}
		return null;
	}

	
	@SuppressWarnings("unchecked")
	private static <T> AffectedRowCountAndKey<T> executeUpdateAndKeepKeys(
			String template, Method method,
			NamedParameterJdbcTemplate jdbc, SqlParameterSource parameters) {
		
		Class<T> keyClass = (Class<T>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		int result = jdbc.update(template, parameters, keyHolder);
		Map<String, Object> keys = keyHolder.getKeys();
		Object key;
		if (keys.size() > 1) {
			AutoGeneratedKey spec = Objects.requireNonNull(withType(method.getDeclaredAnnotations(), AutoGeneratedKey.class), "more than one key for query " + template + ": annotation @AutoGeneratedKey required");
			key = Objects.requireNonNull(keys.get(spec.value()), "the key with name " + spec.value() + " has returned null for query " + template + ": required a non null key");
		} else if (Number.class.isAssignableFrom(keyClass)) {
		    Class<? extends Number> c = (Class<? extends Number>) keyClass;
		    return new AffectedRowCountAndKey<>(result, (T) NumberUtils.convertNumberToTargetClass(keyHolder.getKey(), c));
		} else {
			key = keys.values().iterator().next();
		}
		return new AffectedRowCountAndKey<>(result, keyClass.cast(key));
	}

	private static <T extends  Annotation> T withType(Annotation[] annotations, Class<T> c) {
		if(annotations == null) {
			return null;
		}

		for(Annotation a : annotations) {
			if(a.annotationType() == c) {
				return (T) a;
			}
		}
		return null;
	}

	private static List<Object> handleList(String template,
			NamedParameterJdbcTemplate jdbc, SqlParameterSource parameters,
			SortedSet<ColumnMapperFactory> columnMapperFactories,
			Class<Object> c, HasRowmapper r, Method method) {
		if (r.present) {
			return jdbc.query(template, parameters, r.rowMapper);
		} else {
			RowMapper<Object> rowMapper = matchToOutput(columnMapperFactories, c, method.getAnnotations());
			if(rowMapper != null) {
				return jdbc.query(template, parameters, rowMapper);
			} else {
				return jdbc.queryForList(template, parameters, c);
			}
		}
	}
}
