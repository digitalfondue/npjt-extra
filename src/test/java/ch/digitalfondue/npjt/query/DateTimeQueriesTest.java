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
package ch.digitalfondue.npjt.query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import ch.digitalfondue.npjt.Bind;
import ch.digitalfondue.npjt.Query;
import ch.digitalfondue.npjt.QueryFactory;
import ch.digitalfondue.npjt.TestJdbcConfiguration;
import ch.digitalfondue.npjt.ConstructorAnnotationRowMapper.Column;
import ch.digitalfondue.npjt.mapper.InstantMapper;
import ch.digitalfondue.npjt.mapper.LocalDateMapper;
import ch.digitalfondue.npjt.mapper.LocalDateTimeMapper;
import ch.digitalfondue.npjt.mapper.ZonedDateTimeMapper;

@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestJdbcConfiguration.class)
public class DateTimeQueriesTest {
	
	@Autowired
	DataSource dataSource;

	@Test
	public void dateQueriesTest() {
		QueryFactory qf = new QueryFactory("hsqldb", new JdbcTemplate(dataSource));
		
		qf.addColumnMapperFactory(new ZonedDateTimeMapper.Factory());
		qf.addParameterConverters(new ZonedDateTimeMapper.Converter());
		
		qf.addColumnMapperFactory(new LocalDateMapper.Factory());
		qf.addParameterConverters(new LocalDateMapper.Converter());
		
		qf.addColumnMapperFactory(new LocalDateTimeMapper.Factory());
		qf.addParameterConverters(new LocalDateTimeMapper.Converter());
		
		qf.addColumnMapperFactory(new InstantMapper.Factory());
		qf.addParameterConverters(new InstantMapper.Converter());
		
		DateQueries dq = qf.from(DateQueries.class);
		
		dq.createTable();
		
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
		
		dq.insertValue("KEY", now);
		check(dq, "KEY", now);
		
		dq.insertValue("KEY2", now.toLocalDate());
		check(dq, "KEY2", now.toLocalDate());
		
		dq.insertValue("KEY3", now.toLocalDateTime());
		check(dq, "KEY3", now);
		
		Instant iNow = Instant.now();
		dq.insertValue("KEY4", iNow);
		Assert.assertEquals(iNow, dq.findInstantByKey("KEY4"));
		Assert.assertEquals(iNow, dq.findConfInstantByKey("KEY4").value);
		
	}
	
	private void check(DateQueries dq, String key, LocalDate now) {
		Assert.assertEquals(now, dq.findByKey(key).valueLocalDate);
		Assert.assertEquals(LocalDateTime.of(now, LocalTime.MIDNIGHT), dq.findByKey(key).valueLocalDateTime);
	}

	private void check(DateQueries dq, String key, ZonedDateTime now) {
		Assert.assertEquals(now, dq.findByKey(key).value);
		Assert.assertEquals(now, dq.findDateByKey(key));
		Assert.assertEquals(now.toLocalDate(), dq.findByKey(key).valueLocalDate);
		Assert.assertEquals(now.toLocalDateTime(), dq.findByKey(key).valueLocalDateTime);
	}
	
	public static class Conf {
		final String key;
		final ZonedDateTime value;
		final LocalDate valueLocalDate;
		final LocalDateTime valueLocalDateTime;

		public Conf(@Column("CONF_KEY") String key,
				@Column("CONF_VALUE") ZonedDateTime value,
				@Column("CONF_VALUE") LocalDate valueLocalDate,
				@Column("CONF_VALUE") LocalDateTime valueLocalDateTime) {
			this.key = key;
			this.value = value;
			this.valueLocalDate = valueLocalDate;
			this.valueLocalDateTime = valueLocalDateTime;
		}
	}
	
	public static class ConfInstant {
		final String key;
		final Instant value;
		
		public ConfInstant(@Column("CONF_KEY") String key,
				@Column("CONF_VALUE") Instant value) {
			this.key = key;
			this.value = value;
		}
	}
	
	public interface DateQueries {

		@Query("CREATE TABLE LA_CONF_DATE (CONF_KEY VARCHAR(64) PRIMARY KEY NOT NULL, CONF_VALUE TIMESTAMP NOT NULL)")
		void createTable();
		
		@Query("INSERT INTO LA_CONF_DATE(CONF_KEY, CONF_VALUE) VALUES(:key, :value)")
		int insertValue(@Bind("key") String key, @Bind("value") ZonedDateTime date);
		
		@Query("INSERT INTO LA_CONF_DATE(CONF_KEY, CONF_VALUE) VALUES(:key, :value)")
		int insertValue(@Bind("key") String key, @Bind("value") LocalDate date);
		
		@Query("INSERT INTO LA_CONF_DATE(CONF_KEY, CONF_VALUE) VALUES(:key, :value)")
		int insertValue(@Bind("key") String key, @Bind("value") LocalDateTime date);
		
		@Query("INSERT INTO LA_CONF_DATE(CONF_KEY, CONF_VALUE) VALUES(:key, :value)")
		int insertValue(@Bind("key") String key, @Bind("value") Instant date);
		
		@Query("SELECT * FROM LA_CONF_DATE WHERE CONF_KEY = :key")
		Conf findByKey(@Bind("key") String key);
		
		@Query("SELECT CONF_VALUE FROM LA_CONF_DATE WHERE CONF_KEY = :key")
		Instant findInstantByKey(@Bind("key") String key);
		
		@Query("SELECT * FROM LA_CONF_DATE WHERE CONF_KEY = :key")
		ConfInstant findConfInstantByKey(@Bind("key") String key);
		
		@Query("SELECT CONF_VALUE FROM LA_CONF_DATE WHERE CONF_KEY = :key")
		ZonedDateTime findDateByKey(@Bind("key") String key);
		
	}

}
