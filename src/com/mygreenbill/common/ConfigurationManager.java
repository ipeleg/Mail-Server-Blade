package com.mygreenbill.common;

import com.mygreenbill.Exceptions.ConfigurationException;
import org.apache.log4j.Logger;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by Jacob on 3/18/14.
 */
public class ConfigurationManager
{
    private static ConfigurationManager instance = null;
    private final Logger LOGGER = Logger.getLogger(ConfigurationManager.class);
    private Properties properties = new Properties();

    public static ConfigurationManager getInstance() throws ConfigurationException
    {
        if (instance == null)
            instance = new ConfigurationManager();
        return instance;
    }

    private ConfigurationManager() throws ConfigurationException
    {
        try
        {
            //properties.load(ConfigurationManager.class.getResourceAsStream("/conf/configuration.properties"));
            properties.load(new FileInputStream("/Users/ipeleg/Sites/Mail-Server-Blade/conf/configuration.properties"));
        }
        catch (FileNotFoundException e)
        {
            LOGGER.error("Cannot open configuration file", e);
            throw new ConfigurationException(e.getMessage(), e.getCause());
        }
        catch (IOException e)
        {
            LOGGER.error("Cannot load configuration file from input stream", e);
            throw new ConfigurationException(e.getMessage(), e.getCause());
        }
    }

    /**
     * return the value of the property, if not exists return the default value
     * @param key   the key for the requested property
     * @param defaultValue  default value to return in case the property doesn't exists
     * @return  the property value
     */
    public String getValueWithDefault(String key, String defaultValue)
    {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * return the value of the property, if not exists return the default value
     * @param key   the key for the requested property
     * @param defaultValue  default value to return in case the property doesn't exists
     * @return  the property value
     */
    public int getValueWithDefault(String key, int defaultValue)
    {
        String val = properties.getProperty(key, String.valueOf(defaultValue));
        int toReturn;
        try
        {
            toReturn = Integer.parseInt(val);
        }
        catch (NumberFormatException e)
        {
            LOGGER.debug("Unable to parse value: " + val + " Exception message: " + e.getMessage()
                    + " Using default value: " + defaultValue);
            toReturn = defaultValue;
        }
        return toReturn;

    }

    public String getProperty(String key)
    {
        return properties.getProperty(key);
    }
}
