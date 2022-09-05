package com.eficode.atlassian.insightmanager.tests.runners

/**
 * Used to setup spec/test environments
 */

import com.eficode.devstack.container.impl.NginxContainer
import com.eficode.devstack.deployment.impl.JsmH2Deployment
import com.eficode.atlassian.jiraInstanceManger.JiraInstanceMangerRest
import org.apache.groovy.json.internal.LazyMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Duration


String jiraBaseUrl = "http://jira.test.com:8080"
String jiraDomain = jiraBaseUrl.replaceFirst("https?:\\/?\\/?","").replaceFirst(":\\d+\$", "")
String jiraLicensePath = "./testResources/jira/licenses/jsm.license"
String srLicensePath = "./testResources/jira/licenses/scriptrunnerForJira.license"
String dockerHost = "https://docker.domain.se:2376"
String dockerCertPath = "./testResources/docker/dockerCert"
String JiraInstanceMangerRestSrcRoot = "../../JiraInstanceMangerRest/src" //If set, these source files will be used instead of the git repo using grape
boolean useSecureRemoteDocker = false

//Setup a local nginx container that will host app binaries
boolean useLocalMarketplace = true
String localMarketplaceRootPath = new File("").absolutePath + "/testResources/localMarketplace"
NginxContainer nginxContainer = new NginxContainer()

Logger log = LoggerFactory.getLogger("spec.env.setup")
JsmH2Deployment jsmDep = new JsmH2Deployment(jiraBaseUrl)
jsmDep.jsmContainer.containerImageTag = "4"
jsmDep.jsmContainer.containerMainPort = "8080"
JiraInstanceMangerRest jiraR = new JiraInstanceMangerRest()
jiraR.baseUrl = jiraBaseUrl
jsmDep.setJiraLicense(new File(jiraLicensePath))




if (useSecureRemoteDocker) {
    assert jsmDep.getJsmContainer().setupSecureRemoteConnection(dockerHost, dockerCertPath)
    assert nginxContainer.setupSecureRemoteConnection(dockerHost, dockerCertPath)

}

if (useLocalMarketplace) {
    nginxContainer.containerName = "local-marketplace"
    nginxContainer.stopAndRemoveContainer()
    nginxContainer.bindHtmlRoot(localMarketplaceRootPath)
    nginxContainer.createContainer()
    nginxContainer.startContainer()
    jsmDep.appsToInstall = [("http://" + nginxContainer.ip + "/groovyrunner-6.50.0.jar" ):new File(srLicensePath).text]
}else {
    jsmDep.appsToInstall = ["https://marketplace.atlassian.com/download/apps/6820/version/1005740":new File(srLicensePath).text]

}



jsmDep.removeDeployment()
assert jsmDep.setupDeployment()
jsmDep.installApps() //Temp, remove me
jsmDep.jsmContainer.runBashCommandInContainer("echo 127.0.0.1 $jiraDomain >> /etc/hosts")

jsmDep.installApps()


Map filesToUpdate = [
        "../src/main/groovy/com/eficode/atlassian/insightmanager/" : "com/eficode/atlassian/insightmanager/",
        "tests/com/eficode/atlassian/insightmanager/tests/utils/" : "com/eficode/atlassian/insightmanager/tests/utils/",
        "tests/com/eficode/atlassian/insightmanager/tests/specs/" : "com/eficode/atlassian/insightmanager/tests/specs/"


]

//Install JIRA Instance Manager, either from local sources or as a grape.
if (JiraInstanceMangerRestSrcRoot) {
    filesToUpdate.put((JiraInstanceMangerRestSrcRoot + "/main/groovy/com/eficode/atlassian/jiraInstanceManger/") , "com/eficode/atlassian/jiraInstanceManger/")
    filesToUpdate.put((JiraInstanceMangerRestSrcRoot + "/main/groovy/com/eficode/atlassian/jiraInstanceManger/beans/") , "com/eficode/atlassian/jiraInstanceManger/beans/")
}else {
    jiraR.installGrapeDependency("com.eficode.atlassian", "jirainstancemanger", "1.0.3-SNAPSHOT","https://github.com/eficode/JiraInstanceMangerRest/raw/packages/repository/" )
}

assert jiraR.installGrapeDependency("com.konghq", "unirest-java", "3.13.6", "", "standalone")


assert jiraR.updateScriptrunnerFiles(filesToUpdate)
jiraR.clearCodeCaches()

log.info("Spec Environment setup:" + jiraBaseUrl)


