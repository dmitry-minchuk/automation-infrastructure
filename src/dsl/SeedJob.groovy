import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration
import jenkins.model.*

// defining repository
def repoName = 'pipeline-demo'
def seedJobName = 'SeedJob' // Need to be excluded from deletion list
def enableVnc = 'true'
def repoPath = 'git://github.com/dmitry-minchuk/' + repoName + '.git'

disableScriptApproval()
cloneRepo(repoPath)
def xmlFileNames = getXmlFileList(repoName)
updateJobList(xmlFileNames, seedJobName)

// creating pipeline jobs
xmlFileNames.each { fileName ->
    pipelineJob(fileName) {
        parameters {
            globalVariableParam('SUITE_NAME', fileName, 'Suite name')
//            globalVariableParam('JENKINS_LOCAL_HOST', hostName, 'Host IP')
            globalVariableParam('ENABLE_VNC', enableVnc, 'VNC')
        }

        triggers {}
        description(fileName + '.xml pipeline job')

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
    def xmlFileNames = []
    def dir = new File(repoName + '/src/test/resources/testng_suites/')
    dir.eachFile () { file ->
        fileName = file.getName()
        xmlFileNames.add(fileName.substring(0, fileName.lastIndexOf('.')))
    }
    return xmlFileNames
}

def updateJobList(xmlFileNames, seedJobName) {
    // Getting all registered jobs
    println('=====================================')

    def existingJobs = Jenkins.instance.getAllItems()
    def existingJobNames = []
    existingJobs.each { j ->
        println('Found existing job: ' + j.fullName)
        existingJobNames.add(j.fullName)
    }

    println('=====================================')

    xmlFileNames.each { x ->
        println('Found existing xml suites: ' + x)
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