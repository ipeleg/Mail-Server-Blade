package com.mygreenbill;

import org.apache.log4j.Logger;
import org.jawin.COMException;
import org.jawin.DispatchPtr;
import org.jawin.win32.Ole32;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by ipeleg on 3/20/14.
 */
public class MailServerHandler implements IMailServerHandler
{
    //Logger
    private static final Logger LOGGER = Logger.getLogger(MailServerHandler.class);

    private Properties prop = new Properties();

    /**
     * Constructor for loading the properties file
     */
    public MailServerHandler()
    {
        try
        {
            prop.load(MailServerHandler.class.getResourceAsStream("/configuration.properties")); // Load the file to the properties object
            LOGGER.info("MailServerHandler Object was created");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            LOGGER.error("IOException in MailServerHandler");
        }
    }

    @Override
    public boolean createNewAccount(String accountName, String password, String forwardAddress)
    {
        LOGGER.info("Request for creating new account");
        LOGGER.info("New account name -> " + accountName);
        LOGGER.info("New forward address -> " + forwardAddress);

        try
        {
            Ole32.CoInitialize(); // Initialize the current thread with COM library
            DispatchPtr app = new DispatchPtr(prop.getProperty("app_name"));
            DispatchPtr obje = (DispatchPtr) app.invoke("Authenticate", prop.getProperty("hMailServer_user"), prop.getProperty("hMailServer_pass"));

            // Getting the Domains object
            DispatchPtr domains = (DispatchPtr) app.get("Domains");

            // Getting the Domain object
            DispatchPtr domain = (DispatchPtr) domains.get("ItemByName", prop.getProperty("domain_name"));

            // Getting all the accounts of from Domain object
            DispatchPtr accounts = (DispatchPtr) domain.get("Accounts");

            // Create new account and set the property
            DispatchPtr newAccount = (DispatchPtr) accounts.invoke("Add");

            newAccount.put("Address", accountName + "@" + prop.getProperty("domain_name")); // Set the new account address
            newAccount.put("Password", password); // Set the new account password
            newAccount.put("Active", true); // Activate the new account
            newAccount.put("MaxSize", 100); // Set the account size
            newAccount.put("ForwardAddress", forwardAddress); // Set the address to forward to
            newAccount.put("ForwardEnabled", true); // Set the forward feature enable
            newAccount.put("ForwardKeepOriginal", true); // Keep the original message in our local DB

            newAccount.invoke("Save"); // Save the new account

            Ole32.CoUninitialize(); // Release the COM library
        }
        catch (COMException e)
        {
            e.printStackTrace();
            LOGGER.error("COMException in createNewAccount");
            return false;
        }

        LOGGER.info("New account was successfully created for -> " + accountName);
        return true;
    }

    @Override
    public void setForwardAddress(String accountName, String forwardAddress)
    {

    }

    @Override
    public boolean sendMessage(String toAddress, String message)
    {
        return false;
    }
}
