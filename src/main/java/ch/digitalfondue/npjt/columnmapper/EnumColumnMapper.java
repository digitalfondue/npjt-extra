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
package ch.digitalfondue.npjt.columnmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import ch.digitalfondue.npjt.columnmapper.ColumnMapperFactory.AbstractColumnMapperFactory;

public class EnumColumnMapper extends ColumnMapper {


	public EnumColumnMapper(String name, Class<?> paramType) {
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

	
	public static class EnumColumnMapperFactory extends AbstractColumnMapperFactory {

		@Override
		public ColumnMapper build(String name, Class<?> paramType) {
			return new EnumColumnMapper(name, paramType);
		}

		@Override
		public int order() {
			return Integer.MAX_VALUE - 2;
		}

		@Override
		public boolean accept(Class<?> paramType) {
			return paramType.isEnum();
		}

		@Override
		public RowMapper<Object> getSingleColumnRowMapper(final Class<Object> clazz) {
			return new RowMapper<Object>() {
				@Override
				public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
					return toEnum(rs.getString(1), clazz);
				}
			};
		}
	}
}