griffon.project.dependency.resolution = {
    inherits "global"
    log "warn"
    repositories {
        griffonHome()
        mavenCentral()
        String basePath = pluginDirPath? "${pluginDirPath}/" : ''
        flatDir name: "cassandraLibDir", dirs: ["${basePath}lib"]
    }
    dependencies {
        compile('org.apache.cassandra:cassandra-jdbc:1.0.5-SNAPSHOT',
                'org.apache.cassandra:cassandra-thrift:1.0.2',
                'org.apache.cassandra:cassandra-clientutil:1.0.2') {
            exclude 'junit'            
        }
        compile('commons-dbcp:commons-dbcp:1.4',
                'commons-pool:commons-pool:1.5.6') {
            transitive = false
        }
    }
}

griffon {
    doc {
        logo = '<a href="http://griffon.codehaus.org" target="_blank"><img alt="The Griffon Framework" src="../img/griffon.png" border="0"/></a>'
        sponsorLogo = "<br/>"
        footer = "<br/><br/>Made with Griffon (@griffon.version@)"
    }
}

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    appenders {
        console name: 'stdout', layout: pattern(conversionPattern: '%d [%t] %-5p %c - %m%n')
    }

    error 'org.codehaus.griffon',
          'org.springframework',
          'org.apache.karaf',
          'groovyx.net'
    warn  'griffon'
}