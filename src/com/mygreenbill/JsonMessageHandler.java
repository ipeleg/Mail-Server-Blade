package com.mygreenbill;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.mygreenbill.Exceptions.InitException;
import com.mygreenbill.common.ConnectionManager;
import com.mygreenbill.security.EncryptionType;
import com.mygreenbill.security.EncryptionUtil;

public class JsonMessageHandler
{
    public enum MessageType {ADD_USER};
    
    private final Logger LOGGER = Logger.getLogger(JsonMessageHandler.class); 
    private IMailServerHandler mailServerHandler;
    
    public JsonMessageHandler()
    {
    	mailServerHandler = new MailServerHandler();
    }
    
    /**
     * Processing JSON from an incoming message from the the Management-Blade
     * @param json The incoming JSON to parse
     */
    public void processJson(JSONObject json)
    {
    	// If the JSON is NULL return
        if (json == null)
            return;

        try
        {
        	ConnectionManager connectionManager = ConnectionManager.getInstance();
            JSONObject innerJson = json.getJSONObject("Message"); // Getting the inner JSON object
            int id = innerJson.getInt("messageID"); // Getting the message ID
            String messageMD5 = EncryptionUtil.encryptString(innerJson.toString(), EncryptionType.MD5); // Checking the MD5 of the incoming message

            if (messageMD5.equals(json.getString("CheckSum")))
            {
                JSONObject ackJson = new JSONObject("{messageID: "+ id +", MessageType: ACK}");
                LOGGER.info("Sending ACK on message ID: " + id);
                connectionManager.sendToTrafficBlade(ackJson); // Sending back the ACK json the the management blade
                
                String messageType = innerJson.getString("MessageType"); // Getting the message
                
                switch (MessageType.valueOf(messageType))
                {
                	case ADD_USER:
                		addNewAccount(innerJson.getString("forwardAddress"), innerJson.getString("password"));
                		break;
                }
            }
            else
            {
                LOGGER.info("New Message received from management blade but the MD5 are not equal");
            }
        }
        catch (JSONException e)
        {
            LOGGER.error("JSONException Error in processJson: " + e.getMessage());
        }
        catch (InitException e)
		{
        	LOGGER.error("InitException Error in processJson: " + e.getMessage());
		}
    }
    
    /**
     * Creating new account in the hMailServer
     * @param email The email of the new account to which email will be forwarded
     * @param password The new account password
     */
    public void addNewAccount(String email, String password)
    {
    	// Creating MD5 hash from the new account email address
    	String emailMD5 = EncryptionUtil.encryptString(email, EncryptionType.MD5);
    	
    	mailServerHandler.createNewAccount(emailMD5, password, email);
    }
}
