# Jenkins

This directory controls and documents our CI/CD setup within Jenkins. Efforts have been made to automate as much as possible and document all of the things to the best of our ability.

Ideally an Argo CD instance is just running everything - see the root readme and the one in the argocd dir. See also the secrets section here first just in case.

## Developing the dev tools

If debugging for development reasons you can work via Helm CLI manually, rather than let Argo CD handle it after a Git push. This can be easier when just changing individual or a few files in a tool like Monokle.io

* helm repo add jenkins https://charts.jenkins.io
* helm install terajenkins jenkins/jenkins -f values.yaml -f values-agents.yaml -f values-plugins.yaml -f values-jcasc-general.yaml -n jenkins
  * (or "upgrade" instead of "install")
* Get initial admin password with `kubectl get secret terajenkins -n jenkins -o jsonpath="{.data.jenkins-admin-password}" | base64 --decode && echo`
  * This won't matter if fully provisioning with JCasC already configuring GitHub integration

## GitHub OAuth

Create a new OAuth application in a place like https://github.com/organizations/Terasology/settings/applications and write down the client id and secret.

For the sake of local development ease you can use `jenkins-secret-do-not-recomment.yaml` to prepare the secrets for Kubernetes, just enter the right values as instructed by comments. HOWEVER you do of course not want to commit the actual values, and for on-going maintenance we probably want the secret to not be maintained as part of the chart, at least until using a proper external secrets manager like Vault. For regular operations consider changing the hook resource file to a standard k8s secret, apply it manually, then let Argo handle everything else with the assumption that the secret is available.

Note that the secret added there is for OAuth and would live GitHub-side in a place like https://github.com/organizations/Terasology/settings/applications - after setup you also define a GitHub Server within Jenkins config which in pairs with GitHub _Apps_ on all the target organizations on GitHub. Those get added in places like https://github.com/organizations/MovingBlocks/settings/installations

## GitHub API via GitHub App

Jenkins can have increased access to the GitHub API by authenticating as a _GitHub App._ - and the setup can be reused between Jenkins buiilds, just

Reference: [GitHub App Authentication Guide][github-app]
Original issue: [InfraPlayground#19](https://github.com/MovingBlocks/InfraPlayground/issues/19)

We have a GitHub app [terasology-jenkins-io](https://github.com/apps/terasology-jenkins-io) which can be navigated to via
* MovingBlocks Organization Settings
* Developer Settings
* [GitHub Apps](https://github.com/organizations/MovingBlocks/settings/apps) (note this is _different_ than OAuth Apps)

That app is owned by [MovingBlocks](https://github.com/MovingBlocks), and is additionally installed to the [Terasology](https://github.com/Terasology) and [Nanoware](https://github.com/Nanoware) organizations.

Follow the [guide][github-app] to create Credentials of type **GitHub App**.

+ Manage Jenkins ⇨ Manage Credentials ⇨ store=_Jenkins_ domain=_(global)_ ⇨ Add Credentials
+ The **App Id** is `100575` (visible on the [app's settings page](https://github.com/organizations/MovingBlocks/settings/apps/terasology-jenkins-io)).
+ Do open the *Advanced* options when setting up these credentials and fill in the **Owner** field.
  - We need to create one Credentials entry for each GitHub Organization we operate on. These may use the same App ID and secret, but set different Owners.
    * [ ] owner=`MovingBlocks`, id=`github-app-terasology-jenkins-io`
    * [ ] owner=`Terasology`, id=`gh-app-terasology`
    * [ ] owner=`Nanoware`, id=`gh-app-nanoware`
  - (This is true of August 2021. Check [JENKINS-62220](https://issues.jenkins.io/browse/JENKINS-62220) to see if they've fixed things to require less duplication. Later update: They did, but need to figure out what needs to change before updating the setup and this documentation, if we should even bother)
  - The `id` strings are used by the JobDSL scripts.

[github-app]: https://github.com/jenkinsci/github-branch-source-plugin/blob/master/docs/github-app.adoc (GitHub App Authentication Guide)

The key generated from the GitHub application is included in this repo as `terasology-jenkins-io.github-app.private-key.pkcs8` in the format needed by Jenkins.

## Various tokens

Jenkins has built up a lot of credentials over the years, and all the original instructions are in the https://github.com/MovingBlocks/InfraPlayground repo - for this rejuvenation attempt let us see how few we can get away with:

* GooeyHub the GitHub user - used as our primary robot account for anything automation.

## Plugins

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

## JCasC

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

### Gotchas

* You don't want to overlap between non-JCasC and JCasC config for the _same_ setting, as noted in the documentation (for instance indicating the Jenkins URL itself both ways)
* If you change the arbitrary key for a JCasC snippet Jenkins will happily create a _new_ file under `/var/jenkins_home/casc_configs` - without deleting the _old_ file which still loads with its outdated values, including the potential for clashes! Unsure if there is an easy fix for this other than getting into the file system and deleting the old file directly (or just doing a full wipe but..)
  * Even though the Jenkins pod's _main_ container will end up in an inaccessible crash state if it fails to start you can cheat and use the sidecar meant to reload jcasc with the following: `kubectl exec -it terajenkins-0 -n jenkins --container config-reload -- /bin/sh` then `cd /var/jenkins_home/casc_configs` to get to the good stuff. Then delete away and retry!
* JCasC can be _very_ picky when it comes to parameter lists. A valid config in the UI may produce an "export" JCasC that will not work (as the docs indicate) because some empty field wasn't included in the config snippet.
  * One thing that was missing for GitHub Authorization setup was the "organizationNames" entry - it may well work if left as an empty string rather than undefined (leading to a null, causing param issues?)
* If working locally with any sort of Helm templating approach followed by deploying individual resources (easy to do in Monokle) be mindful that adding new JCasC keys to a values file may also remove them from the default file - which if not redeployed will clash with the new values. Deploy both or the whole thing if in doubt

## More config

* There is a `content.terasology.io` (or whichever domain) defined for Jenkins as a secondary URL beyond the base jenkins subdomain. This is to help host certain other kinds of content from Jenkins like javadoc. Ingress should spin up automatically as part of our setup, unsure if we need any other toggles or if this even has or will go out of date at some point.

TODO:

* Prep for Argo (make an argocd app yaml including the multiple values files)
* Resource allocations (controller)
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
