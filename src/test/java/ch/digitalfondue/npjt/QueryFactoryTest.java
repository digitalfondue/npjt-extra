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
/**
 * This file is part of lavagna.
 *
 * lavagna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * lavagna is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with lavagna.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.digitalfondue.npjt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@RunWith(MockitoJUnitRunner.class)
public class QueryFactoryTest {

	@Mock
	NamedParameterJdbcTemplate jdbc;

	@Mock
	DataSource dataSource;

	public interface QueryTest {
		@Query(type = QueryType.TEMPLATE, value = "SELECT * FROM LA_BOARD_COLUMN_FULL WHERE BOARD_COLUMN_ID = :columnId")
		String findById();

		@Query(type = QueryType.TEMPLATE, value = "SELECT * FROM LA_BOARD_COLUMN_FULL WHERE BOARD_COLUMN_ID = :columnId")
		@QueriesOverride(@QueryOverride(db = "MYSQL", value = "SELECT * FROM LA_BOARD_COLUMN_FULL_MYSQL WHERE BOARD_COLUMN_ID = :columnId"))
		String overrideQuery();
	}

	@Test
	public void testSimpleAnnotationQuery() {
		QueryFactory<QueryTest> qf = new QueryFactory<>(QueryTest.class, "HSQLDB");
		qf.setDataSource(dataSource);

		QueryTest qt = qf.getObject();

		Assert.assertEquals("SELECT * FROM LA_BOARD_COLUMN_FULL WHERE BOARD_COLUMN_ID = :columnId", qt.findById());
	}

	@Test
	public void testOverrideAnnotation() {
		QueryFactory<QueryTest> qf = new QueryFactory<>(QueryTest.class, "HSQLDB");
		qf.setDataSource(dataSource);
		QueryTest qt = qf.getObject();
		Assert.assertEquals("SELECT * FROM LA_BOARD_COLUMN_FULL WHERE BOARD_COLUMN_ID = :columnId", qt.overrideQuery());


		QueryFactory<QueryTest> qfMysql = new QueryFactory<>(QueryTest.class, "MYSQL");
		qfMysql.setDataSource(dataSource);
		QueryTest qtMysql = qfMysql.getObject();
		Assert.assertEquals("SELECT * FROM LA_BOARD_COLUMN_FULL_MYSQL WHERE BOARD_COLUMN_ID = :columnId", qtMysql.overrideQuery());
	}

}
