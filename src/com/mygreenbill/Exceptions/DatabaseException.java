package com.mygreenbill.Exceptions;

/**
 * Created by Jacob on 3/29/14.
 */
public class DatabaseException extends Exception
{
	private static final long serialVersionUID = 1L;

	public DatabaseException(String message)
    {
        super(message);
    }

    public DatabaseException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
