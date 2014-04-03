package com.mygreenbill.Exceptions;

/**
 * Created by Jacob on 3/18/14.
 */
public class ConfigurationException extends Exception
{

    public ConfigurationException(String message)
    {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
