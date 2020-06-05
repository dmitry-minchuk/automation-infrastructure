# Docker-Based Automation Infrastructure
This project consists of 2 logical parts: **installing infrastructure** and **setting DSL script on Jenkins**

## Installing Infrastructure
First of all pull this repository to your AWS Ubuntu server:
```
git clone https://github.com/dmitry-minchuk/automation-infra.git
``` 

Make sure you have following ports opened in your _amazon security group_:
* 80 - nginx (optional)
* 8080 - Selenoid_UI
* 22 - SSH
* 4444 - Selenoid
* 50000 - Jenkins slave-master
* 8088 - Jenkins master UI
* 443 - HTTPS

### Installing Docker
To install Docker and Docker-Compose:
* `chmod 777 installDocker.sh`
* `./installDocker.sh`

### Running start.sh
Start.sh file will create a jenkins folder on your server, pull necessary images and run docker-compose.yml
* `chmod 777 start.sh`
* `./start.sh`

### Running elasticsearchprereq.sh
This file has to be run only for Report Portal server.
* `chmod 777 elasticsearchprereq.sh`
* `./elasticsearchprereq.sh`

Don't forget to change the volume path in the Report Portal compose file:
```
elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch-oss:7.3.0
    restart: always
    volumes:
      - ./data/elasticsearch:/usr/share/elasticsearch/data
```

### Browsers.json
Should be copied to `/etc/selenoid/browsers.json` because this path is used as default in Selenoid configuration in docker-compose.yml

### .env files
.env files were used for storing server IP and transfering this IP further to the JobDSL script, which will send it to the global variables and Jenkinsfile (pipeline script) use it in building the path to the selenium_host. But since there is an ability to get the host IP by executing of short curl command `curl http://checkip.amazonaws.com` right in pipeline script on the fly we don't need .env files eny more (**this information is unchecked in production**). Also .env files are not usable if we have _Amazon_EC2_Plugin_ installed and request additional instances dynamically with AMI (Amazon Machine Image).

### What is in docker-compose.yaml
Docker compose file may contain any items, but 3 of them are must:
* Jenkins
* Selenoid
* Selenoid_UI

Nginx or Mongo are the options if you are going to use Allure as a reporting tool. Report Portal is better to move to separate compose file and raise on the separate server. 

Some Nginx configuration inside the container:
* In `/etc/nginx/nginx.conf` set `sendfile off`
* In `/etc/nginx/conf.d/default.comf` set valid path to your _index.html_
    

## DSL job configuration
After all the containers are up and running we can create a SeedJob that will parse specified repositories and create jobs automatically. Job creation is based on existing .xml files.

To specify new repository - just replace existing demo repository with yours:
```
// defining repositories
def repo = 'git://github.com/dmitry-minchuk/pipeline-demo.git'
```

If you've got several repositories, then you will need to add upper-level .each cycle in `// creating pipeline jobs` section.

Also your repository must contain a _Jenkinsfile.groovy_ with a pipeline script to generate job structure. Pipeline script example:
```
pipeline {
  agent { label 'master' }
  stages {
    stage('test') {
      steps {
        def JENKINS_LOCAL_HOST = sh(script: 'curl http://checkip.amazonaws.com', returnStdout: true)
        sh "mvn clean test " +
                "-Dsuite=" + '${SUITE_NAME} ' +
                "-Dselenium_host=http://" + JENKINS_LOCAL_HOST + ":4444/wd/hub " +
                "-DenableVNC=" + '${ENABLE_VNC}'
      }
    }
  }
}
```

## Required Jenkins Plugins:

* DSL plugin (https://plugins.jenkins.io/job-dsl/)
* Pipeline plugin (https://plugins.jenkins.io/workflow-aggregator/)
* Any github integration plugin (for example: https://plugins.jenkins.io/github-pullrequest/)
* Global Variable String Parameter plugin (https://plugins.jenkins.io/global-variable-string-parameter/)

Optional plugins:
* Allure Jenkins plugin (to get reports in every job results)
* Cucumber plugin (to build pretty Cucumber reports in job results)
* TestNG Results plugin (to build testNG report in job results)
* Post Build Task plugin (to execute any scripts the maven finished its tasts)
* Custom Tools plugin (to add custom software to execute in environment)