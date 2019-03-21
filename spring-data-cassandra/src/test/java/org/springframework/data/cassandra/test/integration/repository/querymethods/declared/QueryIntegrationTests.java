/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.data.cassandra.test.integration.repository.querymethods.declared;

import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.test.integration.repository.querymethods.declared.base.PersonRepository;
import org.springframework.data.cassandra.test.integration.support.AbstractSpringDataEmbeddedCassandraIntegrationTest;
import org.springframework.data.cassandra.test.integration.support.IntegrationTestConfig;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.datastax.driver.core.Session;

/**
 * Integration tests for use with {@link PersonRepository}.
 *
 * @author Matthew T. Adams
 * @author Mark Paluch
 * @soundtrack Mary Jane Kelly - Volbeat
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class QueryIntegrationTests extends AbstractSpringDataEmbeddedCassandraIntegrationTest {

	public static class Config extends IntegrationTestConfig {

		@Override
		public String[] getEntityBasePackages() {
			return new String[] { Person.class.getPackage().getName() };
		}

		@Override
		public SchemaAction getSchemaAction() {
			return SchemaAction.RECREATE_DROP_UNUSED;
		}
	}

	@Autowired PersonRepository personRepository;
	@Autowired Session session;

	@Before
	public void before() {
		deleteAllEntities();
	}

	@Test
	public void testListMethodSingleResult() {

		Person saved = new Person();
		saved.setFirstname(uuid());
		saved.setLastname(uuid());

		saved = personRepository.save(saved);

		List<Person> results = personRepository.findFolksWithLastnameAsList(saved.getLastname());

		assertThat(results).isNotNull();
		assertThat(results.size() == 1).isTrue();
		Person found = results.iterator().next();
		assertThat(found).isNotNull();
		assertThat(saved.getLastname()).isEqualTo(found.getLastname());
		assertThat(saved.getFirstname()).isEqualTo(found.getFirstname());
	}

	@Test
	public void testListMethodMultipleResults() {

		Person saved = new Person();
		saved.setFirstname("a");
		saved.setLastname(uuid());

		saved = personRepository.save(saved);

		Person saved2 = new Person();
		saved2.setFirstname("b");
		saved2.setLastname(saved.getLastname());

		saved2 = personRepository.save(saved2);

		List<Person> results = personRepository.findFolksWithLastnameAsList(saved.getLastname());

		assertThat(results).isNotNull();
		assertThat(results.size() == 2).isTrue();
		boolean first = true;
		for (Person person : results) {
			assertThat(person).isNotNull();
			assertThat(person.getLastname()).isEqualTo(saved.getLastname());
			assertThat(person.getFirstname()).isEqualTo(first ? saved.getFirstname() : saved2.getFirstname());
			first = false;
		}
	}

	@Test
	public void testListOfMapOfStringToObjectMethodSingleResult() {

		Person saved = new Person();
		saved.setFirstname(uuid());
		saved.setLastname(uuid());

		saved = personRepository.save(saved);

		List<Map<String, Object>> results = personRepository
				.findFolksWithLastnameAsListOfMapOfStringToObject(saved.getLastname());

		assertThat(results).isNotNull();
		assertThat(results.size() == 1).isTrue();
		Map<String, Object> found = results.iterator().next();
		assertThat(found).isNotNull();
		assertThat(saved.getLastname()).isEqualTo(found.get("lastname"));
		assertThat(saved.getFirstname()).isEqualTo(found.get("firstname"));
	}

	@Test
	public void testEntityMethodResult() {

		Person saved = new Person();
		saved.setFirstname(uuid());
		saved.setLastname(uuid());

		saved = personRepository.save(saved);

		Person found = personRepository.findSingle(saved.getLastname(), saved.getFirstname());

		assertThat(found).isNotNull();
		assertThat(saved.getLastname()).isEqualTo(found.getLastname());
		assertThat(saved.getFirstname()).isEqualTo(found.getFirstname());
	}

	@Test
	public void testListOfMapOfStringToObjectMethodMultipleResults() {

		Person saved = new Person();
		saved.setFirstname("a");
		saved.setLastname(uuid());

		saved = personRepository.save(saved);

		Person saved2 = new Person();
		saved2.setFirstname("b");
		saved2.setLastname(saved.getLastname());

		saved2 = personRepository.save(saved2);

		Collection<Person> results = personRepository.findFolksWithLastnameAsList(saved.getLastname());

		assertThat(results).isNotNull();
		assertThat(results.size() == 2).isTrue();
		boolean first = true;
		for (Person person : results) {
			assertThat(person).isNotNull();
			assertThat(person.getLastname()).isEqualTo(saved.getLastname());
			assertThat(person.getFirstname()).isEqualTo(first ? saved.getFirstname() : saved2.getFirstname());
			first = false;
		}
	}

	@Test
	public void testStringMethodResult() {

		Person saved = new Person();
		saved.setFirstname(uuid());
		saved.setLastname(uuid());
		saved.setNickname(uuid());

		saved = personRepository.save(saved);

		String nickname = personRepository.findSingleNickname(saved.getLastname(), saved.getFirstname());

		assertThat(nickname).isNotNull();
		assertThat(nickname).isEqualTo(saved.getNickname());
	}

	@Test
	public void testBooleanMethodResult() {

		Person saved = new Person();
		saved.setFirstname(uuid());
		saved.setLastname(uuid());
		saved.setCool(true);

		saved = personRepository.save(saved);

		boolean value = personRepository.findSingleCool(saved.getLastname(), saved.getFirstname());

		assertThat(value).isEqualTo(saved.isCool());
	}

	@Test
	public void testDateMethodResult() {

		Person saved = new Person();
		saved.setFirstname(uuid());
		saved.setLastname(uuid());
		saved.setBirthDate(new Date());

		saved = personRepository.save(saved);

		Date value = personRepository.findSingleBirthdate(saved.getLastname(), saved.getFirstname());

		assertThat(value).isEqualTo(saved.getBirthDate());
	}

	@Test
	public void testIntMethodResult() {

		Person saved = new Person();
		saved.setFirstname(uuid());
		saved.setLastname(uuid());
		saved.setNumberOfChildren(1);

		saved = personRepository.save(saved);

		int value = personRepository.findSingleNumberOfChildren(saved.getLastname(), saved.getFirstname());

		assertThat(value).isEqualTo(saved.getNumberOfChildren());
	}

	@Test
	public void testArrayMethodSingleResult() {

		Person saved = new Person();
		saved.setFirstname(uuid());
		saved.setLastname(uuid());

		saved = personRepository.save(saved);

		Person[] results = personRepository.findFolksWithLastnameAsArray(saved.getLastname());

		assertThat(results).isNotNull();
		assertThat(results.length == 1).isTrue();
		Person found = results[0];
		assertThat(found).isNotNull();
		assertThat(saved.getLastname()).isEqualTo(found.getLastname());
		assertThat(saved.getFirstname()).isEqualTo(found.getFirstname());
	}

	@Test
	public void testEscapeSingleQuoteInQueryParameterValue() {

		Person saved = new Person();
		saved.setFirstname("Bri'an" + uuid());
		String lastname = "O'Brian" + uuid();
		saved.setLastname(lastname);

		saved = personRepository.save(saved);

		List<Person> results = personRepository.findFolksWithLastnameAsList(lastname);

		assertThat(results).isNotNull();
		assertThat(results.size() == 1).isTrue();
		for (Person person : results) {
			assertThat(person).isNotNull();
			assertThat(person.getLastname()).isEqualTo(saved.getLastname());
			assertThat(person.getFirstname()).isEqualTo(saved.getFirstname());
		}
	}

	@Test
	public void findOptionalShouldReturnTargetType() {

		Person personToSave = new Person();

		personToSave.setFirstname(uuid());
		personToSave.setLastname(uuid());
		personToSave.setNumberOfChildren(1);

		personToSave = personRepository.save(personToSave);

		Optional<Person> savedPerson = personRepository.findOptionalWithLastnameAndFirstname(personToSave.getLastname(),
				personToSave.getFirstname());

		assertThat(savedPerson.isPresent()).isTrue();
	}

	@Test
	public void findOptionalShouldAbsentOptional() {

		Optional<Person> optional = personRepository.findOptionalWithLastnameAndFirstname("not", "existent");

		assertThat(optional.isPresent()).isFalse();
	}

	@Test // DATACASS-297
	public void streamShouldReturnEntities() {

		for (int i = 0; i < 100; i++) {
			Person person = new Person();

			person.setFirstname(uuid());
			person.setLastname(uuid());
			person.setNumberOfChildren(i);

			personRepository.save(person);
		}

		Stream<Person> allPeople = personRepository.findAllPeople();

		long count = allPeople.peek(new Consumer<Person>() {
			@Override
			public void accept(Person person) {
				assertThat(person).isInstanceOf(Person.class);
			}
		}).count();

		assertThat(count).isEqualTo(100L);
	}
}
