package com.eficode.atlassian.insightmanager

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.properties.APKeys
import com.atlassian.jira.config.util.JiraHome
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.ExecutingHttpRequest
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.ConfigureFacadeImpl
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.IQLFacadeImpl
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.ImportSourceConfigurationFacadeImpl
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.ObjectFacadeImpl
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.ObjectSchemaFacadeImpl
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.ObjectTicketFacadeImpl
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.ObjectTypeAttributeFacadeImpl
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.ObjectTypeFacadeImpl
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.ProgressFacadeImpl
import com.riadalabs.jira.plugins.insight.common.exception.InsightException
import com.riadalabs.jira.plugins.insight.common.exception.RuntimeInsightException
import com.riadalabs.jira.plugins.insight.services.events.EventDispatchOption
import com.riadalabs.jira.plugins.insight.services.imports.model.ImportSource
import com.riadalabs.jira.plugins.insight.services.model.AttachmentBean
import com.riadalabs.jira.plugins.insight.services.model.CommentBean
import com.riadalabs.jira.plugins.insight.services.model.IconBean
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectBean
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectSchemaBean
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectTypeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectAttributeValueBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectHistoryBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectSchemaBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectSchemaPropertyBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeBean
import com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactory
import com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactoryImpl
import com.riadalabs.jira.plugins.insight.services.progress.model.Progress
import com.riadalabs.jira.plugins.insight.services.progress.model.ProgressId
import io.riada.insight.api.graphql.resolvers.objectschema.ObjectSchema
import org.apache.log4j.Logger
import org.joda.time.DateTime

import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DateFormat
import java.time.LocalDateTime


//TODO test clearing of object attributes with [] or null when using updateObjectAttribute and updateObjectAttributes

/**
 * 2.0
 *  Breaking changes:
 *      * Previously several exceptions where caught silently, they are now allowed to propagate and will have to be handled by the
 *        executing script
 *
 *      * updateObjectAttributes and updateObjectAttribute will now clear attribute values if value is [] or null
 *
 *      * The "autoEscalate" functionality has been removed and will have to be handled by the executing script
 *
 */

@WithPlugin("com.riadalabs.jira.plugins.insight")
class InsightManagerForScriptrunner {

    Logger log
    Class objectSchemaFacadeClass
    ObjectSchemaFacadeImpl objectSchemaFacade
    Class objectFacadeClass
    ObjectFacadeImpl objectFacade
    Class iqlFacadeClass
    IQLFacadeImpl iqlFacade
    Class objectTypeFacadeClass
    ObjectTypeFacadeImpl objectTypeFacade
    Class objectTypeAttributeFacadeClass
    ObjectTypeAttributeFacadeImpl objectTypeAttributeFacade
    Class objectAttributeBeanFactoryClass
    ObjectAttributeBeanFactory objectAttributeBeanFactory
    Class ImportSourceConfigurationFacadeClass
    ImportSourceConfigurationFacadeImpl importFacade
    Class ProgressFacadeClass
    ProgressFacadeImpl progressFacade
    Class objectTicketFacadeClass
    ObjectTicketFacadeImpl objectTicketFacade
    Class configureFacadeClass
    ConfigureFacadeImpl configureFacade
    public boolean readOnly
    boolean inJsdBehaviourContext //Set to true if currently executing as a Behaviour in JSD
    String baseUrl
    String jiraDataPath
    JiraAuthenticationContext authContext
    EventDispatchOption eventDispatchOption

    final static String jsdAvatarUrl = "/rest/insight_servicedesk/1.0/object/OBJECT_ID/avatar.png"
    final static String jsdObjectTypeIconUrl = "/rest/insight_servicedesk/1.0/objecttype/OBJECTTYPE_ID/icon.png"
    final static String jiraAvatarUrl = "/rest/insight/1.0/object/OBJECT_ID/avatar.png"
    final static String jiraObjectTypeIconUrl = "/rest/insight/1.0/objecttype/OBJECTTYPE_ID/icon.png"


