/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.data.cassandra.core.convert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.cassandra.test.util.RowMockUtil.column;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.data.cassandra.core.mapping.BasicCassandraPersistentEntity;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.Element;
import org.springframework.data.cassandra.core.mapping.Tuple;
import org.springframework.data.cassandra.test.util.RowMockUtil;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.TupleValue;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;

/**
 * Unit tests for mapped tuples through {@link MappingCassandraConverter}.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class MappingCassandraConverterMappedTupleUnitTests {

	CassandraMappingContext mappingContext;

	MappingCassandraConverter mappingCassandraConverter;

	Row rowMock;

	@Before
	public void setUp() {

		this.mappingContext = new CassandraMappingContext();
		this.mappingCassandraConverter = new MappingCassandraConverter(mappingContext);
		this.mappingCassandraConverter.afterPropertiesSet();
	}

	@Test // DATACASS-523
	public void shouldReadMappedTupleValue() {

		BasicCassandraPersistentEntity<?> entity = this.mappingContext.getRequiredPersistentEntity(MappedTuple.class);

		TupleValue value = entity.getTupleType().newValue("hello", 1);

		this.rowMock = RowMockUtil.newRowMock(
			column("name", "Jon Doe", DataType.text()),
			column("tuple", value, entity.getTupleType())
		);

		Person person = this.mappingCassandraConverter.read(Person.class, rowMock);

		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("Jon Doe");

		MappedTuple tuple = person.getTuple();

		assertThat(tuple.getName()).isEqualTo("hello");
		assertThat(tuple.getPosition()).isEqualTo(1);
	}

	@Test // DATACASS-523
	public void shouldWriteMappedTuple() {

		MappedTuple tuple = new MappedTuple("hello", 1);
		Person person = new Person("Jon Doe", tuple);

		Insert insert = QueryBuilder.insertInto("table");

		this.mappingCassandraConverter.write(person, insert);

		assertThat(insert.toString()).contains("VALUES ('Jon Doe',('hello',1))");
	}

	@Data
	@AllArgsConstructor
	private static class Person {
		String name;
		MappedTuple tuple;
	}

	@Tuple
	@Data
	@AllArgsConstructor
	private static class MappedTuple {
		@Element(0) String name;
		@Element(1) int position;
	}
}
