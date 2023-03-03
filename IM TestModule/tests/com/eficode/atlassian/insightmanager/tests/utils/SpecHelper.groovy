package com.eficode.atlassian.insightmanager.tests.utils

import com.eficode.atlassian.jiraInstanceManager.JiraInstanceManagerRest

import com.eficode.atlassian.jiraInstanceManager.beans.ObjectSchemaBean
import com.eficode.atlassian.jiraInstanceManager.beans.ProjectBean
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
    SpecHelper(String baseUrl, String restAdmin, String restPassword) {
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
        return [project:projectBean, schema:schemaBean]
    }






}
