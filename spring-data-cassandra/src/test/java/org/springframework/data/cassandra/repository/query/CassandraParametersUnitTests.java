/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.cassandra.repository.query;

import static org.assertj.core.api.Assertions.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.domain.Person;

import com.datastax.driver.core.DataType.Name;

/**
 * Unit tests for {@link CassandraParameters}.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class CassandraParametersUnitTests {

	@Mock CassandraQueryMethod queryMethod;

	@Test // DATACASS-296
	public void shouldReturnUnknownDataTypeForSimpleType() throws Exception {

		Method method = PersonRepository.class.getMethod("findByFirstname", String.class);
		CassandraParameters cassandraParameters = new CassandraParameters(method);

		assertThat(cassandraParameters.getParameter(0).getCassandraType()).isNull();
	}

	@Test // DATACASS-296
	public void shouldReturnDataTypeForAnnotatedSimpleType() throws Exception {

		Method method = PersonRepository.class.getMethod("findByFirstTime", String.class);
		CassandraParameters cassandraParameters = new CassandraParameters(method);

		assertThat(cassandraParameters.getParameter(0).getCassandraType().type()).isEqualTo(Name.TIME);
	}

	@Test // DATACASS-296
	public void shouldReturnNoTypeForComplexType() throws Exception {

		Method method = PersonRepository.class.getMethod("findByObject", Object.class);
		CassandraParameters cassandraParameters = new CassandraParameters(method);

		assertThat(cassandraParameters.getParameter(0).getCassandraType()).isNull();
	}

	@Test // DATACASS-296
	public void shouldReturnTypeForAnnotatedType() throws Exception {

		Method method = PersonRepository.class.getMethod("findByAnnotatedObject", Object.class);
		CassandraParameters cassandraParameters = new CassandraParameters(method);

		assertThat(cassandraParameters.getParameter(0).getCassandraType().type()).isEqualTo(Name.TIME);
	}

	@Test // DATACASS-296
	public void shouldReturnTypeForComposedAnnotationType() throws Exception {

		Method method = PersonRepository.class.getMethod("findByComposedAnnotationObject", Object.class);
		CassandraParameters cassandraParameters = new CassandraParameters(method);

		assertThat(cassandraParameters.getParameter(0).getCassandraType().type()).isEqualTo(Name.BOOLEAN);
	}

	interface PersonRepository {

		Person findByFirstname(String firstname);

		Person findByFirstTime(@CassandraType(type = Name.TIME) String firstname);

		Person findByObject(Object firstname);

		Person findByAnnotatedObject(@CassandraType(type = Name.TIME) Object firstname);

		Person findByComposedAnnotationObject(@ComposedCassandraTypeAnnotation Object firstname);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@CassandraType(type = Name.BOOLEAN)
	@interface ComposedCassandraTypeAnnotation {
	}
}
