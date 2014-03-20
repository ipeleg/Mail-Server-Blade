package com.mygreenbill;

/**
 * Created by ipeleg on 3/20/14.
 */
public class MailServerHandler implements IMailServerHandler
{
    @Override
    public boolean createNewAccount(String accountName, String password, String forwardAddress)
    {
        return false;
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
