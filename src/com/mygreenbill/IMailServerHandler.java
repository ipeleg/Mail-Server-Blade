package com.mygreenbill;

/**
 * Created by ipeleg on 3/20/14.
 */
public interface IMailServerHandler
{
    /**
     * The method will create a new account in the hMailServer and also activate the forwarding feature to
     * the given address
     * @param accountName
     * @param password
     * @param forwardAddress
     * @return
     */
    public boolean createNewAccount(String accountName, String password, String forwardAddress);

    /**
     * The method will set a new forward address to the given account
     * @param accountName
     * @param forwardAddress
     * @return
     */
    public boolean setForwardAddress(String accountName, String forwardAddress);

    /**
     *
     * @param toAddress
     * @param messageBody
     * @return
     */
    public boolean sendMessage(String toAddress, String subject, String messageBody);

    public void getAccountAllAttachments(String accountName);
}
