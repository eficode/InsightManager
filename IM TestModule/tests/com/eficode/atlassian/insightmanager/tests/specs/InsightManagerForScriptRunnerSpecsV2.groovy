package com.eficode.atlassian.insightmanager.tests.specs

import org.apache.commons.io.FileUtils
import spock.lang.Shared
import spock.lang.Specification

import java.util.regex.Matcher
import java.util.regex.Pattern

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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    Logger log = LoggerFactory.getLogger(this.class)


    @Shared
    ObjectSchemaBean testSchema


    def setupSpec() {

        log.setLevel(Level.ALL)
        objectSchemaFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectSchemaFacade")
        iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade");
        progressFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ProgressFacade");


        objectSchemaFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectSchemaFacadeClass) as ObjectSchemaFacadeImpl
        iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass) as IQLFacadeImpl
        progressFacade = ComponentAccessor.getOSGiComponentInstanceOfType(progressFacadeClass) as ProgressFacadeImpl

        userManager = ComponentAccessor.getUserManager()
        jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()

        assert specHelper.validateAndCacheSettings()


        if (specHelper.userRunningTheScript == specHelper.jiraAdminUser) {
            log.warn("The jiraAdmin and the user running this script should ideally not be the same.")
        }
    }

    def cleanupSpec() {
        /*
        log.info("Starting cleanup after all tests")

        if (specHelper.deleteScheme(testSchema)) {
            log.debug("\tDeleted test scheme")
            testSchema = null
        } else {
            throw new RuntimeException("Error deleting test scheme:" + testSchema.name)
        }

         */

    }

    def setup() {

        log.info("Starting Setup before feature method")

        /*
        ObjectSchemaBean existingTestSchema =  objectSchemaFacade.findObjectSchemaBeans().find { it.name == "SPOC Testing of IM" }

        if (existingTestSchema != null) {
            assert specHelper.deleteScheme(existingTestSchema) : "Error deleting test schema"
            testSchema == null
        }

         */

        if (testSchema == null) {
            log.debug("\tSetting up ObjectScheme for testing")
            setupTestSchema()
            log.debug("\tSetup Scheme with id:" + testSchema.id)
        }

        log.info("\tFinished Setup before feature method")


    }

    boolean setupTestSchema() {
        testSchema = specHelper.setupTestObjectSchema()

    }

    /*
    def cleanup() {
        log.info("Starting cleanup after feature method")

        if (specHelper.deleteScheme(testSchema)) {
                    log.debug("\tDeleted test scheme")
                    testSchema = null
                } else {
                    throw new RuntimeException("Error deleting test scheme:" + testSchema.name)
                }
    }

     */


    def "Verify that setServiceAccount() finds the user regardless of input type"() {


        when: "Testing with username"


        log.info("Testing setServiceAccount() with username")
        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.setServiceAccount(specHelper.jiraAdminUser.username)

        then: "Checking that im.initialUser and im.serviceUser was set correctly"

        im.initialUser == specHelper.userRunningTheScript
        im.serviceUser == specHelper.jiraAdminUser

        log.info("\tsetServiceAccount when supplied with a username works as intended")


        when: "Testing with applicationUser"
        log.info("Testing setServiceAccount() with applicationUser")
        im = new InsightManagerForScriptrunner()
        im.setServiceAccount(specHelper.jiraAdminUser)

        then: "Checking that im.initialUser and im.serviceUser was set correctly"

        im.initialUser == specHelper.userRunningTheScript
        im.serviceUser == specHelper.jiraAdminUser

        log.info("\tsetServiceAccount when supplied with a applicationUser works as intended")


    }

    def "Verify IQL searching"(String iql, long matchSize, ApplicationUser loggedInUser, ApplicationUser serviceAccount) {


        setup: "Setting up the Jira Logged in user"
        log.info("Will test IQL searching")
        log.info("\tWill run as logged in user:" + loggedInUser)
        log.info("\tWill run IM with service user:" + serviceAccount)
        log.info("\tWill use IQL:" + iql)
        log.info("\tExpect to find $matchSize objects")
        jiraAuthenticationContext.setLoggedInUser(loggedInUser)


        when: "Setting up IM to use the serviceAccount and executing the IQL"
        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.setServiceAccount(serviceAccount)
        ArrayList<ObjectBean> matchingObjects = im.iql(testSchema.id, iql)


        then: "The expected matching number of objects are found"
        matchingObjects.size() == matchSize
        log.debug("\tFound the correct number of objects")

        and: "The currently logged in user is restored"
        jiraAuthenticationContext.loggedInUser == loggedInUser


        cleanup: "Restore the logged in user to the user running the script"
        jiraAuthenticationContext.setLoggedInUser(specHelper.userRunningTheScript)


        where:
        iql                                           | matchSize | loggedInUser                    | serviceAccount
        "ObjectType = \"Object With All Attributes\"" | 2         | specHelper.jiraAdminUser        | specHelper.jiraAdminUser
        "ObjectType = \"Object With All Attributes\"" | 2         | specHelper.insightSchemaManager | specHelper.insightSchemaManager
        "ObjectType = \"Object With All Attributes\"" | 2         | specHelper.insightSchemaUser    | specHelper.insightSchemaUser
        "ObjectType = \"Object With All Attributes\"" | 0         | specHelper.projectCustomer      | specHelper.projectCustomer
        "ObjectType = \"Object With All Attributes\"" | 2         | specHelper.projectCustomer      | specHelper.jiraAdminUser


    }

    def "Test creation of objects with various Attribute Types"(String objectTypeName, Map attributeValuesToSet, Map expectedAttributeValues) {

        setup:
        log.info("Will test creation of objects with all attribute types")
        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.log.setLevel(Level.WARN)

        when:
        log.debug("\tCreating an object:")
        log.debug("\t" * 2 + "ObjectType:" + objectTypeName)
        log.debug("\t" * 2 + "Attributes:" + attributeValuesToSet)

        ObjectBean newObject = im.createObject(testSchema.id, objectTypeName, attributeValuesToSet)
        log.debug("\tThe new objects key is:" + newObject.objectKey)

        Map newObjectValues = im.getObjectAttributeValues(newObject, attributeValuesToSet.keySet() as ArrayList)
        log.debug("\tThe new objects attribute values are:" + newObjectValues)

        then:

        newObjectValues.each { newValue ->

            Map.Entry<String, ArrayList> expectedValue = expectedAttributeValues.find { it.key == newValue.key }


            if (expectedValue.value.first() instanceof Date) {

                log.debug("\t\tTesting if the new Date value ${newValue.value} equals the expected Date value${expectedValue.value}")

                Date expectedDate = expectedValue.value.first() as Date
                Date realDate = newValue.value.first() as Date

                Integer expectedTimestamp = realDate.getTime().div(1000).toInteger()
                Integer realTimestamp = expectedDate.getTime().div(1000).toInteger()

                assert expectedTimestamp == realTimestamp

            } else {
                log.debug("\t\tTesting if the new value ${newValue.value} equals ${expectedValue.value}")
                assert newValue.value == expectedValue.value
            }


        }
        log.info("\tThe attributes where set successfully")


        where:
        objectTypeName               | attributeValuesToSet                                                 | expectedAttributeValues
        "Object With All Attributes" | [Name: "Testing Boolean with true boolean object", Boolean: true]    | [Name: [attributeValuesToSet.Name], Boolean: [true]]
        "Object With All Attributes" | [Name: "Testing Boolean with true string object", Boolean: "true"]   | [Name: [attributeValuesToSet.Name], Boolean: [true]]
        "Object With All Attributes" | [Name: "Testing Boolean with false boolean object", Boolean: false]  | [Name: [attributeValuesToSet.Name], Boolean: [false]]
        "Object With All Attributes" | [Name: "Testing Boolean with false string object", Boolean: "false"] | [Name: [attributeValuesToSet.Name], Boolean: [false]]
        "Object With All Attributes" | [Name: "Testing Integer with string object", Integer: "99"]          | [Name: [attributeValuesToSet.Name], Integer: [99]]
        "Object With All Attributes" | [Name: "Testing Integer with int object", Integer: 99]               | [Name: [attributeValuesToSet.Name], Integer: [99]]
        "Object With All Attributes" | [Name: "Testing Integer with string object", Integer: "99"]          | [Name: [attributeValuesToSet.Name], Integer: [99]]
        "Object With All Attributes" | [Name: "Testing Float with Float object", Float: 99.12]              | [Name: [attributeValuesToSet.Name], Float: [99.12]]
        "Object With All Attributes" | [Name: "Testing Float with string object", Float: "99.12"]           | [Name: [attributeValuesToSet.Name], Float: [99.12]]
        "Object With All Attributes" | [Name: "Testing Date with Date object", Date: new Date()]            | [Name: [attributeValuesToSet.Name], Date: [new Date()]]

    }


    def "Test creation of objects with referenced Attribute Types"(String objectTypeName, String attribute, String referencedObjectIql) {

        setup: "Setting up IM"
        log.info("Will test creation of objects with referenced attribute types")
        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.log.setLevel(Level.WARN)
        ArrayList<ObjectBean> matchingObjects = im.iql(testSchema.id, referencedObjectIql)
        log.debug("\t" * 2 + "Will set value to objects matching IQL:" + referencedObjectIql)

        when: "Creating an object with an array of ObjectBean as attribute value"
        String objectName = "An object created with an array of ObjectBean as attribute value"
        log.debug("\tCreating an object:")
        log.debug("\t" * 2 + "ObjectType:" + objectTypeName)
        log.debug("\t" * 2 + "Object name:" + objectName)
        log.debug("\t" * 2 + "Attribute:" + attribute)
        log.debug("\t" * 2 + "Will set attribute value to:" + matchingObjects)

        ObjectBean newObject = im.createObject(testSchema.id, objectTypeName, [Name: objectName, (attribute): matchingObjects])
        log.debug("\tThe new objects key is:" + newObject.objectKey)

        Map newObjectValues = im.getObjectAttributeValues(newObject, [attribute] as ArrayList)
        log.debug("\tThe new objects attribute values are:" + newObjectValues)

        then: "The new object should have all the objects as attribute value"

        newObjectValues.get(attribute)?.objectKey?.sort() == matchingObjects.objectKey.sort()
        log.info("\tThe attributes where set successfully")


        when: "Creating an object with an array of ObjectBean-IDs as attribute value"
        objectName = "An object created with an array of ObjectBean-IDs as attribute value"
        log.debug("\tCreating an object:")
        log.debug("\t" * 2 + "ObjectType:" + objectTypeName)
        log.debug("\t" * 2 + "Object name:" + objectName)
        log.debug("\t" * 2 + "Attribute:" + attribute)
        log.debug("\t" * 2 + "Will set attribute value to:" + matchingObjects.id)
        newObject = null
        newObjectValues = null

        newObject = im.createObject(testSchema.id, objectTypeName, [Name: objectName, (attribute): matchingObjects.id])
        log.debug("\tThe new objects key is:" + newObject.objectKey)

        newObjectValues = im.getObjectAttributeValues(newObject, [attribute] as ArrayList)
        log.debug("\tThe new objects attribute values are:" + newObjectValues)

        then: "The new object should have all the objects as attribute value"

        newObjectValues.get(attribute)?.objectKey?.sort() == matchingObjects.objectKey.sort()
        log.info("\tThe attributes where set successfully")


        when: "Creating an object with an array of ObjectBean-Keys as attribute value"
        objectName = "An object created with an array of ObjectBean-Keys as attribute value"
        log.debug("\tCreating an object:")
        log.debug("\t" * 2 + "ObjectType:" + objectTypeName)
        log.debug("\t" * 2 + "Object name:" + objectName)
        log.debug("\t" * 2 + "Attribute:" + attribute)
        log.debug("\t" * 2 + "Will set attribute value to:" + matchingObjects.objectKey)
        newObject = null
        newObjectValues = null

        newObject = im.createObject(testSchema.id, objectTypeName, [Name: objectName, (attribute): matchingObjects.objectKey])
        log.debug("\tThe new objects key is:" + newObject.objectKey)

        newObjectValues = im.getObjectAttributeValues(newObject, [attribute] as ArrayList)
        log.debug("\tThe new objects attribute values are:" + newObjectValues)

        then: "The new object should have all the objects as attribute value"

        newObjectValues.get(attribute)?.objectKey?.sort() == matchingObjects.objectKey.sort()
        log.info("\tThe attributes where set successfully")


        where:
        objectTypeName               | attribute | referencedObjectIql
        "Object With All Attributes" | "Object"  | "Name = \"Sample object\""
        "Object With All Attributes" | "Object"  | "objectType = \"Object With All Attributes\""

    }


    def "Test updateObjectAttributes and updateObjectAttribute with various attribute types"(String objectTypeName, Map attributeValuesToSet, Map<String, List> expectedAttributeValues, ApplicationUser loggedInUser, ApplicationUser serviceAccount) {

        setup: "Initiate IM and create a blank test object"
        log.info("*" * 20 + " Testing updateObjectAttributes and updateObjectAttributes with various attribute types " + "*" * 20)

        //Replacing IQL´s in the attributeValuesToSet with actual objects
        attributeValuesToSet = specHelper.replaceIqlWithObjects(attributeValuesToSet, testSchema)
        expectedAttributeValues = specHelper.replaceIqlWithObjects(expectedAttributeValues, testSchema)

        //Make sure all expected values are arrays
        expectedAttributeValues.each {
            if (!(it.value instanceof ArrayList)) {
                it.value = [it.value]
            }
        }

        log.debug("\tWill start testing updating multiple attributes with updateObjectAttributes")
        log.debug("\t\tWill create objects of type:" + objectTypeName)
        log.debug("\t\tWill set the following attributes:" + attributeValuesToSet)
        log.debug("\t\tWill expect the following attributes to be returned:" + expectedAttributeValues)
        log.debug("\t\tWill be logged in as:" + loggedInUser)
        log.debug("\t\tWill use the serviceAccount:" + serviceAccount)


        jiraAuthenticationContext.setLoggedInUser(loggedInUser)
        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.log.setLevel(Level.TRACE)
        im.setServiceAccount(serviceAccount)
        ObjectBean testObject = im.createObject(testSchema.id, objectTypeName, ["Name": "A test name"])
        log.debug("\t\tCreated test object:" + testObject)


        when: "When setting attribute values with updateObjectAttributes"

        im.updateObjectAttributes(testObject, attributeValuesToSet)


        log.debug("\t\tupdateObjectAttributes() was used to set the test objects attributes")
        log.debug("\t\tWill now retrieve the test objects values for the attributes:" + expectedAttributeValues.keySet())
        Map<String, List> attributeValuesAfterUpdate = im.getObjectAttributeValues(testObject, expectedAttributeValues.keySet() as List)
        String valueStringAfterUpdate = attributeValuesAfterUpdate.collect {
            if (it.value instanceof ArrayList) {
                it.value.sort()
            } else {
                it.value
            }
        }.sort().toString()

        String valueStringExpected = expectedAttributeValues.collect {
            if (it.value instanceof ArrayList) {
                it.value.sort()
            } else {
                it.value
            }
        }.sort().toString()

        log.trace("\t" * 3 + "The a values are:" + attributeValuesAfterUpdate.sort())
        log.trace("\t" * 3 + "The a expected values are:" + expectedAttributeValues.sort())


        then: "The objects attributes should be set to the expected values"
        attributeValuesAfterUpdate.sort().toString() == expectedAttributeValues.sort().toString()
        valueStringAfterUpdate == valueStringExpected


        and: "The currently logged in user is restored"
        jiraAuthenticationContext.loggedInUser == loggedInUser
        log.debug("\t\tUpdating of multiple attributes with updateObjectAttributes() was tested successfully")


        when: "When updating single attributes at time"
        log.debug("\tWill start testing updating single attributes at a time with updateObjectAttribute")
        testObject = null
        attributeValuesAfterUpdate = null
        valueStringAfterUpdate = null
        valueStringExpected = null
        testObject = im.createObject(testSchema.id, objectTypeName, ["Name": "A test name"])
        log.debug("\t\tCreated test object:" + testObject)


        attributeValuesToSet.each { attribute, value ->
            log.debug("\t\tUppdating attribute \"$attribute\" with value \"$value\"")
            if (value instanceof ArrayList) {
                log.trace("\t" * 3 + "Value class: ${value?.first()?.class?.simpleName}")
            } else {
                log.trace("\t" * 3 + "Value class: ${value.class.simpleName}")
            }
            im.updateObjectAttribute(testObject, attribute, value)
        }
        log.debug("\t\tWill now retrieve the test objects values for the attributes:" + expectedAttributeValues.keySet())
        attributeValuesAfterUpdate = im.getObjectAttributeValues(testObject, expectedAttributeValues.keySet() as List)
        log.trace("\t" * 3 + "The a values are:" + attributeValuesAfterUpdate)

        valueStringAfterUpdate = attributeValuesAfterUpdate.collect {
            if (it.value.first() instanceof ObjectBean) {
                return it.value.sort { it.id }
            } else {
                return it.value.sort()
            }
        }.sort().toString()
        valueStringExpected = expectedAttributeValues.collect {
            if (it.value.first() instanceof ObjectBean) {
                return it.value.sort { it.id }
            } else {
                return it.value.sort()
            }
        }.sort().toString()

        then:
        valueStringAfterUpdate == valueStringExpected

        and: "The currently logged in user is restored"
        jiraAuthenticationContext.loggedInUser == loggedInUser
        log.debug("\t\tUpdating of single attributes with updateObjectAttribute() was tested successfully")


        cleanup: "Restore the logged in user to the user running the script"
        jiraAuthenticationContext.setLoggedInUser(specHelper.userRunningTheScript)

        log.debug("\\" * 20 + " Testing updateObjectAttributes with various attribute types has finished " + "/" * 20)

        where:
        objectTypeName               | attributeValuesToSet                                                                                                                                                                   | expectedAttributeValues                                                                                                                                   | loggedInUser               | serviceAccount


        "Object With All Attributes" | [Name: "Updating an object attribute with ObjectBeans as jiraAdminUser", Object: [IQL_REPLACE_WITH_BEANS: "objectType = \"Object With All Attributes\""], "Text": "Some text value"]   | [Name: [attributeValuesToSet.Name], Object: attributeValuesToSet.Object, "Text": [attributeValuesToSet.Text]]                                             | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "Object With All Attributes" | [Name: "Updating an object attribute with ObjectBeans as projectCustomer", Object: [IQL_REPLACE_WITH_BEANS: "objectType = \"Object With All Attributes\""], "Text": "Some text value"] | [Name: [attributeValuesToSet.Name], Object: attributeValuesToSet.Object, "Text": [attributeValuesToSet.Text]]                                             | specHelper.projectCustomer | specHelper.jiraAdminUser
        "Object With All Attributes" | [Name: "Updating an object attribute with ObjectKeys as jiraAdminUser", Object: [IQL_REPLACE_WITH_KEYS: "objectType = \"Object With All Attributes\""], "Text": "Some text value"]     | [Name: [attributeValuesToSet.Name], Object: [IQL_REPLACE_WITH_BEANS: "objectType = \"Object With All Attributes\""], "Text": [attributeValuesToSet.Text]] | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "Object With All Attributes" | [Name: "Updating an object attribute with ObjectKeys as projectCustomer", Object: [IQL_REPLACE_WITH_KEYS: "objectType = \"Object With All Attributes\""], "Text": "Some text value"]   | [Name: [attributeValuesToSet.Name], Object: [IQL_REPLACE_WITH_BEANS: "objectType = \"Object With All Attributes\""], "Text": [attributeValuesToSet.Text]] | specHelper.projectCustomer | specHelper.jiraAdminUser
        "Object With All Attributes" | [Name: "Updating an object attribute with ObjectIds as jiraAdminUser", Object: [IQL_REPLACE_WITH_IDS: "objectType = \"Object With All Attributes\""], "Text": "Some text value"]       | [Name: [attributeValuesToSet.Name], Object: [IQL_REPLACE_WITH_BEANS: "objectType = \"Object With All Attributes\""], "Text": [attributeValuesToSet.Text]] | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "Object With All Attributes" | [Name: "Updating an object attribute with ObjectIds as projectCustomer", Object: [IQL_REPLACE_WITH_IDS: "objectType = \"Object With All Attributes\""], "Text": "Some text value"]     | [Name: [attributeValuesToSet.Name], Object: [IQL_REPLACE_WITH_BEANS: "objectType = \"Object With All Attributes\""], "Text": [attributeValuesToSet.Text]] | specHelper.projectCustomer | specHelper.jiraAdminUser
        "Object With All Attributes" | [Name: "Updating an object attribute with ObjectBean as jiraAdminUser", Object: [IQL_REPLACE_WITH_BEAN: "objectType = \"Object With All Attributes\""], "Text": "Some text value"]     | [Name: [attributeValuesToSet.Name], Object: attributeValuesToSet.Object, "Text": [attributeValuesToSet.Text]]                                             | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "Object With All Attributes" | [Name: "Updating an object attribute with ObjectBean as projectCustomer", Object: [IQL_REPLACE_WITH_BEAN: "objectType = \"Object With All Attributes\""], "Text": "Some text value"]   | [Name: [attributeValuesToSet.Name], Object: attributeValuesToSet.Object, "Text": [attributeValuesToSet.Text]]                                             | specHelper.projectCustomer | specHelper.jiraAdminUser
        "Object With All Attributes" | [Name: "Updating an object attribute with ObjectKey as jiraAdminUser", Object: [IQL_REPLACE_WITH_KEY: "objectType = \"Object With All Attributes\""], "Text": "Some text value"]       | [Name: [attributeValuesToSet.Name], Object: [IQL_REPLACE_WITH_BEAN: "objectType = \"Object With All Attributes\""], "Text": [attributeValuesToSet.Text]]  | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "Object With All Attributes" | [Name: "Updating an object attribute with ObjectKey as projectCustomer", Object: [IQL_REPLACE_WITH_KEY: "objectType = \"Object With All Attributes\""], "Text": "Some text value"]     | [Name: [attributeValuesToSet.Name], Object: [IQL_REPLACE_WITH_BEAN: "objectType = \"Object With All Attributes\""], "Text": [attributeValuesToSet.Text]]  | specHelper.projectCustomer | specHelper.jiraAdminUser
        "Object With All Attributes" | [Name: "Updating an object attribute with ObjectId as jiraAdminUser", Object: [IQL_REPLACE_WITH_ID: "objectType = \"Object With All Attributes\""], "Text": "Some text value"]         | [Name: [attributeValuesToSet.Name], Object: [IQL_REPLACE_WITH_BEAN: "objectType = \"Object With All Attributes\""], "Text": [attributeValuesToSet.Text]]  | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "Object With All Attributes" | [Name: "Updating an object attribute with ObjectId as projectCustomer", Object: [IQL_REPLACE_WITH_ID: "objectType = \"Object With All Attributes\""], "Text": "Some text value"]       | [Name: [attributeValuesToSet.Name], Object: [IQL_REPLACE_WITH_BEAN: "objectType = \"Object With All Attributes\""], "Text": [attributeValuesToSet.Text]]  | specHelper.projectCustomer | specHelper.jiraAdminUser


    }


    def 'Test attachment export and import'(ArrayList<String> sourceFileUrls) {

        setup: "Create a source and destination ObjectBean, download source files and add to source object"
        log.info("*" * 20 + " Testing export and import of attachments with default parameters " + "*" * 20)


        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.log.setLevel(Level.WARN)

        ObjectBean sourceObject = im.createObject(testSchema.id, "Source Object", [Name: "Attachment Export Source Object"])
        ObjectBean destinationObject = im.createObject(testSchema.id, "Destination Object", [Name: "Attachment Export Destination Object", "Old Object key": sourceObject.objectKey])
        log.debug("\tWill use source object:" + sourceObject)
        log.debug("\tWill use destination object:" + destinationObject)


        log.debug("\tUsing sourceFileUrls:" + sourceFileUrls.join(","))

        String destinationPath = specHelper.tempDir.path + "/" + this.class.simpleName
        log.debug("\t\tFiles will be temporarily placed here:" + destinationPath)
        ArrayList<File> sourceFiles = sourceFileUrls.collect { sourceFileUrl -> specHelper.downloadFile(sourceFileUrl, destinationPath) }
        assert sourceFiles.size() == sourceFileUrls.size()
        log.debug("\t" * 2 + "Source files downloaded")

        ArrayList<SimplifiedAttachmentBean> sourceObjectAttachments = []
        sourceFiles.each { sourceFile ->
            log.debug("\t" * 2 + "Adding source file ${sourceFile.name} to source object $sourceObject")
            SimplifiedAttachmentBean newAttachmentBean = im.addObjectAttachment(sourceObject, sourceFile)

            assert newAttachmentBean != null: "There was an error adding source file ${sourceFile.name} to source object $sourceObject"
            sourceObjectAttachments.add(newAttachmentBean)
        }

        String exportPath = System.getProperty("java.io.tmpdir") + "/" + this.class.simpleName + "/TestAttachmentExportAndImport"
        assert !new File(exportPath).exists(): "Export directory already exists:" + exportPath

        log.debug("\tWill use export directory:" + exportPath)


        when: "Exporting attachments from source object"
        log.debug("\tStarting export of source objects attachments")
        ArrayList<File> exportedFiles = im.exportObjectAttachments(sourceObject, exportPath)
        log.debug("\t\tExport complete")

        then: "The list of exported files should be the same as the attachments added to the source object"
        exportedFiles.name == sourceObjectAttachments.originalFileName
        log.debug("\t\tThe exported files have the expected names")

        exportedFiles.size() == sourceObjectAttachments.attachmentFile.size()
        log.debug("\t\tThe exported files are of the correct quantity")

        exportedFiles.parentFile.name.every { it == sourceObject.objectKey }
        log.debug("\t\tThe exported files are in the correct parent folder")

        exportedFiles.collect { it.bytes.sha256() } == sourceObjectAttachments.attachmentFile.collect { it.bytes.sha256() }
        log.debug("\t\tThe hashes of the source files and the attachments match")


        when: "Importing the exported attachments to the destination object"
        log.debug("\tTesting import of attachments with default parameters")
        ArrayList<SimplifiedAttachmentBean> importedBeans = im.importObjectAttachments(exportPath)
        log.debug("\t\tImport complete")

        then:
        importedBeans.originalFileName.sort() == sourceObjectAttachments.originalFileName.sort()
        log.debug("\t\tThe imported files have the expected names")

        importedBeans.attachmentFile.size() == sourceObjectAttachments.attachmentFile.size()
        log.debug("\t\tThe imported files are of the correct quantity")

        importedBeans.attachmentFile.collect { it.bytes.sha256() }.sort() == sourceObjectAttachments.attachmentFile.collect { it.bytes.sha256() }.sort()
        log.debug("\t\tThe hashes of the source files and the imported files match")

        importedBeans.attachmentBean.objectId.every { it == destinationObject.id }
        log.debug("\t\tThe attachments where added to the correct destination object")

        importedBeans.attachmentBean.comment.every { it == "Imported from " + sourceObject.objectKey }
        log.debug("\t\tThe attachments where given the correct comment")

        exportedFiles.every { it.canRead() && it.exists() }
        log.debug("\t\tThe source files where not removed")

        log.debug("\t" + "\\" * 20 + " Import with default parameters was tested successfully " + "//" * 20)


        cleanup:
        File exportDirectory = new File(exportPath)
        log.debug("\tDeleting test file from filesystem:" + exportPath)
        FileUtils.deleteDirectory(exportDirectory)
        assert !exportDirectory.exists(): "Error deleting test file:" + exportPath


        where:
        sourceFileUrls | _
        [
                "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png",
                "https://www.atlassian.com/dam/jcr:242ae640-3d6a-472d-803d-45d8dcc2a8d2/Atlassian-horizontal-blue-rgb.svg",
                "https://bitbucket.org/atlassian/jira_docs/downloads/JIRACORESERVER_8.10.pdf"
        ]              | _


    }

    def 'Test readOnly mode of attachment operations'(ArrayList<String> sourceFileUrls) {

        setup: "Create a source and destination ObjectBean, download source files and add to source object"
        log.info("*" * 20 + " Testing readOnly mode of attachment operations " + "*" * 20)

        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.log.setLevel(Level.WARN)
        ObjectBean sourceObject = im.createObject(testSchema.id, "Source Object", [Name: "Testing Read Only - Source Object"])
        ObjectBean destinationObject = im.createObject(testSchema.id, "Destination Object", [Name: "Testing Read Only - Destination Object", "Old Object key": sourceObject.objectKey])
        log.debug("\tWill use source object:" + sourceObject)
        log.debug("\tWill use destination object:" + destinationObject)

        log.debug("\tUsing sourceFileUrls:" + sourceFileUrls.join(","))

        String destinationPath = specHelper.tempDir.path + "/" + this.class.simpleName
        log.debug("\t\tFiles will be temporarily placed here:" + destinationPath)
        ArrayList<File> sourceFiles = sourceFileUrls.collect { sourceFileUrl -> specHelper.downloadFile(sourceFileUrl, destinationPath) }
        assert sourceFiles.size() == sourceFileUrls.size()
        log.debug("\t" * 2 + "Source files downloaded")


        sourceFiles.each { sourceFile ->
            log.debug("\t" * 2 + "Adding source file ${sourceFile.name} to source object $sourceObject")
            SimplifiedAttachmentBean newAttachmentBean = im.addObjectAttachment(sourceObject, sourceFile)

            assert newAttachmentBean != null: "There was an error adding source file ${sourceFile.name} to source object $sourceObject"

        }

        ArrayList<SimplifiedAttachmentBean> sourceObjectAttachments = im.getAllObjectAttachmentBeans(sourceObject)

        String exportPath = System.getProperty("java.io.tmpdir") + "/" + this.class.simpleName + "/TestAttachmentReadOnly"
        assert !new File(exportPath).exists(): "Export directory already exists:" + exportPath

        log.debug("\tWill use export directory:" + exportPath)

        ArrayList<SimplifiedAttachmentBean> destinationAttachmentBeans = []


        when: "Adding attachments to object while in readOnly mode"
        log.debug("\tStarting test of addObjectAttachment() readOnly true")
        im.readOnly = true
        assert im.getAllObjectAttachmentBeans(destinationObject).isEmpty(): "The destination object already has attachments"

        sourceFiles.each { sourceFile ->
            destinationAttachmentBeans.add(im.addObjectAttachment(destinationObject, sourceFile))

        }


        then: "No new attachmentBeans should have been crated and the destination object should not have any attachments"
        destinationAttachmentBeans.every { it == null }
        im.getAllObjectAttachmentBeans(destinationObject).size() == 0
        log.debug("\t" * 2 + " addObjectAttachment() readOnly true was tested successfully")


        when: "Deleting attachments while in readOnly mode"
        log.debug("\tStarting test of deleteObjectAttachment() readOnly true")

        assert sourceObjectAttachments.size() > 0: "The source object does not have any attachments"

        im.readOnly = true
        sourceObjectAttachments.each {
            assert !im.deleteObjectAttachment(it): "Attachments appear to have been deleted even though in readOnly mode, Object:" + sourceObject + ", attachment:" + it.originalFileName
        }

        ArrayList<SimplifiedAttachmentBean> attachmentsAfterDelete = im.getAllObjectAttachmentBeans(sourceObject)

        then: "No Attachments should have been deleted and the source object should have the same attachments as before"
        assert sourceObjectAttachments == attachmentsAfterDelete: "Attachments have changed during the deleteObjectAttachment() operation"
        attachmentsAfterDelete.every { it.isValid() }
        log.debug("\t" * 2 + " deleteObjectAttachment() readOnly true was tested successfully")


        when: "Exporting attachments while in readOnly mode"
        log.debug("\tStarting test of exportObjectAttachments() readOnly true")
        log.trace("\t" * 2 + "Will use export path:" + exportPath + "/export")
        im.readOnly = true
        ArrayList<File> exportedFiles = im.exportObjectAttachments(sourceObject, exportPath + "/export")

        then: "No files should have been exported and no filex should have been placed in the destination directory"
        assert exportedFiles.isEmpty(): "exportObjectAttachments() returned File objects even though in read only mode"
        File exportDirectory = new File(exportPath + "/export")

        assert exportDirectory.listFiles() == null: "exportObjectAttachments() placed files in export folder even though in read only mode"
        log.debug("\t" * 2 + "exportObjectAttachments() readOnly true was tested successfully")


        when: "Importing attachments while in readOnly mode"
        log.debug("\tStarting test of importObjectAttachments() readOnly true")
        im.readOnly = true
        assert im.getAllObjectAttachmentBeans(destinationObject).isEmpty(): "The destination object already has attachments"

        ArrayList<SimplifiedAttachmentBean> importedFiles = im.importObjectAttachments(exportPath + "/export")

        then:
        assert importedFiles.every { it == null }: "importObjectAttachments() returned SimplifiedAttachmentBean even though in read only mode"
        im.getAllObjectAttachmentBeans(destinationObject).size() == 0
        log.debug("\t" * 2 + "importObjectAttachments() readOnly true was tested successfully")


        cleanup:
        log.debug("\tDeleting test file from filesystem:" + exportDirectory.path)
        FileUtils.deleteDirectory(exportDirectory)
        assert !exportDirectory.exists(): "Error deleting test file:" + exportPath


        where:
        sourceFileUrls | _
        [
                "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png",
                "https://www.atlassian.com/dam/jcr:242ae640-3d6a-472d-803d-45d8dcc2a8d2/Atlassian-horizontal-blue-rgb.svg",
                "https://bitbucket.org/atlassian/jira_docs/downloads/JIRACORESERVER_8.10.pdf"
        ]              | _

    }


    def 'Test attachment CRD operations'(String sourceFilePath, boolean testDeletionOfSource, String attachmentComment) {

        setup:
        log.info("Testing attachment operations")
        log.debug("\tUsing sourceFile:" + sourceFilePath)

        String destinationPath = specHelper.tempDir.path + "/" + this.class.simpleName
        log.debug("\tFile will be temporarily placed here:" + destinationPath)


        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.log.setLevel(Level.WARN)

        ObjectBean testObject = im.createObject(testSchema.id, "CRUD Object", [Name: "Testing attachment CRD operations"])
        log.debug("\tWill use test object:" + testObject)
        String expectedAttachmentPath = specHelper.jiraHome.path + "/data/attachments/insight/object/${testObject.id}/"

        log.debug("\tDownloading source file")
        specHelper.log.setLevel(Level.ALL)
        File testFile = specHelper.downloadFile(sourceFilePath, destinationPath)

        assert testFile.exists(): "Error downloading sourceFile:" + sourceFilePath

        String testFileHash = testFile.getBytes().sha256()
        log.debug("\t\tDownload complete: ${testFile.path}, size: " + testFile.size())


        when:
        log.debug("\tTesting attaching file to $testObject")

        SimplifiedAttachmentBean newAttachmentBean = im.addObjectAttachment(testObject, testFile, "", attachmentComment, testDeletionOfSource)

        expectedAttachmentPath += newAttachmentBean.attachmentBean.nameInFileSystem

        then:
        newAttachmentBean != null
        newAttachmentBean.isValid()
        assert new File(expectedAttachmentPath).canRead(): "The attached file wasn't found at the expected path:" + expectedAttachmentPath
        assert newAttachmentBean.attachmentBean.comment == attachmentComment: "The attachment didnt get its expected comment"

        if (testDeletionOfSource) {
            assert !testFile.canRead(): "Source file wasn't deleted by IM when expected"
        } else {
            assert testFile.canRead(): "Source file was unintentionally deleted by IM"
        }

        log.debug("\t\tThe attachment was successfully created")
        log.trace("\t" * 3 + "A SimplifiedAttachmentBean was returned")
        log.trace("\t" * 3 + "The attached file was found at the expected path:" + expectedAttachmentPath)
        log.trace("\t" * 3 + "The attachment was given the expected comment:" + attachmentComment)
        log.trace("\t" * 3 + "The deleteSourceFile parameter was respected")

        when:

        log.debug("\tTesting getAllObjectAttachmentBeans() to find and verify the new attachment")
        ArrayList<SimplifiedAttachmentBean> objectAttachments = im.getAllObjectAttachmentBeans(testObject)
        SimplifiedAttachmentBean retrievedSimplifiedAttachment = objectAttachments.find { it.id == newAttachmentBean.id }

        then:
        assert retrievedSimplifiedAttachment.originalFileName == testFile.name: "The name of the test file and the retrieved attachment file doesn't match"
        assert retrievedSimplifiedAttachment.attachmentFile.getBytes().sha256() == testFileHash: "The hash of the test file and the retrieved attachment file doesn't match"

        assert newAttachmentBean.attachmentBean.comment == retrievedSimplifiedAttachment.attachmentBean.comment: "The comment of the SimplifiedAttachmentBean differs"

        log.debug("\t\tThe new attachment was successfully verified")
        log.trace("\t" * 3 + "getAllObjectAttachmentBeans() returned an array containing an attachment with the expected id")
        log.trace("\t" * 3 + "The returned attachment had the correct originalFileName")
        log.trace("\t" * 3 + "The returned attachment had the correct SHA256 hash")
        log.trace("\t" * 3 + "The returned attachment had the correct comment")


        when:
        log.debug("\tTesting deleteObjectAttachment() by deleting the new attachment")
        boolean deletionResult = im.deleteObjectAttachment(newAttachmentBean)

        then:

        assert deletionResult: "deleteObjectAttachment was unsuccessful and returned false"

        assert im.objectFacade.loadAttachmentBeanById(newAttachmentBean.id) == null
        assert !new File(expectedAttachmentPath).canRead(): "The attached file can still be found at the expected path:" + expectedAttachmentPath
        log.trace("\t\tThe attachment was successfully deleted")


        cleanup:
        if (!testDeletionOfSource) {
            log.debug("Deleting test file from filesystem:" + testFile.path)
            assert testFile.delete(): "Error deleting test file:" + testFile.path
            log.debug("\tThe test file was successfully deleted")
        }

        where:
        sourceFilePath                                                                                             | testDeletionOfSource | attachmentComment
        "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"                       | false                | "no comment"
        "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"                       | true                 | "no comment"
        "https://www.atlassian.com/dam/jcr:242ae640-3d6a-472d-803d-45d8dcc2a8d2/Atlassian-horizontal-blue-rgb.svg" | true                 | " a comment"
        "https://bitbucket.org/atlassian/jira_docs/downloads/JIRACORESERVER_8.10.pdf"                              | false                | "another comment"


    }

    def "Test renderObjectToHtml()"(String objectIql, long matchSize, String attributeToRender, expectedAttributeValue, ApplicationUser loggedInUser, ApplicationUser serviceAccount) {

        setup: "Setting up the Jira Logged in user"

        Pattern pattern = Pattern.compile("<table>\\s*<tr>\\s*<td.*?><b>(.*?):<\\/b><\\/td>\\s*?<td.*?>(.*?)<\\/td>\\s*?<\\/tr><\\/table>")

        log.info("Will test IQL searching")
        log.info("\tWill run as logged in user:" + loggedInUser)
        log.info("\tWill run IM with service user:" + serviceAccount)
        log.info("\tWill use this IQL to find one or zero test objects:" + objectIql)
        log.info("\tExpect to find $matchSize objects")
        jiraAuthenticationContext.setLoggedInUser(loggedInUser)

        when: "Getting the Insight test object"
        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.setServiceAccount(serviceAccount)
        ArrayList<ObjectBean> testObjects = im.iql(testSchema.id, objectIql)

        then: "The matchSize is not larger than 1"
        matchSize <= 1


        and: "The expected number of objects where found"
        testObjects.size() == matchSize


        when: "Rendering the object as html"
        Matcher matcher
        if (matchSize != 0) {
            String objectHtml = im.renderObjectToHtml(testObjects.first(), [attributeToRender])
            matcher = pattern.matcher(objectHtml)
        }


        then: "HTML table appears to be well formed "
        if (matchSize != 0) {
            matcher.size() == 1
            matcher[0].size() == 3
        }


        and: "Table values are correct"
        if (matchSize != 0) {
            matcher[0][1] == attributeToRender
            matcher[0][2] == expectedAttributeValue
        }


        and: "The currently logged in user is restored"
        jiraAuthenticationContext.loggedInUser == loggedInUser


        cleanup: "Restore the logged in user to the user running the script"
        jiraAuthenticationContext.setLoggedInUser(specHelper.userRunningTheScript)

        where:
        objectIql                      | matchSize | attributeToRender | expectedAttributeValue                                                                          | loggedInUser               | serviceAccount
        "\"Name\" = \"Sample object\"" | 1         | "Name"            | "Sample object"                                                                                 | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 1         | "Boolean"         | "true"                                                                                          | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 1         | "Integer"         | "1"                                                                                             | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 1         | "Text"            | "Text value"                                                                                    | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 1         | "Float"           | "3.1415"                                                                                        | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 1         | "URL"             | "http://www.altavista.com"                                                                      | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 1         | "Email"           | "nisse@hult.com"                                                                                | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 1         | "Textarea"        | "<p>Some</p><p>text</p><p>on</p><p>many</p><p>lines</p><p><strong>with formating </strong></p>" | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 1         | "Select"          | "The First Option,The Second Option,The Third Option"                                           | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 1         | "IP Address"      | "127.0.0.1"                                                                                     | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 1         | "Group"           | "jira-administrators"                                                                           | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 1         | "Status"          | "Active"                                                                                        | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 1         | "Date"            | "Wed Sep 16 00:00:00 UTC 2020"                                                                  | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 1         | "DateTime"        | "Sun Sep 13 16:49:26 UTC 2020"                                                                  | specHelper.jiraAdminUser   | specHelper.jiraAdminUser
        "\"Name\" = \"Sample object\"" | 0         | ""                | ""                                                                                              | specHelper.projectCustomer | specHelper.projectCustomer
        "\"Name\" = \"Sample object\"" | 1         | "Email"           | "nisse@hult.com"                                                                                | specHelper.projectCustomer | specHelper.jiraAdminUser


    }


    def "Test Object comment CRD operations"(String objectTypeName, Map attributeValuesToSet) {

        setup:
        log.info("Will test creation of objects with all attribute types")
        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.log.setLevel(Level.WARN)

        log.debug("\tCreating an object:")
        log.debug("\t" * 2 + "ObjectType:" + objectTypeName)
        log.debug("\t" * 2 + "Attributes:" + attributeValuesToSet)

        ObjectBean newObject = im.createObject(testSchema.id, objectTypeName, attributeValuesToSet)
        log.debug("\tThe new objects key is:" + newObject.objectKey)


        when: "Getting comments from an object without comments"
        log.debug("\tWhen getting comments from an object without comments")
        ArrayList<CommentBean> commentBeans = im.getObjectComments(newObject)


        then: "An empty array should be returned"
        log.debug("\t" *2+ "An empty array should be returned")
        assert commentBeans.isEmpty(): "A non empty array was returned"
        log.debug("\t\tThe array is empty:" + commentBeans.empty)


        when: "Manager creates a comment with default access level"
        log.debug("\tWhen manager creates a comment with default access level")

        String defaultAccessCommentText = "A comment with default access level"
        im.setServiceAccount(specHelper.insightSchemaManager)
        CommentBean unrestrictedCommentBean = im.createObjectComment(newObject, defaultAccessCommentText)

        then: "It should be readable by schema manager"
        log.debug("\t"*2 + "It should be readable by schema manager")
        im.getObjectComments(newObject).findAll{it.comment == defaultAccessCommentText}.size() == 1
        log.debug("\t"*2 + (im.getObjectComments(newObject).findAll{it.comment == defaultAccessCommentText}.size() == 1))

        then: "It should be readable by schema user"
        log.debug("\t"*2 + "It should be readable by schema user")
        im.setServiceAccount(specHelper.insightSchemaUser)
        im.getObjectComments(newObject).findAll{it.comment == defaultAccessCommentText}.size() == 1
        log.debug("\t"*2 + (im.getObjectComments(newObject).findAll{it.comment == defaultAccessCommentText}.size() == 1))


        //This is confirmed to fail in Insight 8.6.12, the Insight API ignores supplied authors.
        /* https://jira.mindville.com/browse/ICS-1879
        then: "It should be authored by the schema manager"
        unrestrictedCommentBean.author == specHelper.insightSchemaManager.key
         */


        when: "Manager creates a comment with manager access level"
        log.debug("\tWhen manager creates a comment with default access level")

        String managerAccessCommentText = "A comment with manager access level"
        im.setServiceAccount(specHelper.insightSchemaManager)
        CommentBean restrictedCommentBean = im.createObjectComment(newObject, managerAccessCommentText, CommentBean.ACCESS_LEVEL_MANAGERS)

        then: "It should be readable by schema manager"
        log.debug("\t" *2 + "It should be readable by schema manager")
        im.getObjectComments(newObject).findAll{it.comment == managerAccessCommentText}.size() == 1
        log.debug("\t"*2 + (im.getObjectComments(newObject).findAll{it.comment == managerAccessCommentText}.size() == 1))


        //This is confirmed to fail in Insight 8.6.12, the Insight API will let users view restricted comments.
        /* https://jira.mindville.com/browse/ICS-1860
        then: "It should not be readable by schema user"
        log.debug("\tIt not should be readable by schema user")
        im.setServiceAccount(specHelper.insightSchemaUser)
        im.getObjectComments(newObject).findAll{it.comment == managerAccessCommentText}.size() == 0
        log.debug("\t"*2 + (im.getObjectComments(newObject).findAll{it.comment == managerAccessCommentText}.size() == 0))

         */

        when: "A schema user tries to delete a comment restricted to managers"
        log.debug("\tWhen a schema user tries to delete a comment restricted to managers")
        im.setServiceAccount(specHelper.insightSchemaUser)
        im.deleteObjectComment(restrictedCommentBean)

        then: "It should not be deleted"
        log.debug("\t"*2 + "An error should be thrown")
        InsightException exception = thrown(PermissionInsightException)
        log.debug("\t"*2 + exception.message)



        then: "It should still be accessible to schema managers"
        log.debug("\tIt should still be accessible to schema managers")
        im.setServiceAccount(specHelper.insightSchemaManager)
        im.getObjectComments(newObject).findAll{it.comment == managerAccessCommentText}.size() == 1
        log.debug("\t"*2 + (im.getObjectComments(newObject).findAll{it.comment == managerAccessCommentText}.size() == 1))

        where:
        objectTypeName               | attributeValuesToSet
        "Object With All Attributes" | [Name: "Testing object comments",]

    }

}