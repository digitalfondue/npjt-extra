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

import static org.mockito.Mockito.*;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ch.digitalfondue.npjt.columnmapper.DefaultColumnMapper;

@RunWith(MockitoJUnitRunner.class)
public class DefaultColumnMapperTest {
	
	@Mock
	ResultSet resultSet;
	
	@Test
	public void testNull() throws SQLException {
		DefaultColumnMapper m = new DefaultColumnMapper("PARAM", String.class);
		Assert.assertNull(m.getObject(resultSet));
	}
	
	@Test
	public void testString() throws SQLException {
		DefaultColumnMapper m = new DefaultColumnMapper("PARAM", String.class);
		when(resultSet.getObject("PARAM")).thenReturn("MY_VALUE");
		Assert.assertEquals("MY_VALUE", m.getObject(resultSet));
	}
	
	@Test
	public void testClobHandling() throws SQLException {
		DefaultColumnMapper m = new DefaultColumnMapper("PARAM", String.class);
		
		Clob clob = mock(Clob.class);
		
		String clobValue = "MY_VALUE";
		when(clob.length()).thenReturn((long) clobValue.length());
		when(clob.getSubString(1, clobValue.length())).thenReturn(clobValue);
		
		when(resultSet.getObject("PARAM")).thenReturn(clob);
		
		Assert.assertEquals("MY_VALUE", m.getObject(resultSet));
		
		verify(clob).free();
	}
	
	@Test(expected = SQLException.class)
	public void testExceptionOnClose() throws SQLException {
		DefaultColumnMapper m = new DefaultColumnMapper("PARAM", String.class);
		Clob clob = mock(Clob.class);
		String clobValue = "MY_VALUE";
		when(clob.length()).thenReturn((long) clobValue.length());
		when(clob.getSubString(1, clobValue.length())).thenReturn(clobValue);
		
		when(resultSet.getObject("PARAM")).thenReturn(clob);
		
		doThrow(SQLException.class).when(clob).free();
		
		m.getObject(resultSet);
		
	}
	
	@Test(expected = SQLException.class)
	public void testExceptionOnGetSubString() throws SQLException {
		DefaultColumnMapper m = new DefaultColumnMapper("PARAM", String.class);
		Clob clob = mock(Clob.class);
		String clobValue = "MY_VALUE";
		when(clob.length()).thenReturn((long) clobValue.length());
		when(clob.getSubString(1, clobValue.length())).thenThrow(new SQLException());
		
		when(resultSet.getObject("PARAM")).thenReturn(clob);
		
		
		m.getObject(resultSet);
		
	}

}
