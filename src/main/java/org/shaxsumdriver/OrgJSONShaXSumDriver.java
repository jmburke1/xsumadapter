/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Jason Burke
 */
package org.shaxsumdriver;

import org.json.JSONArray;
import org.json.JSONObject;

public class OrgJSONShaXSumDriver extends GenericRecursiveShaXSumDriver<Object> {

    public OrgJSONShaXSumDriver(String algorithmName, Object takeShaOfThis) {
        super(algorithmName, takeShaOfThis);
    }

    @Override
    protected Object createNewJsonObject() {
        return new JSONObject();
    }

    @Override
    protected boolean shouldTreatLikeJsonObject(Object jsonElement) {
        return jsonElement instanceof JSONObject;
    }

    @Override
    protected boolean shouldTreatLikeJsonArray(Object jsonElement) {
        return jsonElement instanceof JSONArray;
    }

    @Override
    protected boolean shouldTreatLikePrimitiveOrJsonNull(Object jsonElement) {
        JSONObject dummyNullProperty = new JSONObject("{\"whatever\": null}");
        return
                jsonElement == null || dummyNullProperty.get("whatever").equals(jsonElement) ||
                jsonElement instanceof Boolean || jsonElement instanceof Character ||
                jsonElement instanceof Number || jsonElement instanceof String;
    }

    @Override
    protected String[] pullKeysFromJsonObject(Object jsonElement) {
        JSONObject obj = (JSONObject)jsonElement;
        String[] keys = new String[obj.keySet().size()];
        int keyCount = 0;
        for(String key : obj.keySet()) {
            keys[keyCount++] = key;
        }
        return keys;
    }

    @Override
    protected Object getBasedOnKey(String key, Object jsonElement) {
        return ((JSONObject)jsonElement).get(key);
    }

    @Override
    protected void removeBasedOnKey(String key, Object jsonElement) {
        ((JSONObject)jsonElement).remove(key);
    }

    @Override
    protected void addNewJsonObjectToJsonObject(Object currentSingletonPath, String key) {
        ((JSONObject)currentSingletonPath).put(key, new JSONObject());
    }

    @Override
    protected void addNewJsonArrayToJsonObject(Object currentSingletonPath, String key) {
        ((JSONObject)currentSingletonPath).put(key, new JSONArray());
    }

    @Override
    protected void addPrimitiveOrJsonNullToJsonObject(Object currentSingletonPath, String key, Object value) {
        ((JSONObject)currentSingletonPath).put(key, value);
    }

    @Override
    protected int getArraySize(Object element) {
        return ((JSONArray)element).length();
    }

    @Override
    protected Object getBasedOnIndex(int index, Object jsonElement) {
        return ((JSONArray)jsonElement).get(index);
    }

    @Override
    protected void addNewJsonObjectToJsonArray(Object currentSingletonPath) {
        ((JSONArray)currentSingletonPath).put(new JSONObject());
    }

    @Override
    protected void addNewJsonArrayToJsonArray(Object currentSingletonPath) {
        ((JSONArray)currentSingletonPath).put(new JSONArray());
    }

    @Override
    protected void addPrimitiveOrJsonNullToJsonArray(Object currentSingletonPath, Object value) {
        ((JSONArray)currentSingletonPath).put(value);
    }

    @Override
    protected void removeZerothItem(Object jsonElement) {
        ((JSONArray)jsonElement).remove(0);
    }

    @Override
    protected String getAsMinifiedString(Object jsonElement) {
        return jsonElement.toString();
    }
}
