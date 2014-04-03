package com.mygreenbill;

import com.mygreenbill.ssh.ConnectionHandler;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jawin.COMException;
import org.jawin.DispatchPtr;
import org.jawin.win32.Ole32;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.util.Properties;

/**
 * Created by ipeleg on 3/20/14.
 */
public class MailServerHandler implements IMailServerHandler
{
    //Create class logger
    private static final Logger LOGGER = Logger.getLogger(MailServerHandler.class);

    private Properties prop;
    private DispatchPtr hMailServerApp;
    private DispatchPtr domain; // Will hold the hMailServer domain object -> mygreenbill.com
    private DispatchPtr accounts; // Will hold the all the accounts from the domain object

    /**
     * Constructor for loading the properties file
     */
    public MailServerHandler()
    {
        try
        {
            prop = new Properties();
            prop.load(MailServerHandler.class.getResourceAsStream("/conf/configuration.properties")); // Load the file to the properties object
            LOGGER.info("MailServerHandler Object was created");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            LOGGER.error("IOException in MailServerHandler");
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * This function will prepare the hMailServer to communicate with the java code
     * and initializing the local variables domain and accounts
     */
    private void prepareMailServer()
    {
        try
        {
            Ole32.CoInitialize(); // Initialize the current thread with COM library
            hMailServerApp = new DispatchPtr(prop.getProperty("app_name"));
            hMailServerApp.invoke("Authenticate", prop.getProperty("hMailServer_user"), prop.getProperty("hMailServer_pass"));

            // Getting the Domains object
            DispatchPtr domains = (DispatchPtr) hMailServerApp.get("Domains");

            // Getting the Domain object
            domain = (DispatchPtr) domains.get("ItemByName", prop.getProperty("domain_name"));

            // Getting all the accounts of from Domain object
            accounts = (DispatchPtr) domain.get("Accounts");
        }
        catch (COMException e)
        {
            e.printStackTrace();
            LOGGER.error("COMException in prepareMailServer");
            LOGGER.error(e.getMessage());
        }

        LOGGER.info("hMailServer is ready, Domain and Accounts objects were set");
    }

    /**
     * Closing the connection with the hMailServer
     */
    private void closeMailServerConnection()
    {
        try
        {
            Ole32.CoUninitialize(); // Release the COM library
            LOGGER.info("hMailServer is released");
        }
        catch (COMException e)
        {
            e.printStackTrace();
            LOGGER.error("COMException in closeMailServerConnection");
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * The method will parse the given eml file content and return only the base64 string of the attachment
     * @param emlContent The EML file content as one long string
     * @return Returns only the attachment string
     */
    private String getAttachmentString(String emlContent)
    {
        String result = "";
        boolean foundAttachment = false;
        boolean foundLineBreak = false;

        String[] lines = emlContent.split(System.getProperty("line.separator"));
        for (int i=0 ; i<lines.length ; ++i)
        {
            // Remove all end of line characters
            lines[i] = lines[i].replaceAll("(\\r|\\n)", "");

            // If you foundAttachment && foundLineBreak are true we are now reading the base64 of the attachment
            if (foundAttachment && foundLineBreak)
            {
                if ((lines[i].isEmpty()))
                    break;

                result+=lines[i];
            }

            // If we encounter the "attachment" word we'll search for the next line break and then the attachment
            if (lines[i].contains("attachment"))
                foundAttachment = true;

            //  If we encountered the "attachment" already and the line is empty we are starting to read the base64 of the attachment
            if (foundAttachment && (lines[i].isEmpty()))
                foundLineBreak = true;
        }

        return result;
    }

    /**
     * For each new account creating the rule for copying every incoming message attachments to MySQL machine
     * @param accountName The account name
     */
    private void setCopyAttachmentToDbRule(String accountName)
    {
        try
        {
            DispatchPtr account = (DispatchPtr) accounts.get("ItemByAddress", accountName + "@" + prop.getProperty("domain_name"));
            DispatchPtr accountRules = (DispatchPtr) account.get("Rules");

            DispatchPtr newRule = (DispatchPtr) accountRules.invoke("Add");
            newRule.put("Name", "Copy attachment to DB"); // Set the new account password

            // Setting the criteria for the new rule, the rule will be activated for each incoming email
            DispatchPtr newRuleCriterias = (DispatchPtr) newRule.get("Criterias");
            DispatchPtr newRuleCriteria = (DispatchPtr) newRuleCriterias.invoke("Add");
            newRuleCriteria.put("PredefinedField", "6"); // Const eFTMessageSize = 6, the size of the message
            newRuleCriteria.put("MatchType", "4"); // Const eMTGreaterThan = 4
            newRuleCriteria.put("MatchValue", "0"); // The message size should be greater than 0

            newRuleCriteria.invoke("Save"); // Saving the new rule criterias

            // Setting the rule action
            DispatchPtr newRuleActions = (DispatchPtr) newRule.get("Actions");
            DispatchPtr newRuleAction = (DispatchPtr) newRuleActions.invoke("Add");
            newRuleAction.put("Type", "5"); // Const 5 = Run function
            newRuleAction.put("ScriptFunction", "OnIncomingMessage");

            newRuleAction.invoke("Save"); // Saving the new rule actions

            newRule.invoke("Save"); // Save the new rule

        }
        catch (COMException e)
        {
            LOGGER.error("COMException in setCopyAttachmentToDbRule");
            LOGGER.error(e.getMessage());
            return;
        }

        LOGGER.info("\"Copy attachment to DB\" rule was created for -> " + accountName);
    }

    /**
     * For each new account creating the folder in MySQL machine
     * @param accountName The account name
     */
    private void createAccountFolderInMysql(String accountName)
    {
        ConnectionHandler connectionHandler = new ConnectionHandler();
        connectionHandler.createConnection(prop.getProperty("mysql_username"), prop.getProperty("mysql_password"), prop.getProperty("mysql_ip"));
        connectionHandler.createNewFolder(prop.getProperty("mysql_path") + accountName);
        connectionHandler.closeConnection();

        LOGGER.info("New folder in MySQL machine was created for " + accountName);
    }

    @Override
    public boolean createNewAccount(String accountName, String password, String forwardAddress)
    {
        LOGGER.info("Request for creating new account");
        LOGGER.info("Account name -> " + accountName);
        LOGGER.info("Forward address -> " + forwardAddress);

        try
        {
            prepareMailServer(); // Connect to the hMailServer

            // Create new account and set the property
            DispatchPtr newAccount = (DispatchPtr) accounts.invoke("Add");

            newAccount.put("Address", accountName + "@" + prop.getProperty("domain_name")); // Set the new account address
            newAccount.put("Password", password); // Set the new account password
            newAccount.put("Active", true); // Activate the new account
            newAccount.put("MaxSize", 100); // Set the account size
            newAccount.put("ForwardAddress", forwardAddress); // Set the forward address
            newAccount.put("ForwardEnabled", true); // Set the forward feature enable
            newAccount.put("ForwardKeepOriginal", true); // Keep the original message in our local DB

            newAccount.invoke("Save"); // Save the new account

            setCopyAttachmentToDbRule(accountName); // Set the rule for copying files for each incoming mail
            createAccountFolderInMysql(accountName); // Create folder for the account on the MySQL machine

            closeMailServerConnection(); // Closing the connection with mail server
        }
        catch (COMException e)
        {
            LOGGER.error("COMException in createNewAccount");
            LOGGER.error(e.getMessage());
            return false;
        }

        LOGGER.info("New account was successfully created for -> " + accountName);
        return true;
    }

    @Override
    public boolean setForwardAddress(String accountName, String forwardAddress)
    {
        prepareMailServer(); // Connect to the hMailServer

        try
        {
            // Get the account for which new forward address needs to be set
            DispatchPtr account = (DispatchPtr) accounts.get("ItemByAddress", accountName + "@" + prop.getProperty("domain_name"));

            account.put("ForwardAddress", forwardAddress); // Set the new forward address
            account.invoke("Save"); // Save the new account
        }
        catch (COMException e)
        {
            LOGGER.error("COMException in setForwardAddress");
            LOGGER.error(e.getMessage());
            return false;
        }

        closeMailServerConnection(); // Closing the connection with mail server
        LOGGER.info("New forward address was set for: " + accountName + ", to: " + forwardAddress);
        return true;
    }

    @Override
    public boolean sendMessage(String toAddress, String subject, String messageBody)
    {
        final String username = prop.getProperty("donotreplay_user"); // The account from which we send mails to users
        final String pass = prop.getProperty("donotreplay_pass"); // The account password is in the properties file

        // The SMTP server properties
        Properties smtpProps = new Properties();
        smtpProps.put("mail.smtp.auth", "true");
        smtpProps.put("mail.smtp.starttls.enable", "true");
        smtpProps.put("mail.smtp.host", "mail.mygreenbill.com");
        smtpProps.put("mail.smtp.port", "25");

        // Creating a session with the SMTP server
        Session session = Session.getInstance(smtpProps,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, pass);
                    }
                });

        try
        {
            // Preparing the message for sending
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username + "@mygreenbill.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));

            message.setSubject(subject);
            message.setText(messageBody);

            Transport.send(message); // Sending the message
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
            LOGGER.error("MessagingException in sendMessage");
            LOGGER.error(e.getMessage());
            return false;
        }

        LOGGER.info("Message with subject " + subject + ", was sent to: " + toAddress);
        return true;
    }

    public void getAccountAllAttachments(String accountName)
    {
        prepareMailServer(); // Connect to the hMailServer

        try
        {
            // Get the account for which new forward address needs to be set
            DispatchPtr account = (DispatchPtr) accounts.get("ItemByAddress", accountName + "@" + prop.getProperty("domain_name"));

            // Get the account imap folder
            DispatchPtr imapFolders = (DispatchPtr) account.get("IMAPFolders");
            DispatchPtr imapFolder = (DispatchPtr) imapFolders.get("ItemByName", "Inbox");
            DispatchPtr messages = (DispatchPtr) imapFolder.get("Messages");

            // Get the total number of messages in the INBOX folder
            long numberOfMessages = Long.decode(String.valueOf(messages.get("Count")));
            for (long i=0 ; i < numberOfMessages ; ++i)
            {
                DispatchPtr message = (DispatchPtr) messages.get("Item", String.valueOf(i));

                // Get the attachments of the message
                DispatchPtr attachments = (DispatchPtr) message.get("Attachments");
                long numberOfAttachments = Long.decode(String.valueOf(attachments.get("Count")));

                // Check if the message contain attachments
                if (numberOfAttachments != 0)
                {
                    for (long t=0 ; t < numberOfAttachments ; ++t)
                    {
                        DispatchPtr attachment = (DispatchPtr) attachments.get("Item", String.valueOf(t));

                        String messageFileContent = IOUtils.toString(new FileInputStream(String.valueOf(message.get("Filename"))), "utf-8");
                        String attachmentString = getAttachmentString(messageFileContent);

                        // Get the attachment file type
                        String attachmentType = String.valueOf(attachment.get("Filename"));
                        attachmentType = attachmentType.substring(attachmentType.lastIndexOf(".") + 1);

                        // This just an example on how to convert base64 to pdf file
                        //////////////////////////////////////////////////////////////////////////////////
                        byte[] decodedBytes = DatatypeConverter.parseBase64Binary(attachmentString);

                        File file = new File("C:/Users/Administrator/Desktop/test." + attachmentType);
                        FileOutputStream fop = new FileOutputStream(file);

                        fop.write(decodedBytes);
                        fop.flush();
                        fop.close();
                        //////////////////////////////////////////////////////////////////////////////////
                    }
                }
                else
                {
                    LOGGER.info("The message " + message.get("Subject") + " from " + message.get("Date") + " does not contain any attachments");
                }
            }
        }
        catch (COMException e)
        {
            LOGGER.error("COMException in getAccountAllAttachments");
            LOGGER.error(e.getMessage());
        }
        catch (FileNotFoundException e)
        {
            LOGGER.error("FileNotFoundException in getAccountAllAttachments");
            LOGGER.error(e.getMessage());
        }
        catch (IOException e)
        {
            LOGGER.error("IOException in getAccountAllAttachments");
            LOGGER.error(e.getMessage());
        }

        closeMailServerConnection(); // Closing the connection with mail server
    }
}
