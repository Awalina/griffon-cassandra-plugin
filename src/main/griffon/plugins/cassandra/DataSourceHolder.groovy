/*
 * Copyright 2009-2013 the original author or authors.
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

package griffon.plugins.cassandra

import javax.sql.DataSource

import griffon.core.GriffonApplication
import griffon.util.ApplicationHolder
import static griffon.util.GriffonNameUtils.isBlank

/**
 * @author Andres Almiray
 */
class DataSourceHolder {
    private static final String DEFAULT = 'default'
    private static final Object[] LOCK = new Object[0]
    private final Map<String, DataSource> dataSources = [:]

    private static final DataSourceHolder INSTANCE

    static {
        INSTANCE = new DataSourceHolder()
    }

    static DataSourceHolder getInstance() {
        INSTANCE
    }

    private DataSourceHolder() {}

    String[] getDataSourceNames() {
        List<String> dataSourceNames = new ArrayList<String>()
        dataSourceNames.addAll(dataSources.keySet())
        dataSourceNames.toArray(new String[dataSourceNames.size()])
    }

    DataSource getDataSource(String dataSourceName = DEFAULT) {
        if (isBlank(dataSourceName)) dataSourceName = DEFAULT
        retrieveDataSource(dataSourceName)
    }

    void setDataSource(String dataSourceName = DEFAULT, DataSource ds) {
        if (isBlank(dataSourceName)) dataSourceName = DEFAULT
        storeDataSource(dataSourceName, ds)
    }

    boolean isDataSourceConnected(String dataSourceName) {
        if (isBlank(dataSourceName)) dataSourceName = DEFAULT
        retrieveDataSource(dataSourceName) != null
    }

    void disconnectDataSource(String dataSourceName) {
        if (isBlank(dataSourceName)) dataSourceName = DEFAULT
        storeDataSource(dataSourceName, null)
    }

    DataSource fetchDataSource(String dataSourceName) {
        if (isBlank(dataSourceName)) dataSourceName = DEFAULT
        DataSource ds = retrieveDataSource(dataSourceName)
        if (ds == null) {
            GriffonApplication app = ApplicationHolder.application
            ConfigObject config = CassandraConnector.instance.createConfig(app)
            ds = CassandraConnector.instance.connect(app, config, dataSourceName)
        }
        
        if (ds == null) {
            throw new IllegalArgumentException("No such DataSource configuration for name $dataSourceName")
        }
        ds
    }

    private DataSource retrieveDataSource(String dataSourceName) {
        synchronized(LOCK) {
            dataSources[dataSourceName]
        }
    }

    private void storeDataSource(String dataSourceName, DataSource ds) {
        synchronized(LOCK) {
            dataSources[dataSourceName] = ds
        }
    }
}
