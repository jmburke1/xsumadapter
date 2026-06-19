/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Jason Burke
 */
package org.xsumadapter;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

public abstract class GenericRecursiveXSumAdapter<Q> implements XSumAdapter {
    private final Q takingShaOfThis;
    private Q topLevelSingletonPath;
    private final MessageDigest messageDigest;
    private String cached;

    public GenericRecursiveXSumAdapter(String algorithmName, Q takeShaOfThis) {
        cached = null;
        messageDigest = newDigest(algorithmName);
        takingShaOfThis = takeShaOfThis;
    }

    /**
     * Recursively visits the JSON tree and incrementally computes a message digest.
     *
     * <p>Maintains a "singleton path" invariant: at each recursion depth, currentSingletonPath
     * contains exactly one element per level - mirroring the current position in the input tree.
     * The topLevelSingletonPath is the root of this singleton path; currentSingletonPath is always
     * a descendant reference within it. Modifications to currentSingletonPath (add/remove) propagate
     * up to topLevelSingletonPath because they are the same object graph.</p>
     *
     * <p>Keep execution single-threaded; do not parallelize this with ForkJoinPool/parallel streams or
     * any other form of java multithreading.</p>
     */
    public String takeHashOfJsonObject() {
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

    /**
     * Removes a key from the singleton path after its subtree has been fully traversed.
     * This maintains the invariant that currentSingletonPath contains exactly one element per level.
     */
    protected abstract void removeFromSingletonPathByKey(String key, Q q);

    protected abstract void addNewJsonObjectToJsonObject(Q currentSingletonPath, String key);

    protected abstract void addNewJsonArrayToJsonObject(Q currentSingletonPath, String key);

    protected abstract void addPrimitiveOrJsonNullToJsonObject(Q currentSingletonPath, String key, Q value);

    protected abstract int getArraySize(Q element);

    protected abstract Q getBasedOnIndex(int index, Q q);

    protected abstract void addNewJsonObjectToJsonArray(Q currentSingletonPath);

    protected abstract void addNewJsonArrayToJsonArray(Q currentSingletonPath);

    protected abstract void addPrimitiveOrJsonNullToJsonArray(Q currentSingletonPath, Q value);

    /**
     * Removes the zeroth item from the singleton path's current array after its element has been fully traversed.
     * This maintains the invariant that the singleton path contains exactly one element per level.
     */
    protected abstract void removeFromSingletonPathArray(Q q);

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
                removeFromSingletonPathByKey(key, currentSingletonPath);
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
                removeFromSingletonPathArray(currentSingletonPath);
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
            throw new XSumAdapterException(String.format("%s algorithm is not available", algorithmName), nsae);
        }
    }
}