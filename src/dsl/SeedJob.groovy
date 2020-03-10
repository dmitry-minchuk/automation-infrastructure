import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration
import jenkins.model.*

// defining repositories
def repoName = 'pipeline-demo'
def repo = 'git://github.com/dmitry-minchuk/' + repoName + '.git'
def repoCloneCommand = 'git clone ' + repo

// disable Job DSL script approval
println('Disabling jenkins script approval...')
GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class).useScriptSecurity=false
GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class).save()

repoCloneCommand.execute()

// getting host Ip and setting it into pipeline to create correct selenium_host url
//def hostName = System.getenv('JENKINS_LOCAL_HOST')
//println('Found host: ' + hostName)

// collecting .xml suites
def xmlFileNames = []
def dir = new File(repoName + '/src/test/resources/testng_suites/')
dir.eachFile () { file ->
    fileName = file.getName()
    xmlFileNames.add(fileName.substring(0, fileName.lastIndexOf('.')))
}

// enable vnc or not
def enableVnc = 'true'

// Getting all registered jobs
println('=====================================')
existingJobs = Jenkins.instance.getAllItems()
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
def seedJob = 'SeedJob' // Need to be excluded from deletion list

existingJobNames.each { e ->
    if(!xmlFileNames.contains(e) && e != seedJob) {
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
                        remote { url(repo) }
                        branches('master', '**/feature*')
                        scriptPath('Jenkinsfile.groovy')
                        extensions {}  // required as otherwise it may try to tag the repo, which you may not want
                    }
                }
            }
        }
    }
}