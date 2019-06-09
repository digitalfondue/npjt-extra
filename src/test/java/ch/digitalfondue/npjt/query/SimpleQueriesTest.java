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
package ch.digitalfondue.npjt.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import ch.digitalfondue.npjt.Bind;
import ch.digitalfondue.npjt.Query;
import ch.digitalfondue.npjt.QueryFactory;
import ch.digitalfondue.npjt.QueryType;
import ch.digitalfondue.npjt.TestJdbcConfiguration;
import ch.digitalfondue.npjt.ConstructorAnnotationRowMapper.Column;

@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestJdbcConfiguration.class)
public class SimpleQueriesTest {

	@Autowired
	DataSource dataSource;

	/**
	 * Simple DB interaction.
	 */
	@Test
	public void simpleQueriesTest() {
		QueryFactory qf = new QueryFactory("hsqldb", dataSource);

		MySimpleQueries mq = qf.from(MySimpleQueries.class);

		mq.createTable();

		Assert.assertTrue(mq.findAll().isEmpty());
		Assert.assertEquals(1, mq.insertValue("MY_KEY", "MY_VALUE"));

		Conf conf = mq.findByKey("MY_KEY");

		Assert.assertEquals("MY_KEY", conf.key);
		Assert.assertEquals("MY_VALUE", conf.value);

		Assert.assertFalse(mq.findAll().isEmpty());

		Assert.assertEquals(1, mq.update("MY_KEY", "MY_VALUE_UPDATED"));

		Conf confUpd = mq.findByKey("MY_KEY");
		Assert.assertEquals("MY_KEY", confUpd.key);
		Assert.assertEquals("MY_VALUE_UPDATED", confUpd.value);
		
		Assert.assertTrue(mq.findAllKeys().contains("MY_KEY"));
		
		Assert.assertEquals("MY_VALUE_UPDATED", mq.findValueForKey("MY_KEY"));
		
		Assert.assertEquals("MY_TEMPLATE", mq.template());
		
		//Assert.assertEquals("defaultMethod", mq.defaultMethod());
		
		Assert.assertNotNull(mq.getNamedParameterJdbcTemplate());
		
		Assert.assertEquals("MY_VALUE_UPDATED", mq.findOptionalValueForKey("MY_KEY").get());
		
		Assert.assertFalse(mq.findOptionalValueForKey("MY_KEY_NOT").isPresent());
		
		Assert.assertEquals("MY_VALUE_UPDATED", mq.findOptionalWrappedValueForKey("MY_KEY").get().value);
		
		Assert.assertFalse(mq.findOptionalWrappedValueForKey("MY_KEY_NOT").isPresent());

		Assert.assertEquals("defaultMethod", mq.defaultMethod());
	}

	public static class Conf {
		final String key;
		final String value;

		public Conf(@Column("CONF_KEY") String key,
				@Column("CONF_VALUE") String value) {
			this.key = key;
			this.value = value;
		}
	}
	
	public static class MyCustomWrapper {
		final String value;
		
		public MyCustomWrapper(String value) {
			this.value = value;
		}
	}
	
	public static class MyCustomWrapperRowMapper implements RowMapper<MyCustomWrapper> {

		@Override
		public MyCustomWrapper mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new MyCustomWrapper(rs.getString(1));
		}
		
	}

	public interface MySimpleQueries {

		@Query("CREATE TABLE LA_CONF (CONF_KEY VARCHAR(64) PRIMARY KEY NOT NULL, CONF_VALUE CLOB NOT NULL)")
		void createTable();

		@Query("INSERT INTO LA_CONF(CONF_KEY, CONF_VALUE) VALUES(:key, :value)")
		int insertValue(@Bind("key") String key, @Bind("value") String value);

		@Query("SELECT * FROM LA_CONF WHERE CONF_KEY = :key")
		Conf findByKey(@Bind("key") String key);

		@Query("SELECT * FROM LA_CONF")
		List<Conf> findAll();
		
		@Query("SELECT CONF_KEY FROM LA_CONF")
		List<String> findAllKeys();
		
		@Query("SELECT CONF_VALUE FROM LA_CONF WHERE CONF_KEY = :key")
		String findValueForKey(@Bind("key") String key);
		
		@Query("SELECT CONF_VALUE FROM LA_CONF WHERE CONF_KEY = :key")
		Optional<String> findOptionalValueForKey(@Bind("key") String key);
		
		@Query(value = "SELECT CONF_VALUE FROM LA_CONF WHERE CONF_KEY = :key", mapper = MyCustomWrapperRowMapper.class)
		Optional<MyCustomWrapper> findOptionalWrappedValueForKey(@Bind("key") String key);

		@Query("UPDATE LA_CONF SET CONF_VALUE = :value WHERE CONF_KEY = :key")
		int update(@Bind("key") String key, @Bind("value") String value);

		@Query(type = QueryType.TEMPLATE, value = "MY_TEMPLATE")
		String template();
		
		NamedParameterJdbcTemplate getNamedParameterJdbcTemplate();
		
		//Is there any IDE that is able to interpret correctly <source>1.7</source> + <testSource>1.8</testSource> ? 
		default String defaultMethod() {
			return "defaultMethod";
		}
	}

}
