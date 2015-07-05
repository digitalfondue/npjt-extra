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

public class BooleanColumnMapper extends ColumnMapper {

	public BooleanColumnMapper(String name, Class<?> paramType) {
		super(name, paramType);
	}

	@Override
	public Object getObject(ResultSet rs) throws SQLException {
		return extractBoolean(rs.getObject(name));
	}

	private static Object extractBoolean(Object res) {
		Class<?> resClass = res == null ? null : res.getClass();
		if (res == null || Boolean.class.isAssignableFrom(resClass)) {
			return res;
		} else if (Number.class.isAssignableFrom(resClass)) {
			return 1 == ((Number) res).intValue();
		} else if (String.class.isAssignableFrom(resClass)) {
			return "true".equalsIgnoreCase(res.toString());
		} else {
			throw new IllegalArgumentException(
					"was not able to extract a boolean value");
		}
	}

	public static class BooleanColumnMapperFactory extends AbstractColumnMapperFactory {

		@Override
		public ColumnMapper build(String name, Class<?> paramType) {
			return new BooleanColumnMapper(name, paramType);
		}

		@Override
		public int order() {
			return Integer.MAX_VALUE - 1;
		}

		@Override
		public boolean accept(Class<?> paramType) {
			return boolean.class == paramType || Boolean.class == paramType;
		}

		@Override
		public RowMapper<Object> getSingleColumnRowMapper(Class<Object> clzz) {
			return new RowMapper<Object>() {
				@Override
				public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
					return extractBoolean(rs.getObject(1));
				}
			};
		}
	}

}