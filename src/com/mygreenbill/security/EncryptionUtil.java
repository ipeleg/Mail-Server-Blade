package com.mygreenbill.security;

import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Jacob on 3/16/14.
 */
public class EncryptionUtil
{
    // to make sure no one is creating any instance of EncryptionUtil
    private EncryptionUtil(){}

    private static final Logger LOGGER = Logger.getLogger(EncryptionUtil.class);
    private static boolean isLogConfigured = false;

    public static String encryptString(String password, EncryptionType encryptionType)
    {
        configureLog();
        if (encryptionType == EncryptionType.MD5)
        {
            return encryptMD5(password);
        }
        return null;
    }

    private static void configureLog()
    {
        if (!isLogConfigured)
        {
            //PropertyConfigurator.configure("WEB-INF/log4j.properties");
            isLogConfigured = true;
        }
    }
    private static String encryptMD5(String pass)
    {
        if (pass == null || pass.isEmpty())
        {
            LOGGER.error("Try to encrypt null or empty string");
            return null;
        }

        String md5Password = null;
        MessageDigest md = null;
        try
        {
            md = MessageDigest.getInstance("MD5");
            md.update(pass.getBytes(), 0, pass.length());
            md5Password = new BigInteger(1, md.digest()).toString(16);
        } catch (NoSuchAlgorithmException e)
        {
            LOGGER.error("Try to encrypt with invalid algorithm", e);
            return null;
        }

        return md5Password;
    }


}
