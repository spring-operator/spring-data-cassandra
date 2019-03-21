/*
 * Copyright 2019 the original author or authors.
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

import lombok.EqualsAndHashCode;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.data.cassandra.core.cql.WriteOptions;
import org.springframework.data.cassandra.core.query.CriteriaDefinition;
import org.springframework.data.cassandra.core.query.Filter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.policies.RetryPolicy;

/**
 * Extension to {@link WriteOptions} for use with {@code DELETE} operations.
 *
 * @author Mark Paluch
 * @since 2.2
 */
@EqualsAndHashCode(callSuper = true)
public class DeleteOptions extends WriteOptions {

	private static final DeleteOptions EMPTY = new DeleteOptionsBuilder().build();

	private final boolean ifExists;

	private final @Nullable Filter ifCondition;

	private DeleteOptions(@Nullable ConsistencyLevel consistencyLevel, @Nullable RetryPolicy retryPolicy,
			@Nullable Boolean tracing, @Nullable Integer fetchSize, Duration readTimeout, Duration ttl,
			@Nullable Long timestamp, boolean ifExists, @Nullable Filter ifCondition) {

		super(consistencyLevel, retryPolicy, tracing, fetchSize, readTimeout, ttl, timestamp);

		this.ifExists = ifExists;
		this.ifCondition = ifCondition;
	}

	/**
	 * Create a new {@link DeleteOptionsBuilder}.
	 *
	 * @return a new {@link DeleteOptionsBuilder}.
	 */
	public static DeleteOptionsBuilder builder() {
		return new DeleteOptionsBuilder();
	}

	/**
	 * Create default {@link DeleteOptions}.
	 *
	 * @return default {@link DeleteOptions}.
	 */
	public static DeleteOptions empty() {
		return EMPTY;
	}

	/**
	 * Create a new {@link DeleteOptionsBuilder} to mutate properties of this {@link DeleteOptions}.
	 *
	 * @return a new {@link DeleteOptionsBuilder} initialized with this {@link DeleteOptions}.
	 */
	@Override
	public DeleteOptionsBuilder mutate() {
		return new DeleteOptionsBuilder(this);
	}

	/**
	 * @return {@literal true} to apply {@code IF EXISTS} to {@code DELETE} operations.
	 */
	public boolean isIfExists() {
		return this.ifExists;
	}

	/**
	 * @return the {@link Filter IF condition} for conditional deletes.
	 */
	@Nullable
	public Filter getIfCondition() {
		return ifCondition;
	}

	/**
	 * Builder for {@link DeleteOptions}.
	 *
	 * @author Mark Paluch
	 */
	public static class DeleteOptionsBuilder extends WriteOptionsBuilder {

		private boolean ifExists;

		private @Nullable Filter ifCondition;

		private DeleteOptionsBuilder() {}

