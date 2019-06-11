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
import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class ZonedDateTimeMapper extends ColumnMapper {
	
	private static final int ORDER = Integer.MAX_VALUE -2;
	
	private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");
	private static final ZoneId UTC_Z_ID = ZoneId.of("UTC");

	public ZonedDateTimeMapper(String name, Class<?> paramType) {
		super(name, paramType);
	}

	public Object getObject(ResultSet rs) throws SQLException {
		Timestamp timestamp = rs.getTimestamp(name, Calendar.getInstance(UTC_TZ));
		return toZonedDateTime(timestamp);
	}

	private static Object toZonedDateTime(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return ZonedDateTime.ofInstant(timestamp.toInstant(), UTC_Z_ID);
	}

	public static class Converter implements ParameterConverter {

		@Override
		public boolean accept(Class<?> parameterType, Annotation[] annotations) {
			return ZonedDateTime.class.isAssignableFrom(parameterType);
		}

		@Override
		public void processParameter(String parameterName, Object arg, Class<?> parameterType, MapSqlParameterSource ps) {
			Calendar c = null;
			if(arg != null) {
				ZonedDateTime dateTime = ZonedDateTime.class.cast(arg);
				ZonedDateTime utc = dateTime.withZoneSameInstant(UTC_Z_ID);
				c = Calendar.getInstance();
				c.setTimeZone(UTC_TZ);
				c.setTimeInMillis(utc.toInstant().toEpochMilli());
			}
			ps.addValue(parameterName, c, Types.TIMESTAMP);
		}

		@Override
		public int order() {
			return ORDER;
		}

	}


	
	public static class Factory implements ColumnMapperFactory {

		@Override
		public ColumnMapper build(String name, Class<?> paramType) {
			return new ZonedDateTimeMapper(name, paramType);
		}

		@Override
		public int order() {
			return ORDER;
		}

		@Override
		public boolean accept(Class<?> paramType, Annotation[] annotations) {
			return ZonedDateTime.class.isAssignableFrom(paramType);
		}

		@Override
		public RowMapper<Object> getSingleColumnRowMapper(Class<Object> clzz) {
			return (rs, rowNum) -> {
				Timestamp timestamp = rs.getTimestamp(1, Calendar.getInstance(UTC_TZ));
				return toZonedDateTime(timestamp);
			};
		}
		
	}
}