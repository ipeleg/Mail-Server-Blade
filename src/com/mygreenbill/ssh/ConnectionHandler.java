package com.mygreenbill.ssh;

import com.jcraft.jsch.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ConnectionHandler
{
    //Create class logger
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(ConnectionHandler.class);

	private Session session;
	private Channel channel;
    private ChannelSftp channelSftp;
	
	public void closeConnection()
	{
        // Closing connection
        channelSftp.disconnect();
	    channel.disconnect();
	    session.disconnect();
        System.gc(); // Java BUG!!! this line must be added on order to delete files after copy with output stream

        LOGGER.info("Connection was closed");
	}

	/**
	 * Method for creating an SSH connection to a remote computer
	 */
	public void createConnection(String username, String password, String host)
	{
		int port = 22; // Default SSH port
		JSch shell = new JSch();
		
		try
		{
			session = shell.getSession(username, host, port);
			
			// set user password and connect to a channel
	        session.setUserInfo(new SSHUserInfo(password));
	        session.connect();
	        channel = session.openChannel("sftp");
	        channel.connect();
            channelSftp = (ChannelSftp) channel;
            LOGGER.info("Connection to " + host + " was established");
		}
		catch (JSchException e) 
		{
            LOGGER.error("JSchException in createConnection");
            LOGGER.error(e.getMessage());
		}
    }

    public void createNewFolder(String folderPath)
    {
        try
        {
            channelSftp.mkdir(folderPath);
            LOGGER.info("New folder was created " + folderPath);
        }
        catch (SftpException e)
        {
            LOGGER.error("SftpException in createNewFolder");
            LOGGER.error(e.getMessage());
        }
    }

    public void changeFolderOnRemote(String folderPath)
    {
        try
        {
            LOGGER.info("Changing folder to " + folderPath);
            channelSftp.cd(folderPath);
        }
        catch (SftpException e)
        {
            LOGGER.error("SftpException in changeFolderOnRemote");
            LOGGER.error(e.getMessage());
        }
    }

    public void copyFileToRemote(File fileToCopy)
    {
        try
        {
            LOGGER.info("Copying file " + fileToCopy.getName());
            channelSftp.put(new FileInputStream(fileToCopy), fileToCopy.getName());
        }
        catch (SftpException e)
        {
            LOGGER.error("SftpException in copyFileToRemote");
            LOGGER.error(e.getMessage());
        }
        catch (FileNotFoundException e)
        {
            LOGGER.error("FileNotFoundException in copyFileToRemote");
            LOGGER.error(e.getMessage());
        }
    }
}
