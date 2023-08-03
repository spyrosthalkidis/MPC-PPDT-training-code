# MPC-PPDT
[![Build Gradle project](https://github.com/AndrewQuijano/MPC-PPDT/actions/workflows/build-gradle-project.yml/badge.svg)](https://github.com/AndrewQuijano/MPC-PPDT/actions/workflows/build-gradle-project.yml)  
[![codecov](https://codecov.io/gh/AndrewQuijano/MPC-PPDT/branch/main/graph/badge.svg?token=eEtEvBZYu9)](https://codecov.io/gh/AndrewQuijano/MPC-PPDT)  
Implementation of the PPDT in the paper "Privacy Preserving Decision Trees in a Multi-Party Setting: a Level-Based Approach"

## Libraries
* crypto.jar library is from this [repository](https://github.com/AndrewQuijano/Homomorphic_Encryption)
* weka.jar library is from [SourceForge](https://sourceforge.net/projects/weka/files/weka-3-9/3.9.5/),
  download the ZIP file and import the weka.jar file**

** To be confirmed/tested again...

## Installation
It is a requirement to install [SDK](https://sdkman.io/install) to install Gradle.
You need to install the following packages, to ensure everything works as expected
```bash
sudo apt-get install -y default-jdk, default-jre, graphviz, curl, python3-pip
pip3 install pyyaml
pip3 install configobj
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle
```

Run this command and all future commands from `the repository root`, run the following command once to install docker.
**Reboot your machine, then re-run the command to install minikube.**
```bash
bash setup.sh
```

## Usage

### Running it locally

1. Check the `config.properties` file is set to your needs. Currently:
   1. It assumes level-site 0 would use port 9000, level-site 1 would use port 9001, etc.
      1. If you modify this, provide a comma separated string of all the ports for each level-site.
      2. Currently, it assumes ports 9000 - 9009 will be used.
   2. key_size corresponds to the key size of both DGK and Paillier keys.
   3. precision controls how accurate to measure thresholds that are decimals. If a value was 100.1, then a precision of
      1 would set this value to 1001.
   4. The data would point to the directory with the `answer.csv` file and all the training and testing data.
2. Currently, the [test file](src/test/java/EvaluationTest.java) will read from the `data/answers.csv` file.
   1. The first column is the training data set,
      it is required to be a .arff file to be compatible with Weka.
      Alternatively, you can pass a .model file, which is a pre-trained Weka model.
      It is assumed this is a J48 classifier tree model.
   2. The second column would the name of an input file that is tab separated with the feature name and value
   3. The third column would be the expected classification given the input from the second column.
      If there is a mismatch, there will be an assert error.

To run the end-to-end test, run the following:
```bash
sh gradlew build
```

When the testing is done, you will have an output directory containing both the DT model and a text file on how to draw
your tree. Input the contents of the text file into the website [here](https://dreampuf.github.io/GraphvizOnline/) to get a
drawing of what the DT looks like.

### Running on local host Kubernetes Cluster
To make it easier for deploying on the cloud, we also provided a method to export our system into Kubernetes.
This would assume one execution rather than multiple executions.

#### Set Training data set
In the `server_site_training_job.yaml` file, you need to change the first argument to point to the right ARFF file.

#### Creating a Kubernetes Secret
You should set up a Kubernetes secret file, called `ppdt-secrets.yaml` in the `k8/level-sites` folder.
In the yaml file, you will need to replace <SECRET_VALUE> with a random string encoded in Base64.
This secret is used in the AES encryption between level sites.
```yaml
apiVersion: v1
kind: Secret
metadata:
    name: ppdt-secrets
type: Opaque
data:
  aes-key: <SECRET_VALUE>>
```

or you can use the command:

    kubectl create secret generic ppdt-secrets --from-literal=aes-key=<SECRET_VALUE>

#### Using Minikube
You will need to start and configure minikube. When writing the paper, we provided 8 CPUs and 20 GB of memory,
but feel free to modify the arguments that fit your computer's specs.

    minikube start --cpus 8 --memory 20000
    eval $(minikube docker-env)

#### Running Kubernetes Commands
The next step is to deploy the level sites. The level sites need to be deployed
before any other portion of the system. This can be done by using the following
command.

    kubectl apply -f k8/level_sites

You will then need to wait until all the level sites are launched. To verify
this, please run the following command. All the pods that say level_site should have a status _running_.

    kubectl get pods

The output of `kubectl get pods` would look something like:
```
NAME                                         READY   STATUS      RESTARTS        AGE
ppdt-level-site-01-deploy-7dbf5b4cdd-wz6q7   1/1     Running     1 (2m39s ago)   16h
ppdt-level-site-02-deploy-69bb8fd5c6-wjjbs   1/1     Running     1 (2m39s ago)   16h
ppdt-level-site-03-deploy-74f7d95768-r6tn8   1/1     Running     1 (16h ago)     16h
ppdt-level-site-04-deploy-6d99df8d7b-d6qlj   1/1     Running     1 (2m39s ago)   16h
ppdt-level-site-05-deploy-855b649896-82hlm   1/1     Running     1 (2m39s ago)   16h
ppdt-level-site-06-deploy-6578fc8c9b-ntzhn   1/1     Running     1 (16h ago)     16h
ppdt-level-site-07-deploy-6f57496cdd-hlggh   1/1     Running     1 (16h ago)     16h
ppdt-level-site-08-deploy-6d596967b8-mh9hz   1/1     Running     1 (2m39s ago)   16h
ppdt-level-site-09-deploy-8555c56976-752pn   1/1     Running     1 (16h ago)     16h
ppdt-level-site-10-deploy-67b7c5689b-rkl6r   1/1     Running     1 (2m39s ago)   16h
```

It does take time for the level-site to be able to accept connections. Run the following command on the first level-site,
and wait for an output in standard output saying `Ready to accept connections at: 9000`. Use CTRL+C to exit the pod.

    kubectl logs -f $(kubectl get pod -l "pod=ppdt-level-site-01-deploy" -o name)


After verifying that the level-sites are ready, the next step is to
start the server site. To do this, run the following command.

    kubectl apply -f k8/server

To verify that the server site is ready, use the following command to confirm the server_site is _running_
and check the logs to confirm we see `Server ready to get public keys from client-site` so we can exit and run the client.

    kubectl logs -f $(kubectl get pod -l job-name=ppdt-server-deploy -o name)

To run the client, simply run the following commands to start the client and run an evaluation, 
you would point values to something like `/data/hypothyroid.values`

    kubectl apply -f k8/client
    kubectl exec -i -t $(kubectl get pod -l "pod=ppdt-client-deploy" -o name) -- bash -c "gradle run -PchooseRole=weka.finito.client --args <VALUES-FILE>"


To get the results, access the logs as described in the previous steps for both the client and level-sites.

#### Re-running with different experiments
- *Case 1: Re-run with different testing set*  
As the job created the pod, you would connect to the pod and run the modified gradle command with the other VALUES file.
```bash
kubectl exec -i -t $(kubectl get pod -l "pod=ppdt-client-deploy" -o name) -- bash -c "gradle run -PchooseRole=weka.finito.client --args <VALUES-FILE>"
```
- *Case 2: Train level-sites with new DT and new testing set*  
You need to edit the `server_site_training_job.yaml` file to point to a new ARFF file.
```bash
# Delete job
kubectl delete -f k8/server-site
kubectl delete -f k8/client

# Re-apply the jobs
kubectl apply -f k8/server-site
# Wait a few seconds to for server-site to be ready to get the client key...
# Or just check the server-site being ready as shown in the previous section
kubectl apply -f k8/client
kubectl exec -i -t $(kubectl get pod -l "pod=ppdt-client-deploy" -o name)-- bash -c "gradle run -PchooseRole=weka.finito.client --args <VALUES-FILE>"
```
#### Clean up

If you want to re-build everything in the experiment, run the following

    docker system prune --force
    minikube delete

### Running it on an EKS Cluster

#### Installation
- First install [eksctl](https://eksctl.io/introduction/#installation)

- Create a user with sufficient permissions

- Obtain AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY of the user account. [See the documentation provided here](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html)

- run `aws configure` to input the access id and credential.

- Run the following command to create the cluster
```bash
eksctl create cluster --config-file eks-config/config.yaml
```

- Confirm the EKS cluster exists using the following
```bash
eksctl get clusters --region us-east-2
```

#### Running the experiment
- Once you confirm the cluster is created, you need to register the cluster with kubectl:
```bash
aws eks update-kubeconfig --name ppdt --region us-east-2
```

- Run the same commands as shown below. It is similar to [the previous section](#running-kubernetes-commands), but we point to different yaml files since it is pulling the container image from dockerhub.
```bash
# Make sure you aren't running these too early!
kubectl apply -f eks-config/k8/level_sites
kubectl apply -f eks-config/k8/server

kubectl apply -f eks-config/k8/client
kubectl exec <CLIENT-SITE-POD> -- bash -c "gradle run -PchooseRole=weka.finito.client --args <VALUES-FILE>"

kubectl exec ppdt-client-deploy-5795dcd946-bctkd -- bash -c "gradle run -PchooseRole=weka.finito.client --args /data/hypothyroid.values"
```
- Obtain the results of the classification using `kubectl logs` to the pods deployed on EKS.

- If you want to re-run the experiments, follow the same flow [here](#re-running-with-different-experiments).

#### Clean up
Destroy the EKS cluster using the following:
```bash
eksctl delete cluster --config-file eks-config/config.yaml --wait
docker system prune --force
```

## Authors and Acknowledgement
Code Authors: Andrew Quijano, Spyros T. Halkidis, Kevin Gallagher

## License
[MIT](https://choosealicense.com/licenses/mit/)

## Project status
Fully tested and completed. Future work currently includes:
* See if I can run this on AWS EKS too