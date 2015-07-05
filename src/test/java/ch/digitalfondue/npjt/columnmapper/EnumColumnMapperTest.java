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

import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ch.digitalfondue.npjt.columnmapper.EnumColumnMapper;

@RunWith(MockitoJUnitRunner.class)
public class EnumColumnMapperTest {
	
	public enum MyEnum {
		BLA, TEST;
	}

	@Mock
	ResultSet resultSet;
	
	@Test
	public void testNull() throws SQLException {
		EnumColumnMapper m = new EnumColumnMapper("PARAM", MyEnum.class);
		Assert.assertNull(m.getObject(resultSet));
	}
	
	@Test
	public void testValue() throws SQLException {
		EnumColumnMapper m = new EnumColumnMapper("PARAM", MyEnum.class);
		when(resultSet.getString("PARAM")).thenReturn("BLA");
		Assert.assertEquals(MyEnum.BLA, m.getObject(resultSet));
		
		when(resultSet.getString("PARAM")).thenReturn("TEST");
		Assert.assertEquals(MyEnum.TEST, m.getObject(resultSet));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testWrongValue() throws SQLException {
		EnumColumnMapper m = new EnumColumnMapper("PARAM", MyEnum.class);
		when(resultSet.getString("PARAM")).thenReturn("NOT_IN_ENUM");
		m.getObject(resultSet);
	}
}
