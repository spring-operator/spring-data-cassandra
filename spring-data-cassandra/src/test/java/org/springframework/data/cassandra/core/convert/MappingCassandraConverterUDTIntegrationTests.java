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
package org.springframework.data.cassandra.core.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.CqlIdentifier;
import org.springframework.data.cassandra.core.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;
import org.springframework.data.cassandra.core.mapping.UserTypeResolver;
import org.springframework.data.cassandra.domain.AllPossibleTypes;
import org.springframework.data.cassandra.repository.support.AbstractSpringDataEmbeddedCassandraIntegrationTest;
import org.springframework.data.cassandra.repository.support.IntegrationTestConfig;
import org.springframework.data.convert.CustomConversions;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.UserType;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;

/**
 * Integration tests for UDT types through {@link MappingCassandraConverter}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class MappingCassandraConverterUDTIntegrationTests extends AbstractSpringDataEmbeddedCassandraIntegrationTest {

	private static AtomicBoolean initialized = new AtomicBoolean();

	@Configuration
	public static class Config extends IntegrationTestConfig {

		@Override
		public SchemaAction getSchemaAction() {
			return SchemaAction.NONE;
		}

		@Override
		public String[] getEntityBasePackages() {
			return new String[] { AllPossibleTypes.class.getPackage().getName() };
		}

		@Override
		public CustomConversions customConversions() {
			return new CassandraCustomConversions(Arrays.asList(new UDTToCurrencyConverter(),
					new CurrencyToUDTConverter(new SimpleUserTypeResolver(cluster().getObject(), getKeyspaceName()))));
		}
	}

	@Autowired Session session;
	@Autowired MappingCassandraConverter converter;

	@Before
	public void setUp() {

		if (initialized.compareAndSet(false, true)) {

			session.execute("DROP TABLE IF EXISTS addressbook;");
			session.execute("CREATE TYPE IF NOT EXISTS address (zip text, city text, streetlines list<text>);");
			session.execute("CREATE TABLE addressbook (id text PRIMARY KEY, currentaddress FROZEN<address>, "
					+ "alternate FROZEN<address>, previousaddresses FROZEN<list<address>>);");

			session.execute("DROP TABLE IF EXISTS bank;");
			session.execute("CREATE TYPE IF NOT EXISTS currency (currency text);");
			session.execute(
					"CREATE TABLE bank (id text PRIMARY KEY, currency FROZEN<currency>, othercurrencies FROZEN<list<currency>>);");

			session.execute("DROP TABLE IF EXISTS money;");
			session.execute("CREATE TYPE IF NOT EXISTS currency (currency text);");
			session.execute("CREATE TABLE money (currency FROZEN<currency> PRIMARY KEY);");

			session.execute("DROP TABLE IF EXISTS car;");
			session.execute("CREATE TYPE IF NOT EXISTS manufacturer (name text);");
			session.execute("CREATE TYPE IF NOT EXISTS engine (manufacturer FROZEN<manufacturer>);");
			session.execute("CREATE TABLE car (id text PRIMARY KEY, engine FROZEN<engine>);");

			session.execute("DROP TABLE IF EXISTS supplier;");
			session.execute(
					"CREATE TABLE supplier (id text PRIMARY KEY, acceptedCurrencies frozen<map<manufacturer, list<currency>>>);");

		} else {

			session.execute("TRUNCATE addressbook;");
			session.execute("TRUNCATE bank;");
			session.execute("TRUNCATE money;");
			session.execute("TRUNCATE car;");
			session.execute("TRUNCATE supplier;");
		}
	}

	@Test // DATACASS-172
	public void shouldReadMappedUdt() {

		session.execute("INSERT INTO addressbook (id, currentaddress) " + "VALUES ('1', "
				+ "{zip:'69469', city: 'Weinheim', streetlines: ['Heckenpfad', '14']});");

		ResultSet resultSet = session.execute("SELECT * from addressbook");
		AddressBook addressBook = converter.read(AddressBook.class, resultSet.one());

		assertThat(addressBook.getCurrentaddress()).isNotNull();

		AddressUserType address = addressBook.getCurrentaddress();
		assertThat(address.getCity()).isEqualTo("Weinheim");
		assertThat(address.getZip()).isEqualTo("69469");
		assertThat(address.getStreetLines()).contains("Heckenpfad", "14");
	}

	@Test // DATACASS-172
	public void shouldWriteMappedUdt() {

		AddressUserType addressUserType = new AddressUserType();
		addressUserType.setZip("69469");
		addressUserType.setCity("Weinheim");
		addressUserType.setStreetLines(Arrays.asList("Heckenpfad", "14"));

		AddressBook addressBook = new AddressBook();
		addressBook.setId("1");
		addressBook.setCurrentaddress(addressUserType);

		Insert insert = QueryBuilder.insertInto("addressbook");
		converter.write(addressBook, insert);

		assertThat(insert.toString()).isEqualTo("INSERT INTO addressbook (currentaddress,id) "
				+ "VALUES ({zip:'69469',city:'Weinheim',streetlines:['Heckenpfad','14']},'1');");
	}

	@Test // DATACASS-172
	public void shouldReadMappedUdtCollection() {

		session.execute("INSERT INTO addressbook (id,  previousaddresses) " + "VALUES ('1', "
				+ " [{zip:'53773', city: 'Bonn'}, {zip:'12345', city: 'Bonn'}]);");

		ResultSet resultSet = session.execute("SELECT * from addressbook");
		AddressBook addressBook = converter.read(AddressBook.class, resultSet.one());

		assertThat(addressBook.getPreviousaddresses()).hasSize(2);

		AddressUserType address = addressBook.getPreviousaddresses().get(0);

		assertThat(address.getCity()).isEqualTo("Bonn");
		assertThat(address.getZip()).isEqualTo("53773");
		assertThat(address.getStreetLines()).isEmpty();
	}

	@Test // DATACASS-172
	public void shouldWriteMappedUdtCollection() {

		AddressUserType addressUserType = new AddressUserType();
		addressUserType.setZip("69469");
		addressUserType.setCity("Weinheim");
		addressUserType.setStreetLines(Arrays.asList("Heckenpfad", "14"));

		AddressBook addressBook = new AddressBook();
		addressBook.setId("1");
		addressBook.setPreviousaddresses(Collections.singletonList(addressUserType));

		Insert insert = QueryBuilder.insertInto("addressbook");
		converter.write(addressBook, insert);

		assertThat(insert.toString()).isEqualTo("INSERT INTO addressbook (id,previousaddresses) "
				+ "VALUES ('1',[{zip:'69469',city:'Weinheim',streetlines:['Heckenpfad','14']}]);");
	}

	@Test // DATACASS-172
	public void shouldReadUdt() {

		session.execute("INSERT INTO addressbook (id, alternate) " + "VALUES ('1', "
				+ "{zip:'69469', city: 'Weinheim', streetlines: ['Heckenpfad', '14']});");

		ResultSet resultSet = session.execute("SELECT * from addressbook");
		AddressBook addressBook = converter.read(AddressBook.class, resultSet.one());

		assertThat(addressBook.getAlternate()).isNotNull();
		assertThat(addressBook.getAlternate().getString("city")).isEqualTo("Weinheim");
		assertThat(addressBook.getAlternate().getString("zip")).isEqualTo("69469");
	}

	@Test // DATACASS-172
	public void shouldWriteUdt() {

		CassandraPersistentEntity<?> persistentEntity = converter.getMappingContext()
				.getRequiredPersistentEntity(AddressUserType.class);
		UDTValue udtValue = persistentEntity.getUserType().newValue();
		udtValue.setString("zip", "69469");
		udtValue.setString("city", "Weinheim");
		udtValue.setList("streetlines", Arrays.asList("Heckenpfad", "14"));

		AddressBook addressBook = new AddressBook();
		addressBook.setId("1");
		addressBook.setAlternate(udtValue);

		Insert insert = QueryBuilder.insertInto("addressbook");
		converter.write(addressBook, insert);

		assertThat(insert.toString()).isEqualTo("INSERT INTO addressbook (alternate,id) "
				+ "VALUES ({zip:'69469',city:'Weinheim',streetlines:['Heckenpfad','14']},'1');");
	}

	@Test // DATACASS-172
	public void shouldWriteUdtPk() {

		AddressUserType addressUserType = new AddressUserType();
		addressUserType.setZip("69469");
		addressUserType.setCity("Weinheim");
		addressUserType.setStreetLines(Arrays.asList("Heckenpfad", "14"));

		WithMappedUdtId withUdtId = new WithMappedUdtId();
		withUdtId.setId(addressUserType);

		Insert insert = QueryBuilder.insertInto("addressbook");
		converter.write(withUdtId, insert);

		assertThat(insert.toString()).isEqualTo(
				"INSERT INTO addressbook (id) " + "VALUES ({zip:'69469',city:'Weinheim',streetlines:['Heckenpfad','14']});");
	}

	@Test // DATACASS-172
	public void shouldWriteMappedUdtPk() {

		CassandraPersistentEntity<?> persistentEntity = converter.getMappingContext()
				.getRequiredPersistentEntity(AddressUserType.class);
		UDTValue udtValue = persistentEntity.getUserType().newValue();
		udtValue.setString("zip", "69469");
		udtValue.setString("city", "Weinheim");
		udtValue.setList("streetlines", Arrays.asList("Heckenpfad", "14"));

		WithUdtId withUdtId = new WithUdtId();
		withUdtId.setId(udtValue);

		Insert insert = QueryBuilder.insertInto("addressbook");
		converter.write(withUdtId, insert);

		assertThat(insert.toString()).isEqualTo(
				"INSERT INTO addressbook (id) " + "VALUES ({zip:'69469',city:'Weinheim',streetlines:['Heckenpfad','14']});");
	}

	@Test // DATACASS-172
	public void shouldReadUdtWithCustomConversion() {

		session.execute("INSERT INTO bank (id, currency) " + "VALUES ('1', {currency:'EUR'});");

		ResultSet resultSet = session.execute("SELECT * from bank");
		Bank addressBook = converter.read(Bank.class, resultSet.one());

		assertThat(addressBook.getCurrency()).isNotNull();
		assertThat(addressBook.getCurrency().getCurrencyCode()).isEqualTo("EUR");
	}

	@Test // DATACASS-172
	public void shouldReadUdtListWithCustomConversion() {

		session.execute("INSERT INTO bank (id, othercurrencies) " + "VALUES ('1', [{currency:'EUR'}]);");

		ResultSet resultSet = session.execute("SELECT * from bank");
		Bank addressBook = converter.read(Bank.class, resultSet.one());

		assertThat(addressBook.getOtherCurrencies()).hasSize(1).contains(Currency.getInstance("EUR"));
	}

	@Test // DATACASS-172, DATACASS-400
	public void shouldWriteUdtWithCustomConversion() {

		Bank bank = new Bank(null, Currency.getInstance("EUR"), null);

		Insert insert = QueryBuilder.insertInto("bank");
		converter.write(bank, insert);

		assertThat(insert.toString()).isEqualTo("INSERT INTO bank (currency) VALUES ({currency:'EUR'});");
	}

	@Test // DATACASS-172
	public void shouldWriteUdtUpdateWherePrimaryKeyWithCustomConversion() {

		Money money = new Money();
		money.setCurrency(Currency.getInstance("EUR"));

		Update update = QueryBuilder.update("money");
		converter.write(money, update);

		assertThat(update.toString()).isEqualTo("UPDATE money WHERE currency={currency:'EUR'};");
	}

	@Test // DATACASS-172, DATACASS-400
	public void shouldWriteUdtUpdateAssignmentsWithCustomConversion() {

		MoneyTransfer money = new MoneyTransfer("1", Currency.getInstance("EUR"));

		Update update = QueryBuilder.update("money");
		converter.write(money, update);

		assertThat(update.toString()).isEqualTo("UPDATE money SET currency={currency:'EUR'} WHERE id='1';");
	}

	@Test // DATACASS-172
	public void shouldWriteUdtSelectWherePrimaryKeyWithCustomConversion() {

		Money money = new Money();
		money.setCurrency(Currency.getInstance("EUR"));

		Select select = QueryBuilder.select().from("money");
		converter.write(money, select.where());

		assertThat(select.toString()).isEqualTo("SELECT * FROM money WHERE currency={currency:'EUR'};");
	}

	@Test // DATACASS-172
	public void shouldWriteUdtDeleteWherePrimaryKeyWithCustomConversion() {

		Money money = new Money();
		money.setCurrency(Currency.getInstance("EUR"));

		Delete delete = QueryBuilder.delete().from("money");
		converter.write(money, delete.where());

		assertThat(delete.toString()).isEqualTo("DELETE FROM money WHERE currency={currency:'EUR'};");
	}

	@Test // DATACASS-172, DATACASS-400
	public void shouldWriteUdtListWithCustomConversion() {

		Bank bank = new Bank(null, null, Collections.singletonList(Currency.getInstance("EUR")));

		Insert insert = QueryBuilder.insertInto("bank");
		converter.write(bank, insert);

		assertThat(insert.toString()).isEqualTo("INSERT INTO bank (othercurrencies) VALUES ([{currency:'EUR'}]);");
	}

	@Test // DATACASS-172
	public void shouldReadNestedUdt() {

		session.execute("INSERT INTO car (id, engine)  VALUES ('1',  {manufacturer: {name:'a good one'}});");

		ResultSet resultSet = session.execute("SELECT * from car");
		Car car = converter.read(Car.class, resultSet.one());

		assertThat(car.getEngine()).isNotNull();
		assertThat(car.getEngine().getManufacturer()).isNotNull();
		assertThat(car.getEngine().getManufacturer().getName()).isEqualTo("a good one");
	}

	@Test // DATACASS-172, DATACASS-400
	public void shouldWriteNestedUdt() {

		Engine engine = new Engine(new Manufacturer("a good one"));

		Car car = new Car("1", engine);

		Insert insert = QueryBuilder.insertInto("car");
		converter.write(car, insert);

		assertThat(insert.toString())
				.isEqualTo("INSERT INTO car (engine,id) VALUES ({manufacturer:{name:'a good one'}},'1');");
	}

	@Test // DATACASS-487
	public void shouldReadUdtInMap() {

		this.session.execute("INSERT INTO supplier (id, acceptedCurrencies)"
			+ " VALUES ('1', {{name:'a good one'}:[{currency:'EUR'},{currency:'USD'}]});");

		ResultSet resultSet = this.session.execute("SELECT * FROM supplier");
		Supplier supplier = this.converter.read(Supplier.class, resultSet.one());

		assertThat(supplier.getAcceptedCurrencies()).isNotEmpty();

		List<Currency> currencies = supplier.getAcceptedCurrencies().get(new Manufacturer("a good one"));

		assertThat(currencies).contains(Currency.getInstance("EUR"), Currency.getInstance("USD"));
	}

	@Table
	@Getter
	@AllArgsConstructor
	private static class Bank {

		@Id String id;
		Currency currency;
		List<Currency> otherCurrencies;
	}

	@Data
	@Table
	public static class Money {
		@Id private Currency currency;
	}

	@Table
	@AllArgsConstructor
	@Getter
	public static class MoneyTransfer {

		@Id String id;

		private Currency currency;
	}

	@Table
	@Getter
	@AllArgsConstructor
	private static class Car {

		@Id String id;
		Engine engine;
	}

	@UserDefinedType
	@Getter
	@AllArgsConstructor
	private static class Engine {
		Manufacturer manufacturer;
	}

	@UserDefinedType
	@Data
	@AllArgsConstructor
	private static class Manufacturer {
		String name;
	}

	@Table
	@Data
	@AllArgsConstructor
	private static class Supplier {

		@Id String id;
		Map<Manufacturer, List<Currency>> acceptedCurrencies;
	}

	@Data
	@Table
	public static class AddressBook {

		@Id private String id;

		private AddressUserType currentaddress;
		private List<AddressUserType> previousaddresses;
		private UDTValue alternate;
	}

	@Data
	@Table
	public static class WithUdtId {
		@Id private UDTValue id;
	}

	@Data
	@Table
	public static class WithMappedUdtId {
		@Id private AddressUserType id;
	}

	@UserDefinedType("address")
	@Data
	public static class AddressUserType {

		String zip;
		String city;

		List<String> streetLines;
	}

	private static class UDTToCurrencyConverter implements Converter<UDTValue, Currency> {

		@Override
		public Currency convert(UDTValue source) {
			return Currency.getInstance(source.getString("currency"));
		}
	}

	private static class CurrencyToUDTConverter implements Converter<Currency, UDTValue> {

		final UserTypeResolver userTypeResolver;

		CurrencyToUDTConverter(UserTypeResolver userTypeResolver) {
			this.userTypeResolver = userTypeResolver;
		}

		@Override
		public UDTValue convert(Currency source) {
			UserType userType = userTypeResolver.resolveType(CqlIdentifier.of("currency"));
			UDTValue udtValue = userType.newValue();
			udtValue.setString("currency", source.getCurrencyCode());
			return udtValue;
		}
	}
}
