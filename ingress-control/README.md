This directory holds an Argo CD app meant to run an ingress controller and an associated cert manager instance.

TODO: Where do we set a Helm release name, especially for a chart with multiple sub-charts? Is it literally the same as the chart name in that case?




## Troubleshooting tips

* You can preview Kubernetes YAML in a variety of ways, for instance via monokle.io's IDE or with `helm template` which can also immediately target upstream public charts like the following:
  * `helm template cert-manager -f values.yaml jetstack/cert-manager --namespace cert-manager --create-namespace --version v1.12.0  > cert-manager.custom.yaml`
  * This can be a slight improvement on trying to preview our stuff in-place as we use a _chart of charts_ to target cert-manager which means the values file may go funny (same with Artifactory)
