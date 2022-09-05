package com.eficode.atlassian.insightmanager.tests.specs

/**
 * Intended to test CRUD of Objects/attributes
 * Much work remains
 */

import com.eficode.atlassian.insightmanager.InsightManagerForScriptrunner
import com.eficode.atlassian.insightmanager.tests.utils.SpecHelper
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectSchemaBean
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectTypeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectSchemaBean
import org.apache.log4j.Level
import org.apache.log4j.Logger
import spock.lang.Shared
import spock.lang.Specification


class ImObjectSchemaCrudSpec extends Specification {

    @Shared
    Logger log = Logger.getLogger(ImObjectSchemaCrudSpec)
    @Shared
    String jiraBaseUrl = "http://jira.test.com:8080"
    @Shared
    String restAdmin = "admin"
    @Shared
    String restPassword = "admin"

    @Shared
    SpecHelper specHelper = new SpecHelper(jiraBaseUrl, restAdmin, restPassword)


    def setupSpec() {

        log.setLevel(Level.ALL)

        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()

        im.objectSchemaFacade.findObjectSchemaBeans().findAll{it.name.capitalize() == "SPOC" || it.objectSchemaKey.capitalize() == "SPOC"}.each {
            log.info("Deleting pre-existing SPOC schema:" + it.name + "(id: ${it.id})")
            im.deleteObjectSchema(it.id)
        }

    }


    def "Test creation of schemas"() {

        setup:
        log.info("Testing creation of schemas")

        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.log.setLevel(Level.ALL)


        when:
        log.info("\tCreating a schema using default parameters")
        ObjectSchemaBean schemaBean =  im.createObjectSchema("SPOC", "SPOC")
        log.info("\t\tCreated schema with ID:" + schemaBean.id)

        then:
        schemaBean.name == "SPOC"
        schemaBean.objectSchemaKey == "SPOC"
        !schemaBean.objectSchemaPropertyBean.serviceDescCustomersEnabled
        !schemaBean.objectSchemaPropertyBean.allowOtherObjectSchema
        log.info("\t\tSuccessfully created schema with default parameters")
        assert im.deleteObjectSchema(schemaBean.id) : "Error deleting object schema"


        when:
        log.info("\tCreating a schema using non default parameters")
        schemaBean =  im.createObjectSchema("spoc", "spoc", "A description", true, true)
        log.info("\t\tCreated schema with ID:" + schemaBean.id)
        log.info("\t\tCreated schema with Name:" + schemaBean.name)
        log.info("\t\tCreated schema with Key:" + schemaBean.objectSchemaKey)

        then:
        schemaBean.name == "spoc"
        schemaBean.objectSchemaKey == "SPOC"
        schemaBean.objectSchemaPropertyBean.serviceDescCustomersEnabled
        schemaBean.objectSchemaPropertyBean.allowOtherObjectSchema
        log.info("\t\tSuccessfully created schema with non default parameters")

        when:
        log.info("\tTesting deleteObjectSchema in read only mode")
        im.readOnly = true

        then:
        assert im.deleteObjectSchema(schemaBean.id) : "IM did not return true when expected"
        assert im.objectSchemaFacade.loadObjectSchema(schemaBean.id) : "Schema can no longer be found"
        log.info("\t\tSchema was not deleted when in readOnly mode")

        when:
        log.info("\tTesting createObjectSchema in read only mode")
        im.readOnly = false
        im.deleteObjectSchema(schemaBean.id)
        ArrayList<ObjectSchemaBean> existingSchemaIds = im.objectSchemaFacade.findObjectSchemaBeans()
        im.readOnly = true

        then:
        im.createObjectSchema("SPOC", "SPOC") instanceof MutableObjectSchemaBean
        existingSchemaIds.size() == im.objectSchemaFacade.findObjectSchemaBeans().size()
        log.info("\t\tSchema was not created when in readOnly mode")



    }


}