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

import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ch.digitalfondue.npjt.mapper.ZonedDateTimeMapper;

@RunWith(MockitoJUnitRunner.class)
public class ZonedDateTimeMapperTest {
	
	@Mock
	ResultSet resultSet;
	
	@Test
	public void testNull() throws SQLException {
		ZonedDateTimeMapper m = new ZonedDateTimeMapper("PARAM", ZonedDateTime.class);
		Assert.assertNull(m.getObject(resultSet));
	}
	
	@Test
	public void testFromTimestampToZonedDateTime() throws SQLException {
		ZonedDateTimeMapper m = new ZonedDateTimeMapper("PARAM", ZonedDateTime.class);
		
		final int time = 42;
		
		when(resultSet.getTimestamp(eq("PARAM"), any(Calendar.class))).thenReturn(new Timestamp(time));
		
		ZonedDateTime res = (ZonedDateTime) m.getObject(resultSet);
		Assert.assertEquals(time, res.toInstant().toEpochMilli());
		
	}

}
