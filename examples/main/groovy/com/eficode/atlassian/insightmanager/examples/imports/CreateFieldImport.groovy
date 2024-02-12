package com.eficode.atlassian.insightmanager.examples.imports

import com.eficode.atlassian.insightmanager.InsightManagerForScriptrunner
import com.eficode.atlassian.insightmanager.SimplifiedAttributeFactory
import com.eficode.atlassian.jiraInstanceManager.JiraInstanceManagerRest
//import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.ImportSourceConfigurationFacadeImpl
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ImportSourceConfigurationFacade
import com.riadalabs.jira.plugins.insight.services.imports.common.external.DataLocator
import com.riadalabs.jira.plugins.insight.services.imports.common.external.ModuleOTSelector
import com.riadalabs.jira.plugins.insight.services.imports.common.external.model.EmptyValues
import com.riadalabs.jira.plugins.insight.services.imports.common.external.model.MissingObjectsType
import com.riadalabs.jira.plugins.insight.services.imports.common.external.model.MissingReferencesType
import com.riadalabs.jira.plugins.insight.services.imports.common.external.model.UnknownValues
import com.riadalabs.jira.plugins.insight.services.imports.model.ImportSource
import com.riadalabs.jira.plugins.insight.services.imports.model.ImportSourceOT
import com.riadalabs.jira.plugins.insight.services.imports.model.ImportSourceOTAttr
import com.riadalabs.jira.plugins.insight.services.imports.model.importsource.HandleMissingObjects
import com.riadalabs.jira.plugins.insight.services.imports.model.importsource.ImportSourceStatus
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeBean


/**
 * This Scripts demonstrates adding a customized import to an existing object schema
 * It will:
 *  1. Install the Jira/Bitbucket Environment Importer App
 *  2. Create a new Jira Environment Source import in the schema
 *  3. Create a Field ObjectType
 *  4. Create ObjectType mapping in the new import
 *  5. Create Attribute mappings in the new import
 */

String jiraBaseUrl = "http://jira.localhost:8080"
String adminUser = "admin"
String adminPw = adminUser

String importName = "Jira Field Importer"
String runAsUserKey = "JIRAUSER10000"
String importType="rlabs-import-type-jira-environment"
Integer schemaId = 10




//Install the Jira/Bitbucket Environment Importer App
JiraInstanceManagerRest jiraR = new JiraInstanceManagerRest(adminUser, adminPw, jiraBaseUrl)
assert jiraR.installApp("https://marketplace.atlassian.com/download/apps/1217750/version/1020000110") : "Error Installing Insight JIRA/Bitbucket Importer"


InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
ImportSourceConfigurationFacade importFacade = im.importFacade


//Create Object Type
ArrayList<MutableObjectTypeAttributeBean>fieldsAttributes = []
fieldsAttributes += SimplifiedAttributeFactory.getText("Id")
fieldsAttributes += SimplifiedAttributeFactory.getText("Description")
fieldsAttributes += SimplifiedAttributeFactory.getText("Field Type")

ObjectTypeBean fieldObjectType = im.createObjectType("Field", schemaId,im.getIconBean("Gears", schemaId),"", null, fieldsAttributes)


//Create Config for Attribute Import
ArrayList<ImportSourceOTAttr> importSourceOTAttrs = []
Character delimiter = "n"
["Name", "Id", "Description", "Field Type"].each {attributeName ->
    ImportSourceOTAttr importSourceOTAttr = new ImportSourceOTAttr()
    //Set attribute ID for the mapping
    importSourceOTAttr.setObjectTypeAttributeId(im.objectTypeAttributeFacade.loadObjectTypeAttribute(fieldObjectType.id, attributeName).id)
    //Configure DataLocator mapping for the attribute
    importSourceOTAttr.setDataLocator([new DataLocator(attributeName)])
    //Set mapping to "Identifier" if the attributeName is Name
    importSourceOTAttr.setExternalIdPart(attributeName == "Name")
    //Set defaults
    importSourceOTAttr.setDelimiter(delimiter)
    importSourceOTAttr.setBase64(false)

    importSourceOTAttrs.add(importSourceOTAttr)

}

//Create config for ObjectTypeImport
ImportSourceOT newImportSourceOT = new ImportSourceOT()
//Set the data selector for the ObjectType
newImportSourceOT.setModuleOTSelector(new ModuleOTSelector("FIELD"))
//Set ID of the ObjectType to import
newImportSourceOT.setObjectTypeId(fieldObjectType.id)
//Define how missing objects should be handled
newImportSourceOT.handleMissingObjects = new HandleMissingObjects(MissingObjectsType.IGNORE, MissingReferencesType.IGNORE)
//Add the attribute mappings
newImportSourceOT.setImportSourceOTAttrBeans(importSourceOTAttrs)
//Enable the ObjectType import
newImportSourceOT.setImportSourceStatus(ImportSourceStatus.createEnabled())



//Creating the actual Import
ImportSource newImportSource = new ImportSource()
newImportSource.setName(importName)
newImportSource.setRunAsUserKey(runAsUserKey)
newImportSource.setImportSourceModuleKey(importType)
newImportSource.setDefaultHandleEmptyValues(EmptyValues.IGNORE)
newImportSource.setDefaultHandleUnknownValues(UnknownValues.IGNORE)
newImportSource.setObjectSchemaId(schemaId)
newImportSource.setDefaultConcatenator("-")
newImportSource.setJsonConfiguration("{}")


//Create Import Source
newImportSource = importFacade.storeImportSource(newImportSource)

//Set the ImportSourceId of the ObjectType-mapping
newImportSourceOT.setImportSourceId(newImportSource.id)
importFacade.storeImportSourceOT(newImportSourceOT)

//Enable Import Source now that is has an ObjectType-mapping
newImportSource.setImportSourceStatus(ImportSourceStatus.createEnabled())
importFacade.storeImportSource(newImportSource)