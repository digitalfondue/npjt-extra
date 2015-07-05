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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ch.digitalfondue.npjt.columnmapper.BooleanColumnMapper;

@RunWith(MockitoJUnitRunner.class)
public class BooleanColumnMapperTest {

	@Mock
	ResultSet resultSet;

	@Test
	public void testNull() throws SQLException {
		BooleanColumnMapper m = new BooleanColumnMapper("PARAM", boolean.class);
		Assert.assertNull(m.getObject(resultSet));
	}
	
	
	@Test
	public void testBoolean() throws SQLException {
		BooleanColumnMapper m = new BooleanColumnMapper("PARAM", Boolean.class);
		
		when(resultSet.getObject("PARAM")).thenReturn(true);
		
		Assert.assertEquals(true, m.getObject(resultSet));
		
		when(resultSet.getObject("PARAM")).thenReturn(false);
		
		Assert.assertEquals(false, m.getObject(resultSet));
	}
	
	@Test
	public void testNumber() throws SQLException {
		BooleanColumnMapper m = new BooleanColumnMapper("PARAM", Boolean.class);
		
		when(resultSet.getObject("PARAM")).thenReturn(1);
		
		Assert.assertEquals(true, m.getObject(resultSet));
		
		when(resultSet.getObject("PARAM")).thenReturn(-2);
		
		Assert.assertEquals(false, m.getObject(resultSet));
	}
	
	@Test
	public void testString() throws SQLException {
		BooleanColumnMapper m = new BooleanColumnMapper("PARAM", Boolean.class);
		
		when(resultSet.getObject("PARAM")).thenReturn("true");
		
		Assert.assertEquals(true, m.getObject(resultSet));
		
		when(resultSet.getObject("PARAM")).thenReturn("false");
		
		Assert.assertEquals(false, m.getObject(resultSet));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testOtherFailure() throws SQLException {
		BooleanColumnMapper m = new BooleanColumnMapper("PARAM", Boolean.class);
		when(resultSet.getObject("PARAM")).thenReturn(new Date());
		m.getObject(resultSet);
	}
}
