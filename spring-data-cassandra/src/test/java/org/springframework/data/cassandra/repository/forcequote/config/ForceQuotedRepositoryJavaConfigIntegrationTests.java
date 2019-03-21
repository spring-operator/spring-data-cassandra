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
package org.springframework.data.cassandra.repository.forcequote.config;

import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.cassandra.repository.support.IntegrationTestConfig;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Matthew T. Adams
 */
@ContextConfiguration
public class ForceQuotedRepositoryJavaConfigIntegrationTests extends ForceQuotedRepositoryIntegrationTests {

	@Configuration
	@EnableCassandraRepositories(basePackageClasses = ForceQuotedRepositoryTests.class)
	public static class Config extends IntegrationTestConfig {}

	@Test
	public void testExplicit() {
		tests.testExplicit(Explicit.TABLE_NAME);
	}

	@Test
	public void testExplicitPropertiesWithJavaValues() {
		tests.testExplicitProperties(ExplicitProperties.EXPLICIT_STRING_VALUE, ExplicitProperties.EXPLICIT_PRIMARY_KEY);
	}
}
