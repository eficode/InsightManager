package com.eficode.atlassian.insightmanager.tests.runners

/**
 * Used to run spec ImObjectCrudSpec.groovy
 * Presumes _setupSpecEnv.groovy has been setup
 */

import com.eficode.atlassian.jiraInstanceManger.JiraInstanceMangerRest
import org.apache.groovy.json.internal.LazyMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

String jiraBaseUrl = "http://jira.test.com:8080"
Logger log = LoggerFactory.getLogger("ImObjectCrudSpec.runner")
JiraInstanceMangerRest jiraR = new JiraInstanceMangerRest()
jiraR.baseUrl = jiraBaseUrl

jiraR.installGrapeDependency("com.konghq", "unirest-java", "3.13.6", "", "standalone")
jiraR.updateScriptrunnerFiles("tests/com/eficode/atlassian/insightmanager/tests/specs/" : "com/eficode/atlassian/insightmanager/tests/specs/")
//jiraR.clearCodeCaches()
LazyMap spockResult = jiraR.runSpockTest("com.eficode.atlassian.insightmanager.tests.specs", "ImObjectCrudSpec")

log.info(spockResult.isEmpty()  ?  "" : spockResult.toString() )