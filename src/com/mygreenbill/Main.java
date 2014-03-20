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
        Properties prop = new Properties();
        InputStream input = null;

        try
        {
            input = new FileInputStream("conf/configuration.properties");
            prop.load(input);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                input.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        System.out.println(prop.getProperty("app_name"));
    }
}
