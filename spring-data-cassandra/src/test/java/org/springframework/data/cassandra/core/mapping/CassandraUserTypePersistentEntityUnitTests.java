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
package org.springframework.data.cassandra.core.mapping;

import static org.assertj.core.api.Assertions.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.cassandra.core.cql.CqlIdentifier;
import org.springframework.data.util.ClassTypeInformation;

/**
 * Unit tests for {@link CassandraUserTypePersistentEntity}.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class CassandraUserTypePersistentEntityUnitTests {

	@Mock UserTypeResolver userTypeResolverMock;

	@Test // DATACASS-172
	public void isUserDefinedTypeShouldReportTrue() {

		CassandraUserTypePersistentEntity<MappedUdt> type = getEntity(MappedUdt.class);

		assertThat(type.isUserDefinedType()).isTrue();
	}

	@Test // DATACASS-172
	public void getTableNameShouldReturnDefaultName() {

		CassandraUserTypePersistentEntity<MappedUdt> type = getEntity(MappedUdt.class);

		assertThat(type.getTableName()).isEqualTo(CqlIdentifier.of("mappedudt"));
		assertThat(type.getTableName()).isEqualTo(CqlIdentifier.of("Mappedudt"));
	}

	@Test // DATACASS-172
	public void getTableNameShouldReturnDefinedName() {

		CassandraUserTypePersistentEntity<WithName> type = getEntity(WithName.class);

		assertThat(type.getTableName()).isEqualTo(CqlIdentifier.of("withname"));
		assertThat(type.getTableName()).isEqualTo(CqlIdentifier.of("Withname"));
	}

	@Test // DATACASS-172
	public void getTableNameShouldReturnDefinedNameUsingForceQuote() {

		CassandraUserTypePersistentEntity<WithForceQuote> type = getEntity(WithForceQuote.class);

		assertThat(type.getTableName()).isNotEqualTo(CqlIdentifier.of("upperCase", true));
		assertThat(type.getTableName()).isEqualTo(CqlIdentifier.of("UpperCase", true));
	}

	@Test // DATACASS-259
	public void shouldConsiderComposedUserDefinedTypeAnnotation() {

		CassandraUserTypePersistentEntity<TypeWithComposedAnnotation> type = getEntity(TypeWithComposedAnnotation.class);

		assertThat(type.getTableName()).isEqualTo(CqlIdentifier.of("mytype", true));
	}

	private <T> CassandraUserTypePersistentEntity<T> getEntity(Class<T> entityClass) {
		return new CassandraUserTypePersistentEntity<>(ClassTypeInformation.from(entityClass), null, userTypeResolverMock);
	}

	@UserDefinedType
	static class MappedUdt {}

	@UserDefinedType("withname")
	static class WithName {}

	@UserDefinedType(value = "UpperCase", forceQuote = true)
	static class WithForceQuote {}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	@UserDefinedType(forceQuote = true)
	@interface ComposedUserDefinedTypeAnnotation {

		@AliasFor(annotation = UserDefinedType.class)
		String value() default "mytype";
	}

	@ComposedUserDefinedTypeAnnotation()
	static class TypeWithComposedAnnotation {}
}
