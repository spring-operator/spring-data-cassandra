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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.cql.CqlIdentifier;
import org.springframework.data.cassandra.core.cql.QueryOptions;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.UserTypeResolver;
import org.springframework.data.cassandra.domain.AddressType;
import org.springframework.data.cassandra.domain.Person;
import org.springframework.data.cassandra.repository.Consistency;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.support.UserTypeBuilder;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.query.ExtensionAwareQueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.ReflectionUtils;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.UserType;

/**
 * Unit tests for {@link StringBasedCassandraQuery}.
 *
 * @author Matthew T. Adams
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class StringBasedCassandraQueryUnitTests {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	@Mock private CassandraOperations operations;
	@Mock private UDTValue udtValue;
	@Mock private UserTypeResolver userTypeResolver;

	private RepositoryMetadata metadata;
	private MappingCassandraConverter converter;
	private ProjectionFactory factory;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		CassandraMappingContext mappingContext = new CassandraMappingContext();

		mappingContext.setUserTypeResolver(userTypeResolver);

		this.metadata = AbstractRepositoryMetadata.getMetadata(SampleRepository.class);
		this.converter = new MappingCassandraConverter(mappingContext);
		this.factory = new SpelAwareProxyProjectionFactory();

		this.converter.afterPropertiesSet();

		when(operations.getConverter()).thenReturn(converter);
	}

	@Test // DATACASS-117
	public void bindsIndexParameterCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByLastname", String.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), "Matthews");

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname = ?;");
		assertThat(actual.getObject(0)).isEqualTo("Matthews");
	}

	@Test // DATACASS-259
	public void bindsIndexParameterForComposedQueryAnnotationCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByComposedQueryAnnotation", String.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), "Matthews");

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname = ?;");
		assertThat(actual.getObject(0)).isEqualTo("Matthews");
	}

	@Test // DATACASS-117
	public void bindsAndEscapesIndexParameterCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByLastname", String.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), "Mat\th'ew\"s");

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname = ?;");
		assertThat(actual.getObject(0)).isEqualTo("Mat\th'ew\"s");
	}

	@Test // DATACASS-117
	public void bindsAndEscapesBytesIndexParameterCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByLastname", String.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }));

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname = ?;");
		assertThat(actual.getObject(0)).isEqualTo(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }));
	}

	@Test // DATACASS-117
	public void bindsIndexParameterInListCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByLastNameIn", Collection.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), Arrays.asList("White", "Heisenberg"));

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname IN (?);");
		assertThat(actual.getObject(0)).isEqualTo(Arrays.asList("White", "Heisenberg"));
	}

	@Test // DATACASS-117
	public void bindsIndexParameterIsListCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByLastNamesAndAge", Collection.class, int.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), Arrays.asList("White", "Heisenberg"), 42);

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastnames = [?] AND age = ?;");
		assertThat(actual.getObject(0)).isEqualTo(Arrays.asList("White", "Heisenberg"));
		assertThat(actual.getObject(1)).isEqualTo(42);
	}

	@Test(expected = QueryCreationException.class) // DATACASS-117
	public void referencingUnknownIndexedParameterShouldFail() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByOutOfBoundsLastNameShouldFail", String.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), "Hello");

		cassandraQuery.createQuery(accessor);
	}

	@Test(expected = QueryCreationException.class) // DATACASS-117
	public void referencingUnknownNamedParameterShouldFail() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByUnknownParameterLastNameShouldFail", String.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), "Hello");

		cassandraQuery.createQuery(accessor);
	}

	@Test // DATACASS-117
	public void bindsIndexParameterInSetCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByLastNameIn", Collection.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), new HashSet<>(Arrays.asList("White", "Heisenberg")));

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname IN (?);");
		assertThat(actual.getObject(0)).isEqualTo(new HashSet<>(Arrays.asList("White", "Heisenberg")));
	}

	@Test // DATACASS-117
	public void bindsNamedParameterCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByNamedParameter", String.class, String.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), "Walter", "Matthews");

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname = ?;");
		assertThat(actual.getObject(0)).isEqualTo("Matthews");
	}

	@Test // DATACASS-117
	public void bindsIndexExpressionParameterCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByIndexExpressionParameter", String.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), "Matthews");

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname = ?;");
		assertThat(actual.getObject(0)).isEqualTo("Matthews");
	}

	@Test // DATACASS-117
	public void bindsExpressionParameterCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByExpressionParameter", String.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), "Matthews");

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname = ?;");
		assertThat(actual.getObject(0)).isEqualTo("Matthews");
	}

	@Test // DATACASS-117
	public void bindsConditionalExpressionParameterCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByConditionalExpressionParameter", String.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), "Matthews");

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname = ?;");
		assertThat(actual.getObject(0)).isEqualTo("Woohoo");

		accessor = new CassandraParametersParameterAccessor(cassandraQuery.getQueryMethod(), "Walter");

		actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname = ?;");
		assertThat(actual.getObject(0)).isEqualTo("Walter");
	}

	@Test // DATACASS-117
	public void bindsReusedParametersCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByLastnameUsedTwice", String.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), "Matthews");

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname = ? or firstname = ?;");
		assertThat(actual.getObject(0)).isEqualTo("Matthews");
		assertThat(actual.getObject(1)).isEqualTo("Matthews");
	}

	@Test // DATACASS-117
	public void bindsMultipleParametersCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByLastnameAndFirstname", String.class, String.class);
		CassandraParametersParameterAccessor accessor = new CassandraParametersParameterAccessor(
				cassandraQuery.getQueryMethod(), "Matthews", "John");

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname=? AND firstname=?;");
		assertThat(actual.getObject(0)).isEqualTo("Matthews");
		assertThat(actual.getObject(1)).isEqualTo("John");
	}

	@Test // DATACASS-296
	public void bindsConvertedParameterCorrectly() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByCreatedDate", LocalDate.class);
		CassandraParameterAccessor accessor = new ConvertingParameterAccessor(converter,
				new CassandraParametersParameterAccessor(cassandraQuery.getQueryMethod(), LocalDate.of(2010, 7, 4)));

		SimpleStatement actual = cassandraQuery.createQuery(accessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE createdDate=?;");
		assertThat(actual.getObject(0)).isInstanceOf(com.datastax.driver.core.LocalDate.class);
		assertThat(actual.getObject(0).toString()).isEqualTo("2010-07-04");
	}

	@Test // DATACASS-172
	public void bindsMappedUdtPropertyCorrectly() throws Exception {

		UserType addressType = UserTypeBuilder.forName("address").withField("city", DataType.varchar())
				.withField("country", DataType.varchar()).build();

		when(userTypeResolver.resolveType(CqlIdentifier.of("address"))).thenReturn(addressType);

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByMainAddress", AddressType.class);
		CassandraParameterAccessor accessor = new ConvertingParameterAccessor(converter,
				new CassandraParametersParameterAccessor(cassandraQuery.getQueryMethod(), new AddressType()));

		SimpleStatement stringQuery = cassandraQuery.createQuery(accessor);

		assertThat(stringQuery.toString()).isEqualTo("SELECT * FROM person WHERE address=?;");
		assertThat(stringQuery.getObject(0).toString()).isEqualTo("{city:NULL,country:NULL}");
	}

	@Test // DATACASS-172
	public void bindsUdtValuePropertyCorrectly() throws Exception {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByMainAddress", UDTValue.class);
		CassandraParameterAccessor accessor = new ConvertingParameterAccessor(converter,
				new CassandraParametersParameterAccessor(cassandraQuery.getQueryMethod(), udtValue));

		SimpleStatement stringQuery = cassandraQuery.createQuery(accessor);

		assertThat(stringQuery.toString()).isEqualTo("SELECT * FROM person WHERE address=?;");
		assertThat(stringQuery.getObject(0).toString()).isEqualTo("udtValue");
	}

	@Test // DATACASS-146
	public void shouldApplyQueryOptions() {

		QueryOptions queryOptions = QueryOptions.builder().fetchSize(777).build();

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByLastname", QueryOptions.class, String.class);

		CassandraParametersParameterAccessor parameterAccessor =
				new CassandraParametersParameterAccessor(cassandraQuery.getQueryMethod(), queryOptions, "Matthews");

		SimpleStatement actual = cassandraQuery.createQuery(parameterAccessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname = ?;");
		assertThat(actual.getObject(0)).isEqualTo("Matthews");
		assertThat(actual.getFetchSize()).isEqualTo(777);
	}

	@Test // DATACASS-146
	public void shouldApplyConsistencyLevel() {

		StringBasedCassandraQuery cassandraQuery = getQueryMethod("findByLastname", String.class);

		CassandraParametersParameterAccessor parameterAccessor =
				new CassandraParametersParameterAccessor(cassandraQuery.getQueryMethod(), "Matthews");

		SimpleStatement actual = cassandraQuery.createQuery(parameterAccessor);

		assertThat(actual.toString()).isEqualTo("SELECT * FROM person WHERE lastname = ?;");
		assertThat(actual.getObject(0)).isEqualTo("Matthews");
		assertThat(actual.getConsistencyLevel()).isEqualTo(ConsistencyLevel.LOCAL_ONE);
	}

	private StringBasedCassandraQuery getQueryMethod(String name, Class<?>... args) {

		Method method = ReflectionUtils.findMethod(SampleRepository.class, name, args);

		CassandraQueryMethod queryMethod =
				new CassandraQueryMethod(method, metadata, factory, converter.getMappingContext());

		return new StringBasedCassandraQuery(queryMethod, operations, PARSER,
				ExtensionAwareQueryMethodEvaluationContextProvider.DEFAULT);
	}

	@SuppressWarnings("unused")
	private interface SampleRepository extends Repository<Person, String> {

		@Query("SELECT * FROM person WHERE lastname = ?0;")
		@Consistency(ConsistencyLevel.LOCAL_ONE)
		Person findByLastname(String lastname);

		@Query("SELECT * FROM person WHERE lastname = ?0;")
		Person findByLastname(QueryOptions queryOptions, String lastname);

		@Query("SELECT * FROM person WHERE lastname = ?0 or firstname = ?0;")
		Person findByLastnameUsedTwice(String lastname);

		@Query("SELECT * FROM person WHERE lastname = :lastname;")
		Person findByNamedParameter(@Param("another") String another, @Param("lastname") String lastname);

		@Query("SELECT * FROM person WHERE lastname = :#{[0]};")
		Person findByIndexExpressionParameter(String lastname);

		@Query("SELECT * FROM person WHERE lastnames = [?0] AND age = ?1;")
		Person findByLastNamesAndAge(Collection<String> lastname, int age);

		@Query("SELECT * FROM person WHERE lastname = ?0 AND age = ?2;")
		Person findByOutOfBoundsLastNameShouldFail(String lastname);

		@Query("SELECT * FROM person WHERE lastname = :unknown;")
		Person findByUnknownParameterLastNameShouldFail(String lastname);

		@Query("SELECT * FROM person WHERE lastname IN (?0);")
		Person findByLastNameIn(Collection<String> lastNames);

		@Query("SELECT * FROM person WHERE lastname = :#{#lastname};")
		Person findByExpressionParameter(@Param("lastname") String lastname);

		@Query("SELECT * FROM person WHERE lastname = :#{#lastname == 'Matthews' ? 'Woohoo' : #lastname};")
		Person findByConditionalExpressionParameter(@Param("lastname") String lastname);

		@Query("SELECT * FROM person WHERE lastname=?0 AND firstname=?1;")
		Person findByLastnameAndFirstname(String lastname, String firstname);

		@Query("SELECT * FROM person WHERE createdDate=?0;")
		Person findByCreatedDate(LocalDate createdDate);

		@Query("SELECT * FROM person WHERE address=?0;")
		Person findByMainAddress(AddressType address);

		@Query("SELECT * FROM person WHERE address=?0;")
		Person findByMainAddress(UDTValue udtValue);

		@ComposedQueryAnnotation
		Person findByComposedQueryAnnotation(String lastname);

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Query("SELECT * FROM person WHERE lastname = ?0;")
	@interface ComposedQueryAnnotation { }

}
