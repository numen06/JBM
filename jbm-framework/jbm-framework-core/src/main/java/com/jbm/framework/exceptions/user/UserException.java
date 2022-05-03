package com.jbm.framework.exceptions.user;

import com.jbm.framework.exceptions.base.BaseException;

/**
 * 用户信息异常类
 * 
 * @author wesley.zhang
 */
public class UserException extends BaseException
{
    private static final long serialVersionUID = 1L;

    public UserException(String code, Object[] args)
    {
        super("user", code, args, null);
    }
}
