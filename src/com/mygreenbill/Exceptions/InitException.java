package com.mygreenbill.Exceptions;

/**
 * Created by Jacob on 3/22/14.
 */
public class InitException extends Exception
{
	private static final long serialVersionUID = 1L;

	public InitException(String message)
    {
        super(message);
    }

    public InitException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