    InsightManagerForScriptrunner() {


        //The facade classes
        objectSchemaFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectSchemaFacade")
        objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
        objectTypeFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade")
        objectTypeAttributeFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade")
        objectAttributeBeanFactoryClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactory")
        iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade")
        ImportSourceConfigurationFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ImportSourceConfigurationFacade")
        ProgressFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ProgressFacade")
        objectTicketFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTicketFacade")
        configureFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ConfigureFacade")

        //The facade instances
        objectSchemaFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectSchemaFacadeClass) as ObjectSchemaFacadeImpl
        objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass) as ObjectFacadeImpl
        objectTypeFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectTypeFacadeClass) as ObjectTypeFacadeImpl
        objectTypeAttributeFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectTypeAttributeFacadeClass) as ObjectTypeAttributeFacadeImpl
        objectAttributeBeanFactory = ComponentAccessor.getOSGiComponentInstanceOfType(objectAttributeBeanFactoryClass) as ObjectAttributeBeanFactoryImpl
        iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass) as IQLFacadeImpl
        importFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ImportSourceConfigurationFacadeClass) as ImportSourceConfigurationFacadeImpl
        progressFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ProgressFacadeClass) as ProgressFacadeImpl
        objectTicketFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectTicketFacadeClass) as ObjectTicketFacadeImpl
        configureFacade = ComponentAccessor.getOSGiComponentInstanceOfType(configureFacadeClass) as ConfigureFacadeImpl

        //Atlassian Managers
        authContext = ComponentAccessor.getJiraAuthenticationContext()


        //Static Paths
        baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL)
        jiraDataPath = ComponentAccessor.getComponentOfType(JiraHome).getDataDirectory().path


        log = Logger.getLogger(this.class.name)
        log.trace("InsightManager has been instantiated")


        eventDispatchOption = EventDispatchOption.DISPATCH

        inJsdBehaviourContext = new ExecutingHttpRequest()?.get()?.servletPath?.startsWith("/rest/scriptrunner/behaviours/latest/jsd/jsd")


    }


    /**
     * Get list of all ImportSource for a Schema
     * @param SchemaID Schema ID
     * @return ArrayList with ImportSources
     */
    ArrayList<ImportSource> listImports(int SchemaID) {


        log.debug("Getting import jobs for schema $SchemaID")
        ArrayList<ImportSource> imports = importFacade.findImportSourcesBySchema(SchemaID)

        log.trace("\tFound Imports:")
        imports.each {
            log.trace("\t\tName:\"" + it.name + "\" ID:" + it.id)
        }

        log.debug("Found " + imports.size() + " imports")


        return imports

    }

    /**
     * Starts an Insight Import and by default waits for it to finish
     * @param Import The import to be run, can be an int ImportSourceID or ImportSource object.
     * @param objectsToImport What objects to import, can be a mix of ObjectTypeIDs,ObjectBeans and ObjectTypeNames in a List. If left empty all objects will be imported
     * @param timeOut (millis) defaults to 10min, if set to 0millis wont wait for import to finish just start it and return immediately.
     * @return Progress object
     */
    Progress runImport(def Import, List objectsToImport = [], long timeOut = 600000) {

        log.info("Running import")

        int importSourceId
        ImportSource importSourceObject
        Progress progress = null


        try {
            if (Import instanceof Integer) {
                importSourceId = Import
                importSourceObject = importFacade.loadImportSource(importSourceId)

            } else if (Import instanceof ImportSource) {
                importSourceId = Import.id
                importSourceObject = Import

            } else {
                throw new InputMismatchException("runImport() does not support import of type:" + Import.getClass().toString())
            }

            log.trace("\tImportSource ID:" + importSourceId)
            log.trace("\timportSource Name:" + importSourceObject.name)


            if (objectsToImport.isEmpty()) {
                log.trace("\tImporting all Objects")
                if (readOnly) {
                    log.debug("\tCurrently in read only mode, wont run import. Will return previous import result")
                    progress = getImportProgress(importSourceId)
                } else {
                    progress = importFacade.startImportSource(importSourceId)
                }

            } else {

                log.trace("\tImport only objects of type:" + objectsToImport)
                Map<Object, Integer> AllOTS = [:]
                List<Integer> OTStoImport = []


                importSourceObject.importSourceOTS.each { OTS ->

                    AllOTS[OTS.objectTypeBean.name] = OTS.id
                    AllOTS[OTS.objectTypeBean.id] = OTS.id
                }


                objectsToImport.each { importObject ->
                    if (importObject instanceof ObjectBean || importObject instanceof MutableObjectBean) {


                        if (!AllOTS.containsKey(importObject.getId())) {
                            throw new RuntimeException("Object to import could not be found:" + importObject)
                        } else {
                            OTStoImport.add(AllOTS.get(importObject.getId()))
                        }

                    } else if (importObject instanceof Integer) {
                        if (!AllOTS.containsKey(importObject)) {
                            throw new RuntimeException("Object to import could not be found:" + importObject)
                        } else {
                            OTStoImport.add(AllOTS.get(importObject))
                        }
                    } else if (importObject instanceof String) {
                        if (!AllOTS.containsKey(importObject)) {
                            throw new RuntimeException("Object to import could not be found:" + importObject)
                        } else {
                            OTStoImport.add(AllOTS.get(importObject))
                        }
                    } else {
                        throw new RuntimeException("Object to import $importObject is of unkown type " + importObject.getClass())
                    }

                }


                log.trace("\t\tDetermined OTS IDs of Objects to import to be:" + OTStoImport)
                if (readOnly) {
                    log.debug("\tCurrently in read only mode, wont run import. Will return previous import result")
                    progress = getImportProgress(importSourceId)
                } else {
                    progress = importFacade.startImportSourceForSpecificOTs(importSourceId, OTStoImport)
                }


            }


            if (progress == null) {
                throw new RuntimeException("Failed to run import $importSourceId")
            }

            if (timeOut != 0) {
                long startOfImport = System.currentTimeMillis()

                while (System.currentTimeMillis() < startOfImport + timeOut) {

                    if (!progress.inProgress.toBoolean()) {
                        break
                    } else {
                        sleep(500)
                    }
                    log.trace("\tImport status:" + progress.status.toString() + " " + progress.progressInPercent + "%")

                }

                if ((startOfImport + timeOut) < System.currentTimeMillis()) {
                    throw new RuntimeException("Import timed out, it took longer than " + timeOut / 1000 + " seconds")
                } else {
                    log.info("\tImport took " + (System.currentTimeMillis() - startOfImport) / 1000 + " seconds to complete")
                }
            }


        } catch (ex) {
            log.error("\tError running import:" + ex.message)
            logRelevantStacktrace(ex.stackTrace)
            throw ex

        }


        if (timeOut == 0) {
            log.info("Import started")
        } else {
            log.info("Import finished")
        }

        return progress
    }

    /**
     * Get the progress object of an import (without triggering it)
     * @param Import can be an int ImportSourceID or ImportSource object.
     * @return progress object. progress.finished (true/false) will tell you if currently running.
     */
    Progress getImportProgress(def Import) {


        log.info("Getting Progress object for Import")

        ImportSource importSourceObject

        if (Import instanceof Integer) {

            importSourceObject = importFacade.loadImportSource(Import)

        } else if (Import instanceof ImportSource) {

            importSourceObject = Import

        } else {
            throw new NullPointerException(Import.toString() + " is not a valid import")
        }


        log.debug("\tDetermined import to be: ${importSourceObject.name} (${importSourceObject.id})")
        ProgressId progressId = ProgressId.create(importSourceObject.id.toString(), "imports")

        Progress progress = progressFacade.getProgress(progressId)
        log.debug("Got progress, ${progress.progressInPercent}%")

        return progress


    }

    /**
     * Should events be dispatched when you create/update/delete objects?
     * @param dispatch true or false, default is true
     */
    void dispatchEvents(boolean dispatch) {

        if (dispatch) {
            this.eventDispatchOption = EventDispatchOption.DISPATCH
        } else {
            this.eventDispatchOption = EventDispatchOption.DO_NOT_DISPATCH
        }

    }

    /**
     * Runs an IQL and returns matching objects
     * @param schemaId What scheme to run the IQL on
     * @param iql The IQL to be run
     * @return An array containing ObjectBeans
     */
    ArrayList<ObjectBean> iql(int schemaId, String iql) {

        log.debug("Running IQL \"" + iql + "\" on schema " + schemaId)
        ArrayList<ObjectBean> objects = []
        objects = iqlFacade.findObjectsByIQLAndSchema(schemaId, iql)

        log.trace("\t Objects:")
        objects.each {
            log.trace("\t\t" + it)
        }
        log.debug(objects.size() + " objects returned")

        return objects

    }

    /**
     * Clears value of attribute
     * @param object Can be object ID, Object Key or ObjectBean
     * @param attribute name (string) or id (int)
     */

    void clearObjectAttribute(def object, def attribute) {

        log.debug("Clearing attribute $attribute of object ${object}")


        ObjectBean objectBean = getObjectBean(object)


        if (readOnly) {
            log.debug("Object Attribute not updated as currently in read only mode")
        } else {

            try {
                ObjectAttributeBean attributeBean = objectFacade.loadObjectAttributeBean(objectBean.id, attribute)
                if (attributeBean != null) {

                    objectFacade.deleteObjectAttributeBean(attributeBean.id, this.eventDispatchOption)

                } else {
                    log.debug("\tAttribute is already empty")
                }

            } catch (Exception ex) {
                log.warn("Could not clear object (${object}) attribute (${attribute}) due to:" + ex.getMessage())
                log.debug("\tException" + ex)
                logRelevantStacktrace(ex.stackTrace)
                throw ex
            }
        }

    }

    /**
     * Creates a new object with Attribute Values
     * The label attribute must be populated (as an attribute)
     * @param schemeId id of the scheme where you want to create your object
     * @param objectTypeName Name of the object type you want to create
     * @param AttributeValues A map containing the Attributes and values to be set. The Attribute can be represented as an ID or a case insensitive string [AttributeID1: ValueA, attributename: ValueB]
     * @return The created ObjectBean
     */
    ObjectBean createObject(int schemeId, String objectTypeName, Map AttributeValues) {


        int objectTypeId = objectTypeFacade.findObjectTypeBeansFlat(schemeId).find { it.name == objectTypeName }.id


        return createObject(objectTypeId, AttributeValues)
    }

    /**
     * Creates a new object with Attribute Values
     * The label attribute must be populated (as an attribute)
     * @param ObjectTypeId ID of the object type you want to create
     * @param AttributeValues A map containing the Attributes and values to be set. The Attribute can be represented as an ID or a case insensitive string [AttributeID1: ValueA, attributename: ValueB]
     * @return The created ObjectBean
     */
    ObjectBean createObject(Integer ObjectTypeId, Map AttributeValues) {

        log.debug("Creating object with ObjectTypeId: $ObjectTypeId and attribute values $AttributeValues")

        ObjectTypeBean objectTypeBean
        MutableObjectBean mutableObjectBean = null

        try {

            objectTypeBean = objectTypeFacade.loadObjectTypeBean(ObjectTypeId)

            if (objectTypeBean == null) {
                throw new RuntimeException("Could not find ObjectType with ID " + ObjectTypeId)
            }

            mutableObjectBean = objectTypeBean.createMutableObjectBean()


            log.debug("\t" + AttributeValues.size() + " AttributeValues have been supplied, adding them to new object")


            ArrayList availableAttributes = objectTypeAttributeFacade.findObjectTypeAttributeBeans(ObjectTypeId).collectMany {
                [it.getId(), it.getName().toLowerCase()]
            }


            ArrayList<ObjectAttributeBean> objectAttributeBeans = []


            AttributeValues.each { attributeValue ->

                ObjectTypeAttributeBean objectTypeAttributeBean
                if (attributeValue.key instanceof Integer) {
                    log.trace("\tCreating Attribute Bean with ID:" + attributeValue.key + " and value:" + attributeValue.value + ", attribute input value type:" + attributeValue.value.class.simpleName)

                    if (!availableAttributes.contains(attributeValue.key)) {
                        throw new RuntimeException("Attribute " + attributeValue.key + " could not be found for objectID:" + ObjectTypeId)
                    }

                    objectTypeAttributeBean = getObjectTypeAttributeBean(attributeValue.key, ObjectTypeId)
                } else if (attributeValue.key instanceof String) {
                    log.trace("\tCreating Attribute Bean with Name:" + attributeValue.key + " and value:" + attributeValue.value + ", attribute input value type:" + attributeValue?.value?.class?.simpleName)

                    if (!availableAttributes.contains(attributeValue.key.toLowerCase())) {
                        throw new RuntimeException("Attribute " + attributeValue.key + " could not be found for objectID:" + ObjectTypeId)
                    }

                    objectTypeAttributeBean = getObjectTypeAttributeBean(attributeValue.key, ObjectTypeId)
                } else {
                    throw new RuntimeException("Attribute " + attributeValue.key + " could not be found for objectID:" + ObjectTypeId)
                }


                MutableObjectAttributeBean attributeBean
                if (attributeValue.value instanceof ArrayList) {

                    ArrayList<String> valueStrings = []


                    attributeValue.value.each {
                        if (it instanceof ObjectBean) {
                            valueStrings.add(it.objectKey)
                        } else if (it instanceof ApplicationUser) {
                            valueStrings.add(it.key)
                        } else {
                            valueStrings.add(it.toString())
                        }
                    }

                    attributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(mutableObjectBean, objectTypeAttributeBean, *valueStrings)

                } else if (attributeValue.value instanceof Date || attributeValue.value instanceof DateTime || attributeValue.value instanceof LocalDateTime) {


                    DateFormat dateFormat = DateFormat.getDateInstance()
                    attributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(mutableObjectBean, objectTypeAttributeBean, dateFormat, dateFormat, attributeValue.value as String)

                } else {

                    if (attributeValue.value instanceof ObjectBean) {

                        attributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(mutableObjectBean, objectTypeAttributeBean, attributeValue.value.objectKey as String)


                    } else if (attributeValue.value instanceof ApplicationUser) {
                        attributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(mutableObjectBean, objectTypeAttributeBean, attributeValue.value.key as String)

                    } else {
                        attributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(mutableObjectBean, objectTypeAttributeBean, attributeValue.value as String)
                    }

                }


                log.trace("\t\tCreated Attribute Bean:" + attributeBean.collect { ["Attribute ID: " + it.objectTypeAttributeId, "Values:" + it.objectAttributeValueBeans.value] }.flatten())
                log.trace("\t" * 3 + "Input Attribute Name was:" + attributeValue.key + ", Input Value was:" + attributeValue.value)

                if ([attributeValue.value].flatten().size() != attributeBean.objectAttributeValueBeans.size()) {

                    throw new InputMismatchException("Failed to create ObjectAttributeBean based on input data:" + attributeValue)
                }

                objectAttributeBeans.add(attributeBean)

            }


            mutableObjectBean.setObjectAttributeBeans(objectAttributeBeans)
            if (readOnly) {
                log.debug("\tCurrently in readOnly mode, wont be storing object")

                return null

            } else {
                log.trace("\tStoring object")
                ObjectBean newObject = objectFacade.storeObjectBean(mutableObjectBean, this.eventDispatchOption)
                log.info(newObject?.objectKey + " created.")

                return newObject
            }


        } catch (ex) {
            log.error("Error creating object:" + ex.message)
            logRelevantStacktrace(ex.stackTrace)
            throw ex
        }

    }


    /**
     * Updates a single objects single attribute with one or multiple values.
     * @param object Can be object ID, Object Key or ObjectBean
     * @param attribute Can be name of Attribute or AttributeID
     * @param value Can be an array of values or a single object such as a string, ApplicationUser
     *           An attribute value set to [] or null will be cleared.
     * @return Returns the new/updated ObjectAttributeBean
     */
    ObjectAttributeBean updateObjectAttribute(def object, def attribute, def value) {

        log.info("Updating Object $object attribute $attribute with value $value")


        ObjectBean objectBean = getObjectBean(object)
        ObjectAttributeBean newObjectAttributeBean = null


        try {

            log.trace("\tObjectbean:" + objectBean)

            MutableObjectTypeAttributeBean attributeBean = getObjectTypeAttributeBean(attribute, objectBean.objectTypeId).createMutable()


            MutableObjectAttributeBean newAttributeBean
            ObjectAttributeBean oldAttributeBean
            if (value != null && value != []) {

                if (value instanceof ArrayList) {
                    //make sure everything is a string
                    if (value.first() instanceof ObjectBean) {
                        value = value.collect { it.id.toString() }
                    } else {
                        value = value.collect { it.toString() }
                    }

                    newAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(objectBean, attributeBean, *value)

                } else {

                    if (value instanceof ObjectBean) {
                        value = value.id
                    }

                    newAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(objectBean, attributeBean, value as String)

                }

                oldAttributeBean = objectFacade.loadObjectAttributeBean(objectBean.id, attributeBean.id)
                // If attribute exist reuse the old id for the new attribute
                if (oldAttributeBean != null) {
                    newAttributeBean.setId(oldAttributeBean.id)
                }

            }


            if (readOnly) {
                log.info("Attribute not updated, currently in read only mode")
                return null
            } else {

                if (value == null || value == []) {
                    clearObjectAttribute(objectBean, attribute)
                    newObjectAttributeBean = objectFacade.loadObjectAttributeBean(objectBean.id, attributeBean.id)
                } else {
                    newObjectAttributeBean = objectFacade.storeObjectAttributeBean(newAttributeBean, this.eventDispatchOption)
                }


                if (newObjectAttributeBean != null) {
                    log.info("Successfully updated attribute")
                    return newObjectAttributeBean
                } else {
                    log.error("Failed to update attribute")
                    throw new RuntimeInsightException("Error updating Object $object attribute $attribute with value $value")
                }
            }


        } catch (ex) {
            log.error("\tError updating Object $object attribute $attribute with value $value:" + ex.message)
            logRelevantStacktrace(ex.stackTrace)
            throw ex

        }


    }

    /**
     * Updates a single objects multiple attributes with one or multiple values.
     * @param object Can be object ID, Object Key or ObjectBean
     * @param attributeValueMap is a map containing Keys representing AttributeID or Attribute Name and values that are either an array or a single string.
     *          An attribute value set to [] or null will be cleared.
     *          [AttributeID1: ValueA, attributename: [ValueB1,ValueB2]]
     *
     * @return Returns and array of the new/updated ObjectAttributeBean
     */
    ArrayList<ObjectAttributeBean> updateObjectAttributes(def object, Map attributeValueMap) {

        log.info("Updating ${attributeValueMap.size()} Object $object attributes")


        ObjectBean objectBean = getObjectBean(object)

        ArrayList<ObjectAttributeBean> newObjectAttributeBeans = []

        try {

            log.trace("\tObjectbean:" + objectBean)


            attributeValueMap.clone().each { Map.Entry map ->

                if (map.value == null || map.value == []) {
                    clearObjectAttribute(objectBean, map.key)
                } else {

                    MutableObjectTypeAttributeBean attributeBean = getObjectTypeAttributeBean(map.key, objectBean.objectTypeId).createMutable()


                    MutableObjectAttributeBean newAttributeBean
                    if (map.value instanceof ArrayList) {
                        //make sure everything is a string

                        if (map.value.first() instanceof ObjectBean) {
                            map.value = map.value.collect { it.id.toString() }
                        } else {
                            map.value = map.value.collect { it.toString() }
                        }

                        newAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(objectBean, attributeBean, *map.value)


                    } else {

                        if (map.value instanceof ObjectBean) {
                            map.value = map.value.id
                        }

                        newAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(objectBean, attributeBean, map.value as String)
                    }


                    ObjectAttributeBean oldAttributeBean = objectFacade.loadObjectAttributeBean(objectBean.id, attributeBean.id)


                    // If attribute exist reuse the old id for the new attribute
                    if (oldAttributeBean != null) {
                        newAttributeBean.setId(oldAttributeBean.id)
                    }

                    if (readOnly) {
                        log.info("Attribute not updated, currently in read only mode")
                        return null
                    } else {

                        ObjectAttributeBean newObjectAttributeBean = objectFacade.storeObjectAttributeBean(newAttributeBean, this.eventDispatchOption)


                        if (newObjectAttributeBean != null) {
                            newObjectAttributeBeans.add(newObjectAttributeBean)
                            log.info("Successfully updated attribute")

                        } else {
                            log.error("Failed to update attribute")
                            throw new RuntimeException("Failed to update Object (${objectBean.objectKey}) attribute: ${map.key} with value: ${map.value}")

                        }
                    }
                }


            }


        } catch (all) {
            log.error("\tError updating object attribute:" + all.message)
            log.error("\tTried updating object:" + object)
            log.error("\tWith attributes:" + attributeValueMap)
            logRelevantStacktrace(all.stackTrace)
            throw all

        }


        return newObjectAttributeBeans

    }

    ObjectTypeAttributeBean getObjectTypeAttributeBean(def attribute, Integer ObjectTypeId = null) {

        log.trace("Getting ObjectTypeAttributeBean for objectType " + ObjectTypeId + " and attribute:" + attribute)
        ObjectTypeAttributeBean objectTypeAttributeBean = null

        try {
            if (attribute instanceof Integer) {
                objectTypeAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(attribute as Integer)
            } else {

                objectTypeAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttribute(ObjectTypeId, attribute as String)
            }


            if (objectTypeAttributeBean == null) {
                throw new RuntimeException("Attribute " + attribute.toString() + " could not be found" + (ObjectTypeId ? " for object $ObjectTypeId." : "."))
            } else {
                return objectTypeAttributeBean
            }
        } catch (ex) {
            log.error("\tError getting Attribute " + attribute.toString() + (ObjectTypeId ? " for object $ObjectTypeId." : "."))
            log.error("\t\t" + ex.message)
            logRelevantStacktrace(ex.stackTrace)
            throw ex

        }


    }


    /**
     *  A flexible method for returning an ObjectBean
     * @param object can be ObjectBean/MutableObjectBean,Object Key (string) or object ID (int).
     * @return ObjectBean
     */
    ObjectBean getObjectBean(def object) {

        ObjectBean objectBean = null


        if (object instanceof ObjectBean || object instanceof MutableObjectBean) {

            //Refreshes the object
            objectBean = objectFacade.loadObjectBean(object.id)
        } else if (object instanceof String || object instanceof Integer) {

            objectBean = objectFacade.loadObjectBean(object)

        }

        if (objectBean == null) {
            throw new RuntimeException("Failed to find object $object")
        }

        return objectBean
    }

    /**
     * Gets all values of single object attribute. Returns the values as list
     * @param Object Can be ObjectBean/MutableObjectBean,Object Key (string) or object ID (int).
     * @param Attribute can be attribute ID (int) or attribute name (string)
     * @return List of results, if a referenced object is part of the result an ObjectBean will be returned, if empty an empty list.
     */
    List getObjectAttributeValues(def Object, def Attribute) {


        ObjectBean object = getObjectBean(Object)
        ObjectTypeAttributeBean objectTypeAttributeBean = getObjectTypeAttributeBean(Attribute, object.objectTypeId)

        log.info("Getting object (${object.objectKey}) attribute value (${objectTypeAttributeBean.name})")

        //if there are attribute beans, return them if not return empty list
        List<ObjectAttributeValueBean> valueBeans = objectFacade.loadObjectAttributeBean(object.id, objectTypeAttributeBean.id) ? objectFacade.loadObjectAttributeBean(object.id, objectTypeAttributeBean.id).getObjectAttributeValueBeans() : []


        log.trace("\tGot values:" + valueBeans.collect { it.value })

        List values = valueBeans.collect {
            if (it.referencedObjectBeanId != null) {
                return getObjectBean(it.value)
            } else if (objectTypeAttributeBean.status) {
                String statusName = configureFacade.loadStatusTypeBean(it.value as int).name
                return statusName
            } else {
                return it.value
            }
        }

        return values

    }


    /**
     * Get multiple attribute values from object
     * @param Object id, Objectbean, key
     * @param Attributes name of attributes (string) or id (integer), if left empty all attributes with values will be returned.
     * @return A map with where the key is the name of the attribute (not id) and the value is an array of values.
     *          The values are generally of the type they are in Insight, so referenced objects are returned as objects.
     *          If the attribute is empty the key will still be in in the map but with an empty array.
     *          [Created:[Mon Jan 27 09:47:56 UTC 2020], projectID:[], Involved Brands:[Brand1 (STI-30), Brand2 (STI-31)]]
     */
    Map<String, ArrayList> getObjectAttributeValues(def Object, List Attributes = []) {

        Map<String, ArrayList> returnData = [:]

        ObjectBean object = getObjectBean(Object)

        ArrayList<ObjectAttributeBean> attributeBeans = object.getObjectAttributeBeans()

        if (Attributes != []) {
            log.info("For object \"$object\", getting attribute values:" + Attributes)
            List<ObjectTypeAttributeBean> filteredObjectTypeAttributeBeans = []
            Attributes.each {
                filteredObjectTypeAttributeBeans.add(getObjectTypeAttributeBean(it, object.objectTypeId))
            }


            attributeBeans.removeAll { !filteredObjectTypeAttributeBeans.id.contains(it.objectTypeAttributeId) }

        }


        attributeBeans.each { attributeBean ->


            ObjectTypeAttributeBean objectTypeAttributeBean = getObjectTypeAttributeBean(attributeBean.objectTypeAttributeId, object.objectTypeId)
            List<ObjectAttributeValueBean> valueBeans = attributeBean.objectAttributeValueBeans

            ArrayList values = valueBeans.collect {


                if (it.referencedObjectBeanId != null) {
                    return getObjectBean(it.value)
                } else if (objectTypeAttributeBean.status) {

                    String statusName = configureFacade.loadStatusTypeBean(it.value as int).name
                    return statusName

                } else {
                    return it.value
                }

            }

            returnData.put(objectTypeAttributeBean.name, values)
            log.debug("\tGot:" + objectTypeAttributeBean.name + ":" + values)


        }


        return returnData
    }


    /**
     * This will create a HTML table showing some or all of an objects attributes
     * @param Object id, Objectbean, key
     * @param Attributes name of attributes (string) or id (integer), if left empty all attributes will be returned.
     * @return A HTML string
     */
    String renderObjectToHtml(def Object, List Attributes = []) {


        ObjectBean object = getObjectBean(Object)
        Map<String, ArrayList> attributes = getObjectAttributeValues(object, Attributes)

        String returnHtml


        if (object.hasAvatar) {
            returnHtml = "<p style = 'line-height: 20px'>\n"

            if (inJsdBehaviourContext) {
                returnHtml += "   <img src = '${baseUrl}${jsdAvatarUrl.replace("OBJECT_ID", object.id.toString())}?size=48' style='vertical-align: middle' /><b> ${object.label}</b>\n"
            } else {
                returnHtml += "   <img src = '${baseUrl}${jiraAvatarUrl.replace("OBJECT_ID", object.id.toString())}?size=48' style='vertical-align: middle' /><b> ${object.label}</b>\n"
            }
            returnHtml += "</p>" +
                    "<p>" +
                    "<table>"

        } else {
            returnHtml = "<p style = 'line-height: 20px'>\n"

            if (inJsdBehaviourContext) {
                returnHtml += "   <img src = '${baseUrl}${jsdObjectTypeIconUrl.replace("OBJECTTYPE_ID", object.objectTypeId.toString())}?size=48' style='vertical-align: middle' /><b> ${object.label}</b>\n"
            } else {
                returnHtml += "   <img src = '${baseUrl}${jiraObjectTypeIconUrl.replace("OBJECTTYPE_ID", object.objectTypeId.toString())}?size=48' style='vertical-align: middle' /><b> ${object.label}</b>\n"
            }

            returnHtml += "</p>\n" +
                    "<p>\n" +
                    "<table>\n"

        }

        attributes.each {

            if (!it.value.isEmpty() && (it.value.first() instanceof ObjectBean || it.value.first() instanceof MutableObjectBean)) {
                returnHtml += "" +
                        "   <tr>\n" +
                        "        <td valign=\"top\"><b>${it.key}:</b></td>\n" +
                        "        <td valign=\"top\">\n"
                it.value.sort { it.label }.each { referencedObject ->

                    if (referencedObject.hasAvatar) {

                        if (inJsdBehaviourContext) {
                            returnHtml += "     <img src = '${baseUrl}${jsdAvatarUrl.replace("OBJECT_ID", referencedObject.id.toString())}?size=16' style='vertical-align: middle' />${referencedObject.label}"
                        } else {
                            returnHtml += "     <img src = '${baseUrl}${jiraAvatarUrl.replace("OBJECT_ID", referencedObject.id.toString())}?size=16' style='vertical-align: middle' />${referencedObject.label}"
                        }

                    } else {

                        if (inJsdBehaviourContext) {
                            returnHtml += "   <img src = '${baseUrl}${jsdObjectTypeIconUrl.replace("OBJECTTYPE_ID", referencedObject.objectTypeId.toString())}?size=16' style='vertical-align: middle' /><b> ${referencedObject.label}</b>\n"
                        } else {
                            returnHtml += "   <img src = '${baseUrl}${jiraObjectTypeIconUrl.replace("OBJECTTYPE_ID", referencedObject.objectTypeId.toString())}?size=16' style='vertical-align: middle' /><b> ${referencedObject.label}</b>\n"
                        }

                    }

                }

                returnHtml += "    </tr>"

            } else {
                returnHtml += "" +
                        "   <tr>\n" +
                        "        <td valign=\"top\"><b>${it.key}:</b></td>\n" +
                        "        <td valign=\"top\">${it.value.join(",")}</td>\n" +
                        "    </tr>"
            }


        }

        returnHtml += "" +
                "</table>" +
                "</p>"


        return returnHtml

    }

    /**
     * Deletes an object
     * @param object Can be object ID, Object Key or ObjectBean
     * @return boolean representing success or failure
     */
    boolean deleteObject(def object) {

        log.debug("Deleting object:" + object)
        ObjectBean objectBean = getObjectBean(object)

        log.trace("\tObject id:" + objectBean.id)

        if (readOnly) {
            log.info("\tCurrently in readOnly mode, not deleting object $objectBean")
            return false
        } else {
            objectFacade.deleteObjectBean(objectBean.id, this.eventDispatchOption)
            if (objectFacade.loadObjectBean(objectBean.id) == null) {
                log.info("\tDeleted object $objectBean")
                return true
            } else {
                log.error("\tFailed to delete object $objectBean")
                return false
            }
        }


    }

    /**
     * Returns all history beans for an object
     * @param object key, id or objectbean
     * @return Array of history beans
     */
    ArrayList<ObjectHistoryBean> getObjectHistory(def object) {

        ObjectBean objectBean = getObjectBean(object)

        return objectFacade.findObjectHistoryBean(objectBean.id)

    }

    /**
     * This method will retrieve all SimplifiedAttachmentBeans belonging to an object.
     * @param object key, id or objectbean
     * @return ArrayList containing SimplifiedAttachmentBean
     * <b>Note</b> that the File object will have a different file name than the original file name.
     */
    ArrayList<SimplifiedAttachmentBean> getAllObjectAttachmentBeans(def object) {

        log.info("Will get attachments for object:" + object)
        ArrayList<SimplifiedAttachmentBean> objectAttachments = [:]
        ObjectBean objectBean


        try {

            objectBean = getObjectBean(object)
            assert objectBean != null: "Could not find objectbean based on $object"
            ArrayList<AttachmentBean> attachmentBeans = objectFacade.findAttachmentBeans(objectBean.id)
            log.debug("\tFound ${attachmentBeans.size()} attachment beans for the object")

            attachmentBeans.each {
                log.debug("\t" * 2 + it.getFilename() + " (${it.getNameInFileSystem()}) " + it.mimeType)


            }

            objectAttachments.addAll(attachmentBeans.collect { new SimplifiedAttachmentBean(it) })


        } catch (ex) {
            log.error("There was an error trying to retrieve attachments for object:" + object)
            log.error(ex.message)
            throw ex

        }


        log.info("\tSuccessfully retrieved ${objectAttachments.size()} attachments")

        return objectAttachments

    }


    /**
     * Add an attachment to an object
     * @param object key, id or objectbean of the object you want to attatch to
     * @param file the file you´d like to attach
     * @param attachmentName (Optional) Specify a name for the attachment, if non is given the file name will be used
     * @param attachmentComment (Optional) a comment relevant to the attachment, note that this is not the same as an object comment
     * @param deleteSourceFile (Default: False) Should the source file be deleted?
     * @return A SimplifiedAttachmentBean representing the new attachment
     */
    SimplifiedAttachmentBean addObjectAttachment(def object, File file, String attachmentName = "", String attachmentComment = "", boolean deleteSourceFile = false) {


        log.info("Will add attachment ${file.name} to object:" + object.toString())

        ObjectBean objectBean
        File sourceFile

        try {
            objectBean = getObjectBean(object)
            assert objectBean != null: "Could not find objectbean based on $object"


            if (readOnly) {
                log.info("\tCurrently in readOnly mode, wont add attachment")
                return null
            } else {
                if (deleteSourceFile) {
                    sourceFile = file
                } else {
                    sourceFile = new File(file.path + "_temp")
                    Files.copy(file.toPath(), sourceFile.toPath())
                }


                AttachmentBean attachmentBean = objectFacade.addAttachmentBean(objectBean.id, sourceFile, attachmentName ?: file.name, Files.probeContentType(sourceFile.toPath()), attachmentComment)


                assert attachmentBean != null && attachmentBean.nameInFileSystem != null
                log.debug("\tThe attachment was successfully stored and given the name:" + attachmentBean.nameInFileSystem)
                return new SimplifiedAttachmentBean(attachmentBean)
            }


        } catch (ex) {
            log.error("There was an error trying add attachment ${sourceFile.name} to object:" + object)
            log.error(ex.message)
            throw ex

        }


    }


