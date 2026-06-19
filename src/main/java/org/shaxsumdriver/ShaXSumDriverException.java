/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Jason Burke
 */
package org.shaxsumdriver;

public class ShaXSumDriverException extends RuntimeException {
    public ShaXSumDriverException(String cause, Throwable throwable) {
        super(cause, throwable);
    }
    public ShaXSumDriverException(String cause) {
        super(cause);
    }
}