		private DeleteOptionsBuilder(DeleteOptions deleteOptions) {

			super(deleteOptions);

			this.ifExists = deleteOptions.ifExists;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.cql.WriteOptions.WriteOptionsBuilder#consistencyLevel(com.datastax.driver.core.ConsistencyLevel)
		 */
		@Override
		public DeleteOptionsBuilder consistencyLevel(ConsistencyLevel consistencyLevel) {

			super.consistencyLevel(consistencyLevel);
			return this;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.cql.WriteOptions.WriteOptionsBuilder#retryPolicy(com.datastax.driver.core.policies.RetryPolicy)
		 */
		@Override
		public DeleteOptionsBuilder retryPolicy(RetryPolicy driverRetryPolicy) {

			super.retryPolicy(driverRetryPolicy);
			return this;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.cql.WriteOptions.WriteOptionsBuilder#fetchSize(int)
		 */
		@Override
		public DeleteOptionsBuilder fetchSize(int fetchSize) {

			super.fetchSize(fetchSize);
			return this;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.cql.WriteOptions.WriteOptionsBuilder#readTimeout(long)
		 */
		@Override
		public DeleteOptionsBuilder readTimeout(long readTimeout) {

			super.readTimeout(readTimeout);
			return this;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.cql.WriteOptions.WriteOptionsBuilder#readTimeout(long, java.util.concurrent.TimeUnit)
		 */
		@Override
		@Deprecated
		public DeleteOptionsBuilder readTimeout(long readTimeout, TimeUnit timeUnit) {

			super.readTimeout(readTimeout, timeUnit);
			return this;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.cql.WriteOptions.WriteOptionsBuilder#readTimeout(java.time.Duration)
		 */
		@Override
		public DeleteOptionsBuilder readTimeout(Duration readTimeout) {

			super.readTimeout(readTimeout);
			return this;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.cql.WriteOptions.WriteOptionsBuilder#ttl(java.time.Duration)
		 */
		@Override
		public DeleteOptionsBuilder ttl(Duration ttl) {

			super.ttl(ttl);
			return this;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.cql.WriteOptions.WriteOptionsBuilder#tracing(boolean)
		 */
		@Override
		public DeleteOptionsBuilder tracing(boolean tracing) {

			super.tracing(tracing);
			return this;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.cql.WriteOptions.WriteOptionsBuilder#withTracing()
		 */
		@Override
		public DeleteOptionsBuilder withTracing() {

			super.withTracing();
			return this;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.cql.WriteOptions.WriteOptionsBuilder#ttl(int)
		 */
		public DeleteOptionsBuilder ttl(int ttl) {

			super.ttl(ttl);
			return this;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.cql.WriteOptions.WriteOptionsBuilder#timestamp(long)
		 */
		@Override
		public DeleteOptionsBuilder timestamp(long timestamp) {

			super.timestamp(timestamp);
			return this;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.cassandra.core.cql.WriteOptions.WriteOptionsBuilder#timestamp(java.time.Instant)
		 */
		@Override
		public DeleteOptionsBuilder timestamp(Instant timestamp) {

			super.timestamp(timestamp);
			return this;
		}

		/**
		 * Use light-weight transactions by applying {@code IF EXISTS}. Replaces a previous {@link #ifCondition(Filter)}.
		 *
		 * @return {@code this} {@link DeleteOptionsBuilder}
		 */
		public DeleteOptionsBuilder withIfExists() {
			return ifExists(true);
		}

		/**
		 * Use light-weight transactions by applying {@code IF EXISTS}. Replaces a previous {@link #ifCondition(Filter)}.
		 *
		 * @param ifNotExists {@literal true} to enable {@code IF EXISTS}.
		 * @return {@code this} {@link DeleteOptionsBuilder}
		 */
		public DeleteOptionsBuilder ifExists(boolean ifNotExists) {

			this.ifExists = ifNotExists;
			this.ifCondition = null;

			return this;
		}

		/**
		 * Use light-weight transactions by applying {@code IF} {@link CriteriaDefinition condition}. Replaces a previous
		 * {@link #ifCondition(Filter)} and {@link #ifExists(boolean)}.
		 *
		 * @param criteria the {@link Filter criteria} to apply for conditional updates, must not be {@literal null}.
		 * @return {@code this} {@link DeleteOptionsBuilder}
		 */
		public DeleteOptionsBuilder ifCondition(CriteriaDefinition criteria) {

			Assert.notNull(criteria, "CriteriaDefinition must not be null");

			return ifCondition(Filter.from(criteria));
		}

		/**
		 * Use light-weight transactions by applying {@code IF} {@link Filter condition}. Replaces a previous
		 * {@link #ifCondition(Filter)} and {@link #ifExists(boolean)}.
		 *
		 * @param condition the {@link Filter condition} to apply for conditional deletes, must not be {@literal null}.
		 * @return {@code this} {@link DeleteOptionsBuilder}
		 */
		public DeleteOptionsBuilder ifCondition(Filter condition) {

			Assert.notNull(condition, "Filter condition must not be null");

			this.ifCondition = condition;
			this.ifExists = false;

			return this;
		}

		/**
		 * Builds a new {@link DeleteOptions} with the configured values.
		 *
		 * @return a new {@link DeleteOptions} with the configured values
		 */
		public DeleteOptions build() {
			return new DeleteOptions(this.consistencyLevel, this.retryPolicy, this.tracing, this.fetchSize, this.readTimeout,
					this.ttl, this.timestamp, this.ifExists, this.ifCondition);
		}
	}
}
