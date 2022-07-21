package com.eficode.atlassian.insightmanager.tests.specs

//import com.eficode.atlassian.insightmanager.InsightManagerForScriptrunner
import com.eficode.atlassian.insightmanager.tests.utils.SpecHelper
import spock.lang.Specification



class ImObjectCrudSpec extends Specification {

    String jiraBaseUrl = "http://jira.test.com:8080"
    String restAdmin = "admin"
    String restPassword = "admin"

    SpecHelper specHelper = new SpecHelper(jiraBaseUrl, restAdmin, restPassword)


    def "Test iql of objects in template schema"() {

        setup:
        //InsightManagerForScriptrunner im = new InsightManagerForScriptrunner()
        specHelper.jiraR.getProjects()
        //Map projectAndSchemaMap = specHelper.createSampleProjectAndSchema("Spoc2", "SPOC2")



    }


}