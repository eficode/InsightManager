package com.eficode.atlassian.insightmanager.tests.runners



import com.eficode.devstack.deployment.impl.JsmH2Deployment
import com.eficode.atlassian.jiraInstanceManger.JiraInstanceMangerRest
import org.apache.groovy.json.internal.LazyMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory


String jiraBaseUrl = "http://jira.test.com:8080"
String jiraDomain = jiraBaseUrl.replaceFirst("https?:\\/?\\/?","").replaceFirst(":\\d+\$", "")
String jiraLicensePath = "./testResources/jira/licenses/jsm.license"
String srLicensePath = "./testResources/jira/licenses/scriptrunnerForJira.license"
String dockerHost = "https://docker:2376"
String dockerCertPath = "./testResources/docker/dockerCert"
boolean useSecureRemoteDocker = true


Logger log = LoggerFactory.getLogger("IMSpecV2")
JsmH2Deployment jsmDep = new JsmH2Deployment(jiraBaseUrl)
JiraInstanceMangerRest jiraR = new JiraInstanceMangerRest()
jiraR.baseUrl = jiraBaseUrl



jsmDep.setJiraLicense(new File(jiraLicensePath))
jsmDep.appsToInstall = ["https://marketplace.atlassian.com/download/apps/6820/version/1005740":new File(srLicensePath).text]

if (useSecureRemoteDocker) {
    assert jsmDep.getJsmContainer().setupSecureRemoteConnection(dockerHost, dockerCertPath)

}
//assert jsmDep.setupDeployment()
//jsmDep.installApps()
//jsmDep.jsmContainer.runBashCommandInContainer("echo 127.0.0.1 $jiraDomain >> /etc/hosts")



Map filesToUpdate = [
        "../src/com/eficode/atlassian/insightmanager/" : "com/eficode/atlassian/insightmanager/"
]

assert jiraR.updateScriptrunnerFiles(filesToUpdate)


//LazyMap spockResult = jiraR.runSpockTest("globalCustomizations.flexCapacitor.tests", "FlexCapacitorSpec")

//log.info(spockResult.toPrettyJsonString())
