package com.eficode.atlassian.insightmanager.tests.utils

import com.atlassian.crowd.embedded.api.Group
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.groups.GroupManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.UserDetails
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.user.util.UserUtil
import com.eficode.atlassian.jiraInstanceManager.JiraInstanceManagerRest
import com.eficode.atlassian.jiraInstanceManager.beans.MarketplaceApp
import com.eficode.atlassian.jiraInstanceManager.beans.ObjectSchemaBean
import com.eficode.atlassian.jiraInstanceManager.beans.ProjectBean
//import com.eficode.devstack.deployment.impl.JsmH2Deployment
import kong.unirest.Cookie
import kong.unirest.Cookies
import kong.unirest.Unirest


import org.apache.log4j.Level
import org.apache.log4j.Logger

class SpecHelper {


    static Logger log = Logger.getLogger(this.class)
    static UserManager userManager = ComponentAccessor.getUserManager()
    static GroupManager groupManager = ComponentAccessor.getGroupManager()
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


    static ArrayList<ApplicationUser> createSpocUsers(int numberOfUsers, String userPrefix = "Spoc", boolean addUniqueSuffix = true, String userEmailPrefix = "spoc@spoc.com", ArrayList<String> groups = ["jira-servicedesk-users"]) {

        ArrayList<ApplicationUser> newUsers = []

        Calendar now = Calendar.getInstance()
        String userSuffix = "_" + now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE)

        for (int i = 1; i <= numberOfUsers; i++) {
            String userName = userPrefix
            String userEmail = userEmailPrefix

            if (addUniqueSuffix) {
                userName += i + userSuffix
                userEmail += i + userSuffix
            }

            ApplicationUser existingUser = userManager.getUserByName(userName) as ApplicationUser

            if (existingUser) {
                groups.each { group ->
                    groupManager.addUserToGroup(existingUser, groupManager.getGroup(group))
                }
                newUsers.add(existingUser)
            } else {
                newUsers.add(createUser(userName, userName, "123456", userEmail, groups) as ApplicationUser)
            }
        }
        return newUsers
    }

    static ApplicationUser createUser(String username, String displayName, String password = "", String emailAddress = "", ArrayList<String> groups = []) {
        log.info("Creating user:" + displayName + "($username)")
        UserDetails userDetails = new UserDetails(username, displayName)

        if (password) {
            userDetails.withPassword(password)
        }

        if (emailAddress) {
            userDetails.withEmail(emailAddress)
        }

        ApplicationUser newUser = userManager.createUser(userDetails)
        log.info("\tCreated:" + newUser.key)

        groups.each { groupName ->


            Group group = groupManager.getGroup(groupName)
            log.info("\tAdding ${newUser.key} to group " + group.name)

            groupManager.addUserToGroup(newUser, group)

        }
        return newUser
    }


    /*
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

     */


}
