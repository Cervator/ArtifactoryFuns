

If debugging for development reasons you can install via Helm CLI manually as well as update, rather than let Argo CD. This is easier in a tool like Monokle.io after going to a multi approach for values files.

* helm repo add jenkins https://charts.jenkins.io
* helm install terajenkins jenkins/jenkins -f values.yaml -n jenkins
* helm upgrade terajenkins jenkins/jenkins -f values.yaml -n jenkins
* Get initial admin password with `kubectl get secret terajenkins -n jenkins -o jsonpath="{.data.jenkins-admin-password}" | base64 --decode && echo`

If the Jenkins pod is stuck on init (especially afte ran update) odds are it tried to install a set of plugins where it got confused about dependencies. Hoping the right Helm config will make it not do that and only explicitly install the pinned set of plugins.

### Plugins

For ease we are simply indicating version-pinned plugins via Helm values file, using a custom image might be _slightly_ more efficient but hardly worth it.

The pinned-list can be generated and maintained by an included "PluginAuditizer" utility job that writes out plugin version lists in a few different formats. Typical approach:

* Run the job to see it print out the lists, two last ones in particular
  * "plugins.txt style" - includes version pins for _currently installed plugins_
  * "plugins.txt style - latest" - simply makes a list variant with `:latest` everywhere
* To prepare for an upgrade take the "latest" list contents and paste them into `values-plugins.yaml` to replace the pinned versions
* Apply the updated config (Helm/Argo) - possibly to a test Jenkins with an already-updated controller version (may still work on a pre-upgraded controller but some plugins may complain)
* Do any testing to see if the newer plugin versions cause trouble
* Run the "PluginAuditizer" again and grab the _pinned_ list this time and paste it into `values-plugins.yaml`
* Re-apply the config (should result in no change but pins to plugins to avoid surprises later)

TODO:

* Prep for Argo (make an argocd app yaml including the multiple values files)
* Retest that an upgrade doesn't screw up trying to install plugins again despite being pinned
* Resource allocations (controller)
* Add 1 executor to the controller for Job DSL (unless it has gone flyweight?)
* Add the Job DSL jobs including pulling in the PluginAuditizer
* Add remaining build agents
  * Having the instance cap accepted so it will show in the UI hasn't worked so far - but overall cap is OK for now
* Possible auth via Dex to GitHub and Jenkins RBAC based on GitHub groups
* Other GitHub setup - we were using an app to do Checks and such
* Credentials
* Test an import of the old thin backup and make any plugin/config adjustments
* Move to jenkins.terasology.io and start running some jobs - config will be there but no artifacts
* Backup? Sure would be nice to offload artifacts rather than keep them in Jenkins build records, and clean up other cruft like old analytics after a few builds
* Optimization if needed (fancy Jenkins ran on an SSD storage class)
