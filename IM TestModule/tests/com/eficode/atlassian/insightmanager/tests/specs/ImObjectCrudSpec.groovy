package com.eficode.atlassian.insightmanager.tests.specs

/**
 * Intended to test CRUD of Objects/attributes
 * Much work remains to be transferred from older test scripts
 * ObjectSchema crud acctions are needed to be added to IM to allow setup of schemas needed for testing
 */

import com.eficode.atlassian.insightmanager.InsightManagerForScriptrunner
import com.eficode.atlassian.insightmanager.tests.utils.SpecHelper
import com.eficode.atlassian.jiraInstanceManager.beans.ObjectSchemaBean
import com.eficode.atlassian.jiraInstanceManager.beans.ProjectBean
import org.apache.log4j.Level
import org.apache.log4j.Logger
import spock.lang.Shared
import spock.lang.Specification



class ImObjectCrudSpec extends Specification {

    @Shared
    Logger log = Logger.getLogger(ImObjectCrudSpec)
    @Shared
    String jiraBaseUrl = "http://jira.test.com:8080"
    @Shared
    String restAdmin = "admin"
    @Shared
    String restPassword = "admin"

    @Shared
    SpecHelper specHelper = new SpecHelper(jiraBaseUrl, restAdmin, restPassword)

    @Shared
    ProjectBean projectBean

    @Shared
    ObjectSchemaBean schemaBean

    def setupSpec() {

        log.setLevel(Level.ALL)

    }

    def setup() {
        Map projectAndSchemaMap = specHelper.createSampleProjectAndSchema("Spoc", "SPOC")
        projectBean = projectAndSchemaMap.project
        schemaBean = projectAndSchemaMap.schema

        log.info("Created Project:" + projectBean.projectName + " (id ${projectBean.projectId})")
        log.info("Created Schema:" + schemaBean.name + " (id ${schemaBean.id})")
    }

    def cleanup() {

        specHelper.jiraR.deleteProject(projectBean) ? log.info("Deleted Project:" + projectBean.projectName + "(${projectBean.projectId})") : log.error("Error deleting Project:" + projectBean.projectName + "(${projectBean.projectId})")
        specHelper.jiraR.deleteInsightSchema(schemaBean.id) ? log.info("Deleted Schema:" + schemaBean.name + "(${schemaBean.id})") : log.error("Error deleting Schema:" + schemaBean.name + "(${schemaBean.id})")
    }

    def "Test iql of objects in template schema"() {

        setup:
        log.info("Testing simple IQL operations")

        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()

        expect:
        im.iql(schemaBean.id, "objectSchemaId = ${schemaBean.id}").size() >= 100
        im.iql(schemaBean.id, "objectSchemaId = ${schemaBean.id}").collect {it.objectTypeId}.unique().size() >= 10

        //Check using schema name, instead of id
        im.iql(schemaBean.id, "objectSchema = \"${schemaBean.name}\"").size() >= 100

        log.info("\tSimple IQL operations where tested successfully.")

    }


}