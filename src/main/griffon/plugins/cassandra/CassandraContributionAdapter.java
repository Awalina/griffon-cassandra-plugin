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

import java.util.Map;

/**
 * @author Andres Almiray
 */
public class CassandraContributionAdapter implements CassandraContributionHandler {
    private static final String DEFAULT = "default";

    private CassandraProvider provider = DefaultCassandraProvider.getInstance();

    public void setCassandraProvider(CassandraProvider provider) {
        this.provider = provider != null ? provider : DefaultCassandraProvider.getInstance();
    }

    public CassandraProvider getCassandraProvider() {
        return provider;
    }

    public <R> R withCql(Closure<R> closure) {
        return withCql(DEFAULT, closure);
    }

    public <R> R withCql(String dataSourceName, Closure<R> closure) {
        return provider.withCql(dataSourceName, closure);
    }

    public <R> R withCql(CallableWithArgs<R> callable) {
        return withCql(DEFAULT, callable);
    }

    public <R> R withCql(String dataSourceName, CallableWithArgs<R> callable) {
        return provider.withCql(dataSourceName, callable);
    }
}