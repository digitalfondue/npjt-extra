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
package ch.digitalfondue.npjt;

import java.util.Properties;

import javax.sql.DataSource;

import org.hsqldb.jdbc.JDBCDataSourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

public class TestJdbcConfiguration {

	@Bean
	public DataSource getDataSource() throws Exception {
		Properties prop = new Properties();
		prop.put("url", "jdbc:hsqldb:mem:extra");
		prop.put("user", "sa");
		prop.put("password", "");
		return JDBCDataSourceFactory.createDataSource(prop);
	}

	/*@Bean
	DataSource getPostgresqlDataSource() {
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName("org.postgresql.Driver");
		ds.setUrl("jdbc:postgresql://localhost:5432/alfio");
		ds.setUsername("postgres");
		ds.setPassword("password");
		return ds;
	}*/

	@Bean
	public PlatformTransactionManager getPlatfomrTransactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}
}
