package com.eficode.atlassian.insightmanager.tests.runners

/**
 * Used to run spec ImObjectSchemaCrudSpec.groovy
 * Presumes setupSpecEnv.groovy has been setup
 */

import com.eficode.atlassian.jiraInstanceManager.JiraInstanceManagerRest
import com.eficode.atlassian.jiraInstanceManager.beans.SpockResult
import org.apache.groovy.json.internal.LazyMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

String jiraBaseUrl = "http://jira.localhost:8080"
Logger log = LoggerFactory.getLogger("ImObjectCrudSpec.runner")
JiraInstanceManagerRest jiraR = new JiraInstanceManagerRest(jiraBaseUrl)


jiraR.clearCodeCaches(["insight"])
jiraR.updateScriptrunnerFiles("tests/com/eficode/atlassian/insightmanager/tests/specs/" : "com/eficode/atlassian/insightmanager/tests/specs/")
jiraR.updateScriptrunnerFiles("tests/com/eficode/atlassian/insightmanager/tests/utils/" : "com/eficode/atlassian/insightmanager/tests/utils/")
jiraR.updateScriptrunnerFiles("../src/main/groovy/com/eficode/atlassian/insightmanager/" : "com/eficode/atlassian/insightmanager/")

jiraR.installGrapeDependency("com.konghq", "unirest-java", "3.13.6", "", "standalone")
jiraR.installGrapeDependency("com.eficode.atlassian", "jirainstancemanager", "1.5.2-SNAPSHOT", "https://github.com/eficode/JiraInstanceManagerRest/raw/packages/repository/")

//jiraR.clearCodeCaches()
//LazyMap spockResult = jiraR.runSpockTest("com.eficode.atlassian.insightmanager.tests.specs", "PocSpec")

//SpockResult spockResult = jiraR.runSpockTest("com.eficode.atlassian.insightmanager.tests.specs", "ImObjectSchemaCrudSpec","Test Creation of ObjectTypes with attributes" )
//SpockResult spockResult = jiraR.runSpockTest("com.eficode.atlassian.insightmanager.tests.specs", "ImObjectSchemaCrudSpec","Verify cardinality and overwrites is respected" )
//SpockResult spockResult = jiraR.runSpockTest("com.eficode.atlassian.insightmanager.tests.specs", "ImObjectSchemaCrudSpec","Test ObjectSchema Role CRUD" )
SpockResult spockResult = jiraR.runSpockTest("com.eficode.atlassian.insightmanager.tests.specs", "ImObjectSchemaCrudSpec","Test ObjectType Role CRUD" )
//SpockResult spockResult = jiraR.runSpockTest("com.eficode.atlassian.insightmanager.tests.specs", "ImObjectSchemaCrudSpec")

log.info(spockResult.toString() )