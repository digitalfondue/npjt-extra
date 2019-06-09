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

import ch.digitalfondue.npjt.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import ch.digitalfondue.npjt.ConstructorAnnotationRowMapper.Column;

@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestJdbcConfiguration.class, QueryScannerConfiguration.class})
public class BooleanQueriesTest {

	@Autowired
	BoolQueries bq;

	@Test
	public void simpleQueriesTest() {

		bq.createTable();

		bq.insertValue("KEY", true, true, true);
		
		BoolConf bc = bq.findByKey("KEY");
		
		Assert.assertTrue(bc.confBool);
		Assert.assertTrue(bc.confStr);
		Assert.assertTrue(bc.confInt);
		
		bq.insertValue("KEY2", false, true, false);
		BoolConf bc2 = bq.findByKey("KEY2");
		
		Assert.assertFalse(bc2.confBool);
		Assert.assertTrue(bc2.confStr);
		Assert.assertFalse(bc2.confInt);
		
		Assert.assertTrue(bq.findConfBoolByKey("KEY"));
		Assert.assertFalse(bq.findConfBoolByKey("KEY2"));
		
	}

	public static class BoolConf {

		final String key;
		final boolean confBool;
		final Boolean confStr;
		final boolean confInt;

		public BoolConf(@Column("CONF_KEY") String key,
				@Column("CONF_BOOL") boolean confBool, @Column("CONF_STR") Boolean confStr,
				@Column("CONF_INT") boolean confInt) {
			this.key = key;
			this.confBool = confBool;
			this.confStr = confStr;
			this.confInt = confInt;
		}

	}

	@QueryRepository
	public interface BoolQueries {
		@Query("CREATE TABLE LA_CONF_BOOL (CONF_KEY VARCHAR(64) PRIMARY KEY NOT NULL, CONF_BOOL BOOLEAN NOT NULL, CONF_STR VARCHAR(255) NOT NULL, CONF_INT INTEGER NOT NULL)")
		void createTable();

		@Query("INSERT INTO LA_CONF_BOOL(CONF_KEY, CONF_BOOL, CONF_STR, CONF_INT) VALUES(:key, :confBool, :confStr, :confInt)")
		int insertValue(@Bind("key") String key,
				@Bind("confBool") Boolean confBool,
				@Bind("confStr") Boolean confStr,
				@Bind("confInt") Boolean confInt);
		
		@Query("SELECT * FROM LA_CONF_BOOL WHERE CONF_KEY = :key")
		BoolConf findByKey(@Bind("key") String key);
		
		@Query("SELECT CONF_BOOL FROM LA_CONF_BOOL WHERE CONF_KEY = :key")
		Boolean findConfBoolByKey(@Bind("key") String key);
	}

}
