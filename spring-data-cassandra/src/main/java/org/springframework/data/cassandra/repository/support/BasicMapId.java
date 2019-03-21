/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.cassandra.repository.support;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.cassandra.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.mapping.CassandraPersistentProperty;
import org.springframework.data.cassandra.repository.MapId;
import org.springframework.util.Assert;

/**
 * Simple implementation of {@link MapId}.
 * <p/>
 * <em>Note:</em> This could be extended in various cool ways, like one that takes a type and validates that the given
 * name corresponds to an actual field or bean property on that type. There could also be another one that uses a
 * {@link CassandraPersistentEntity} and {@link CassandraPersistentProperty} instead of a String name.
 *
 * @author Matthew T. Adams
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class BasicMapId implements MapId {

	private final Map<String, Serializable> map = new HashMap<String, Serializable>();

	/**
	 * Create a new and empty {@link BasicMapId}.
	 */
	public BasicMapId() {}

	/**
	 * Create a new {@link BasicMapId} given a {@link Map} of key-value tuples.
	 *
	 * @param map must not be {@literal null}.
	 */
	public BasicMapId(Map<String, Serializable> map) {

		Assert.notNull(map, "Map must not be null");
		this.map.putAll(map);
	}

	/**
	 * Factory method. Convenient if imported statically.
	 *
	 * @return {@link BasicMapId}
	 */
	public static MapId id() {
		return new BasicMapId();
	}

	/**
	 * Factory method. Convenient if imported statically.
	 *
	 * @return {@link BasicMapId}
	 */
	public static MapId id(String name, Serializable value) {
		return new BasicMapId().with(name, value);
	}

	/**
	 * Factory method. Convenient if imported statically.
	 *
	 * @return {@link BasicMapId}
	 */
	public static MapId id(MapId id) {
		return new BasicMapId(id);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.cassandra.repository.MapId#with(java.lang.String, java.io.Serializable)
	 */
	@Override
	public BasicMapId with(String name, Serializable value) {
		put(name, value);
		return this;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	@Override
	public void clear() {
		map.clear();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(Object name) {
		return map.containsKey(name);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	@Override
	public Set<java.util.Map.Entry<String, Serializable>> entrySet() {
		return map.entrySet();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object that) {
		if (this == that) {
			return true;
		}
		if (that == null) {
			return false;
		}
		if (!(that instanceof Map)) { // we can be equal to a Map
			return false;
		}
		return map.equals(that);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	@Override
	public Serializable get(Object name) {
		return map.get(name);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return map.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	@Override
	public Set<String> keySet() {
		return map.keySet();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Serializable put(String name, Serializable value) {
		return map.put(name, value);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	@Override
	public void putAll(Map<? extends String, ? extends Serializable> source) {
		map.putAll(source);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	@Override
	public Serializable remove(Object name) {
		return map.remove(name);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	@Override
	public int size() {
		return map.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	@Override
	public Collection<Serializable> values() {
		return map.values();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		StringBuilder s = new StringBuilder("{ ");

		boolean first = true;
		for (Map.Entry<String, Serializable> entry : map.entrySet()) {

			if (first) {
				first = false;
			} else {
				s.append(", ");
			}

			s.append(entry.getKey()).append(" : ").append(entry.getValue());
		}

		return s.append(" }").toString();
	}
}
