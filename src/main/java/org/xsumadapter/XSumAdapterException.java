/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Jason Burke
 */
package org.xsumadapter;

public class XSumAdapterException extends RuntimeException {
    public XSumAdapterException(String cause, Throwable throwable) {
        super(cause, throwable);
    }
    public XSumAdapterException(String cause) {
        super(cause);
    }
}