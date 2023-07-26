## Optimizing Jenkins

Documentation here covers the more optional tasks of making Jenkins run _nicely_ - which may or may not be needed depending on context. Some historical observations are also included.


### SSD storage

Jenkins works best on SSD, even the controller, especially when it comes to dealing with lots of little files involved in analytics uploading and so forth. In the Cloudbees Core Jenkins days we ran that way but may not really need it anymore. The way the extra storage class was set up (which may or may be needed anymore) was by applying the following Kubernetes resource to a GKE cluster:

```
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: ssd
provisioner: kubernetes.io/gce-pd
allowVolumeExpansion: true
parameters:
  type: pd-ssd
```
