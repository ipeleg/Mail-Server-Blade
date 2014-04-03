package com.mygreenbill.Exceptions;

/**
 * Created by Jacob on 3/29/14.
 */
public class DatabaseException extends Exception
{
    public DatabaseException(String message)
    {
        super(message);
    }

    public DatabaseException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
