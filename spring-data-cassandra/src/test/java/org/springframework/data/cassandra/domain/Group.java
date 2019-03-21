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
package org.springframework.data.cassandra.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * @author Mark Paluch
 * @see <a href=
 *      "http://www.datastax.com/dev/blog/basic-rules-of-cassandra-data-modeling">http://www.datastax.com/dev/blog/basic-rules-of-cassandra-data-modeling</a>
 */
@Table
@Data
@NoArgsConstructor
public class Group {

	@PrimaryKey private GroupKey id;

	private String email;
	private int age;

	public Group(GroupKey id) {
		this.id = id;
	}
}
