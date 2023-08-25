package com.eficode.atlassian.insightmanager.tests.runners

/**
 * Used to setup spec/test environments
 */

import com.eficode.devstack.container.impl.NginxContainer
import com.eficode.devstack.deployment.impl.JsmH2Deployment
import com.eficode.atlassian.jiraInstanceManager.JiraInstanceManagerRest
import org.slf4j.Logger
import org.slf4j.LoggerFactory



String jiraBaseUrl = "http://jira.localhost:8080"
String jiraDomain = jiraBaseUrl.replaceFirst("https?:\\/?\\/?","").replaceFirst(":\\d+\$", "")
String userHome = System.getProperty("user.home")
String jiraLicensePath = userHome + "/.licenses/jira/jsm.license"
String srLicensePath = userHome + "/.licenses/jira/sr.license"
String dockerHost = ""
String dockerCertPath = ""
String JiraInstanceManagerRestSrcRoot = ""// "../../JiraInstanceManagerRest/src" //If set, these source files will be used instead of the git repo using grape


//Setup a local nginx container that will host app binaries
boolean useLocalMarketplace = false
String localMarketplaceRootPath = new File("").absolutePath + "/testResources/localMarketplace"
NginxContainer nginxContainer = new NginxContainer()

Logger log = LoggerFactory.getLogger("spec.env.setup")
JsmH2Deployment jsmDep = new JsmH2Deployment(jiraBaseUrl, dockerHost, dockerCertPath)
jsmDep.removeDeployment()
jsmDep.jsmContainer.containerImageTag = "latest"
jsmDep.jsmContainer.enableJvmDebug("5005")
jsmDep.setJiraLicense(new File(jiraLicensePath).text)
JiraInstanceManagerRest jiraR = new JiraInstanceManagerRest(jiraBaseUrl)







if (useLocalMarketplace) {
    nginxContainer.containerName = "local-marketplace"
    nginxContainer.stopAndRemoveContainer()
    nginxContainer.bindHtmlRoot(localMarketplaceRootPath)
    nginxContainer.createContainer()
    nginxContainer.startContainer()
    jsmDep.appsToInstall = [("http://" + nginxContainer.ips.first() + "/groovyrunner-6.50.0.jar" ):new File(srLicensePath).text] as Map
}else {
    jsmDep.appsToInstall = ["https://marketplace.atlassian.com/download/apps/6820/version/1005740":new File(srLicensePath).text]

}




assert jsmDep.setupDeployment()
//jsmDep.jsmContainer.runBashCommandInContainer("echo 127.0.0.1 $jiraDomain >> /etc/hosts")
jsmDep.installApps()


Map filesToUpdate = [
        "../src/main/groovy/com/eficode/atlassian/insightmanager/" : "com/eficode/atlassian/insightmanager/",
        "tests/com/eficode/atlassian/insightmanager/tests/utils/" : "com/eficode/atlassian/insightmanager/tests/utils/",
        "tests/com/eficode/atlassian/insightmanager/tests/specs/" : "com/eficode/atlassian/insightmanager/tests/specs/"


]

//Install JIRA Instance Manager, either from local sources or as a grape.
if (JiraInstanceManagerRestSrcRoot) {
    filesToUpdate.put((JiraInstanceManagerRestSrcRoot + "/main/groovy/com/eficode/atlassian/jiraInstanceManager/") , "com/eficode/atlassian/jiraInstanceManager/")
    filesToUpdate.put((JiraInstanceManagerRestSrcRoot + "/main/groovy/com/eficode/atlassian/jiraInstanceManager/beans/") , "com/eficode/atlassian/jiraInstanceManager/beans/")
}else {
    jiraR.installGrapeDependency("com.eficode.atlassian", "jirainstancemanager", "1.5.2-SNAPSHOT","https://github.com/eficode/JiraInstanceManagerRest/raw/packages/repository/" )
    //jiraR.installGrapeDependency("com.eficode.atlassian", "jirainstancemanager", "1.5.2-SNAPSHOT", "https://github.com/eficode/JiraInstanceManagerRest/raw/packages/repository/", "standalone")
}

assert jiraR.installGrapeDependency("com.konghq", "unirest-java", "3.13.6", "", "standalone")


assert jiraR.updateScriptrunnerFiles(filesToUpdate)
jiraR.clearCodeCaches()

log.info("Spec Environment setup:" + jiraBaseUrl)


