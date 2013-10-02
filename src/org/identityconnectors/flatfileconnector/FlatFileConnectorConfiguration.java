package org.identityconnectors.flatfileconnector;

import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.common.security.*;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.common.logging.Log;
import java.beans.Introspector;
import java.io.File;

public class FlatFileConnectorConfiguration extends AbstractConfiguration {
    private static final Log LOG = Log.getLog(FlatFileConnectorConfiguration.class);    
    private File targetFile = null;
    private String uniqueAttribute = null;    
    private File customScriptForProvisioning = null;
    private File customScriptForRecon = null;    
    private String hostName = null;
    private String userName = null;
    private GuardedString password = null;
    private File lookupReconFile = null;
    
    public void setLookupReconFile(File lookupReconFile) {
        System.out.println("lookupReconFile = "+lookupReconFile);
        this.lookupReconFile = lookupReconFile;
    }
    @ConfigurationProperty(order = 1, required = true,helpMessageKey = "configuration.lookupReconFile.help",
                           displayMessageKey = "configuration.lookupReconFile.display")
    public File getLookupReconFile() {
        return lookupReconFile;
    }
    public void setCustomScriptForRecon(File customScriptForRecon) {
        this.customScriptForRecon = customScriptForRecon;
    }
    @ConfigurationProperty(order = 1, required = false,helpMessageKey = "configuration.customscriptfileforrecon.help",
                           displayMessageKey = "configuration.customscriptfileforrecon.display")
    public File getCustomScriptForRecon() {
        return customScriptForRecon;
    }
    public void setCustomScriptForProvisioning(File customScriptForProvisioning) {
        this.customScriptForProvisioning = customScriptForProvisioning;
    }
    @ConfigurationProperty(order = 1, required = false,helpMessageKey = "configuration.customscriptfileforprovisioning.help",
                           displayMessageKey = "configuration.customscriptfileforprovisioning.display")
    public File getCustomScriptForProvisioning() {
        return customScriptForProvisioning;
    }    
    public void setTargetFile(File targetFile) {
        System.out.println("targetFile = "+targetFile);
        this.targetFile = targetFile;
    }
    @ConfigurationProperty(order = 1, required = true,helpMessageKey = "configuration.targetfile.help",
                           displayMessageKey = "configuration.targetfile.display")
    public File getTargetFile() {
        return targetFile;
    }
    public void setUniqueAttribute(String uniqueAttribute) {
        this.uniqueAttribute = uniqueAttribute;
    }    
    @ConfigurationProperty(required = true,helpMessageKey="configuration.uniqueattribute.help",
                           displayMessageKey="configuration.uniqueattribute.display")
    public String getUniqueAttribute() {
        return uniqueAttribute;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() {
        if(this.targetFile == null || this.uniqueAttribute == null){
            throw new ConfigurationException("Required parameters are null!!");
        }
        validateRequired();   
    }
    
    
    public FlatFileConnectorConfiguration() {
        // it's java bean
    }
    /**
     * 
     * Validates that all required properties are in place.
     * 
     * @throws ConfigurationException in case value of required property is missing.
     */
    private void validateRequired() {
        try {
            PropertyDescriptor[] propertyDescs = Introspector.getBeanInfo(this.getClass()).getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescs) {
                Method getter = propertyDescriptor.getReadMethod();
                assert getter != null;
                ConfigurationProperty annotation = getter.getAnnotation(ConfigurationProperty.class);
                if (annotation != null && annotation.required()) {
                    Object value = getter.invoke(this);
                    if (value == null) {
                        // missing required property
                        throw new ConfigurationException(getMessage("validate.missingPropertyValue", getMessage(annotation.displayMessageKey())));
                    }
                    else if (value instanceof String && StringUtil.isBlank((String)value)) {
                        // blank String
                        throw new ConfigurationException(getMessage("validate.missingPropertyValue", getMessage(annotation.displayMessageKey())));
                    }
                }
            }
        } catch (IntrospectionException ex) {
            // should not happen, just log it
            LOG.warn(ex, "Error when validating required properties.");
        } catch (IllegalAccessException ex) {
            // should not happen, just log it
            LOG.warn(ex, "Error when validating required properties.");
        } catch (InvocationTargetException ex) {
            // should not happen, just log it
            LOG.warn(ex, "Error when validating required properties.");
        }
    }


    /**
     * Returns localized message.
     *
     * @param key Message key.
     * @return Localized message.
     */
    public String getMessage(String key) {
        return getConnectorMessages().format(key, key);
    }

    /**
     * Returns localized message.
     *
     * @param key Message key.
     * @param objects Values to be set to message placeholders.
     * @return Localized message.
     */
    public String getMessage(String key, Object... objects) {
        return getConnectorMessages().format(key, key, objects);
    }


    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    @ConfigurationProperty(required = true,helpMessageKey="configuration.hostName.help",displayMessageKey="configuration.hostName.display")
    public String getHostName() {
        return hostName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    @ConfigurationProperty(required = true,helpMessageKey="configuration.userName.help",displayMessageKey="configuration.userName.display")
    public String getUserName() {
        return userName;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }

    @ConfigurationProperty(required = true,helpMessageKey="configuration.password.help",displayMessageKey="configuration.password.display")
    public GuardedString getPassword() {
        return password;
    }
}
