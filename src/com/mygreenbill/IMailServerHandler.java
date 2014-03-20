package com.mygreenbill;

/**
 * Created by ipeleg on 3/20/14.
 */
public interface IMailServerHandler
{
    public boolean createNewAccount(String accountName, String password, String forwardAddress);
    public void setForwardAddress(String accountName, String forwardAddress);
    public boolean sendMessage(String toAddress, String message);
}
