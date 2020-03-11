import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration
import jenkins.model.*

// defining repository
def repoName = 'pipeline-demo'
def seedJobName = 'SeedJob' // Need to be excluded from deletion list
def repoPath = 'git://github.com/dmitry-minchuk/' + repoName + '.git'

//jenkins job parameters
def suiteName = 'suiteName'
def enableVnc = 'enableVNC'
def jenkinsDefaultRetryCount = 'jenkinsDefaultRetryCount'

disableScriptApproval()
cloneRepo(repoPath)
def xmlFiles = getXmlFileList(repoName)
updateJobList(xmlFiles, seedJobName)

// creating pipeline jobs
xmlFiles.each { xmlFile ->
    pipelineJob(xmlFile.getKey()) {
        parameters {
//            globalVariableParam('JENKINS_LOCAL_HOST', hostName, 'Host IP')
            globalVariableParam(suiteName, xmlFile.getKey(), 'Suite name')
            globalVariableParam(enableVnc, retrieveFileRawValue(xmlFile.getValue() as File, enableVnc), 'Video streaming from selenoid_ui.')
            globalVariableParam(jenkinsDefaultRetryCount, retrieveFileRawValue(xmlFile.getValue() as File, jenkinsDefaultRetryCount), 'Number of attempts for UI failing test.')
        }

        triggers {}
        description(xmlFile.getKey() + '.xml pipeline job')

        definition {
            cpsScm {
                scm {
                    git {
                        remote { url(repoPath) }
                        branches('master', '**/feature*')
                        scriptPath('Jenkinsfile.groovy')
                        extensions {}  // required as otherwise it may try to tag the repoPath, which you may not want
                    }
                }
            }
        }
    }
}

def disableScriptApproval() {
    // disable Job DSL script approval
    println('Disabling jenkins script approval...')
    GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class).useScriptSecurity=false
    GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class).save()
}

def cloneRepo(repo) {
    def repoCloneCommand = 'git clone ' + repo
    repoCloneCommand.execute()
}

def getHostIp() {
    // getting host Ip and setting it into pipeline to create correct selenium_host url
    def hostName = System.getenv('JENKINS_LOCAL_HOST')
    println('Found host: ' + hostName)
    return hostName
}

def getXmlFileList(repoName) {
    // collecting .xml suites
    def xmlFiles = [:]
    def dir = new File(repoName + '/src/test/resources/testng_suites/')
    dir.eachFile () { file ->
        xmlFiles.put(file.getName(), file)
    }
    return xmlFiles
}

def updateJobList(xmlFiles, seedJobName) {
    // Getting all registered jobs
    println('=====================================')

    def existingJobs = Jenkins.instance.getAllItems()
    def existingJobNames = []
    existingJobs.each { j ->
        println('Found existing job: ' + j.fullName)
        existingJobNames.add(j.fullName)
    }

    println('=====================================')

    def xmlFileNames = []
    xmlFiles.each { x ->
        println('Found existing xml suites: ' + x.getKey())
        xmlFileNames.add(x.getKey())
    }

    println('=====================================')

    def jobsToDelete = []
    existingJobNames.each { e ->
        if(!xmlFileNames.contains(e) && e != seedJobName) {
            jobsToDelete.add(e)
        }
    }

    existingJobs.each() { e ->
        if(jobsToDelete.contains(e.name)) {
            println('Deleting next job: ' + e.name)
            e.delete()
        }
    }

    println('=====================================')
}

def retrieveFileRawValue(File file, String parameterName) {
    def splitFile = ""
    if (file.text.length() > 0) {
        splitFile = file.text.split("<")
    }
    def parameterValue = splitFile.find { it.toString().contains(parameterName)}.toString()
    println(parameterName + ': ' + parameterValue)
    return parameterValue
}