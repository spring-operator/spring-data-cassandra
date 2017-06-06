/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.cql.core;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.util.Assert;

import com.datastax.driver.core.Host;
import com.datastax.driver.core.exceptions.DriverException;

/**
 * {@link HostMapper} to to map hosts into {@link RingMember} objects.
 *
 * @author David Webb
 * @author Mark Paluch
 * @param <T>
 */
public enum RingMemberHostMapper implements HostMapper<RingMember> {

	INSTANCE;

	/* (non-Javadoc)
	 * @see org.springframework.data.cql.core.HostMapper#mapHosts(java.util.Iterable)
	 */
	@Override
	public Collection<RingMember> mapHosts(Iterable<Host> hosts) throws DriverException {

		Assert.notNull(hosts, "Hosts must not be null");

		return StreamSupport.stream(hosts.spliterator(), false).map(RingMember::from).collect(Collectors.toList());
	}
}
