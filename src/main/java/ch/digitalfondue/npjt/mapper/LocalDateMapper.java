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
package ch.digitalfondue.npjt.mapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import ch.digitalfondue.npjt.mapper.ColumnMapperFactory.AbstractColumnMapperFactory;
import ch.digitalfondue.npjt.mapper.ParameterConverter.AbstractParameterConverter;

public class LocalDateMapper extends ColumnMapper {
	
	private static final int ORDER = Integer.MAX_VALUE - 3;
	
	public LocalDateMapper(String name, Class<?> paramType) {
		super(name, paramType);
	}

	@Override
	public Object getObject(ResultSet rs) throws SQLException {
		return toLocalDate(rs.getDate(name));
	}
	
	private static LocalDate toLocalDate(Date d) {
		return d != null ? d.toLocalDate() : null;
	}
	
	public static class Converter extends AbstractParameterConverter {

		@Override
		public boolean accept(Class<?> parameterType) {
			return LocalDate.class.equals(parameterType);
		}

		@Override
		public void processParameter(String parameterName, Object arg,
				Class<?> parameterType, MapSqlParameterSource ps) {
			ps.addValue(parameterName, arg != null ? Date.valueOf((LocalDate) arg) : null, Types.DATE);
		}

		@Override
		public int order() {
			return ORDER;
		}
	}

	
	public static class Factory extends AbstractColumnMapperFactory {

		@Override
		public ColumnMapper build(String name, Class<?> paramType) {
			return new LocalDateMapper(name, paramType);
		}

		@Override
		public int order() {
			return ORDER;
		}

		@Override
		public boolean accept(Class<?> paramType) {
			return LocalDate.class.equals(paramType);
		}

		@Override
		public RowMapper<Object> getSingleColumnRowMapper(Class<Object> clzz) {
			return new RowMapper<Object>() {
				@Override
				public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
					return toLocalDate(rs.getDate(1));
				}
			};
		}
		
	}

}
