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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.data.cassandra.core.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.core.mapping.CassandraPersistentProperty;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.util.ClassUtils;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;

/**
 * Reactive query executions for Cassandra.
 *
 * @author Mark Paluch
 * @since 2.0
 */
@FunctionalInterface
interface ReactiveCassandraQueryExecution {

	Object execute(Statement statement, Class<?> type);

	/**
	 * {@link ReactiveCassandraQueryExecution} for a {@link org.springframework.data.domain.Slice}.
	 *
	 * @author Hleb Albau
	 * @since 2.1
	 */
	@RequiredArgsConstructor
	final class SlicedExecution implements ReactiveCassandraQueryExecution {

		private final @NonNull ReactiveCassandraOperations operations;
		private final @NonNull Pageable pageable;

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.repository.query.CassandraQueryExecution#execute(java.lang.String, java.lang.Class)
		 */
		@Override
		public Object execute(Statement statement, Class<?> type) {

			CassandraPageRequest.validatePageable(pageable);

			Statement statementToUse = statement.setFetchSize(pageable.getPageSize());

			if (pageable instanceof CassandraPageRequest) {
				statementToUse = statementToUse.setPagingState(((CassandraPageRequest) pageable).getPagingState());
			}

			return operations.slice(statementToUse, type);
		}
	}

	/**
	 * {@link ReactiveCassandraQueryExecution} for collection returning queries.
	 *
	 * @author Mark Paluch
	 */
	@RequiredArgsConstructor
	final class CollectionExecution implements ReactiveCassandraQueryExecution {

		private final @NonNull ReactiveCassandraOperations operations;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.cassandra.repository.query.ReactiveCassandraQueryExecution#execute(java.lang.String, java.lang.Class)
		 */
		@Override
		public Object execute(Statement statement, Class<?> type) {
			return operations.select(statement, type);
		}
	}

	/**
	 * {@link ReactiveCassandraQueryExecution} to return a single entity.
	 *
	 * @author Mark Paluch
	 */
	@RequiredArgsConstructor
	final class SingleEntityExecution implements ReactiveCassandraQueryExecution {

		private final @NonNull ReactiveCassandraOperations operations;
		private final boolean limiting;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.cassandra.repository.query.ReactiveCassandraQueryExecution#execute(java.lang.String, java.lang.Class)
		 */
		@Override
		public Object execute(Statement statement, Class<?> type) {

			return operations.select(statement, type).buffer(2).map(objects -> {

				if (objects.isEmpty()) {
					return null;
				}

				if (objects.size() == 1 || limiting) {
					return objects.get(0);
				}

				throw new IncorrectResultSizeDataAccessException(1, objects.size());
			});
		}
	}

	/**
	 * {@link ReactiveCassandraQueryExecution} for an Exists query supporting count and regular row-data for exists
	 * calculation.
	 *
	 * @author Mark Paluch
	 * @since 2.1
	 */
	@RequiredArgsConstructor
	final class ExistsExecution implements ReactiveCassandraQueryExecution {

		private final @NonNull ReactiveCassandraOperations operations;

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.repository.query.ReactiveCassandraQueryExecution#execute(com.datastax.driver.core.Statement, java.lang.Class)
		 */
		@Override
		public Object execute(Statement statement, Class<?> type) {

			Mono<List<Row>> rows = this.operations.getReactiveCqlOperations().queryForRows(statement).buffer(2).next();

			return rows.map(it -> {

				if (it.isEmpty()) {
					return false;
				}

				if (it.size() == 1) {

					Row row = it.get(0);

					if (ProjectionUtil.qualifiesAsCountProjection(row)) {

						Object object = row.getObject(0);

						return ((Number) object).longValue() > 0;
					}
				}

				return true;
			}).switchIfEmpty(Mono.just(false));
		}
	}

	/**
	 * An {@link ReactiveCassandraQueryExecution} that wraps the results of the given delegate with the given result
	 * processing.
	 *
	 * @author Mark Paluch
	 */
	@RequiredArgsConstructor
	final class ResultProcessingExecution implements ReactiveCassandraQueryExecution {

		private final @NonNull ReactiveCassandraQueryExecution delegate;
		private final @NonNull Converter<Object, Object> converter;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.cassandra.repository.query.ReactiveCassandraQueryExecution#execute(java.lang.String, java.lang.Class)
		 */
		@SuppressWarnings("ConstantConditions")
		@Override
		public Object execute(Statement statement, Class<?> type) {
			return converter.convert(delegate.execute(statement, type));
		}
	}

	/**
	 * A {@link Converter} to post-process all source objects using the given {@link ResultProcessor}.
	 *
	 * @author Mark Paluch
	 */
	@RequiredArgsConstructor
	final class ResultProcessingConverter implements Converter<Object, Object> {

		private final @NonNull ResultProcessor processor;
		private final @NonNull MappingContext<? extends CassandraPersistentEntity<?>, CassandraPersistentProperty> mappingContext;
		private final @NonNull EntityInstantiators instantiators;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public Object convert(Object source) {

			ReturnedType returnedType = processor.getReturnedType();

			if (ClassUtils.isPrimitiveOrWrapper(returnedType.getReturnedType())) {
				return source;
			}

			if (returnedType.isInstance(source)) {
				return source;
			}

			Converter<Object, Object> converter = new DtoInstantiatingConverter(returnedType.getReturnedType(),
					mappingContext, instantiators);

			return processor.processResult(source, converter);
		}
	}
}
