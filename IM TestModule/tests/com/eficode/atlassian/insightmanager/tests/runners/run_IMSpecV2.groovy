package com.eficode.atlassian.insightmanager.tests.runners



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
String JiraInstanceMangerRestSrcRoot = "../../JiraInstanceMangerRest/src"
boolean useSecureRemoteDocker = true


Logger log = LoggerFactory.getLogger("IMSpecV2")
JsmH2Deployment jsmDep = new JsmH2Deployment(jiraBaseUrl)
jsmDep.jsmContainer.containerImageTag = "4"
JiraInstanceMangerRest jiraR = new JiraInstanceMangerRest()
jiraR.baseUrl = jiraBaseUrl





jsmDep.setJiraLicense(new File(jiraLicensePath))
jsmDep.appsToInstall = ["https://marketplace.atlassian.com/download/apps/6820/version/1005740":new File(srLicensePath).text]

if (useSecureRemoteDocker) {
    assert jsmDep.getJsmContainer().setupSecureRemoteConnection(dockerHost, dockerCertPath)

}



//jsmDep.removeDeployment()
//assert jsmDep.setupDeployment()
//jsmDep.jsmContainer.runBashCommandInContainer("echo 127.0.0.1 $jiraDomain >> /etc/hosts")

//jsmDep.installApps()


Map filesToUpdate = [
        "../src/com/eficode/atlassian/insightmanager/" : "com/eficode/atlassian/insightmanager/",
        "tests/com/eficode/atlassian/insightmanager/tests/utils/" : "com/eficode/atlassian/insightmanager/tests/utils/",
        "tests/com/eficode/atlassian/insightmanager/tests/specs/" : "com/eficode/atlassian/insightmanager/tests/specs/"


]

if (JiraInstanceMangerRestSrcRoot) {
    filesToUpdate.put((JiraInstanceMangerRestSrcRoot + "/main/groovy/com/eficode/atlassian/jiraInstanceManger/") , "com/eficode/atlassian/jiraInstanceManger/")
    filesToUpdate.put((JiraInstanceMangerRestSrcRoot + "/main/groovy/com/eficode/atlassian/jiraInstanceManger/beans/") , "com/eficode/atlassian/jiraInstanceManger/beans/")
}

assert jiraR.updateScriptrunnerFiles(filesToUpdate)

jiraR.clearCodeCaches()
//jiraR.installGrapeDependency("com.eficode.atlassian", "jirainstancemanger", "1.0.3-SNAPSHOT","https://github.com/eficode/JiraInstanceMangerRest/raw/packages/repository/" )
jiraR.installGrapeDependency("com.konghq", "unirest-java", "3.13.6", "", "standalone")

LazyMap spockResult = jiraR.runSpockTest("com.eficode.atlassian.insightmanager.tests.specs", "ImObjectCrudSpec")

log.info(spockResult.toPrettyJsonString())
