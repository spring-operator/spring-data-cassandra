/*
 * Copyright 2016-2019 the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Single;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.domain.AllPossibleTypes;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.threeten.bp.LocalDateTime;

import com.datastax.driver.core.DataType;

/**
 * Unit tests for {@link ReactiveCassandraParameterAccessor}.
 *
 * @author Mark Paluch
 * @soundtrack Ace Of Base - Cruel Summer (Album Edit)
 */
@RunWith(MockitoJUnitRunner.class)
public class ReactiveCassandraParameterAccessorUnitTests {

	private ReactiveCassandraParameterAccessor accessor;
	private GenericConversionService conversionService = new GenericConversionService();

	@Mock ProjectionFactory projectionFactory;

	RepositoryMetadata metadata = new DefaultRepositoryMetadata(PossibleRepository.class);
	CassandraMappingContext context = new CassandraMappingContext();

	@Test // DATACASS-335
	public void returnsCassandraSimpleType() throws Exception {

		Method method = PossibleRepository.class.getMethod("findByFirstname", Flux.class);
		ReactiveCassandraParameterAccessor accessor = new ReactiveCassandraParameterAccessor(
				getCassandraQueryMethod(method), new Object[] { Flux.just("firstname") });

		assertThat(accessor.getDataType(0)).isEqualTo(DataType.varchar());
	}

	@Test // DATACASS-335
	public void shouldReturnNoTypeForComplexTypes() throws Exception {

		Method method = PossibleRepository.class.getMethod("findByLocalDateTime", Mono.class);
		ReactiveCassandraParameterAccessor accessor = new ReactiveCassandraParameterAccessor(
				getCassandraQueryMethod(method), new Object[] { Flux.just(LocalDateTime.of(2000, 10, 11, 12, 13, 14)) });

		assertThat(accessor.getDataType(0)).isNull();

	}

	@Test // DATACASS-335
	public void returnTypeForAnnotatedParameter() throws Exception {

		Method method = PossibleRepository.class.getMethod("findByAnnotatedByLocalDateTime", Single.class);
		ReactiveCassandraParameterAccessor accessor = new ReactiveCassandraParameterAccessor(
				getCassandraQueryMethod(method), new Object[] { Single.just(LocalDateTime.of(2000, 10, 11, 12, 13, 14)) });

		assertThat(accessor.getDataType(0)).isEqualTo(DataType.date());
	}

	@Test // DATACASS-335
	public void returnTypeForAnnotatedParameterWhenUsingStringValue() throws Exception {

		Method method = PossibleRepository.class.getMethod("findByAnnotatedObject", Mono.class);
		ReactiveCassandraParameterAccessor accessor = new ReactiveCassandraParameterAccessor(
				getCassandraQueryMethod(method), new Object[] { Mono.just("") });

		assertThat(accessor.getDataType(0)).isEqualTo(DataType.date());
	}

	@Test // DATACASS-335
	public void returnTypeForAnnotatedParameterWhenUsingNullValue() throws Exception {

		Method method = PossibleRepository.class.getMethod("findByAnnotatedObject", Mono.class);
		ReactiveCassandraParameterAccessor accessor = new ReactiveCassandraParameterAccessor(
				getCassandraQueryMethod(method), new Object[] { Mono.just("") });

		assertThat(accessor.getDataType(0)).isEqualTo(DataType.date());
	}

	private CassandraQueryMethod getCassandraQueryMethod(Method method) {
		return new ReactiveCassandraQueryMethod(method, metadata, projectionFactory, context);
	}

	interface PossibleRepository extends Repository<AllPossibleTypes, Long> {

		Flux<AllPossibleTypes> findByFirstname(Flux<String> firstname);

		Flux<AllPossibleTypes> findByLocalDateTime(Mono<LocalDateTime> dateTime);

		Flux<AllPossibleTypes> findByAnnotatedByLocalDateTime(
				@CassandraType(type = DataType.Name.DATE) Single<LocalDateTime> dateTime);

		Flux<AllPossibleTypes> findByAnnotatedObject(@CassandraType(type = DataType.Name.DATE) Mono<Object> dateTime);
	}
}
