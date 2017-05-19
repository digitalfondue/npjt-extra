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

import java.util.List;

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

@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestJdbcConfiguration.class)
public class EnumQueriesTest {

	@Autowired
	DataSource dataSource;

	@Test
	public void enumQueriesTest() {
		QueryFactory qf = new QueryFactory("hsqldb", new JdbcTemplate(dataSource));
		EnumQueries eq = qf.from(EnumQueries.class);
		
		eq.createTable();

		eq.insert(null);
		
		eq.insert(TestEnum.TEST);
		eq.insert(TestEnum.TEST2);

		Assert.assertEquals(null, eq.findByNull());
		Assert.assertEquals(TestEnum.TEST, eq.findByKey(TestEnum.TEST));
		Assert.assertEquals(TestEnum.TEST2, eq.findByKey(TestEnum.TEST2));
		
		Assert.assertEquals(TestEnum.TEST, eq.findContainerByKey(TestEnum.TEST).key);
		Assert.assertEquals(null, eq.findContainerByKeyNull().key);
		
		List<TestEnum> all = eq.findAll();
		Assert.assertTrue(all.contains(TestEnum.TEST));
		Assert.assertTrue(all.contains(TestEnum.TEST2));
		Assert.assertTrue(all.contains(null));
	}
	
	public enum TestEnum {
		TEST, TEST2;
	}
	
	public static class EnumContainer {
		final TestEnum key;
		
		public EnumContainer(@Column("CONF_KEY") TestEnum key) {
			this.key = key;
		}
	}
	
	public interface EnumQueries {

		@Query("CREATE TABLE LA_CONF_ENUM (CONF_KEY VARCHAR(64))")
		void createTable();
		
		@Query("INSERT INTO LA_CONF_ENUM (CONF_KEY) VALUES (:key)")
		int insert(@Bind("key") TestEnum test);
		
		//most useless method ever :D
		@Query("SELECT CONF_KEY FROM LA_CONF_ENUM WHERE CONF_KEY = :key")
		TestEnum findByKey(@Bind("key") TestEnum test);

		@Query("SELECT CONF_KEY FROM LA_CONF_ENUM WHERE CONF_KEY is null")
		TestEnum findByNull();
		
		@Query("SELECT CONF_KEY FROM LA_CONF_ENUM WHERE CONF_KEY = :key")
		EnumContainer findContainerByKey(@Bind("key") TestEnum test);

		@Query("SELECT CONF_KEY FROM LA_CONF_ENUM WHERE CONF_KEY is null")
		EnumContainer findContainerByKeyNull();
		
		@Query("SELECT CONF_KEY FROM LA_CONF_ENUM")
		List<TestEnum> findAll();
		
	}
}
