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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ThreeTenBackPortConverters;
import org.springframework.util.ClassUtils;
import org.threeten.bp.LocalDate;

/**
 * Helper class to register {@link Converter} implementations for the ThreeTen Backport project in case it's present on
 * the classpath.
 *
 * @author Mark Paluch
 * @see http://www.threeten.org/threetenbp
 * @since 1.5
 */
public abstract class CassandraThreeTenBackPortConverters {

	private static final boolean THREE_TEN_BACK_PORT_IS_PRESENT = ClassUtils.isPresent("org.threeten.bp.LocalDateTime",
			ThreeTenBackPortConverters.class.getClassLoader());

	private CassandraThreeTenBackPortConverters() {}

	/**
	 * Returns the converters to be registered. Will only return converters in case ThreeTen Backport is on the class
	 * path.
	 *
	 * @return
	 */
	public static Collection<Converter<?, ?>> getConvertersToRegister() {

		if (!THREE_TEN_BACK_PORT_IS_PRESENT) {
			return Collections.emptySet();
		}

		List<Converter<?, ?>> converters = new ArrayList<>();

		converters.add(CassandraLocalDateToLocalDateConverter.INSTANCE);
		converters.add(LocalDateToCassandraLocalDateConverter.INSTANCE);

		return converters;
	}

	/**
	 * Simple singleton to convert {@link com.datastax.driver.core.LocalDate}s to their {@link LocalDate} representation.
	 *
	 * @author Mark Paluch
	 */
	public enum CassandraLocalDateToLocalDateConverter
			implements Converter<com.datastax.driver.core.LocalDate, LocalDate> {

		INSTANCE;

		@Override
		public LocalDate convert(com.datastax.driver.core.LocalDate source) {
			return LocalDate.of(source.getYear(), source.getMonth(), source.getDay());
		}
	}

	/**
	 * Simple singleton to convert {@link LocalDate}s to their {@link com.datastax.driver.core.LocalDate} representation.
	 *
	 * @author Mark Paluch
	 */
	public enum LocalDateToCassandraLocalDateConverter
			implements Converter<LocalDate, com.datastax.driver.core.LocalDate> {

		INSTANCE;

		@Override
		public com.datastax.driver.core.LocalDate convert(LocalDate source) {
			return com.datastax.driver.core.LocalDate.fromYearMonthDay(source.getYear(), source.getMonthValue(),
					source.getDayOfMonth());
		}
	}
}
