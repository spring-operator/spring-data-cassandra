/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.cassandra.core.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;

/**
 * Identifies the annotated field of a composite primary key class as a primary key field that is either a partition or
 * cluster key field.
 *
 * @author Matthew T. Adams
 * @author Mark Paluch
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = { ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD })
public @interface PrimaryKeyColumn {

	/**
	 * The name of the column in the table.
	 */
	@AliasFor(attribute = "name")
	String value() default "";

	/**
	 * The name of the column in the table.
	 */
	@AliasFor(attribute = "value")
	String name() default "";

	/**
	 * The order of this column relative to other primary key columns.
	 */
	int ordinal() default Integer.MIN_VALUE;

	/**
	 * The type of this key column. Default is {@link PrimaryKeyType#CLUSTERED}.
	 */
	PrimaryKeyType type() default PrimaryKeyType.CLUSTERED;

	/**
	 * The cluster ordering of this column if {@link #type()} is {@link PrimaryKeyType#CLUSTERED}, otherwise ignored.
	 * Default is {@link Ordering#ASCENDING}.
	 */
	Ordering ordering() default Ordering.ASCENDING;

	/**
	 * Whether to cause the column name to be force-quoted.
	 */
	boolean forceQuote() default false;
}
