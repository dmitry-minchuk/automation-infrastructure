//mvn command
StringBuilder mavenCommand = new StringBuilder('mvn clean test')

//jenkins job parameters named as in .xml suite or config.properties
def suiteName = 'suite_name'
def enableVnc = 'enable_vnc'
def jenkinsDefaultRetryCount = 'retry_count'
def env = 'env'
def cron = 'cron'

//building maven command with original maven property names and its values
appendMvnCommand(mavenCommand, "suite", getGlobalVariableValue(suiteName))
buildSeleniumHostProperty(mavenCommand)
appendMvnCommand(mavenCommand, jenkinsDefaultRetryCount, getGlobalVariableValue(jenkinsDefaultRetryCount))
appendMvnCommand(mavenCommand, 'enableVNC', getGlobalVariableValue(enableVnc))
appendMvnCommand(mavenCommand, env, getGlobalVariableValue(env))
println('Maven command executable: ' + mavenCommand)

//pipeline itself
pipeline {
  agent any
  if(isValueNotEmpty(getGlobalVariableValue(cron))) {
    triggers {
      cron(getGlobalVariableValue(cron))
    }
  }
  stages {
    stage('test') {
      steps {
        sh mavenCommand
      }
    }
  }
}

//helpers
static def appendMvnCommand(StringBuilder mavenCommand, String mvnPropertyName, String mvnPropertyValue) {
  if (isValueNotEmpty(mvnPropertyValue)) {
    mavenCommand.append(" -D" + mvnPropertyName + "=" + mvnPropertyValue)
  }
}

def buildSeleniumHostProperty(StringBuilder mavenCommand) {
  String serverIp = sh(script: 'curl http://checkip.amazonaws.com', returnStdout: true)
  String mvnProperty = ' -Dselenium_host=http://' + serverIp + ':4444/wd/hub'
  mavenCommand.append(mvnProperty)
}

static def getGlobalVariableValue(String variableName) {
  return '${' + variableName + '}'
}

static def isValueNotEmpty(String value) {
  return value != null && "" != value
}