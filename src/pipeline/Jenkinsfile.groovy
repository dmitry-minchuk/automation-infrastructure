//jenkins job parameters
def suiteName = 'suiteName'
def enableVnc = 'enableVNC'
def jenkinsDefaultRetryCount = 'jenkinsDefaultRetryCount'

pipeline {
  agent { label 'master' }
  stages {
    stage('test') {
      steps {
        def JENKINS_LOCAL_HOST = sh(script: 'curl http://checkip.amazonaws.com', returnStdout: true)
        sh "mvn clean test " +
                "-Dsuite=" + '${' + suiteName + '} ' +
                "-Dselenium_host=http://" + JENKINS_LOCAL_HOST + ":4444/wd/hub " +
                "-DenableVNC=" + '${' + enableVnc + '} '
      }
    }

// Following steps may not work since they were added as example

//    stage('allure report') {
//      steps {
//        script {
//          allure([
//                  includeProperties: false,
//                  jdk: '',
//                  properties: [],
//                  reportBuildPolicy: 'ALWAYS',
//                  results: [[path: 'allure-results']]
//          ])
//        }
//      }
//    }

//    stage ('cucumber report') {
//      steps {
//        cucumber buildStatus: "UNSTABLE",
//                fileIncludePattern: "**/*.json",
//                jsonReportDirectory: 'target/cucumber-reports'
//      }
//    }
  }
}