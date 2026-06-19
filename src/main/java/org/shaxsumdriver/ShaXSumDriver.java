/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Jason Burke
 */
package org.shaxsumdriver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

public class ShaXSumDriver {
    private final JsonObject takingShaOfThis;
    private JsonElement topLevelSingletonPath;
    private final MessageDigest messageDigest;
    private String cached;

    public ShaXSumDriver(String algorithmName, JsonObject takeShaOfThis) {
        cached = null;
        messageDigest = newDigest(algorithmName);
        takingShaOfThis = takeShaOfThis;
    }

    /**
     * Recursively visits a JsonElement and incrementally computes a message digest
     */
    /* Note: This traversal intentionally mutates a shared in-progress JsonElement tree.
     * Keep execution single-threaded; do not parallelize this with ForkJoinPool/parallel streams.
     */
    public String takeShaOfJsonObject() {
        if(cached != null) {
            return cached;
        }
        topLevelSingletonPath = new JsonObject();
        traverse(takingShaOfThis, topLevelSingletonPath);
        cached = HexFormat.of().formatHex(messageDigest.digest());
        return cached;
    }

    private void traverse(JsonElement element, JsonElement currentSingletonPath) {
        if(element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            JsonObject singletonPathAsObj = currentSingletonPath.getAsJsonObject();
            String[] sorted = new String[obj.keySet().size()];
            int keyCount = 0;
            for(String key : obj.keySet()) {
                sorted[keyCount++] = key;
            }
            Arrays.sort(sorted);
            for(String key : sorted) {
                JsonElement value = obj.get(key);
                if(value.isJsonObject()) {
                    singletonPathAsObj.add(key, new JsonObject());
                } else if(value.isJsonArray()) {
                    singletonPathAsObj.add(key, new JsonArray());
                } else if(value.isJsonPrimitive() || value.isJsonNull()) {
                    singletonPathAsObj.add(key, value);
                }
                traverse(value, singletonPathAsObj.get(key));
                singletonPathAsObj.remove(key);
            }
            if(sorted.length == 0) {
                messageDigest.update(topLevelSingletonPath.toString().getBytes(StandardCharsets.UTF_8));
            }
        } else if(element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            JsonArray singletonPathAsArr = currentSingletonPath.getAsJsonArray();
            for(int i = 0; i < arr.size(); i++) {
                JsonElement item = arr.get(i);
                if(item.isJsonObject()) {
                    singletonPathAsArr.add(new JsonObject());
                } else if(item.isJsonArray()) {
                    singletonPathAsArr.add(new JsonArray());
                } else if(item.isJsonPrimitive() || item.isJsonNull()) {
                    singletonPathAsArr.add(item);
                }
                traverse(item, singletonPathAsArr.get(0));
                singletonPathAsArr.remove(0);
            }
            if(arr.size() == 0) {
                messageDigest.update(topLevelSingletonPath.toString().getBytes(StandardCharsets.UTF_8));
            }
        } else {
            messageDigest.update(topLevelSingletonPath.toString().getBytes(StandardCharsets.UTF_8));
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
