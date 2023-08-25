package com.eficode.atlassian.insightmanager.tests.specs

import com.atlassian.jira.user.ApplicationUser

/**
 * Intended to test CRUD of Objects/attributes
 * Much work remains
 */

import com.eficode.atlassian.insightmanager.InsightManagerForScriptrunner
import com.eficode.atlassian.insightmanager.SimplifiedAttributeFactory
import com.eficode.atlassian.insightmanager.tests.utils.SpecHelper
import com.riadalabs.jira.plugins.insight.services.model.IconBean
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectSchemaBean
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.Type as AttributeType
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.DefaultType as AttributeDefaultType
import com.riadalabs.jira.plugins.insight.services.model.ObjectSchemaBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeBean
import com.riadalabs.jira.plugins.insight.services.model.RoleBean
import com.riadalabs.jira.plugins.insight.services.model.RoleType
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

        im.objectSchemaFacade.findObjectSchemaBeans().findAll { it.name.capitalize() == "SPOC" || it.objectSchemaKey.capitalize() == "SPOC" }.each {
            log.info("Deleting pre-existing SPOC schema:" + it.name + "(id: ${it.id})")
            im.deleteObjectSchema(it.id)
        }


        MutableObjectSchemaBean.metaClass.toString = { MutableObjectSchemaBean bean ->
            return "${bean.name}, Key: ${bean.objectSchemaKey}, Id:" + bean.id
        }

        ObjectTypeBean.metaClass.toString = { ObjectTypeBean bean ->
            return "${bean.name}, Id:" + bean.id
        }

        MutableObjectTypeAttributeBean.metaClass.toString = { MutableObjectTypeAttributeBean bean ->
            return "${bean.name}, Id:" + bean.id
        }

        ObjectTypeAttributeBean.metaClass.toString = { ObjectTypeAttributeBean bean ->
            return "${bean.name}, Id:" + bean.id
        }


    }


    def "Test ObjectSchema Role CRUD"() {

        setup:
        log.info("Testing CRUD of ObjectSchema and ObjectType Role CRUD")
        String schemaName = "Testing Roles"
        String schemaKey = "ROLE"

        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.log.setLevel(Level.ALL)
        im.deleteObjectSchema(schemaName, schemaKey) ?: log.info("\tRemoved a pre-existing schema")
        ObjectSchemaBean schemaBean
        schemaBean = im.createObjectSchema(schemaName, schemaKey)
        IconBean iconBean = im.getIconBean("Gear", schemaBean.id)
        ObjectTypeBean objectTypeWithManager = im.createObjectType("ObjectType with Manager", schemaBean.id, iconBean, "This objectType has a manager")
        log.info("Created ObjectType:" + objectTypeWithManager)
        //schemaBean = im.getObjectSchema(schemaKey)


        ArrayList<ApplicationUser> spocUsers = SpecHelper.createSpocUsers(3)
        log.info("Created/Reusing ${spocUsers.size()} Spoc users")
        spocUsers.each { log.info("\t" + it.toString()) }


        when: "Checking the Schema Roles and RoleActors for a new schema"
        log.info("\tChecking the default Roles and RoleActors for the new schema")
        ArrayList<RoleBean> schemaRoleBeans = im.getObjectSchemaRoleBeans(schemaBean.id)

        then: "They should have the excepted defaults"
        assert schemaRoleBeans.size() == 3: "A new schema did not have the expected RoleBeans"
        assert schemaRoleBeans.roleActorBeans.flatten().size() == 6: "A new schema did not have the expected RoleActors"
        log.info("\t\tThe new schema has the expected 3 roles and 6 RoleActors")

        when: "Clearing all schema role actors"
        log.info("\tClearing all schema role actors")
        schemaRoleBeans.each {
            log.debug("\t" * 2 + "Clearing " + it.type)
            im.clearRoleActors(it.id)
        }
        schemaRoleBeans = im.getObjectSchemaRoleBeans(schemaBean.id)


        then: "The schema should have no Role Actors"
        assert schemaRoleBeans.roleActorBeans.flatten().size() == 0: "Error clearing all of the Schema Role Actors"
        log.info("\t" * 2 + "Schema RoleActors where successfully cleared")


        when: "Adding two group to all roles"
        log.info("Adding \"jira-servicedesk-users\" and \"jira-administrators\" to all roles")
        RoleType.values().findAll { it.name().startsWith("SCHEMA_") }.each { roleType ->
            log.info("\tUpdating $roleType")
            im.addSchemaRoleActors(schemaBean.id, roleType, ["jira-servicedesk-users", "jira-administrators"], [])
        }
        schemaRoleBeans = im.getObjectSchemaRoleBeans(schemaBean.id)
        log.info("\tFinished adding groups to roles")


        then: "Checking that the API returns two groups and no users per role"
        log.info("Checking that the API returns two groups and no users per role")
        schemaRoleBeans.each {

            ArrayList<String> groupActors = it.roleActorBeans.findAll { it.type == "atlassian-group-role-actor" }.typeParameter
            log.debug("\t" + it.type + " has group actors:")
            groupActors.each {
                log.info("\t" * 2 + it)
            }
            assert groupActors == ["jira-servicedesk-users", "jira-administrators"]: "The ${it.type} Role in schema ${schemaBean.objectSchemaKey} has unexpected group actors:" + groupActors.join(", ")
            assert it.roleActorBeans.findAll { it.type == "atlassian-user-role-actor" }.size() == 0: "The ${it.type} Role in schema ${schemaBean.objectSchemaKey} has unexpected users actors"

        }
        log.info("\tThe API confirms the groups was added to the correct Schema and Roles")


        when: "Adding Spoc users to all roles"
        log.info("Adding users ${spocUsers.key.join(", ")} to all roles")
        RoleType.values().findAll { it.name().startsWith("SCHEMA_") }.each { roleType ->
            log.info("\tUpdating $roleType")
            im.addSchemaRoleActors(schemaBean.id, roleType, [], spocUsers.key)
        }
        schemaRoleBeans = im.getObjectSchemaRoleBeans(schemaBean.id)
        log.info("\tFinished adding users to roles")

        then:
        "Checking that the API returns two groups and ${spocUsers.size()} users per role"
        log.info("Checking that the API returns two groups and ${spocUsers.size()} users per role")
        schemaRoleBeans.each {

            ArrayList<String> userActors = it.roleActorBeans.findAll { it.type == "atlassian-user-role-actor" }.typeParameter
            log.debug("\t" + it.type + " has user actors:")
            userActors.each {
                log.info("\t" * 2 + it)
            }
            assert userActors.sort() == spocUsers.key.sort(): "The ${it.type} Role in schema ${schemaBean.objectSchemaKey} has unexpected user actors:" + userActors.join(", ")
            assert it.roleActorBeans.findAll { it.type == "atlassian-group-role-actor" }.size() == 2: "The ${it.type} Role in schema ${schemaBean.objectSchemaKey} has unexpected group actors"

        }
        log.info("\tThe API confirms the users was added to the correct Schema and Roles and that existing group actors where unaffected by the change")


        when: "Adding actors to a role that already has actors"

        log.info("Testing adding actors to a role that already has actors")
        schemaRoleBeans = im.getObjectSchemaRoleBeans(schemaBean.id)
        schemaRoleBeans.each { im.clearRoleActors(it.id) }
        schemaRoleBeans = im.getObjectSchemaRoleBeans(schemaBean.id)
        assert schemaRoleBeans.roleActorBeans.flatten().isEmpty()
        log.debug("\tCleared all roles in Schema $schemaKey")
        log.debug("\tSetting one user and one group to each role")
        schemaRoleBeans.each { roleBean ->
            im.setRoleActors(
                    [
                            "atlassian-user-role-actor" : [spocUsers.first().key],
                            "atlassian-group-role-actor": ["jira-servicedesk-users"]
                    ] as Map,
                    roleBean.id
            )
        }
        schemaRoleBeans = im.getObjectSchemaRoleBeans(schemaBean.id)
        schemaRoleBeans.each {roleBean ->
            log.debug("\t"*2 + "${roleBean.type.name()} actors set to: " + roleBean.roleActorBeans.typeParameter.flatten().join(","))
            assert roleBean.roleActorBeans.typeParameter.flatten().size() == 2 : roleBean.type.name() + " was not given the expected actors"
        }
        log.info("\tSchema now has one group and one user per role, making sure adding new users/groups leaves the original actors")
        RoleType.values().findAll {it.name().startsWith("SCHEMA_")}.each {schemaRole ->

            log.debug("\t"*2 + "Adding jira-administrators and ${spocUsers.last().key} to ${schemaRole.name()}")
            im.addSchemaRoleActors(schemaBean.id, schemaRole, ["jira-administrators"], [spocUsers.last().key])
        }
        schemaRoleBeans = im.getObjectSchemaRoleBeans(schemaBean.id)

        then: "The pre-existing actors should still have their roles"

        schemaRoleBeans.each {roleBean ->
            log.debug("\t"*2 + "${roleBean.type.name()} actors set to: " + roleBean.roleActorBeans.typeParameter.flatten().sort().join(","))
            assert roleBean.roleActorBeans.typeParameter.flatten().sort() == ["jira-servicedesk-users","jira-administrators", spocUsers.first().key, spocUsers.last().key ].sort() : roleBean.type.name() + " was not given the expected actors"
        }
        log.info("\tConfirmed that addSchemaRoleActors() left any pre-existing actors alone")


        // im.addSchemaRoleActorGroups(schemaBean.id, RoleType.SCHEMA_USER, ["jira-servicedesk-users", "jira-administrators"])

    }

    def "Test Creation of ObjectTypes with attributes"() {
        setup:
        log.info("Testing creation of schemas")
        String schemaName = "Testing ObjectTypeCreation"
        String schemaKey = "OBJECT"
        String iconBeanName = "Gear"

        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.log.setLevel(Level.ALL)
        im.deleteObjectSchema(schemaName, schemaKey) ?: log.info("\tRemoved a pre-existing schema")
        ObjectSchemaBean schemaBean = im.createObjectSchema(schemaName, schemaKey)

        log.info("\tCreated schema:" + schemaBean.objectSchemaKey)

        IconBean iconBean = im.getIconBean(iconBeanName, schemaBean.id)
        assert iconBean: "Error finding IconBean: $iconBeanName"
        log.info("\tRetrieved IconBean:" + iconBean.id)

        when: "Creating an object type with no initial attributes"

        log.info("\tCreating an ObjectType with no custom attributes")
        ObjectTypeBean objectTypeWithNoAtr = im.createObjectType("Object with no attributes", schemaBean.id, iconBean, "An object with no attributes")
        log.info("\t\tCreated ObjectType:" + objectTypeWithNoAtr.name + " (${objectTypeWithNoAtr.id})")
        ArrayList<ObjectTypeAttributeBean> defaultAttributes = im.objectTypeAttributeFacade.findObjectTypeAttributeBeans(objectTypeWithNoAtr.id)
        log.info("\t" * 2 + "ObjectType has these attributes:" + defaultAttributes.collect { it.name + " (Id:${it.id})" }.join(", "))
        then: "An object with only default attributes should be created in the expected Schema with the correct icon"
        defaultAttributes.name.sort() == ["Created", "Key", "Name", "Updated"]
        objectTypeWithNoAtr.iconBean.id == iconBean.id
        objectTypeWithNoAtr.iconBean.name == iconBeanName
        objectTypeWithNoAtr.iconBean.objectSchemaId == null
        objectTypeWithNoAtr.objectSchemaId == schemaBean.id

        when: "Creating a new object with all attribute type"

        ArrayList<MutableObjectTypeAttributeBean> allAttributeTypes = createAllPossibleAttributes(schemaBean, objectTypeWithNoAtr)
        ObjectTypeBean objectTypeWithAllAtr = im.createObjectType("Object with all attributes", schemaBean.id, iconBean, "An object with all attributes", null, allAttributeTypes)

        then: "Perform a light check, verifying all types where created, looking closer at some of the complex types"
        assert im.objectTypeAttributeFacade.loadObjectTypeAttribute(objectTypeWithAllAtr.id, "A Status Attribute with 3 parameters").multiTypeValues == im.configureFacade.findAllStatusTypeBeans(schemaBean.id).findAll { it.name in ["Active", "Closed"] }.collect { it.id.toString() }: "Available statuses wasn't set properly"
        assert im.objectTypeAttributeFacade.loadObjectTypeAttribute(objectTypeWithAllAtr.id, "A Status Attribute with 4 parameters").multiTypeValues == im.configureFacade.findAllStatusTypeBeans(schemaBean.id).findAll { it.name in ["Active", "Closed"] }.collect { it.id.toString() }: "Available statuses wasn't set properly"
        assert im.objectTypeAttributeFacade.loadObjectTypeAttribute(objectTypeWithAllAtr.id, "A Select Attribute with 3 parameters").options == "First Element,Second Element": "Available options wasn't set properly"
        assert im.objectTypeAttributeFacade.loadObjectTypeAttribute(objectTypeWithAllAtr.id, "A ReferencedObject Attribute with 4 parameters").referenceObjectTypeId == objectTypeWithNoAtr.id: "Referenced attribute type is not referencing the expected objectType"
        assert im.objectTypeAttributeFacade.loadObjectTypeAttribute(objectTypeWithAllAtr.id, "A ReferencedObject Attribute with 4 parameters").referenceTypeBean.name == "Link": "Referenced attribute type is not using the expected reference typ"
        AttributeDefaultType.values().every { defaultType ->
            im.objectTypeAttributeFacade.findObjectTypeAttributeBeans(objectTypeWithAllAtr.id).findAll { it.defaultType == defaultType }.size() >= 2
        }
        AttributeType.values().every { attributeType ->

            if (attributeType in [AttributeType.CONFLUENCE, AttributeType.VERSION, AttributeType.PROJECT]) {
                log.warn("Skipping testing of attribute type:" + attributeType.name())
                return true
            }
            im.objectTypeAttributeFacade.findObjectTypeAttributeBeans(objectTypeWithAllAtr.id).findAll { it.type == attributeType }.size() >= 2

        }


    }


    def "Verify default cardinality and overwrites is respected"() {

        setup:
        log.info("Verifying Cardinality is respected")
        log.info("\tWhen adding more values then cardinality allows, the value should be overwritten")
        String schemaName = "Testing Cardinality"
        String schemaKey = "CARD"
        String iconBeanName = "Gear"


        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.log.setLevel(Level.ALL)
        im.deleteObjectSchema(schemaName, schemaKey) ?: log.info("\tRemoved a pre-existing schema")
        ObjectSchemaBean schemaBean = im.createObjectSchema(schemaName, schemaKey)
        IconBean iconBean = im.getIconBean(iconBeanName, schemaBean.id)


        when: "Creating a new ObjectType with Status, Date and Date Time attributes"
        log.info("\tCreated schema:" + schemaBean.toString())

        MutableObjectTypeAttributeBean statusAttributeBean = SimplifiedAttributeFactory.getStatus("A Status Attribute", schemaBean.id, ["Active", "Closed", "Running", "Stopped"], "Max cardinality set to 1")
        MutableObjectTypeAttributeBean dateAttributeBean = SimplifiedAttributeFactory.getDate("A Date Attribute")
        MutableObjectTypeAttributeBean dateTimeAttributeBean = SimplifiedAttributeFactory.getDate_time("A DateTime Attribute")

        ObjectTypeBean objectTypeBean = im.createObjectType("Cardinal ObjectType", schemaBean.id, iconBean, "", null, [statusAttributeBean, dateAttributeBean, dateTimeAttributeBean])
        log.info("\tCreated objectType:" + objectTypeBean.toString())
        log.info("\tWith attributes:")
        log.info("\t" * 2 + statusAttributeBean.toString())
        log.info("\t" * 2 + dateAttributeBean.toString())
        log.info("\t" * 2 + dateTimeAttributeBean.toString())
        log.info("\t" * 2 + dateTimeAttributeBean.getClass().simpleName)


        then: "The objectType should be created accordingly"
        assert im.objectTypeAttributeFacade.findObjectTypeAttributeBeans(objectTypeBean.id).name.containsAll([statusAttributeBean.name, dateAttributeBean.name, dateAttributeBean.name]): "All attributes wherent created"
        assert im.objectTypeAttributeFacade.loadObjectTypeAttribute(objectTypeBean.id, statusAttributeBean.name).maximumCardinality == 1: "Status attribute did not default to Cardinality Max 1"
        assert im.objectTypeAttributeFacade.loadObjectTypeAttribute(objectTypeBean.id, dateAttributeBean.name).maximumCardinality == 1: "Date attribute did not default to Cardinality Max 1"
        assert im.objectTypeAttributeFacade.loadObjectTypeAttribute(objectTypeBean.id, dateTimeAttributeBean.name).maximumCardinality == 1: "DateTime attribute did not default to Cardinality Max 1"

        when: "Creating an object of the new type with sensible values"
        Map<String, Object> initialValuesSet = [Name: "A new object", (statusAttributeBean.name): "Active", (dateAttributeBean.name): new Date(), (dateTimeAttributeBean.name): new Date()] as Map<String, Object>
        ObjectBean objectBean = im.createObject(objectTypeBean.id, initialValuesSet)
        log.info("\tCreated a new object of the new type:" + objectBean.toString())
        log.info("\t" * 2 + "Created with the initial attribute values:")
        initialValuesSet.each { log.info("\t" * 3 + it) }

        Map<String, ArrayList> initialValuesReturned = im.getObjectAttributeValues(objectBean)
        log.info("\t" * 2 + "When queried, API returned attribute values:")
        initialValuesReturned.each { log.info("\t" * 3 + it) }

        then: "All Values returned by API should be arrays with size == 1"
        initialValuesReturned.every { it.value.size() == 1 }
        log.info("\t" * 2 + "The returned attribute values appear valid")

        when: "Setting new values of the expected type, individually per attribute"
        sleep(1100)
        im.updateObjectAttribute(objectBean, statusAttributeBean.name, "Closed")
        im.updateObjectAttribute(objectBean, dateAttributeBean.name, new Date())
        im.updateObjectAttribute(objectBean, dateTimeAttributeBean.name, new Date())
        Map<String, ArrayList> updatedValuesReturned = im.getObjectAttributeValues(objectBean)


        then: "All attributes should be updated, and still only have one value"
        updatedValuesReturned.each { it.value.size() == 1 }
        initialValuesReturned[statusAttributeBean.name] != updatedValuesReturned[statusAttributeBean.name]
        initialValuesReturned[dateAttributeBean.name] != updatedValuesReturned[dateAttributeBean.name]
        initialValuesReturned[dateTimeAttributeBean.name] != updatedValuesReturned[dateTimeAttributeBean.name]


        when: "Setting new values using arrays, individually per attribute"
        sleep(1100)
        im.updateObjectAttribute(objectBean, statusAttributeBean.name, ["Active"])
        im.updateObjectAttribute(objectBean, dateAttributeBean.name, [new Date()])
        im.updateObjectAttribute(objectBean, dateTimeAttributeBean.name, [new Date()])
        Map<String, ArrayList> updatedValuesReturned2 = im.getObjectAttributeValues(objectBean)

        then: "All attributes should be updated, and still only have one value"
        updatedValuesReturned2.each { it.value.size() == 1 }
        updatedValuesReturned[statusAttributeBean.name] != updatedValuesReturned2[statusAttributeBean.name]
        updatedValuesReturned[dateAttributeBean.name] != updatedValuesReturned2[dateAttributeBean.name]
        updatedValuesReturned[dateTimeAttributeBean.name] != updatedValuesReturned2[dateTimeAttributeBean.name]


        when: "Setting all the attributes at once"
        sleep(1100)
        im.updateObjectAttributes(objectBean, [(statusAttributeBean.name): "Closed", (dateAttributeBean.name): new Date(), (dateTimeAttributeBean.name): new Date()])
        Map<String, ArrayList> updatedValuesReturned3 = im.getObjectAttributeValues(objectBean)

        then: "All attributes should be updated, and still only have one value"
        updatedValuesReturned3.each { it.value.size() == 1 }
        updatedValuesReturned2[statusAttributeBean.name] != updatedValuesReturned3[statusAttributeBean.name]
        updatedValuesReturned2[dateAttributeBean.name] != updatedValuesReturned3[dateAttributeBean.name]
        updatedValuesReturned2[dateTimeAttributeBean.name] != updatedValuesReturned3[dateTimeAttributeBean.name]

        when: "Setting all the attributes at once, with multiple values per attribute"
        im.updateObjectAttributes(objectBean, [(statusAttributeBean.name): ["Active", "Closed"], (dateAttributeBean.name): [new Date(), new Date(100)], (dateTimeAttributeBean.name): [new Date(), new Date(200)]])

        then: "All attributes should still only have one value"
        im.getObjectAttributeValues(objectBean).each { it.value.size() == 1 }

        when: "Setting new values using arrays with multiple values, individually per attribute"
        sleep(1100)
        im.updateObjectAttribute(objectBean, statusAttributeBean.name, ["Closed", "Active"])
        im.updateObjectAttribute(objectBean, dateAttributeBean.name, [new Date(200), new Date()])
        im.updateObjectAttribute(objectBean, dateTimeAttributeBean.name, [new Date(300), new Date()])

        then: "All attributes should still only have one value"
        im.getObjectAttributeValues(objectBean).each { it.value.size() == 1 }

    }


    def "Test creation of schemas"() {

        setup:
        log.info("Testing creation of schemas")

        InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        im.log.setLevel(Level.ALL)


        when:
        log.info("\tCreating a schema using default parameters")
        ObjectSchemaBean schemaBean = im.createObjectSchema("SPOC", "SPOC")
        log.info("\t\tCreated schema with ID:" + schemaBean.id)

        then:
        schemaBean.name == "SPOC"
        schemaBean.objectSchemaKey == "SPOC"
        !schemaBean.objectSchemaPropertyBean.serviceDescCustomersEnabled
        !schemaBean.objectSchemaPropertyBean.allowOtherObjectSchema
        log.info("\t\tSuccessfully created schema with default parameters")
        assert im.deleteObjectSchema(schemaBean.id): "Error deleting object schema"


        when:
        log.info("\tCreating a schema using non default parameters")
        schemaBean = im.createObjectSchema("spoc", "spoc", "A description", true, true)
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
        assert im.deleteObjectSchema(schemaBean.id): "IM did not return true when expected"
        assert im.objectSchemaFacade.loadObjectSchema(schemaBean.id): "Schema can no longer be found"
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
     * The attributes are created using the methods in SimplifiedAttributeFactory, while using reflection.
     * Decent but not 100% care has been taken trying to test all different versions of the parameters users
     * @param objectSchema The schema that the attributes should belong to
     * @param preExistingObjectType A pre-existing object type that will be used for the getReferencedObject attribute type
     * @return An array of all the MutableObjectTypeAttributeBean
     */
    ArrayList<MutableObjectTypeAttributeBean> createAllPossibleAttributes(ObjectSchemaBean objectSchema, ObjectTypeBean preExistingObjectType) {


        ArrayList<Method> simpAtrBeanMethods = SimplifiedAttributeFactory.getDeclaredMethods().findAll { it.returnType == MutableObjectTypeAttributeBean }
        ArrayList<Method> simpAtrBeanMethodsTested = []
        ArrayList<MutableObjectTypeAttributeBean> attributeBeans = []

        simpAtrBeanMethods.each { method ->


            log.debug("Testing method:" + method.name + "(${method.parameterTypes.simpleName.join(", ")})")
            log.debug("\tThe method expects ${method.parameterCount} parameters:" + method.parameterTypes.simpleName.join(", "))

            MutableObjectTypeAttributeBean attributeBean
            String attributeName = "A ${method.name.replace("get", "")} Attribute with ${method.parameterCount} parameters"
            String attributeDescription = "This is the objectType description"

            if (method.parameterCount <= 2 && method.parameters.type.every { it == String }) {
                if (method.parameterCount == 1) {
                    attributeBean = method.invoke(null, attributeName) as MutableObjectTypeAttributeBean
                    attributeDescription = ""

                } else if (method.parameterCount == 2) {
                    attributeBean = method.invoke(null, attributeName, attributeDescription) as MutableObjectTypeAttributeBean

                } else {
                    throw new InputMismatchException("Expected only methods with 1 or 2 parameters, ${method.name} appears invalid")
                }
            } else if (method.name == "getText" && method.parameterTypes.toList() == [String, String, boolean]) {
                attributeDescription = attributeDescription + ", attribute is set as label"
                attributeBean = method.invoke(null, attributeName, attributeDescription, true) as MutableObjectTypeAttributeBean
                assert attributeBean.label: "Error creating a Text attribute set as Label"
            } else if (method.name == "getReferencedObject" && method.parameterTypes.toList() == [String, int, int, String]) {
                attributeBean = method.invoke(null, attributeName, objectSchema.id, preExistingObjectType.id, "Link") as MutableObjectTypeAttributeBean
                attributeDescription = ""
                assert attributeBean.referenceObjectTypeId == preExistingObjectType.id: "Error creating a ReferencedObject attribute"
                assert attributeBean.referenceTypeBean.name == "Link": "Error creating a ReferencedObject attribute"

            } else if (method.name == "getReferencedObject" && method.parameterTypes.toList() == [String, int, int, String, String]) {
                attributeBean = method.invoke(null, attributeName, objectSchema.id, preExistingObjectType.id, "Link", attributeDescription) as MutableObjectTypeAttributeBean
                assert attributeBean.referenceObjectTypeId == preExistingObjectType.id: "Error creating a ReferencedObject attribute"
                assert attributeBean.referenceTypeBean.name == "Link": "Error creating a ReferencedObject attribute"

            } else if (method.name == "getSelect" && method.parameterTypes.toList() == [String, String, ArrayList]) {
                attributeBean = method.invoke(null, attributeName, attributeDescription, ["First Element", "Second Element"]) as MutableObjectTypeAttributeBean
                assert attributeBean.options == "First Element,Second Element"
            } else if (method.name == "getStatus" && method.parameterTypes.toList() == [String, Integer, ArrayList, String]) {
                attributeBean = method.invoke(null, attributeName, objectSchema.id, ["Active", "Closed"], attributeDescription) as MutableObjectTypeAttributeBean
                //This currently fails due to broken Insight API.
                //assert attributeBean.additionalValue == "Active,Closed"
            } else if (method.name == "getStatus" && method.parameterTypes.toList() == [String, Integer, ArrayList]) {
                attributeBean = method.invoke(null, attributeName, objectSchema.id, ["Active", "Closed"]) as MutableObjectTypeAttributeBean
                attributeDescription = ""
                //This currently fails due to broken Insight API.
                //assert attributeBean.additionalValue == "Active,Closed"
            } else if (method.name == "getStatus" && method.parameterTypes.toList() == [String, Integer]) {
                attributeBean = method.invoke(null, attributeName, objectSchema.id) as MutableObjectTypeAttributeBean
                attributeDescription = ""
                assert attributeBean.additionalValue == null: "Expected no additionalValue when creating a Status attribute without statuses"
            } else if (method.name == "getGroup" && method.parameterTypes.toList() == [String, String, boolean]) {
                attributeBean = method.invoke(null, attributeName, attributeDescription, false) as MutableObjectTypeAttributeBean
                assert attributeBean.additionalValue == "HIDE_PROFILE": "Expected additionalValue when creating a Group attribute with showOnProfile = false to be HIDE_PROFILE"
            } else if (method.name == "getUrl" && method.parameterTypes.toList() == [String, String, boolean]) {
                attributeBean = method.invoke(null, attributeName, attributeDescription, true) as MutableObjectTypeAttributeBean
                assert attributeBean.additionalValue == "ENABLED": "Expected additionalValue when creating a URL attribute with enablePing = true to be ENABLED"
            } else if (method.name == "getUser" && method.parameterTypes.toList() == [String, String, ArrayList, boolean]) {
                attributeBean = method.invoke(null, attributeName, attributeDescription, ["jira-administrators"], false) as MutableObjectTypeAttributeBean
                assert attributeBean.typeValue == "jira-administrators": "Expected typeValue of a user to be the supplied groups"
                assert attributeBean.additionalValue == "HIDE_PROFILE": "Expected additionalValue when creating a User attribute with showOnProfile = false to be HIDE_PROFILE"
            } else if (method.name == "getUser" && method.parameterTypes.toList() == [String, String, ArrayList]) {
                attributeBean = method.invoke(null, attributeName, attributeDescription, ["jira-administrators"]) as MutableObjectTypeAttributeBean
                assert attributeBean.typeValue == "jira-administrators": "Expected typeValue of a user to be the supplied groups"
                assert attributeBean.additionalValue == "SHOW_PROFILE": "Expected additionalValue when creating a User attribute with showOnProfile left to its default to be SHOW_PROFILE"
            }

            assert attributeBean.name == attributeName: "The new attributeBean was given an unexpected name"
            assert attributeBean.description == attributeDescription: "The new attributeBean was given an unexpected descriptions"


            assert attributeBean.getClass() == MutableObjectTypeAttributeBean: method.name + " yielded an unexpected class: " + attributeBean.getClass()
            log.debug("\tThe method returned the expected class and with the correct name and description")

            attributeBeans.add(attributeBean)
            simpAtrBeanMethodsTested.add(method)

        }

        ArrayList<Method> untestedMethods = simpAtrBeanMethods.findAll { !simpAtrBeanMethodsTested.contains(it) }
        if (untestedMethods) {
            log.warn("Missed testing these methods:")
            untestedMethods.each { log.warn(it.name + " " + it.parameterTypes.simpleName.join(", ")) }
        }
        assert untestedMethods.empty: "Error untested methods in SimplifiedAttributeFactory found"
        assert attributeBeans.size() == simpAtrBeanMethods.size(): "Error untested methods in SimplifiedAttributeFactory found"


        ObjectTypeAttributeBean.DefaultType.values().each { type ->
            assert attributeBeans.defaultType.contains(type): "Default Attribute Type $type was not tested"
        }
        ObjectTypeAttributeBean.Type.values().each { type ->
            assert attributeBeans.type.contains(type) || type in [ObjectTypeAttributeBean.Type.CONFLUENCE, ObjectTypeAttributeBean.Type.VERSION, ObjectTypeAttributeBean.Type.PROJECT]: "Attribute Type $type was not tested"
        }

        return attributeBeans

    }


}