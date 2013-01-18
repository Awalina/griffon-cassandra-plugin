/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.plugins.cassandra;

import griffon.util.CallableWithArgs;
import groovy.lang.Closure;

/**
 * @author Andres Almiray
 */
public interface CassandraContributionHandler {
    void setCassandraProvider(CassandraProvider provider);

    CassandraProvider getCassandraProvider();

    <R> R withCql(Closure<R> closure);

    <R> R withCql(String dataSourceName, Closure<R> closure);

    <R> R withCql(CallableWithArgs<R> callable);

    <R> R withCql(String dataSourceName, CallableWithArgs<R> callable);
}