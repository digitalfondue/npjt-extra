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
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import ch.digitalfondue.npjt.columnmapper.ColumnMapperFactory.AbstractColumnMapperFactory;

public class DefaultColumnMapper extends ColumnMapper {

	public DefaultColumnMapper(String name, Class<?> paramType) {
		super(name, paramType);
	}

	public Object getObject(ResultSet rs) throws SQLException {
		int columnIdx = rs.findColumn(name);
		return JdbcUtils.getResultSetValue(rs, columnIdx, paramType);
	}

		
	public static class DefaultColumnMapperFactory extends AbstractColumnMapperFactory {

		@Override
		public ColumnMapper build(String name, Class<?> paramType) {
			return new DefaultColumnMapper(name, paramType);
		}

		@Override
		public int order() {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean accept(Class<?> paramType) {
			return true;
		}

		@Override
		public RowMapper<Object> getSingleColumnRowMapper(Class<Object> clzz) {
			return new SingleColumnRowMapper<Object>(clzz);
		}
		
	}
}
