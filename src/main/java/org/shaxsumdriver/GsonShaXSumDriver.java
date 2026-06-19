/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Jason Burke
 */
package org.shaxsumdriver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GsonShaXSumDriver extends GenericRecursiveShaXSumDriver<JsonElement> {

    public GsonShaXSumDriver(String algorithmName, JsonElement takeShaOfThis) {
        super(algorithmName, takeShaOfThis);
    }

    @Override
    protected JsonElement createNewJsonObject() {
        return new JsonObject();
    }

    @Override
    protected boolean shouldTreatLikeJsonObject(JsonElement jsonElement) {
        return jsonElement.isJsonObject();
    }

    @Override
    protected boolean shouldTreatLikeJsonArray(JsonElement jsonElement) {
        return jsonElement.isJsonArray();
    }

    @Override
    protected boolean shouldTreatLikePrimitiveOrJsonNull(JsonElement jsonElement) {
        return jsonElement.isJsonPrimitive() || jsonElement.isJsonNull();
    }

    @Override
    protected String[] pullKeysFromJsonObject(JsonElement jsonElement) {
        JsonObject obj = jsonElement.getAsJsonObject();
        String[] keys = new String[obj.keySet().size()];
        int keyCount = 0;
        for(String key : obj.keySet()) {
            keys[keyCount++] = key;
        }
        return keys;
    }

    @Override
    protected JsonElement getBasedOnKey(String key, JsonElement jsonElement) {
        return jsonElement.getAsJsonObject().get(key);
    }

    @Override
    protected void removeBasedOnKey(String key, JsonElement jsonElement) {
        jsonElement.getAsJsonObject().remove(key);
    }

    @Override
    protected void addNewJsonObjectToJsonObject(JsonElement currentSingletonPath, String key) {
        currentSingletonPath.getAsJsonObject().add(key, new JsonObject());
    }

    @Override
    protected void addNewJsonArrayToJsonObject(JsonElement currentSingletonPath, String key) {
        currentSingletonPath.getAsJsonObject().add(key, new JsonArray());
    }

    @Override
    protected void addPrimitiveOrJsonNullToJsonObject(JsonElement currentSingletonPath, String key, JsonElement value) {
        currentSingletonPath.getAsJsonObject().add(key, value);
    }

    @Override
    protected int getArraySize(JsonElement element) {
        return element.getAsJsonArray().size();
    }

    @Override
    protected JsonElement getBasedOnIndex(int index, JsonElement jsonElement) {
        return jsonElement.getAsJsonArray().get(index);
    }

    @Override
    protected void addNewJsonObjectToJsonArray(JsonElement currentSingletonPath) {
        currentSingletonPath.getAsJsonArray().add(new JsonObject());
    }

    @Override
    protected void addNewJsonArrayToJsonArray(JsonElement currentSingletonPath) {
        currentSingletonPath.getAsJsonArray().add(new JsonArray());
    }

    @Override
    protected void addPrimitiveOrJsonNullToJsonArray(JsonElement currentSingletonPath, JsonElement value) {
        currentSingletonPath.getAsJsonArray().add(value);
    }

    @Override
    protected void removeZerothItem(JsonElement jsonElement) {
        jsonElement.getAsJsonArray().remove(0);
    }
}
