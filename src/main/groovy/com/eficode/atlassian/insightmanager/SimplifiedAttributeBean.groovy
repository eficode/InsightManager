
package com.eficode.atlassian.insightmanager

import com.atlassian.jira.component.ComponentAccessor
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.Type as Type
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.DefaultType as DefaultType
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.impl.ConfigureFacadeImpl
import com.riadalabs.jira.plugins.insight.services.model.ReferenceTypeBean

class SimplifiedAttributeBean {

    static Class configureFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ConfigureFacade")
    static ConfigureFacadeImpl configureFacade = ComponentAccessor.getOSGiComponentInstanceOfType(configureFacadeClass) as ConfigureFacadeImpl



    /**
     * Get a Integer attribute
     * @param name Name of attribute
     * @param description Description of attribute
     * @return a new MutableObjectTypeAttributeBean
     */
    static MutableObjectTypeAttributeBean getInteger(String name, String description = "") {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.INTEGER)
        attributeBean.setDescription(description)
        return  attributeBean
    }

    /**
     * Get a Boolean attribute
     * @param name Name of attribute
     * @param description Description of attribute
     * @return a new MutableObjectTypeAttributeBean
     */
    static MutableObjectTypeAttributeBean getBoolean(String name, String description = "") {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.BOOLEAN)
        attributeBean.setDescription(description)
        return  attributeBean
    }

    /**
     * Get a Double attribute
     * @param name Name of attribute
     * @param description Description of attribute
     * @return a new MutableObjectTypeAttributeBean
     */
    static MutableObjectTypeAttributeBean getDouble(String name, String description = "") {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.DOUBLE)
        attributeBean.setDescription(description)
        return  attributeBean
    }

    /**
     * Get a Date attribute
     * @param name Name of attribute
     * @param description Description of attribute
     * @return a new MutableObjectTypeAttributeBean
     */
    static MutableObjectTypeAttributeBean getDate(String name, String description = "") {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.DATE)
        attributeBean.setDescription(description)
        return  attributeBean
    }

    /**
     * Get a Time attribute
     * @param name Name of attribute
     * @param description Description of attribute
     * @return a new MutableObjectTypeAttributeBean
     */
    static MutableObjectTypeAttributeBean getTime(String name, String description = "") {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.TIME)
        attributeBean.setDescription(description)
        return  attributeBean
    }

    /**
     * Get a Date_time attribute
     * @param name Name of attribute
     * @param description Description of attribute
     * @return a new MutableObjectTypeAttributeBean
     */
    static MutableObjectTypeAttributeBean getDate_time(String name, String description = "") {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.DATE_TIME)
        attributeBean.setDescription(description)
        return  attributeBean
    }

    /**
     * Get a Email attribute
     * @param name Name of attribute
     * @param description Description of attribute
     * @return a new MutableObjectTypeAttributeBean
     */
    static MutableObjectTypeAttributeBean getEmail(String name, String description = "") {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.EMAIL)
        attributeBean.setDescription(description)
        return  attributeBean
    }

    /**
     * Get a Textarea attribute
     * @param name Name of attribute
     * @param description Description of attribute
     * @return a new MutableObjectTypeAttributeBean
     */
    static MutableObjectTypeAttributeBean getTextarea(String name, String description = "") {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.TEXTAREA)
        attributeBean.setDescription(description)
        return  attributeBean
    }

    /**
     * Get a Ipaddress attribute
     * @param name Name of attribute
     * @param description Description of attribute
     * @return a new MutableObjectTypeAttributeBean
     */
    static MutableObjectTypeAttributeBean getIpaddress(String name, String description = "") {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.IPADDRESS)
        attributeBean.setDescription(description)
        return  attributeBean
    }

    /**
     * Get a Text attribute
     * @param name Name of attribute
     * @param description Description of attribute
     * @param isLabel set to true if this should be the label of the objectType
     * @return
     */    static MutableObjectTypeAttributeBean getText(String name, String description = "",  boolean isLabel = false) {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.TEXT)
        attributeBean.setDescription(description)
        attributeBean.setLabel(isLabel)
        if (isLabel) {
            attributeBean.setIndexed(true)
        }
        return  attributeBean
    }

    static MutableObjectTypeAttributeBean getSelect(String name, String description = "",  ArrayList<String> options = []) {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.SELECT)
        attributeBean.setDescription(description)
        if (options.size()) {
            attributeBean.setOptions(options.join(","))
        }
        return  attributeBean
    }

    static MutableObjectTypeAttributeBean getUrl(String name, String description = "", boolean enablePing = false) {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.DEFAULT, DefaultType.URL)
        attributeBean.setDescription(description)
        attributeBean.setAdditionalValue(enablePing ? "ENABLED" : "DISABLED")
        return  attributeBean
    }


    static MutableObjectTypeAttributeBean getUser(String name, String description = "",  ArrayList<String>groupNames = [], boolean showOnProfile = true) {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.USER, DefaultType.NONE)
        attributeBean.setDescription(description)
        groupNames ? attributeBean.setTypeValue(groupNames.join(",")) : null
        attributeBean.setAdditionalValue(showOnProfile ? "SHOW_PROFILE" : "HIDE_PROFILE")
        return  attributeBean
    }


    /**
     * Get a referenced object attribute type
     * @param name Name of the attribute
     * @param schemaId Id where the attribute is to be created
     * @param refObjectTypeId Id of the object type to reference
     * @param refName The name of the reference type (specified in the "Additional Value" column in the GUI)
     * @param description Attribute description
     * @return
     */
    static MutableObjectTypeAttributeBean getReferencedObject(String name, int schemaId, int refObjectTypeId, String refName,  String description = "") {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.REFERENCED_OBJECT, DefaultType.NONE)
        attributeBean.setDescription(description)
        attributeBean.setReferenceObjectTypeId(refObjectTypeId)
        ReferenceTypeBean refBean = configureFacade.findAllReferenceTypeBeans(schemaId).find {it.name == refName}
        attributeBean.setReferenceTypeBean(refBean)
        attributeBean.setIndexed(true)
        return  attributeBean
    }


    static MutableObjectTypeAttributeBean getGroup(String name, String description = "", boolean showOnProfile = true) {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.GROUP, DefaultType.NONE)
        attributeBean.setDescription(description)
        attributeBean.setAdditionalValue(showOnProfile ? "SHOW_PROFILE" : "HIDE_PROFILE")
        return  attributeBean
    }


    /**
     * Get a status attribute type
     * @param name Name of the attribute
     * @param schemaId Id where the attribute is to be created, only needed if supplying statusNames
     * @param statusNames List of statuses that should be available
     * @param description Attribute description
     * @return a new MutableObjectTypeAttributeBean
     */
    static MutableObjectTypeAttributeBean getStatus(String name,Integer schemaId = null, ArrayList<String> statusNames = [], String description = "") {
        MutableObjectTypeAttributeBean attributeBean = new MutableObjectTypeAttributeBean(name, Type.STATUS, DefaultType.NONE)
        attributeBean.setDescription(description)
        if (!statusNames.empty) {
            assert schemaId != null : "When creating a status attribute with statusNames an objectId must be supplied"
            ArrayList<String>statusIds = configureFacade.findAllStatusTypeBeans(schemaId).findAll {statusNames.contains(it.name)}.id.collect {it.toString()}
            assert statusIds.size() == statusNames.size() : "Error finding statuses for new status attributes, only found IDs:" + statusIds.join(",")
            attributeBean.setTypeValue(statusIds.join(","))

        }

        return attributeBean
    }



}