/**
 * Delete an attachment
 * @param attachment Id, AttachmentBean or SimplifiedAttachmentBean
 * @return true if successful
 */
    boolean deleteObjectAttachment(def attachment) {

        log.info("Will delete attachment ($attachment)")
        int attachmentId = 0

        if (attachment instanceof Integer || attachment instanceof Long) {
            attachmentId = attachment
        } else if (attachment instanceof AttachmentBean) {
            attachmentId = attachment.id
        } else if (attachment instanceof SimplifiedAttachmentBean) {
            attachmentId = attachment.id
        }


        if (attachmentId == 0) {

            throw new InputMismatchException("Could not determine attachment based on $attachment")
        }

        log.debug("\tDetermined AttachmentBean ID to be:" + attachmentId)

        try {

            if (readOnly) {
                log.info("\tCurrently in readOnly mode, wont delete attachment")
                return false
            } else {
                AttachmentBean deletedBean = objectFacade.deleteAttachmentBean(attachmentId)
                log.info("\t" + deletedBean.filename + " was deleted from object ${deletedBean.objectId}")
                return true
            }


        } catch (ex) {
            log.error("There was an error trying to delete attachment ${attachment}")
            log.error(ex.message)
            throw ex
        }

    }


    /**
     * This method will export the attachments of an Object
     * The exported files will be exported to $destinationDirectory/$objectKey/
     * The attachment name will be used as filename, if this results in duplicate file names they will be renamed $atachmentName_DUPLICATE1..n
     * @param object key, id or objectbean of the object you want to export from
     * @param destinationDirectory The output directory, a new child folder with the $objectKey will be created and the files will be put in to that folder
     * @return An Array with the exported File objects
     */
    ArrayList<File> exportObjectAttachments(def object, String destinationDirectory) {

        ObjectBean objectBean = getObjectBean(object)
        ArrayList<File> exportedFiles = []

        log.info("Will export all attachments from object:" + objectBean)

        if (destinationDirectory[-1] != "/") {
            destinationDirectory += "/"
        }

        destinationDirectory += objectBean.objectKey + "/"
        if (readOnly) {
            log.debug("\tCurrently in read only mode or the attachments would be exported to:" + destinationDirectory)
        } else {
            File outputDir = new File(destinationDirectory)
            assert !outputDir.exists(): "Output directory already exists:" + outputDir.path
            outputDir.mkdirs()

            assert outputDir.isDirectory(): "Could not create export directory:" + destinationDirectory
            log.debug("\tThe attachments will be exported to:" + destinationDirectory)

        }

        ArrayList<SimplifiedAttachmentBean> sourceBeans = getAllObjectAttachmentBeans(objectBean)
        log.debug("\tObject has ${sourceBeans.size()} attachments")
        log.debug("\tWill require a total of " + sourceBeans.collect { it.attachmentBean.fileSize }.sum() + "bytes.")

        sourceBeans.each { sourceBean ->

            log.trace("\t" * 2 + "Exporting " + sourceBean.originalFileName + " (ID: ${sourceBean.id})")

            if (readOnly) {
                log.info("\tCurrently in readOnly mode, wont export attachment")
            } else {
                String exportedFilePath = destinationDirectory + sourceBean.originalFileName
                long duplicateNr = 1

                if (new File(exportedFilePath).exists()) {
                    while (new File(exportedFilePath + "_DUPLICATE" + duplicateNr).exists()) {
                        duplicateNr++
                    }
                    exportedFilePath += "_DUPLICATE" + duplicateNr
                    log.trace("\t" * 3 + "A file with the same name has already been exported, will rename output file:" + sourceBean.originalFileName + "_DUPLICATE" + duplicateNr)
                }

                File exportedFile = Files.copy(sourceBean.attachmentFile.toPath(), Paths.get(exportedFilePath)).toFile()
                assert sourceBean.attachmentFile.size() == exportedFile.size(): "Size mismatch when exporting" + sourceBean.originalFileName + " (ID: ${sourceBean.id})"
                exportedFiles.add(exportedFile)
            }


        }


        return exportedFiles


    }


    /**
     * This method will import object attachments using the following steps:<br>
     *  <ol>
     *  <li>Determine source object key based on sourceDirectoryPath sub folder name<br> </li>
     *  <li>Determine destination object based on matchingIQL<br> </li>
     *  <li>For every attachment sourceDirectoryPath/$SOURCE_OBJECT_KEY <br>
     *        3.1 Determine if the destination object already has the attachment in question and skip it if ignoreDuplicates == true<br>
     *        3.2 Attach the file to the destination Object and add the attachment comment attachmentComment if it != ""<br>
     *        3.3 If deleteSourceFiles == true, the attached source file will be deleted </li>
     *  </ol>
     *
     * @param sourceDirectoryPath A folder containing one or more folders with the source objects key as name and the files to be attached in those folders. This is the structure given by exportObjectAttachments()
     * @param matchingIQL This IQL should match a single destination object in all of Insight. It must contain the keyword SOURCE_OBJECT_KEY which will be replaced at runtime by the source objects key (based on the folder name)
     * @param attachmentComment (Optional) A comment can be added to the attachment if wanted.  SOURCE_OBJECT_KEY may be used in the comment and will be replaced by the source objects key.
     * @param deleteSourceFiles (Default: False) Should the imported source files be deleted?
     * @param ignoreDuplicates Should duplicates not be imported? Duplicates are determined based on file name and SHA256 hash.
     * @return An Array containing the new SimplifiedAttachmentBeans
     */
    ArrayList<SimplifiedAttachmentBean> importObjectAttachments(String sourceDirectoryPath, String matchingIQL = "\"Old Object key\" = SOURCE_OBJECT_KEY", String attachmentComment = "Imported from SOURCE_OBJECT_KEY", boolean deleteSourceFiles = false, boolean ignoreDuplicates = true) {

        log.info("Will import attachments")
        log.debug("\tWill use the source directory:" + sourceDirectoryPath)


        ArrayList<SimplifiedAttachmentBean> newAttachmentBeans = []
        File sourceDir = new File(sourceDirectoryPath)
        ArrayList<File> objectDirs = sourceDir.listFiles()

        objectDirs.each { sourceDirFile ->
            assert sourceDirFile.isDirectory(): "Source directory may only contain directories, ${sourceDirFile.name} is a file"
            assert sourceDirFile.name.matches("\\w+-\\d+"): "Source directory may only contain directories named after object keys, ${sourceDirFile.name} doesnt look like an object key"
        }
        log.trace("\t" * 2 + "The source directory appears to be valid")

        log.debug("\tWill use the matching IQL:" + matchingIQL)
        assert matchingIQL.contains("SOURCE_OBJECT_KEY"): "The matchingIQL parameter must contain the string:\"SOURCE_OBJECT_KEY\""
        log.trace("\t" * 2 + "The matching IQL appears to be valid")


        objectDirs.each { objectDir ->
            String sourceObjectKey = objectDir.name
            ArrayList<File> sourceFiles = objectDir.listFiles()
            String destinationIql = matchingIQL.replace("SOURCE_OBJECT_KEY", sourceObjectKey)
            ObjectBean destinationObject
            ArrayList<SimplifiedAttachmentBean> destinationObjectPreexistingAttachments

            log.debug("\tWill import attachments from object directory:" + sourceObjectKey)
            log.debug("\tWill determine destination object with IQL:" + destinationIql)

            ArrayList<ObjectBean> destinationObjects = iqlFacade.findObjects(destinationIql)


            if (destinationObjects.size() != 1) {
                log.error("Could not determine destination object for attachments from source object: " + sourceObjectKey)
                log.error("Affected source directory:" + objectDir.path)
                log.error("Tried to determine destination object with IQL:" + destinationIql)
                if (destinationObjects.size() == 0) {
                    log.error("\tNo objects matches the IQL")
                } else {
                    log.error("\tMore than one objects matches the IQL:")
                    destinationObjects.each { log.error("\t" * 3 + it) }
                }
                throw new InputMismatchException("Could not determine destination object for attachments from source object: " + sourceObjectKey)
            }
            destinationObject = destinationObjects.first()
            log.debug("\tDetermined destination object to be:" + destinationObject)
            log.debug("\tWill evaluate " + sourceFiles.size() + " files for import")


            if (ignoreDuplicates) {
                log.debug("\tWill Ignore duplicates")
                log.trace("\t\tGetting destination objects current attachments")
                destinationObjectPreexistingAttachments = getAllObjectAttachmentBeans(destinationObject)
                log.trace("\t" * 3 + "Destination object currently has " + destinationObjectPreexistingAttachments.size() + " attachments")
                destinationObjectPreexistingAttachments.each { preexistingAttachment ->
                    boolean duplicateFound = sourceFiles.removeIf { sourceFile ->
                        sourceFile.name == preexistingAttachment.originalFileName &&
                                sourceFile.bytes.sha256() == preexistingAttachment.attachmentFile.bytes.sha256()
                    }
                    if (duplicateFound) {
                        log.trace("\t" * 2 + "Will ignore source file that is duplicate of:" + preexistingAttachment.originalFileName)
                    }

                }

            } else {
                log.debug("\tWill import duplicates")
            }

            log.debug("\tStarting import of ${sourceFiles.size()} files")
            sourceFiles.each { sourceFile ->
                String attachmentName = sourceFile.name
                log.trace("\t" * 2 + "Importing:" + attachmentName)
                if (sourceFile.isDirectory()) {
                    log.error("Importing directories is not supported:" + sourceFile.path)
                    throw new FileSystemException("Importing directories is not supported:" + sourceFile.path)
                }
                if (attachmentName.matches(".*_DUPLICATE\\d+\$")) {
                    attachmentName = attachmentName.replaceFirst("_DUPLICATE\\d+\$", "")
                    log.trace("\t" * 3 + "The file name is a duplicate, but will be imported as:" + attachmentName)
                }


                SimplifiedAttachmentBean newAttachmentBean = addObjectAttachment(destinationObject, sourceFile, attachmentName, attachmentComment.replace("SOURCE_OBJECT_KEY", sourceObjectKey), deleteSourceFiles)

                if (newAttachmentBean == null && readOnly) {
                    log.info("\tCurrently in readOnly mode, attachment was not imported")
                    newAttachmentBeans.add(null)
                } else if (!newAttachmentBean.isValid()) {
                    log.error("There was an error attaching a file to " + destinationObject)
                    log.error("\tSource file:" + sourceFile.path)
                    log.error("\tResulting SimplifiedAttachmentBean:" + newAttachmentBean.properties)
                    throw new InputMismatchException("There was an error attaching a file to " + destinationObject)
                } else {
                    log.trace("\t" * 3 + "Success!")
                    newAttachmentBeans.add(newAttachmentBean)
                }


            }

            log.debug("\tFinished importing attachments from object directory:" + sourceObjectKey)
        }


        return newAttachmentBeans

    }


    /**
     * Create an object comment
     * Note that as of 2020-02 the author of the comment is not completely deterministic: https://jira.mindville.com/browse/ICS-1879
     * @param object id, key, objectBean of the object you´d like to add a comment to
     * @param commentText The text/body of the comment
     * @param accessLevel The access level of the comment, default is Users
     * @return The newly created CommentBean
     */
    CommentBean createObjectComment(def object, String commentText, int accessLevel = CommentBean.ACCESS_LEVEL_USERS) {

        log.info("Adding comment to object:" + object)
        log.debug("\tComment Text:" + commentText)
        log.debug("\tAccess Level:" + accessLevel)

        CommentBean newComment = new CommentBean()

        newComment.setComment(commentText)
        newComment.setRole(accessLevel)

        newComment.setAuthor(authContext.loggedInUser.key)
        newComment.setObjectId(getObjectBean(object).id)
        newComment = objectFacade.storeCommentBean(newComment)


        log.debug("\tAuthor:" + newComment?.author)
        log.debug("\tObject ID:" + newComment?.objectId)

        if (newComment != null && newComment.id != null) {
            log.info("\tComment successfully created, ID:" + newComment.id)
        }


        return newComment

    }


    /**
     * Get commentBeans from an object
     * @param object id, key or objectBean of the object you´d like to get comments from
     * @return an Array of CommentBeans
     */
    ArrayList<CommentBean> getObjectComments(def object) {

        log.info("Will get comments for object:" + object.toString())

        ArrayList<CommentBean> comments = objectFacade.findCommentBeans(getObjectBean(object).id)

        log.info("\tReturning ${comments.size()} comments")

        return comments

    }


    /**
     * Delete an object comment
     * @param comment the id or CommentBean
     * @return a boolean representing success
     * @throws com.riadalabs.jira.plugins.insight.common.exception.InsightException
     */
    boolean deleteObjectComment(def comment) throws InsightException {

        int commentId

        if (comment instanceof Integer || comment instanceof Long) {
            commentId = comment
        } else if (comment instanceof CommentBean) {
            commentId = comment.id
        } else {
            throw new InputMismatchException("Error when deleting object comment. The comment parameter is of an unknown type:" + comment?.class?.simpleName as String)
        }

        log.info("Deleting object comment:" + commentId)
        if (readOnly) {
            log.info("\tCurrently in read only mode, comment wont be deleted, returning true")
            return true
        } else {
            objectFacade.deleteCommentBean(commentId)
        }
        boolean success = objectFacade.loadCommentBean(commentId) == null

        log.info("\tComment deleted:" + success)

        return success


    }


    /**
     * Get an IconBean that is either avilable in a schema or globally.
     * If two matches are found, the icon from the matching schema will be preferred.
     * @param iconName Name of the icon
     * @param schemaId Id of the schema
     * @return an IconBean object, or null if not found
     */
    IconBean getIconBean(String iconName, int schemaId) {

        log.info("Getting IconBean with name $iconName that is available globally or in schema $schemaId")

        ArrayList<IconBean> iconBeans = configureFacade.findAllIconBeans(schemaId).findAll {it.name == iconName}
        log.debug("\tFound ${iconBeans.size()} matching iconBeans")
        IconBean matchingBean = iconBeans.size() == 1 ? iconBeans.first()  : iconBeans.find {it.objectSchemaId == schemaId}

        if (iconBeans.size() == 1) {
            log.info("\tReturning bean ${matchingBean.name} (${matchingBean.id}) from schema: ${matchingBean.objectSchemaId}")
            return iconBeans.first()
        }else if (iconBeans.size() > 1) {
            log.info("\tFound multiple matching icons, returning bean ${matchingBean.name} (${matchingBean.id}) from schema: ${matchingBean.objectSchemaId}")
            return iconBeans.find {it.objectSchemaId == schemaId}
        }
        log.warn("\tCould not find iconBean with name $iconName")
        return null
    }


    /**
     * Create a new objectType
     * @param name
     * @param schemaId
     * @param description (optional)
     * @param parentTypeId (optional)
     * @return the new ObjectTypeBean or if in readOnly the yet to be stored MutableObjectTypeBean
     */
    ObjectTypeBean createObjectType(String name, int schemaId, IconBean iconBean, String description = "", Integer parentTypeId = null, ArrayList<MutableObjectTypeAttributeBean> attributeBeans = []) {
        MutableObjectTypeBean mutableObjectTypeBean = new MutableObjectTypeBean().tap {objectType ->
            objectType.name = name
            objectType.objectSchemaId = schemaId
            objectType.description = description
            objectType.parentObjectTypeId = parentTypeId
            objectType.iconBean = iconBean
        }

        if (readOnly) {
            return mutableObjectTypeBean
        }
        ObjectTypeBean objectTypeBean = objectTypeFacade.createObjectTypeBean(mutableObjectTypeBean)

        log.info("Created Object Type:${objectTypeBean.name } (${objectTypeBean.id})")
        if (attributeBeans) {
            log.info("\tCreating ${attributeBeans.size()} attributes for the object")
            attributeBeans.each {attributeBean  ->
                log.debug("\t"*2 + "Creating attribute ${attributeBean.name} (${attributeBean.type.name()}-${attributeBean.defaultType.name()})")
                ObjectTypeAttributeBean newAttribute =  objectTypeAttributeFacade.storeObjectTypeAttributeBean(attributeBean, objectTypeBean.id)
                log.debug("\t"*3 + "Created attribute:" + newAttribute.id)
            }


        }


        return objectTypeBean

    }

    /**
     * Make an ObjectType abstract
     * @param objectType The object type to modify
     * @return An updated ObjectTypeBean representation
     */
    ObjectTypeBean setObjectTypeAbstract(ObjectTypeBean objectType) {

        MutableObjectTypeBean mutableObjectTypeBean = objectType.createMutable()

        mutableObjectTypeBean.setAbstractObjectType(true)

        return objectTypeFacade.updateObjectTypeBean(mutableObjectTypeBean)

    }

    /**
     * Get object schema based on key
     * @param schemaKey Key of the object schema
     * @return The corresponding ObjectSchemaBean or null if not found
     */
    ObjectSchemaBean getObjectSchema(String schemaKey) {
        return objectSchemaFacade.findObjectSchemaBeans().find {it.objectSchemaKey == schemaKey}
    }


    /**
     * Will create a new object schema
     * @param name Name of new schema
     * @param key Key of new schema
     * @param description (optional)
     * @param allowOtherObjectSchema Should other schemas be allowed to reference this schema
     * @param serviceDescCustomersEnabled Should JSM customers have access to this schema
     * @return the new ObjectSchemaBean or MutableObjectSchemaBean if readOnly
     */
    ObjectSchemaBean createObjectSchema(String name, String key, String description = "", boolean allowOtherObjectSchema = false, boolean serviceDescCustomersEnabled = false) {

        MutableObjectSchemaBean schemaBean = new MutableObjectSchemaBean().tap { schema ->
            schema.name = name
            schema.objectSchemaKey = key
            schema.description = description
        }

        if (readOnly) {
            return schemaBean
        }

        ObjectSchemaBean newSchema = objectSchemaFacade.storeObjectSchemaBean(schemaBean)

        //Properties must be set after creation
        ObjectSchemaPropertyBean propertyBean = newSchema.objectSchemaPropertyBean
        propertyBean.setAllowOtherObjectSchema(allowOtherObjectSchema)
        propertyBean.setServiceDescCustomersEnabled(serviceDescCustomersEnabled)
        objectSchemaFacade.storeObjectSchemaProperty(newSchema.id, propertyBean)

        return objectSchemaFacade.loadObjectSchema(newSchema.id)

    }
    /**
     * Delete an object schema and all of its objects
     * @param id
     * @return true on success or when in readOnly mode
     */
    boolean deleteObjectSchema(int id) {

        log.info("Deleting ObjectSchema with id: $id")
        if (readOnly) {
            log.info("\tCurrently in read only, wont delete")
            return true
        }

        objectSchemaFacade.deleteObjectSchemaBean(id)
        if (objectSchemaFacade.findObjectSchemaBeans().findAll { it.id == id }.empty) {
            log.info("\tObjectSchema was deleted")
            return true
        } else {
            log.warn("\tFailed to delete object schema ${id}")
            return false
        }
    }

    /**
     * Delete an object schema with a matching name AND key
     * @param schemaName The name of the schema to delete
     * @param schemaKey THe key of the schema to delete
     * @return true on success or when in readOnly mode, false if the schema wasn't found or successfully deleted
     */
    boolean deleteObjectSchema(String schemaName, String schemaKey) {

       log.info("Deleting ObjectSchema with name: $schemaName, and key: $schemaKey")
        if (readOnly) {
            log.info("\tCurrently in read only, wont delete")
            return true
        }

        ObjectSchemaBean schemaBean = objectSchemaFacade.findObjectSchemaBeans().find { it.name == schemaName && it.objectSchemaKey == schemaKey }

        if (!schemaBean) {
            log.warn("\tCould not find ObjectSchema with name: $schemaName, and key: $schemaKey. Nothing was deleted")
            return false
        }

        objectSchemaFacade.deleteObjectSchemaBean(schemaBean.id)
        if (objectSchemaFacade.findObjectSchemaBeans().findAll { it.id == schemaBean.id }.empty) {
            log.info("\tObjectSchema was deleted")
            return true
        } else {
            log.warn("\tFailed to delete object schema ${schemaBean.id}")
            return false
        }

    }




    void logRelevantStacktrace(StackTraceElement[] stacktrace) {

        stacktrace.each {
            if (it.className.contains(this.class.name)) {
                log.error(it)

            }
        }


    }
}



