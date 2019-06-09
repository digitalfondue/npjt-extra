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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import ch.digitalfondue.npjt.query.QueryRepo;
import ch.digitalfondue.npjt.query.deeper.QueryRepo2;

@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestJdbcConfiguration.class, QueryScannerConfiguration.class})
public class QueryRepositoryScannerTest {

	@Autowired
	QueryRepo queryRepo;
	
	@Autowired
	QueryRepo2 queryRepo2;
	
	@Test
	public void checkInjection() {
		Assert.assertNotNull(queryRepo);
		Assert.assertNotNull(queryRepo2);
	}
	
}
