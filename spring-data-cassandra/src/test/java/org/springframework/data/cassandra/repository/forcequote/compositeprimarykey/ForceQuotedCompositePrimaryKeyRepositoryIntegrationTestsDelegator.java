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
package org.springframework.data.cassandra.repository.forcequote.compositeprimarykey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.repository.support.AbstractSpringDataEmbeddedCassandraIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Matthew T. Adams
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class ForceQuotedCompositePrimaryKeyRepositoryIntegrationTestsDelegator
		extends AbstractSpringDataEmbeddedCassandraIntegrationTest {

	ForceQuotedCompositePrimaryKeyRepositoryTests tests = new ForceQuotedCompositePrimaryKeyRepositoryTests();

	@Autowired ImplicitRepository implicitRepository;
	@Autowired ExplicitRepository explicitRepository;
	@Autowired CassandraTemplate cassandraTemplate;

	@Before
	public void before() {

		tests.implicitRepository = implicitRepository;
		tests.explicitRepository = explicitRepository;
		tests.cassandraTemplate = cassandraTemplate;

		tests.before();
	}

	@Test
	public void testImplicit() {
		tests.testImplicit();
	}

	public void testExplicit(String tableName, String stringValueColumnName, String keyZeroColumnName,
			String keyOneColumnName) {

		tests.testExplicit(tableName, stringValueColumnName, keyZeroColumnName, keyOneColumnName);
	}
}
