/*
 * Copyright 2011-2013 the original author or authors.
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

import griffon.core.GriffonClass
import griffon.core.GriffonApplication
import griffon.plugins.cassandra.CassandraConnector
import griffon.plugins.cassandra.CassandraEnhancer
import griffon.plugins.cassandra.CassandraContributionHandler

import static griffon.util.ConfigUtils.getConfigValueAsBoolean

/**
 * @author Andres Almiray
 */
class CassandraGriffonAddon {
    void addonPostInit(GriffonApplication app) {
        CassandraConnector.instance.createConfig(app)
        def types = app.config.griffon?.cassandra?.injectInto ?: ['controller']
        for(String type : types) {
            for(GriffonClass gc : app.artifactManager.getClassesOfType(type)) {
                if (CassandraContributionHandler.isAssignableFrom(gc.clazz)) continue
                CassandraEnhancer.enhance(gc.metaClass)
            }
        }
    }

    Map events = [
        LoadAddonsEnd: { app, addons ->
            if (getConfigValueAsBoolean(app.config, 'griffon.cassandra.connect.onstartup', true)) {
                ConfigObject config = CassandraConnector.instance.createConfig(app)
                CassandraConnector.instance.connect(app, config)
            }
        },
        ShutdownStart: { app ->
            ConfigObject config = CassandraConnector.instance.createConfig(app)
            CassandraConnector.instance.disconnect(app, config)
        }
    ]
}
