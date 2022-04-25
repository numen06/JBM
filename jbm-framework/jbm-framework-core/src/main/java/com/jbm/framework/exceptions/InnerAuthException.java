package com.jbm.framework.exceptions;

/**
 * 内部认证异常
 * 
 * @author wesley.zhang
 */
public class InnerAuthException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public InnerAuthException(String message)
    {
        super(message);
    }
}
