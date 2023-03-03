package com.eficode.atlassian.insightmanager.tests.runners

/**
 * Used to run spec ImObjectSchemaCrudSpec.groovy
 * Presumes setupSpecEnv.groovy has been setup
 */

import com.eficode.atlassian.jiraInstanceManager.JiraInstanceManagerRest
import org.apache.groovy.json.internal.LazyMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

String jiraBaseUrl = "http://jira.localhost:8080"
Logger log = LoggerFactory.getLogger("ImObjectCrudSpec.runner")
JiraInstanceManagerRest jiraR = new JiraInstanceManagerRest(jiraBaseUrl)



jiraR.updateScriptrunnerFiles("tests/com/eficode/atlassian/insightmanager/tests/specs/" : "com/eficode/atlassian/insightmanager/tests/specs/")
jiraR.updateScriptrunnerFiles("tests/com/eficode/atlassian/insightmanager/tests/utils/" : "com/eficode/atlassian/insightmanager/tests/utils/")
jiraR.updateScriptrunnerFiles("../src/main/groovy/com/eficode/atlassian/insightmanager/" : "com/eficode/atlassian/insightmanager/")
jiraR.clearCodeCaches()
jiraR.installGrapeDependency("com.konghq", "unirest-java", "3.13.6", "", "standalone")
//jiraR.clearCodeCaches()
//LazyMap spockResult = jiraR.runSpockTest("com.eficode.atlassian.insightmanager.tests.specs", "PocSpec")

LazyMap spockResult = jiraR.runSpockTest("com.eficode.atlassian.insightmanager.tests.specs", "ImObjectSchemaCrudSpec","Test Creation of ObjectTypes with attributes" )

log.info(spockResult.isEmpty()  ?  "" : spockResult.toString() )