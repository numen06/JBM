/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.jbm.framework.cloud.auth.exception;

/**
 * @author wesley
 * @date 😴2017年12月21日20:44:38
 */
public class AuthCheckedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AuthCheckedException() {
    }

    public AuthCheckedException(String message) {
        super(message);
    }

    public AuthCheckedException(Throwable cause) {
        super(cause);
    }

    public AuthCheckedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthCheckedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
