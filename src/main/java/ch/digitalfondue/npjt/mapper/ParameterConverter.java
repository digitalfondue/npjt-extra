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
package ch.digitalfondue.npjt.mapper;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import java.lang.annotation.Annotation;
import java.sql.Connection;

public interface ParameterConverter {

	boolean accept(Class<?> parameterType, Annotation[] annotations);


	interface AdvancedParameterConverter extends ParameterConverter {
		void processParameter(ProcessParameterContext processParameterContext);

		@Override
		default void processParameter(String parameterName, Object arg, Class<?> parameterType, MapSqlParameterSource ps) {
			throw new IllegalStateException("should not be executed");
		}
	}

	/**
	 *
	 *
	 * @param parameterName
	 * @param arg
	 * @param parameterType
	 * @param ps
	 */
	void processParameter(String parameterName, Object arg, Class<?> parameterType, MapSqlParameterSource ps);
	
	int order();

	class ProcessParameterContext {
		private final NamedParameterJdbcTemplate jdbc;
		private final String parameterName;
		private final Class<?> parameterType;
		private final Annotation[] parameterAnnotations;
		private final Object arg;
		private final MapSqlParameterSource ps;

		public ProcessParameterContext(NamedParameterJdbcTemplate jdbc, String parameterName, Object arg, Class<?> parameterType, Annotation[] parameterAnnotations, MapSqlParameterSource ps) {
			this.jdbc = jdbc;
			this.parameterName = parameterName;
			this.arg = arg;
			this.parameterType = parameterType;
			this.parameterAnnotations = parameterAnnotations;
			this.ps = ps;
		}

		public NamedParameterJdbcTemplate getJdbc() {
			return jdbc;
		}

		public Connection getConnection() {
			return DataSourceUtils.getConnection(jdbc.getJdbcTemplate().getDataSource());
		}

		public Class<?> getParameterType() {
			return parameterType;
		}

		public Annotation[] getParameterAnnotations() {
			return parameterAnnotations;
		}

		public Object getArg() {
			return arg;
		}

		public String getParameterName() {
			return parameterName;
		}

		public MapSqlParameterSource getParameterSource() {
			return ps;
		}
	}
}
