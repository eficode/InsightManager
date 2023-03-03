package com.eficode.atlassian.insightmanager.tests.specs

/**
 * Intended to test CRUD of Objects/attributes
 * Much work remains
 */

import com.eficode.atlassian.insightmanager.InsightManagerForScriptrunner
import com.eficode.atlassian.insightmanager.SimplifiedAttributeBean
import com.eficode.atlassian.insightmanager.tests.utils.SpecHelper
import com.riadalabs.jira.plugins.insight.services.model.IconBean
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectSchemaBean
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectTypeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectSchemaBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.Type as AttributeType
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.DefaultType as AttributeDefaultType
import io.riada.insight.api.graphql.resolvers.objectschema.ObjectSchema
import org.apache.log4j.Level
import org.apache.log4j.Logger
import spock.lang.Shared
import spock.lang.Specification

import java.lang.reflect.Method


class ImObjectSchemaCrudSpec extends Specification {

    @Shared
    Logger log = Logger.getLogger(ImObjectSchemaCrudSpec)
    @Shared
    String jiraBaseUrl = "http://jira.lcalhost:8080"
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

    def "Test Creation of ObjectTypes with attributes"() {
        //TODO continue here
        //TODO investigate available Statuses not being set for new attributes
        //TODO investigate the Time attribute
        //TODO investigate renaming SimplifiedAttributeBean as its not really a bean but a factory
        setup:
        log.info("Testing creation of schemas")
        String schemaName = "Testing ObjectTypeCreation"
        String schemaKey = "OBJECT"
        String iconBeanName = "Gear"

        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.log.setLevel(Level.ALL)
        im.deleteObjectSchema(schemaName, schemaKey) ?: log.info("\tRemoved a pre-existing schema")
        ObjectSchemaBean schemaBean =  im.createObjectSchema("Testing ObjectTypeCreation", "OBJECT")

        log.info("\tCreated schema:" + schemaBean.objectSchemaKey)

        IconBean iconBean = im.getIconBean(iconBeanName, schemaBean.id)
        assert iconBean : "Error finding IconBean: $iconBeanName"
        log.info("\tRetrieved IconBean:" + iconBean.id)

        when:"Creating an object type with no initial attributes"

        log.info("\tCreating an ObjectType with no custom attributes")
        ObjectTypeBean objectTypeWithNoAtr = im.createObjectType("Object with no attributes", schemaBean.id, iconBean, "An object with no attributes")
        log.info("\t\tCreated ObjectType:" + objectTypeWithNoAtr.name + " (${objectTypeWithNoAtr.id})")
        ArrayList<ObjectTypeAttributeBean> defaultAttributes =   im.objectTypeAttributeFacade.findObjectTypeAttributeBeans(objectTypeWithNoAtr.id)
        log.info("\t"*2 + "ObjectType has these attributes:" + defaultAttributes.collect {it.name + " (Id:${it.id})"}.join(", "))
        then: "An object with only default attributes should be created in the expected Schema with the correct icon"
        defaultAttributes.name.sort() == ["Created","Key",  "Name", "Updated"]
        objectTypeWithNoAtr.iconBean.id == iconBean.id
        objectTypeWithNoAtr.iconBean.name == iconBeanName
        objectTypeWithNoAtr.iconBean.objectSchemaId == null
        objectTypeWithNoAtr.objectSchemaId == schemaBean.id

        when: "Creating a new object with all attribute type"

        ArrayList<MutableObjectTypeAttributeBean>allAttributeTypes = createAllPossibleAttributes(schemaBean, objectTypeWithNoAtr)
        ObjectTypeBean objectTypeWithAllAtr = im.createObjectType("Object with all attributes", schemaBean.id, iconBean, "An object with all attributes",null,  allAttributeTypes)
        
        then:
        true


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


    /**
     * Creates All possible attribute types except yet to be implemented: Confluence, Version, Project
     * The attributes are created using the methods in SimplifiedAttributeBean, while using reflection.
     * Decent but not 100% care has been taken trying to test all different versions of the parameters users
     * @param objectSchema The schema that the attributes should belong to
     * @param preExistingObjectType A pre-existing object type that will be used for the getReferencedObject attribute type
     * @return An array of all the MutableObjectTypeAttributeBean
     */
    ArrayList<MutableObjectTypeAttributeBean>createAllPossibleAttributes(ObjectSchemaBean objectSchema, ObjectTypeBean preExistingObjectType) {


        ArrayList<Method> simpAtrBeanMethods =  SimplifiedAttributeBean.getDeclaredMethods().findAll {it.returnType == MutableObjectTypeAttributeBean}
        ArrayList<Method> simpAtrBeanMethodsTested = []
        ArrayList<MutableObjectTypeAttributeBean>attributeBeans = []

        simpAtrBeanMethods.each {method ->


            log.debug("Testing method:" + method.name + "(${method.parameterTypes.simpleName.join(", ")})")
            log.debug("\tThe method expects ${method.parameterCount} parameters:" + method.parameterTypes.simpleName.join(", "))

            MutableObjectTypeAttributeBean attributeBean
            String attributeName = "A ${method.name.replace("get", "")} Attribute with ${method.parameterCount} parameters"
            String attributeDescription = "This is the objectType description"

            if (method.parameterCount <= 2 && method.parameters.type.every {it == String}) {
                if (method.parameterCount == 1) {
                    attributeBean = method.invoke(null, attributeName) as MutableObjectTypeAttributeBean
                    attributeDescription = ""

                }else if (method.parameterCount == 2) {
                    attributeBean = method.invoke(null, attributeName, attributeDescription) as MutableObjectTypeAttributeBean

                }else {
                    throw new InputMismatchException("Expected only methods with 1 or 2 parameters, ${method.name} appears invalid")
                }
            }else if (method.name == "getText" && method.parameterTypes.toList() == [String, String, boolean]) {
                attributeDescription = attributeDescription+ ", attribute is set as label"
                attributeBean = method.invoke(null, attributeName, attributeDescription, true) as MutableObjectTypeAttributeBean
                assert attributeBean.label : "Error creating a Text attribute set as Label"
            } else if (method.name == "getReferencedObject" && method.parameterTypes.toList() == [String, int, int, String]) {
                attributeBean = method.invoke(null, attributeName, objectSchema.id, preExistingObjectType.id, "Link") as MutableObjectTypeAttributeBean
                attributeDescription = ""
                assert attributeBean.referenceObjectTypeId == preExistingObjectType.id : "Error creating a ReferencedObject attribute"
                assert attributeBean.referenceTypeBean.name == "Link" : "Error creating a ReferencedObject attribute"

            }else if (method.name == "getReferencedObject" && method.parameterTypes.toList() == [String, int, int, String, String]) {
                attributeBean = method.invoke(null, attributeName, objectSchema.id, preExistingObjectType.id, "Link", attributeDescription) as MutableObjectTypeAttributeBean
                assert attributeBean.referenceObjectTypeId == preExistingObjectType.id : "Error creating a ReferencedObject attribute"
                assert attributeBean.referenceTypeBean.name == "Link" : "Error creating a ReferencedObject attribute"

            }else if (method.name == "getSelect" && method.parameterTypes.toList() == [String, String, ArrayList]) {
                attributeBean = method.invoke(null, attributeName, attributeDescription, ["First Element", "Second Element"]) as MutableObjectTypeAttributeBean
                assert attributeBean.options == "First Element,Second Element"
            }else if (method.name == "getStatus" && method.parameterTypes.toList() == [String, Integer, ArrayList, String]) {
                attributeBean = method.invoke(null, attributeName, objectSchema.id, ["Active", "Closed"], attributeDescription) as MutableObjectTypeAttributeBean
                //This currently fails due to broken Insight API.
                //assert attributeBean.additionalValue == "Active,Closed"
            }else if (method.name == "getStatus" && method.parameterTypes.toList() == [String, Integer, ArrayList]) {
                attributeBean = method.invoke(null, attributeName, objectSchema.id, ["Active", "Closed"]) as MutableObjectTypeAttributeBean
                attributeDescription = ""
                //This currently fails due to broken Insight API.
                //assert attributeBean.additionalValue == "Active,Closed"
            }else if (method.name == "getStatus" && method.parameterTypes.toList() == [String, Integer]) {
                attributeBean = method.invoke(null, attributeName, objectSchema.id) as MutableObjectTypeAttributeBean
                attributeDescription = ""
                assert attributeBean.additionalValue == null : "Expected no additionalValue when creating a Status attribute without statuses"
            }else if (method.name == "getGroup" && method.parameterTypes.toList() == [String, String, boolean ]) {
                attributeBean = method.invoke(null, attributeName, attributeDescription, false) as MutableObjectTypeAttributeBean
                assert attributeBean.additionalValue == "HIDE_PROFILE" : "Expected additionalValue when creating a Group attribute with showOnProfile = false to be HIDE_PROFILE"
            }else if (method.name == "getUrl" && method.parameterTypes.toList() == [String, String, boolean ]) {
                attributeBean = method.invoke(null, attributeName, attributeDescription, true) as MutableObjectTypeAttributeBean
                assert attributeBean.additionalValue == "ENABLED" : "Expected additionalValue when creating a URL attribute with enablePing = true to be ENABLED"
            }else if (method.name == "getUser" && method.parameterTypes.toList() == [String, String, ArrayList, boolean ]) {
                attributeBean = method.invoke(null, attributeName, attributeDescription, ["jira-administrators"], false) as MutableObjectTypeAttributeBean
                assert attributeBean.typeValue == "jira-administrators" : "Expected typeValue of a user to be the supplied groups"
                assert attributeBean.additionalValue == "HIDE_PROFILE" : "Expected additionalValue when creating a User attribute with showOnProfile = false to be HIDE_PROFILE"
            }else if (method.name == "getUser" && method.parameterTypes.toList() == [String, String, ArrayList ]) {
                attributeBean = method.invoke(null, attributeName, attributeDescription, ["jira-administrators"]) as MutableObjectTypeAttributeBean
                assert attributeBean.typeValue == "jira-administrators" : "Expected typeValue of a user to be the supplied groups"
                assert attributeBean.additionalValue == "SHOW_PROFILE" : "Expected additionalValue when creating a User attribute with showOnProfile left to its default to be SHOW_PROFILE"
            }

            assert attributeBean.name == attributeName : "The new attributeBean was given an unexpected name"
            assert attributeBean.description == attributeDescription: "The new attributeBean was given an unexpected descriptions"


            assert attributeBean.getClass() == MutableObjectTypeAttributeBean : method.name + " yielded an unexpected class: " + attributeBean.getClass()
            log.debug("\tThe method returned the expected class and with the correct name and description")

            attributeBeans.add(attributeBean)
            simpAtrBeanMethodsTested.add(method)

        }

        ArrayList<Method>untestedMethods = simpAtrBeanMethods.findAll {!simpAtrBeanMethodsTested.contains(it)}
        if (untestedMethods) {
            log.warn("Missed testing these methods:")
            untestedMethods.each {log.warn(it.name + " " + it.parameterTypes.simpleName.join(", ") )}
        }
        assert untestedMethods.empty : "Error untested methods in SimplifiedAttributeBean found"
        assert attributeBeans.size() == simpAtrBeanMethods.size(): "Error untested methods in SimplifiedAttributeBean found"


        ObjectTypeAttributeBean.DefaultType.values().each {type->
            assert attributeBeans.defaultType.contains(type) : "Default Attribute Type $type was not tested"
        }
        ObjectTypeAttributeBean.Type.values().each {type ->
            assert attributeBeans.type.contains(type) || type in [ObjectTypeAttributeBean.Type.CONFLUENCE, ObjectTypeAttributeBean.Type.VERSION, ObjectTypeAttributeBean.Type.PROJECT] : "Attribute Type $type was not tested"
        }

        return attributeBeans

    }


}