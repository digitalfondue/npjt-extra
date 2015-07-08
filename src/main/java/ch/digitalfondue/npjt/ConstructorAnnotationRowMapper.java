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
package ch.digitalfondue.npjt;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

import ch.digitalfondue.npjt.mapper.ColumnMapper;
import ch.digitalfondue.npjt.mapper.ColumnMapperFactory;

public class ConstructorAnnotationRowMapper<T> implements RowMapper<T> {

	private final Constructor<T> con;
	private final ColumnMapper[] mappedColumn;

	/**
	 * Check if the given class has the correct form.
	 * 
	 * <ul>
	 * <li>must have exactly one public constructor.</li>
	 * <li>must at least have one parameter.</li>
	 * <li>all the parameters must be annotated with @Column annotation.</li>
	 * </ul>
	 * 
	 * @param clazz
	 * @return
	 */
	public static boolean hasConstructorInTheCorrectForm(Class<?> clazz) {
		
		if (clazz.getConstructors().length != 1) {
			return false;
		}

		Constructor<?> con = clazz.getConstructors()[0];

		if (con.getParameterTypes().length == 0) {
			return false;
		}

		Annotation[][] parameterAnnotations = con.getParameterAnnotations();
		for (Annotation[] as : parameterAnnotations) {
			if (!hasColumnAnnotation(as)) {
				return false;
			}
		}

		return true;
	}

	private static boolean hasColumnAnnotation(Annotation[] as) {
		if (as == null || as.length == 0) {
			return false;
		}
		for (Annotation a : as) {
			if (a.annotationType().isAssignableFrom(Column.class)) {
				return true;
			}
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	public ConstructorAnnotationRowMapper(Class<T> clazz, Collection<ColumnMapperFactory> columnMapperFactories) {
		int constructorCount = clazz.getConstructors().length;
		Assert.isTrue(constructorCount == 1, "The class " + clazz.getName()
				+ " must have exactly one public constructor, "
				+ constructorCount + " are present");

		con = (Constructor<T>) clazz.getConstructors()[0];
		mappedColumn = from(clazz, con.getParameterAnnotations(), con.getParameterTypes(), columnMapperFactories);
	}

	@Override
	public T mapRow(ResultSet rs, int rowNum) throws SQLException {
		Object[] vals = new Object[mappedColumn.length];

		for(int i = 0; i < mappedColumn.length; i++) {
			vals[i] = mappedColumn[i].getObject(rs);
		}
		

		try {
			return con.newInstance(vals);
		} catch (ReflectiveOperationException e) {
			throw new SQLException(e);
		} catch (IllegalArgumentException e) {
			throw new SQLException(
					"type mismatch between the expected one from the construct and the one passed,"
							+ " check 1: some values are null and passed to primitive types 2: incompatible numeric types",
					e);
		}
	}

	private static ColumnMapper[] from(Class<?> clazz, Annotation[][] annotations, Class<?>[] paramTypes, Collection<ColumnMapperFactory> columnMapperFactories) {
		ColumnMapper[] res = new ColumnMapper[annotations.length];
		for (int i = 0; i < annotations.length; i++) {
			res[i] = findColumnAnnotationValue(clazz, i, annotations[i], paramTypes[i], columnMapperFactories);
		}
		return res;
	}

	private static ColumnMapper findColumnAnnotationValue(Class<?> clazz,
			int position, Annotation[] annotations, Class<?> paramType, Collection<ColumnMapperFactory> columnMapperFactories) {

		for (Annotation a : annotations) {
			if (Column.class.isAssignableFrom(a.annotationType())) {
				String name = ((Column) a).value();
				for(ColumnMapperFactory factory : columnMapperFactories) {
					if(factory.accept(paramType)) {
						return factory.build(name, paramType);
					}
				}
				throw new IllegalStateException(
						"Did not found any matching ColumnMapperFactory for class: "
								+ clazz.getName()
								+ " in constructor at position " + position);
			}
		}

		throw new IllegalStateException(
				"No annotation @Column found for class: " + clazz.getName()
						+ " in constructor at position " + position);
	}

	

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface Column {
		/**
		 * Column name
		 * 
		 * @return
		 */
		String value();
	}

}
