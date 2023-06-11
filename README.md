# ArtifactoryFuns

## Pre-requsites

* A decently recent Kubernetes cluster with enough access to apply cluster-wide stuff via `kubectl`
* This Git repository updated and available to have Kubernetes commands executed against it
* A domain with DNS record management available - will be adding A records for `*` and `@` pointing at an ingress controller IP

## Steps

See the individual directories for more technical details on each topic.

1. Kick off the process with `helm install terargo-argocd . -f values.yaml --namespace argocd --create-namespace` in the "argocd" directory (cd there with a terminal or use something like monokle.io's IDE)
1. Stuff should spin up in a few minutes, both Argo CD and an initial application "ingress-control" that sets up an ingress controller (and more) - get its IP and apply that to the domain
  * You may have to wait a bit here depending on the mood of DNS replication ...
1. Verify that `https://argocd.[yourdomain]` pulls up, log in with `admin` to validate further (see dedicated readme) (you'll have to OK an invalid cert - plain http will hit a redirect loop on login)
  * Get the password with `kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d && echo` (works readily within a Google Cloud Shell, less so on Windows)
1. With the domain confirmed go edit `argocd/templates/argocd-ingress.yaml` and uncomment lines starting with # (not indented) to allow Lets Encrypt to pull a fully valid certificate next try. Commit & push to Git.
  * Naturally if we've already done that this repo won't show those few lines commented out in Git as we are live - see the dedicated readme for details.
1. Click the button to synchronize Argo CD _inside_ Argo CD - yep it manages itself! You may get funny behavior for a moment as Argo redeploys itself.
  * Note that the Argo app for Argo itself may show as out of sync even before updating Git - when the app is established an extra k8s label may be affixed to the related resources
1. Sync other apps as desired, such as Artifactory - other apps should already be configured to start with production-level Lets Encrypt, you only need the extra step the first time (with Argo)
1. Over time simply modify Git as needed then go sync the relevant app in Argo (or YOLO into enabling auto-sync!)
