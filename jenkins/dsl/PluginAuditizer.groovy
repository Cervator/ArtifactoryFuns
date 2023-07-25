def jobName = "PluginAuditizer"

job("GitOpsUtility/$jobName") {
    description('Prints a listing of installed plugins in a variety of useful formats')
    label("master")

    steps {
        systemGroovyCommand ('''
        import jenkins.model.Jenkins
        import hudson.PluginWrapper

        def pipelineyThingsArray = []
        def pluginTxtArray = []
        def pluginTxtArrayLatest = []
        // Veracode plugin if installed is not distributed from the main index and has to be handled manually
        def pluginsToExclude = "veracode-jenkins-plugin"

        Jenkins.instance.pluginManager.getPlugins().each { plugin ->

            // TODO: Test this more
            if (pluginsToExclude.contains(plugin.shortName)) {
                println("<!-- ***** EXCLUDED PLUGIN ${plugin.shortName} - found in exclude list")
            } else {
                println("<!-- plugin wiki: ${plugin.manifest.getMainAttributes().getValue("Url")} -->")
                println("<!-- plugin url:  https://plugins.jenkins.io/${plugin.shortName}/ -->")
                println("<dependency>")
                println("    <groupId>${plugin.manifest.mainAttributes.getValue('Group-Id')}</groupId>")
                println("    <artifactId>${plugin.shortName}</artifactId>")
                println("    <version>${plugin.version}</version>")
                println("</dependency>")

                pipelineyThingsArray.add( "compile group: '${plugin.manifest.mainAttributes.getValue('Group-Id')}', name: '${plugin.shortName}', version: '${plugin.version}', ext: 'jar'\n" )
                pluginTxtArray.add( plugin.shortName + ":" + plugin.version + "\n" )
                pluginTxtArrayLatest.add( plugin.shortName + ":latest\n" )

                List<PluginWrapper.Dependency> pluginDependencies = plugin.dependencies
                if (pluginDependencies.size() > 0) {
                    PluginWrapper.Dependency dep = pluginDependencies.get(0)
                    println "First dependency: " + dep.shortName + " : " + dep.version + ", optional? " + dep.optional
                }
            }
        }

        println "Gradle style (for working on Jenkins Groovy scripts in an IDE): "
        //println
        pipelineyThingsArray.sort()
        pipelineyThingsList = ""
        for (str in pipelineyThingsArray) {
            pipelineyThingsList += str
        }
        println pipelineyThingsList

        println "plugins.txt style: "
        pluginTxtArray.sort()
        pluginTxtList = ""
        for (str in pluginTxtArray) {
            pluginTxtList += str
        }
        println pluginTxtList

        println "plugins.txt style - latest: "
        //println pluginTxtThingsLatest
        pluginTxtArrayLatest.sort()
        pluginTxtListLatest = ""
        for (str in pluginTxtArrayLatest) {
            pluginTxtListLatest += str
        }
        println pluginTxtListLatest

        ''')
    }
}
