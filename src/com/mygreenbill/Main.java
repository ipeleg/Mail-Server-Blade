package com.mygreenbill;

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
        IMailServerHandler obj = new MailServerHandler();
        //obj.createNewAccount("hanny", "1234", "barhanny@gmail.com");
        obj.setForwardAddress("hanny", "hannybanister@gmail.com");
    }
}
