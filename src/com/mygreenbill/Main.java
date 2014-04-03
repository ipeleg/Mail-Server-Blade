package com.mygreenbill;

import com.mygreenbill.Exceptions.ConfigurationException;
import com.mygreenbill.Exceptions.InitException;
import com.mygreenbill.common.ConfigurationManager;
import com.mygreenbill.common.ConnectionManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by ipeleg on 3/20/14.
 */
public class Main
{
    public static void main(String[] args)
    {

        try
        {
            ConnectionManager connectionManager = ConnectionManager.getInstance();
        }
        catch (InitException e)
        {
            e.printStackTrace();
        }

        //IMailServerHandler obj = new MailServerHandler();
        //obj.createNewAccount("hanny", "1234", "barhanny@gmail.com");
        // obj.setForwardAddress("hanny", "hannybanister@gmail.com");
        // obj.sendMessage("ipeleg@hotmail.com", "Some subject", "Hello idan\n\nThis is a testing email");
        // obj.getAccountAllAttachments("ipeleg");
    }
}
