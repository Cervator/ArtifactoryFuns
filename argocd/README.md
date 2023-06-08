# Argo CD

This app is a continuous deployment (CD) tool meant to help deploy other apps.

To initially provision this tool simply run `kubectl apply -k argocd` in the root directory for this repository. To delete things use `delete` instead of `apply`

The setup of Argo should automatically generate app entries including one for "ingress-control" set to auto-sync, meaning it should apply its resources right away which includes setup of an ingress controller plus cert-manager, including its issuers delayed till the Helm chart itself is done executing to have the right CRDs available.

TODO: After that point ingress should be ready, but the controller's new IP is not yet pointed at a domain to allow routing and certs to work (do not allow ingress definitions to generate certs at this point). Apply the IP to DNS manually then see if it works (some time later?)

## Approach

The goal by using Argo CD is having as much config and setup present in this readme's Git repo as possible, with minimal effort needed to maintain or even rebuild our infrastructure as needed.

Argo CD is a Kubernetes-native CD operator meant to help maintain applications hosted within its cluster (or other clusters). It can manage apps (including itself) in a variety of ways, including _Helm_

**Helm** is a well-known orchestration tool that relies on charts - template files that can depend on other charts and load values from given files to replace within the chart. You deploy a given chart as a "release" which you can list with `helm ls` (you need to supply a namespace to go beyond the default)

For an easy way to figure out which values can be supplied run something like `helm show values artifactory-oss --repo https://charts.jfrog.io` - however there are two common gotchas:

* A chart may be based on another chart, in which case you'd need to repeat the command on the other chart. This is the case with Artifactory which is actually a small _tree_ of charts. The OSS chart depends on the base chart and the above command only shows the values within the OSS chart, not the ones from "upstream"
* The suggested way to use Helm charts within Argo is to make your _own_ chart that is based on your desired chart, so you can supply your own values file and version track your chart release level. This gets fun in two ways: version numbers become like nesting dolls and when you wrap a chart you also have to wrap your values - see the Artifactory example which ends up being `artifactory-oss.artifactory.artifactory`
  * Your chart's version is the release you're deploying to your Kubernetes cluster
  * The target chart's version is the release you're actually using
  * The application _inside_ the target chart also has its own version number - sometimes numbers get partially or wholly duplicated between layers

You can also "render" a completed chart showing your values supplied by using `helm template -f "values.yaml" jfrog/artifactory-oss` but you do tend to get a _lot_ of YAML thrown in your face. GUI tooling may be advised, such as https://monokle.io/

Note: The chosen initial Helm release name for the bootstrapping part of the infra setup will be used dynamically later by Argo CD. Other apps have their release name hard coded in their Argo CD application manifest files (the config maps under the templates directory)

## Manage Argo CD via Argo CD

An Argo installation can be managed by itself - that is, after the initial installation you can see Argo CD as an app inside it, even tweak it there or make adjustments in Git that can then sync either automatically or via manual sync operation within Argo. For more details see https://argo-cd.readthedocs.io/en/stable/operator-manual/declarative-setup/#manage-argo-cd-using-argo-cd

(TODO: Help fix a link there? Should go to something like https://raw.githubusercontent.com/argoproj/argo-cd/v2.7.2/manifests/core-install/kustomization.yaml if you want a version pinned base "primary" install. I earlier used https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml but then that's not a Kustomize base like the other, although it does mention Kustomize as part of its single-file setup)

In short: In our Git repo we have a Kustomize app setup for Argo itself, which _in theory_ could be the only thing to do other than make sure a cluster exists (and tweak any config in Git if something has changed)

Adding resources here for 2-3 reasons:

* Finalizing the Argo CD setup (subdomain with https)
* Establishing a base set of applications managed by Argo (which are themselves simply CRDs applied to Kubernetes)
* Adding more config for Argo itself (like credentials to get to the right Git repo - which would need a secrets manager or something like transcrypt)
  * There are lots of good examples in the repo that manages Argo's Argo: https://github.com/argoproj/argoproj-deployments/blob/master/argocd/kustomization.yaml

_Update:_ Trying Helm rather than Kustomize to try to apply the CRDs with less hassle. See https://github.com/argoproj/argo-helm/tree/main/charts/argo-cd

## Steps for after initial setup

When you have a fresh new Argo CD it should come with bunch of stuff preconfigured thanks to the tree of YAML in this repo, you should be able to retrieve the initial admin password either directly as a secret or by using the Argo CLI which you can install [as per the below instructions](https://argo-cd.readthedocs.io/en/stable/cli_installation/)

For the easier secret approach simply execute `kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d`

For the full CLI setup follow these steps, with the last one allowing you to change the admin password (which User Info / Update Password will also let you do in the GUI)

* `curl -sSL -o argocd-linux-amd64 https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64`
* `sudo install -m 555 argocd-linux-amd64 /usr/local/bin/argocd`
* `rm argocd-linux-amd64`
* `argocd admin initial-password -n argocd`
* `argocd account update-password`

## Later maybes

* There are plenty of plugins and other extensions to Argo CD we could look at. The suggested template `kustomization.yaml` came with a little component section related to https://github.com/argoproj-labs/argocd-extensions which may be worth checking out. Would enable by adding a `components:` block with `- https://github.com/argoproj-labs/argocd-extensions/manifests` in it
* We could also potential _remove_ pieces we do not need - like Dex is an included component dealing with certificates or secrets or something, but does our instance actually use it? Notifications?

## Todos / Troubleshooting

* Either enable insecure https on first pass for Argo itself or accept accessing via https on an insecure cert
* If the login page just loops back to itself try switching from http to https - the annotations in `argocd-ingress` are likely responsible and may be doable in a nicer up front format
