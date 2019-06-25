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

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.npjt.ConstructorAnnotationRowMapper.Column;
import ch.digitalfondue.npjt.mapper.ColumnMapperFactory;
import ch.digitalfondue.npjt.mapper.DefaultMapper.Factory;

public class ConstructorAnnotationRowMapperTest {
	
	private static final List<ColumnMapperFactory> DEFAULT_COLUMN_MAPPER_FACTORY = Collections.<ColumnMapperFactory>singletonList(new Factory()); 

	@Test
	public void testCorrectMapping() {
		new ConstructorAnnotationRowMapper<>(Mapping.class, DEFAULT_COLUMN_MAPPER_FACTORY);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testsMoreThanOnePublicConstructor() {
		new ConstructorAnnotationRowMapper<>(MultiplePublicConstructor.class, DEFAULT_COLUMN_MAPPER_FACTORY);
	}

	@Test(expected = IllegalStateException.class)
	public void testMissingColumnAnnotation() {
		new ConstructorAnnotationRowMapper<>(MissingColumn.class, DEFAULT_COLUMN_MAPPER_FACTORY);
	}
	
	@Test(expected = IllegalStateException.class)
	public void testNoMatchingColumnMapperFactory() {
		new ConstructorAnnotationRowMapper<>(Mapping.class, Collections.<ColumnMapperFactory>emptyList());
	}
	
	@Test
	public void testsMoreThanOnePublicConstructorForm() {
		Assert.assertFalse(ConstructorAnnotationRowMapper.hasConstructorInTheCorrectForm(MultiplePublicConstructor.class));
	}
	
	@Test
	public void testMissingColumnAnnotationForm() {
		Assert.assertFalse(ConstructorAnnotationRowMapper.hasConstructorInTheCorrectForm(MissingColumn.class));
	}
	
	@Test
	public void testZeroArgConstructorForm() {
		Assert.assertFalse(ConstructorAnnotationRowMapper.hasConstructorInTheCorrectForm(ZeroArgConstructor.class));
	}

		
	public static class Mapping {
		public Mapping(@Column("COL_1") String a, @Column("COL_2") int b) {
		}
	}

	public static class ZeroArgConstructor {
		
		public ZeroArgConstructor() {
		}
		
	}
	
	public static class MultiplePublicConstructor {
		public MultiplePublicConstructor() {
		}

		public MultiplePublicConstructor(String s) {
		}
	}

	public static class MissingColumn {
		public MissingColumn(String a) {

		}
	}
}
