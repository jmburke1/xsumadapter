/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Jason Burke
 */
package org.shaxsumdriver;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

public abstract class GenericRecursiveShaXSumDriver<Q> implements ShaXSumDriver {
    private final Q takingShaOfThis;
    private Q topLevelSingletonPath;
    private final MessageDigest messageDigest;
    private String cached;

    public GenericRecursiveShaXSumDriver(String algorithmName, Q takeShaOfThis) {
        cached = null;
        messageDigest = newDigest(algorithmName);
        takingShaOfThis = takeShaOfThis;
    }

    /**
     * Recursively visits a Q and incrementally computes a message digest
     */
    /* Note: This traversal intentionally mutates a shared in-progress Q tree.
     * Keep execution single-threaded; do not parallelize this with ForkJoinPool/parallel streams.
     */
    public String takeShaOfJsonObject() {
        if(cached != null) {
            return cached;
        }
        topLevelSingletonPath = createNewJsonObject();
        traverse(takingShaOfThis, topLevelSingletonPath);
        cached = HexFormat.of().formatHex(messageDigest.digest());
        return cached;
    }

    protected abstract Q createNewJsonObject();

    protected abstract boolean shouldTreatLikeJsonObject(Q q);

    protected abstract boolean shouldTreatLikeJsonArray(Q q);

    protected abstract boolean shouldTreatLikePrimitiveOrJsonNull(Q q);

    protected abstract String[] pullKeysFromJsonObject(Q q);

    protected abstract Q getBasedOnKey(String key, Q q);

    protected abstract void removeBasedOnKey(String key, Q q);

    protected abstract void addNewJsonObjectToJsonObject(Q currentSingletonPath, String key);

    protected abstract void addNewJsonArrayToJsonObject(Q currentSingletonPath, String key);

    protected abstract void addPrimitiveOrJsonNullToJsonObject(Q currentSingletonPath, String key, Q value);

    protected abstract int getArraySize(Q element);

    protected abstract Q getBasedOnIndex(int index, Q q);

    protected abstract void addNewJsonObjectToJsonArray(Q currentSingletonPath);

    protected abstract void addNewJsonArrayToJsonArray(Q currentSingletonPath);

    protected abstract void addPrimitiveOrJsonNullToJsonArray(Q currentSingletonPath, Q value);

    protected abstract void removeZerothItem(Q q);

    protected abstract String getAsMinifiedString(Q q);

    private void traverse(Q element, Q currentSingletonPath) {
        if(shouldTreatLikeJsonObject(element)) {
            String[] sorted = pullKeysFromJsonObject(element);
            Arrays.sort(sorted);
            for(String key : sorted) {
                Q value = getBasedOnKey(key, element);
                if(shouldTreatLikeJsonObject(value)) {
                    addNewJsonObjectToJsonObject(currentSingletonPath, key);
                } else if(shouldTreatLikeJsonArray(value)) {
                    addNewJsonArrayToJsonObject(currentSingletonPath, key);
                } else if(shouldTreatLikePrimitiveOrJsonNull(value)) {
                    addPrimitiveOrJsonNullToJsonObject(currentSingletonPath, key, value);
                }
                traverse(value, getBasedOnKey(key, currentSingletonPath));
                removeBasedOnKey(key, currentSingletonPath);
            }
            if(sorted.length == 0) {
                messageDigest.update(getAsMinifiedString(topLevelSingletonPath).getBytes(StandardCharsets.UTF_8));
            }
        } else if(shouldTreatLikeJsonArray(element)) {
            int arrSize = getArraySize(element);
            for(int i = 0; i < arrSize; i++) {
                Q item = getBasedOnIndex(i, element);
                if(shouldTreatLikeJsonObject(item)) {
                    addNewJsonObjectToJsonArray(currentSingletonPath);
                } else if(shouldTreatLikeJsonArray(item)) {
                    addNewJsonArrayToJsonArray(currentSingletonPath);
                } else if(shouldTreatLikePrimitiveOrJsonNull(item)) {
                    addPrimitiveOrJsonNullToJsonArray(currentSingletonPath, item);
                }
                traverse(item, getBasedOnIndex(0, currentSingletonPath));
                removeZerothItem(currentSingletonPath);
            }
            if(arrSize == 0) {
                messageDigest.update(getAsMinifiedString(topLevelSingletonPath).getBytes(StandardCharsets.UTF_8));
            }
        } else {
            messageDigest.update(getAsMinifiedString(topLevelSingletonPath).getBytes(StandardCharsets.UTF_8));
        }
    }
    private static MessageDigest newDigest(String algorithmName) {
        try {
            return MessageDigest.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException nsae) {
            throw new ShaXSumDriverException(String.format("%s algorithm is not available", algorithmName), nsae);
        }
    }
}
