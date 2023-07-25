

If debugging for development reasons you can install via Helm CLI manually as well as update, rather than let Argo CD. This is easier in a tool like Monokle.io after going to a multi approach for values files.

* helm repo add jenkins https://charts.jenkins.io
* helm install terajenkins jenkins/jenkins -f values.yaml -n jenkins
* helm upgrade terajenkins jenkins/jenkins -f values.yaml -n jenkins
* Get initial admin password with `kubectl get secret terajenkins -n jenkins -o jsonpath="{.data.jenkins-admin-password}" | base64 --decode && echo`

If the Jenkins pod is stuck on init (especially afte ran update) odds are it tried to install a set of plugins where it got confused about dependencies. Hoping the right Helm config will make it not do that and only explicitly install the pinned set of plugins.

### Initial secrets

For the sake of local development ease you can use the included Helm hook to prepare a secret, just enter the right values as instructed by comments. HOWEVER you do of course not want to commit the actual values, and for on-going maintenance we probably want the secret to not be maintained as part of the chart, at least until using a proper external secrets manager like Vault. For regular operations consider changing the hook resource file to a standard k8s secret, apply it manually, then let Argo handle everything else with the assumption that the secret is available.

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

### JCasC

We use a recommended approach from the official Helm image to [split config-as-code into multiple files](https://github.com/jenkinsci/helm-charts/blob/main/charts/jenkins/README.md#breaking-out-large-config-as-code-scripts) which then originate from multiple Helm values files.

Files with JCasC sections result in files being created at `/var/jenkins_home/casc_configs` in the Jenkins pod, where they are then loaded as multiple sources for config. Each file is named after its arbitrary key before the `|` such as `general.yaml` from the following snippet:

```
jenkins:
  controller:
    JCasC:
      configScripts:
        general: |
          jenkins: ...
```

#### Gotchas

* You don't want to overlap between non-JCasC and JCasC config for the _same_ setting, as noted in the documentation (for instance indicating the Jenkins URL itself both ways)
* If you change the arbitrary key for a JCasC snippet Jenkins will happily create a _new_ file under `/var/jenkins_home/casc_configs` - without deleting the _old_ file which still loads with its outdated values, including the potential for clashes! Unsure if there is an easy fix for this other than getting into the file system and deleting the old file directly (or just doing a full wipe but..)
  * Even though the Jenkins pod's _main_ container will end up in an inaccessible crash state if it fails to start you can cheat and use the sidecar meant to reload jcasc with the following: `kubectl exec -it terajenkins-0 -n jenkins --container config-reload -- /bin/sh` then `cd /var/jenkins_home/casc_configs` to get to the good stuff. Then delete away and retry!

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
