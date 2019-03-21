/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.cassandra.core;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.data.cassandra.core.cql.CqlIdentifier;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of {@link ExecutableDeleteOperation}.
 *
 * @author Mark Paluch
 * @see org.springframework.data.cassandra.core.ExecutableDeleteOperation
 * @see org.springframework.data.cassandra.core.query.Query
 * @since 2.1
 */
@RequiredArgsConstructor
class ExecutableDeleteOperationSupport implements ExecutableDeleteOperation {

	private final @NonNull CassandraTemplate template;

	/* (non-Javadoc)
	 * @see org.springframework.data.cassandra.core.ExecutableDeleteOperation#remove(java.lang.Class)
	 */
	@Override
	public ExecutableDelete delete(Class<?> domainType) {

		Assert.notNull(domainType, "DomainType must not be null");

		return new ExecutableDeleteSupport(this.template, domainType, Query.empty(), null);
	}

	// TODO: rethink the implementation
	// While the use of final fields and construction on mutation effectively makes this class Thread-safe,
	// it is possible this implementation could generate a high-level of young-gen garbage on the JVM heap,
	// particularly if the template delete(..) (and this class) are used inside of a loop for a large number
	// of domain types.  Of course, this assumption is highly contingent on the user's `Query`
	// in addition to his/her application design.

	@RequiredArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	static class ExecutableDeleteSupport implements ExecutableDelete, TerminatingDelete {

		@NonNull CassandraTemplate template;

		@NonNull Class<?> domainType;

		@NonNull Query query;

		@Nullable CqlIdentifier tableName;

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.ExecutableDeleteOperation.DeleteWithTable#inTable(org.springframework.data.cassandra.core.cql.CqlIdentifier)
		 */
		@Override
		public DeleteWithQuery inTable(CqlIdentifier tableName) {

			Assert.notNull(tableName, "Table name must not be null");

			return new ExecutableDeleteSupport(this.template, this.domainType, this.query, tableName);
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.ExecutableDeleteOperation.DeleteWithQuery#matching(org.springframework.data.cassandra.core.query.Query)
		 */
		@Override
		public TerminatingDelete matching(Query query) {

			Assert.notNull(query, "Query must not be null");

			return new ExecutableDeleteSupport(this.template, this.domainType, query, this.tableName);
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.ExecutableDeleteOperation.TerminatingDelete#all()
		 */
		public WriteResult all() {
			return this.template.doDelete(this.query, this.domainType, getTableName());
		}

		private CqlIdentifier getTableName() {
			return this.tableName != null ? this.tableName : this.template.getTableName(this.domainType);
		}
	}
}
