package com.eficode.atlassian.insightmanager.tests.utils

import com.eficode.atlassian.jiraInstanceManager.JiraInstanceManagerRest
import com.eficode.atlassian.jiraInstanceManager.beans.MarketplaceApp
import com.eficode.atlassian.jiraInstanceManager.beans.ObjectSchemaBean
import com.eficode.atlassian.jiraInstanceManager.beans.ProjectBean
import com.eficode.devstack.deployment.impl.JsmH2Deployment
import kong.unirest.Cookie
import kong.unirest.Cookies
import kong.unirest.Unirest


import org.apache.log4j.Level
import org.apache.log4j.Logger

class SpecHelper {


    Logger log = Logger.getLogger(this.class)
    JiraInstanceManagerRest jiraR

    String baseUrl
    String restAdmin
    String restPassword


    Cookies sudoCookies

    SpecHelper(String baseUrl, String restAdmin = "admin", String restPassword = "admin") {
        this.baseUrl = baseUrl
        this.restAdmin = restAdmin
        this.restPassword = restPassword

        Unirest.config().defaultBaseUrl(this.baseUrl).setDefaultBasicAuth(this.restAdmin, this.restPassword)
        jiraR = new JiraInstanceManagerRest(this.restAdmin, this.restPassword, this.baseUrl)

        sudoCookies = jiraR.acquireWebSudoCookies()

        log.setLevel(Level.DEBUG)
    }


    Map createSampleProjectAndSchema(String name, String keyPrefix) {

        String projectKey = jiraR.getAvailableProjectKey(keyPrefix)
        Map resultMap = jiraR.createInsightProjectWithSampleData(name, projectKey)

        ProjectBean projectBean = resultMap.project as ProjectBean
        ObjectSchemaBean schemaBean = resultMap.schema as ObjectSchemaBean
        return [project: projectBean, schema: schemaBean]
    }


    /**
     * Get the Marketplace URL for SR Datacenter
     * @param version
     * @return
     */
    static String getSrUrl(String version = "latest") {

        ArrayList<MarketplaceApp> apps = JiraInstanceManagerRest.searchMarketplace("com.onresolve.jira.groovy.groovyrunner", MarketplaceApp.Hosting.Datacenter)
        assert apps.size() == 1: "Error getting ScriptRunner marketplace app"

        String url = apps.first().getVersion(version, MarketplaceApp.Hosting.Datacenter)?.downloadUrl

        return url

    }


    JsmH2Deployment createBlankJira(String jsmVersion = "latest", String srVersion = "latest") {

        JsmH2Deployment jsmDep = new JsmH2Deployment(baseUrl)

        String userHome = System.getProperty("user.home")
        String jiraLicensePath = userHome + "/.licenses/jira/jsm.license"
        String srLicensePath = userHome + "/.licenses/jira/sr.license"


        jsmDep.jsmContainer.containerImageTag = jsmVersion
        jsmDep.setJiraLicense(new File(jiraLicensePath).text)

        jsmDep.appsToInstall = [(getSrUrl(srVersion)):new File(srLicensePath).text]

        jsmDep.removeDeployment()
        jsmDep.setupDeployment()


        return jsmDep



    }


}
