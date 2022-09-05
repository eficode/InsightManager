
package com.eficode.atlassian.insightmanager.tests.specs

import com.eficode.devstack.deployment.impl.JsmH2Deployment
import org.apache.commons.io.FileUtils
import org.apache.log4j.Level
import spock.lang.Shared
import spock.lang.Specification

import java.util.regex.Matcher
import java.util.regex.Pattern

//import com.eficode.atlassian.insightmanager.InsightManagerForScriptrunner
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.util.JiraHome
import com.atlassian.jira.permission.GlobalPermissionKey
import com.atlassian.jira.project.Project
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.GlobalPermissionManager
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.util.BaseUrl
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.IQLFacadeImpl
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.InsightPermissionFacadeImpl
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.ObjectSchemaFacadeImpl
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.ProgressFacadeImpl
import com.riadalabs.jira.plugins.insight.common.exception.InsightException
import com.riadalabs.jira.plugins.insight.common.exception.PermissionInsightException
import com.riadalabs.jira.plugins.insight.services.model.CommentBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectSchemaBean
import com.riadalabs.jira.plugins.insight.services.progress.ProgressCategory
import com.riadalabs.jira.plugins.insight.services.progress.model.Progress
import com.riadalabs.jira.plugins.insight.services.progress.model.ProgressId
import org.apache.log4j.Logger
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import jline.internal.InputStreamReader
import org.apache.commons.io.FileUtils
import org.apache.groovy.json.internal.LazyMap
import org.junit.runner.JUnitCore
import org.junit.runner.Request
import org.junit.runner.Result
import spock.config.ConfigurationException
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.concurrent.TimeoutException
import java.util.regex.Matcher
import java.util.regex.Pattern


class InsightManagerForScriptRunnerSpecsV2 extends Specification {



    /*
    @Shared
    String jiraAdminUserName = "anders"
    @Shared
    String jiraAdminPassword = "anders"

     */


    @Shared
    Class iqlFacadeClass
    @Shared
    IQLFacadeImpl iqlFacade
    @Shared
    ObjectSchemaFacadeImpl objectSchemaFacade
    @Shared
    Class objectSchemaFacadeClass
    @Shared
    ProgressFacadeImpl progressFacade
    @Shared
    Class progressFacadeClass

    @Shared
    UserManager userManager
    @Shared
    JiraAuthenticationContext jiraAuthenticationContext


    @Shared
    Logger log = Logger.getLogger(this.class)

    @Shared
    JsmH2Deployment jsmDeployment = new JsmH2Deployment("http://jira.domain.se:8080")

    @Shared
    String dockerEngineUrl = "https://docker.domain.se:2376"

    @Shared
    String dockerCertDirPath = "testResources/docker/dockerCert"

    @Shared
    ObjectSchemaBean testSchema

    def setupSpec() {

        log.setLevel(Level.ALL)


    }




}
