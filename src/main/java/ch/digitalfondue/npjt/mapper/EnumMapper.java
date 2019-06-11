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

import java.lang.annotation.Annotation;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class EnumMapper extends ColumnMapper {
	
	private static final int ORDER = Integer.MAX_VALUE - 1;


	public EnumMapper(String name, Class<?> paramType) {
		super(name, paramType);
	}

	
	@Override
	public Object getObject(ResultSet rs) throws SQLException {
		String res = rs.getString(name);
		return toEnum(res, paramType);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object toEnum(String res, Class<?> paramType) {
		Class<? extends Enum> enumType = (Class<? extends Enum<?>>) paramType;
		return res == null ? null : Enum.valueOf(enumType, res.trim());
	}
	
	public static class Converter implements ParameterConverter {

		@Override
		public boolean accept(Class<?> parameterType, Annotation[] annotations) {
			return parameterType.isEnum();
		}

		@Override
		public void processParameter(String parameterName, Object arg, Class<?> parameterType, MapSqlParameterSource ps) {
			ps.addValue(parameterName, arg == null ? null : ((Enum<?>)arg).name());
		}

		@Override
		public int order() {
			return ORDER;
		}

	}

	
	public static class Factory implements ColumnMapperFactory {

		@Override
		public ColumnMapper build(String name, Class<?> paramType) {
			return new EnumMapper(name, paramType);
		}

		@Override
		public int order() {
			return ORDER;
		}

		@Override
		public boolean accept(Class<?> paramType, Annotation[] annotations) {
			return paramType.isEnum();
		}

		@Override
		public RowMapper<Object> getSingleColumnRowMapper(final Class<Object> clazz) {
			return (rs, rowNum) -> toEnum(rs.getString(1), clazz);
		}
	}
}