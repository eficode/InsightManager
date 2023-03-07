import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.Type as Type
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.DefaultType as DefaultType
import org.apache.log4j.Level
import org.apache.log4j.Logger


Logger log = Logger.getLogger("beanGenerator")
log.setLevel(Level.ALL)

/**
 * Generates the SimplifiedAttributeFactory-class based on available types
 */

String baseClass = """
package com.eficode.atlassian.insightmanager

import com.atlassian.jira.component.ComponentAccessor
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.Type as Type
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.DefaultType as DefaultType
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.ConfigureFacadeImpl
import com.riadalabs.jira.plugins.insight.services.model.ReferenceTypeBean

/**
 * A class intended to make creating new Attributes easier.<p>
 * It has has static methods for creating attributes with the most common settings.<p>
 * Additional configurations can be added to the Bean returned, configurations such as:<p>
 *  1. bean.setMaximumCardinality()<p>
 *  2. bean.setMinimumCardinality()<p>
 *  3. bean.setIndexed()<p>
 */
class SimplifiedAttributeFactory {

    static Class configureFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ConfigureFacade")
    static ConfigureFacadeImpl configureFacade = ComponentAccessor.getOSGiComponentInstanceOfType(configureFacadeClass) as ConfigureFacadeImpl


BODY-GOES-HERE

}

"""

String body = ""

log.info("Generating the default-type methods")
ArrayList<String>defaultTypes = DefaultType.values().collect {it.toString().toLowerCase().capitalize()}
defaultTypes.findAll {!["None", "Select", "Text", "Url"].contains(it)}.each {type ->

    body += "\n" +
            "    /**\n" +
            "     * Get a $type attribute\n" +
            "     * @param name Name of attribute\n" +
            "     * @param description Description of attribute\n" +
            "     * @return a new MutableObjectTypeAttributeBean\n" +
            "     */\n"+
            "    static MutableObjectTypeAttributeBean get$type(String name, String description = \"\") {\n" +
            "        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.${type.toUpperCase()})\n" +
            "        attributeBean.setDescription(description)\n" +
            "        return  attributeBean\n" +
            "    }" +
            "\n"
}
//Text with label support
body += "\n" +
        "    /**\n" +
        "     * Get a Text attribute\n" +
        "     * @param name Name of attribute\n" +
        "     * @param description Description of attribute\n" +
        "     * @param isLabel set to true if this should be the label of the objectType\n" +
        "     * @return\n" +
        "     */"+
        "    static MutableObjectTypeAttributeBean getText(String name, String description = \"\",  boolean isLabel = false) {\n" +
        "        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.TEXT)\n" +
        "        attributeBean.setDescription(description)\n" +
        "        attributeBean.setLabel(isLabel)\n" +
        "        if (isLabel) {\n" +
        "            attributeBean.setIndexed(true)\n" +
        "        }" +
        "        return  attributeBean\n" +
        "    }" +
        "\n"

body += "\n" +
        "    static MutableObjectTypeAttributeBean getSelect(String name, String description = \"\",  ArrayList<String> options = []) {\n" +
        "        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.SELECT)\n" +
        "        attributeBean.setDescription(description)\n" +
        "        if (options.size()) {\n" +
        "            attributeBean.setOptions(options.join(\",\"))\n" +
        "        }\n" +
        "        return  attributeBean\n" +
        "    }\n"

body += "\n" +
        "    static MutableObjectTypeAttributeBean getUrl(String name, String description = \"\", boolean enablePing = false) {\n" +
        "        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.URL)\n" +
        "        attributeBean.setDescription(description)\n" +
        "        attributeBean.setAdditionalValue(enablePing ? \"ENABLED\" : \"DISABLED\")\n" +
        "        return  attributeBean\n" +
        "    }\n" +
        "\n"

body += "\n" +
        "    static MutableObjectTypeAttributeBean getUser(String name, String description = \"\",  ArrayList<String>groupNames = [], boolean showOnProfile = true) {\n" +
        "        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.USER, DefaultType.NONE)\n" +
        "        attributeBean.setDescription(description)\n" +
        "        groupNames ? attributeBean.setTypeValue(groupNames.join(\",\")) : null\n" +
        "        attributeBean.setAdditionalValue(showOnProfile ? \"SHOW_PROFILE\" : \"HIDE_PROFILE\")\n" +
        "        return  attributeBean\n" +
        "    }\n" +
        "\n"

body += "\n" +
       "    /**\n" +
        "     * Get a referenced object attribute type\n" +
        "     * @param name Name of the attribute\n" +
        "     * @param schemaId Id where the attribute is to be created\n" +
        "     * @param refObjectTypeId Id of the object type to reference\n" +
        "     * @param refName The name of the reference type (specified in the \"Additional Value\" column in the GUI)\n" +
        "     * @param description Attribute description\n" +
        "     * @return\n" +
        "     */\n" +
        "    static MutableObjectTypeAttributeBean getReferencedObject(String name, int schemaId, int refObjectTypeId, String refName,  String description = \"\") {\n" +
        "        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.REFERENCED_OBJECT, DefaultType.NONE)\n" +
        "        attributeBean.setDescription(description)\n" +
        "        attributeBean.setReferenceObjectTypeId(refObjectTypeId)\n" +
        "        ReferenceTypeBean refBean = configureFacade.findAllReferenceTypeBeans(schemaId).find {it.name == refName}\n" +
        "        attributeBean.setReferenceTypeBean(refBean)\n" +
        "        attributeBean.setIndexed(true)\n" +
        "        return  attributeBean\n" +
        "    }\n"+
        "\n"
body += "\n" +
        "    static MutableObjectTypeAttributeBean getGroup(String name, String description = \"\", boolean showOnProfile = true) {\n" +
        "        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.GROUP, DefaultType.NONE)\n" +
        "        attributeBean.setDescription(description)\n" +
        "        attributeBean.setAdditionalValue(showOnProfile ? \"SHOW_PROFILE\" : \"HIDE_PROFILE\")\n" +
        "        return  attributeBean\n" +
        "    }\n" +
        "\n"

body += "\n" +
        "    /**\n" +
        "     * Get a status attribute type\n" +
        "     * @param name Name of the attribute\n" +
        "     * @param schemaId Id where the attribute is to be created, only needed if supplying statusNames\n" +
        "     * @param statusNames List of statuses that should be available\n" +
        "     * @param description Attribute description\n" +
        "     * @return a new MutableObjectTypeAttributeBean\n" +
        "     */\n" +
        "    static MutableObjectTypeAttributeBean getStatus(String name,Integer schemaId = null, ArrayList<String> statusNames = [], String description = \"\") {\n" +
        "        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.STATUS, DefaultType.NONE)\n" +
        "        attributeBean.setDescription(description)\n" +
        "        if (!statusNames.empty) {\n" +
        "            assert schemaId != null : \"When creating a status attribute with statusNames an objectId must be supplied\"\n" +
        "            ArrayList<String>statusIds = configureFacade.findAllStatusTypeBeans(schemaId).findAll {statusNames.contains(it.name)}.id.collect {it.toString()}\n" +
        "            assert statusIds.size() == statusNames.size() : \"Error finding statuses for new status attributes, only found IDs:\" + statusIds.join(\",\")\n" +
        "            attributeBean.setTypeValue(statusIds.join(\",\"))\n" +
        "         \n" +
        "        }\n" +
        "       return attributeBean\n" +
        "    }\n" +
        "\n"

String completeClass = baseClass.replace("BODY-GOES-HERE", body)
println(completeClass)