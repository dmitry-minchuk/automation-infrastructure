import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration
import jenkins.model.*

def seedJobName = 'SeedJob' // Need to be excluded from the deletion list

// defining repository
def repoName = 'pipeline-demo'
def repoPath = 'git://github.com/dmitry-minchuk/' + repoName + '.git'

//jenkins job parameters
def suiteName = 'suite_name'
def enableVnc = 'enable_vnc'
def jenkinsDefaultRetryCount = 'retry_count'
def env = 'env'
def cron = 'cron'

String cronValue = '0 5 31 2 *' //default cron

disableScriptApproval()
cloneRepo(repoPath)
LinkedHashMap<String,File> xmlFiles = getXmlFileList(repoName)
updateJobList(xmlFiles, seedJobName)

println("\nGenerating pipeline jobs...")
xmlFiles.each { xmlFile ->
    pipelineJob(xmlFile.getKey()) {
        parameters {
            globalVariableParam(suiteName, xmlFile.getKey(), 'Suite name')

            if(isParameterExists(retrieveFileRawValue(xmlFile.getValue(), enableVnc))) {
                globalVariableParam(enableVnc, retrieveFileRawValue(xmlFile.getValue(), enableVnc), 'Video streaming from selenoid_ui.')
            }
            if(isParameterExists(retrieveFileRawValue(xmlFile.getValue(), jenkinsDefaultRetryCount))) {
                globalVariableParam(jenkinsDefaultRetryCount, retrieveFileRawValue(xmlFile.getValue(), jenkinsDefaultRetryCount), 'Number of attempts for UI failing test.')
            }
            if(isParameterExists(retrieveFileRawValue(xmlFile.getValue(), env))) {
                globalVariableParam(env, retrieveFileRawValue(xmlFile.getValue(), env), 'Test environment to run the suite in.')
            }
            if(isParameterExists(retrieveFileRawValue(xmlFile.getValue(), cron))) {
                globalVariableParam(cron, retrieveFileRawValue(xmlFile.getValue(), cron), 'Scheduling rule for the suite.')
            }
            if(isParameterExists(retrieveFileRawValue(xmlFile.getValue(), cron))) {
                cronValue = retrieveFileRawValue(xmlFile.getValue(), cron)
            }
            pipelineTriggers {
                triggers {
                    cron(cronValue)
                }
            }
        }

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

//helpers
@SuppressWarnings("GroovyAssignabilityCheck")
def disableScriptApproval() {
    println("\nDisabling jenkins script approval.")
    GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class).useScriptSecurity=false
    GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class).save()
}

static def cloneRepo(String repo) {
    String repoCloneCommand = 'git clone ' + repo
    println('Executing following script: ' + repoCloneCommand)
    repoCloneCommand.execute()
}

static def getXmlFileList(String repoName) {
    // collecting .xml suites
    LinkedHashMap<String,File> xmlFiles = [:]
    def dir = new File(repoName + '/src/test/resources/testng_suites/')
    dir.eachFile () { file ->
        xmlFiles.put(file.getName(), file)
    }
    return xmlFiles
}

def updateJobList(LinkedHashMap<String,File> xmlFiles, String seedJobName) {
    println("\nGetting all registered jobs:")

    def existingJobs = Jenkins.instance.getAllItems()
    def existingJobNames = []
    existingJobs.each { j ->
        println('Found existing job: ' + j.fullName)
        existingJobNames.add(j.fullName)
    }

    println("\nGetting all xml files:")

    def xmlFileNames = []
    xmlFiles.each { x ->
        println('Found existing xml suites: ' + x.getKey())
        xmlFileNames.add(x.getKey())
    }

    println("\nLooking for jobs to delete...")

    def jobsToDelete = []
    existingJobNames.each { e ->
        if(!xmlFileNames.contains(e) && e != seedJobName) {
            jobsToDelete.add(e)
        }
    }

    println("\nUnnecessary job deletion...")

    existingJobs.each() { e ->
        if(jobsToDelete.contains(e.name)) {
            println('Deleting next job: ' + e.name)
            e.delete()
        }
    }

    println("Unnecessary job deletion complete.\n")
}

def retrieveFileRawValue(File file, String parameterName) {
    def splitFile = ''
    if (file.text.length() > 0) {
        splitFile = file.text.split('<')
        String parameterRaw = splitFile.find { it.toString().contains(parameterName)}.toString()
        String parameterValue = parameterRaw.substring(parameterRaw.lastIndexOf('=') + 1).replaceAll('"', '').replaceAll('/>', '');
        println(parameterName + ': ' + parameterValue)
        return parameterValue
    }
    throw new RuntimeException("Empty .xml file!")
}

static def isParameterExists(String parameterName) {
 return parameterName != null && "" != parameterName && !parameterName.equalsIgnoreCase("null")
}