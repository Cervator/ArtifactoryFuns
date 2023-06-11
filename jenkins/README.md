

If debugging for development reasons you can install via Helm CLI manually as well as update, rather than let Argo CD

* helm repo add jenkins https://charts.jenkins.io
* helm install terajenkins jenkins/jenkins -f values.yaml -n jenkins
* helm upgrade terajenkins jenkins/jenkins -f values.yaml -n jenkins
* Get initial admin password with `kubectl get secret terajenkins -n jenkins -o jsonpath="{.data.jenkins-admin-password}" | base64 --decode && echo`

If the Jenkins pod is stuck on init (especially afte ran update) odds are it tried to install a set of plugins where it got confused about dependencies. Hoping the right Helm config will make it not do that and only explicitly install the pinned set of plugins.

TODO:

* Resource allocations
* Add our build agent definitions via JCasC (one came installed by default and works fine)
* Possible auth via Dex to GitHub and Jenkins RBAC based on GitHub groups
* Other GitHub setup - we were using an app to do Checks and such
* Credentials
* Test an import of the old thin backup and make any plugin/config adjustments
* Move to jenkins.terasology.io and start running some jobs - config will be there but no artifacts
* Backup? Sure would be nice to offload artifacts rather than keep them in Jenkins build records, and clean up other cruft like old analytics after a few builds
* Optimization if needed (fancy Jenkins ran on an SSD storage class)