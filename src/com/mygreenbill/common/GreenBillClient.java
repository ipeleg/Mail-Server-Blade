package com.mygreenbill.common;

/**
 * Created by Jacob on 3/28/14.
 */
public interface GreenBillClient extends Runnable
{
    public void sendMessage(RequestJson message);
}
