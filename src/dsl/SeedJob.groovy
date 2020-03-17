import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration
import jenkins.model.*

String seedJobName = 'SeedJob' // Need to be excluded from the deletion list

// defining repository
String repoName = 'pipeline-demo'
String repoPath = 'git://github.com/dmitry-minchuk/' + repoName + '.git'

//jenkins job parameters
String enableVnc = 'enable_vnc'
String jenkinsDefaultRetryCount = 'retry_count'
String env = 'env'
String cron = 'cron'

String cronValue = '0 5 31 2 *' //default never executable cron

String deleteClonedRepo = "rm -r " + repoName
deleteClonedRepo.execute()

disableScriptApproval()
cloneRepo(repoPath)
LinkedHashMap<String,File> xmlFiles = getXmlFileList(repoName)
updateJobList(xmlFiles, seedJobName)

println("\nGenerating pipeline jobs...")
xmlFiles.each { xmlFile ->
    pipelineJob(xmlFile.getKey()) {
        //building maven command with original maven property names and its values
        StringBuilder mavenCommand = new StringBuilder("mvn clean test")
        buildSeleniumHostProperty(mavenCommand)
        appendMvnCommand(mavenCommand, "suite", xmlFile.getKey().replaceAll('.xml', ''))

        if(isParameterExists(retrieveFileRawValue(xmlFile.getValue(), enableVnc))) {
            appendMvnCommand(mavenCommand, 'enableVNC', retrieveFileRawValue(xmlFile.getValue(), enableVnc))
        }

        if(isParameterExists(retrieveFileRawValue(xmlFile.getValue(), jenkinsDefaultRetryCount))) {
            appendMvnCommand(mavenCommand, jenkinsDefaultRetryCount, retrieveFileRawValue(xmlFile.getValue(), jenkinsDefaultRetryCount))
        }

        if(isParameterExists(retrieveFileRawValue(xmlFile.getValue(), env))) {
            appendMvnCommand(mavenCommand, env, retrieveFileRawValue(xmlFile.getValue(), env))
        }

        println('Maven command executable for ' + xmlFile.getKey() + ' job: ' + mavenCommand)

        parameters {
            globalVariableParam('MVN_COMMAND', mavenCommand.toString(), 'Maven executable for ' + xmlFile.getKey() + '.xml pipeline job')

        }

        if(isParameterExists(retrieveFileRawValue(xmlFile.getValue(), cron))) {
            cronValue = retrieveFileRawValue(xmlFile.getValue(), cron)
        }
        triggers {
            scm '0 5 31 2 *'
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

static def cloneRepo(String repoPath) {
    String repoCloneCommand = 'git clone ' + repoPath
    println('Executing following script: ' + repoCloneCommand)
    repoCloneCommand.execute()
    sleep(5000)
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
    if (file.text.length() > 0) {
        def splitFile = file.text.split('<')
        String parameterRaw = splitFile.find { it.toString().contains(parameterName)}.toString()
        String parameterValue = parameterRaw.substring(parameterRaw.lastIndexOf('=') + 1).replaceAll('"', '').replaceAll('/>', '').trim()
        println(parameterName + ': ' + parameterValue)
        return parameterValue
    }
    throw new RuntimeException("Empty .xml file!")
}

static def isParameterExists(String parameterName) {
    return parameterName != null && "" != parameterName && !parameterName.equalsIgnoreCase("null")
}

static def appendMvnCommand(StringBuilder mavenCommand, String mvnPropertyName, String mvnPropertyValue) {
    if (isParameterExists(mvnPropertyValue)) {
        mavenCommand.append(" -D" + mvnPropertyName + "=" + mvnPropertyValue)
    }
}

def buildSeleniumHostProperty(StringBuilder mavenCommand) {
    String serverIp = "curl http://checkip.amazonaws.com".execute().text.trim()
    String mvnProperty = " -Dselenium_host=http://" + serverIp + ":4444/wd/hub"
    mavenCommand.append(mvnProperty)
}