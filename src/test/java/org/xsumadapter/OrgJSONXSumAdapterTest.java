/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Jason Burke
 */
package org.xsumadapter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class OrgJSONXSumAdapterTest {

    @Test
    void shouldTakeHashDeterministically() {
        // Build a complex nested JSON with various types
        JSONObject testJson = new JSONObject();
        testJson.put("name", "John Doe");
        testJson.put("emptyString", "");
        testJson.put("age", 30);
        testJson.put("isActive", true);
        testJson.put("hadPremiumMembership", false);
        testJson.put("astrologicalSign", "CROATOAN");
        JSONObject address;
        {
            address = new JSONObject();
            address.put("street", "123 Main St");
            address.put("city", "New York");
            address.put("zipCode", "10001");
        }
        testJson.put("address", address);
        JSONArray hobbies;
        {
            hobbies = new JSONArray();
            hobbies.put("reading");
            hobbies.put("coding");
            JSONArray codingTypes;
            {
                codingTypes = new JSONArray();
                codingTypes.put("functional");
                codingTypes.put("objectOriented");
            }
            hobbies.put(codingTypes);
            hobbies.put("");
            hobbies.put(42);
        }
        testJson.put("hobbies", hobbies);
        JSONArray contacts;
        {
            contacts = new JSONArray();
            JSONObject contact1;
            {
                contact1 = new JSONObject();
                contact1.put("type", "email");
                contact1.put("value", "john@example.com");
            }
            JSONObject contact2;
            {
                contact2 = new JSONObject();
                contact2.put("value", "CROATOAN");
                contact2.put("type", "phone");
            }
            contacts.put(contact2);
            contacts.put(contact1);
        }
        testJson.put("contacts", contacts);
        testJson.put("affinity", new JSONObject());
        testJson.put("adjunct", new JSONArray());
        testJson = new JSONObject(testJson.toString().replace("\"CROATOAN\"", "null"));
        XSumAdapter xSumAdapter = new OrgJSONXSumAdapter("SHA-256", testJson);
        String actualHash = xSumAdapter.takeHashOfJsonObject();
        String expectedHashBasedOnThis =
                "{\"address\":{\"city\":\"New York\"}}" +
                "{\"address\":{\"street\":\"123 Main St\"}}" +
                "{\"address\":{\"zipCode\":\"10001\"}}" +
                "{\"adjunct\":[]}" +
                "{\"affinity\":{}}" +
                "{\"age\":30}" +
                "{\"astrologicalSign\":null}" +
                "{\"contacts\":[{\"type\":\"phone\"}]}" +
                "{\"contacts\":[{\"value\":null}]}" +
                "{\"contacts\":[{\"type\":\"email\"}]}" +
                "{\"contacts\":[{\"value\":\"john@example.com\"}]}" +
                "{\"emptyString\":\"\"}" +
                "{\"hadPremiumMembership\":false}" +
                "{\"hobbies\":[\"reading\"]}" +
                "{\"hobbies\":[\"coding\"]}" +
                "{\"hobbies\":[[\"functional\"]]}" +
                "{\"hobbies\":[[\"objectOriented\"]]}" +
                "{\"hobbies\":[\"\"]}" +
                "{\"hobbies\":[42]}" +
                "{\"isActive\":true}" +
                "{\"name\":\"John Doe\"}";
        MessageDigest expectedMessageDigest = newDigestForUnitTest("SHA-256");
        expectedMessageDigest.update(expectedHashBasedOnThis.getBytes(StandardCharsets.UTF_8));
        String expectedHash = HexFormat.of().formatHex(expectedMessageDigest.digest());
        Assertions.assertEquals(expectedHash, actualHash);
    }

    @Test
    void shouldReturnCachedWhenTryToComputeTwice() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", "value");
        XSumAdapter xSumAdapter = new OrgJSONXSumAdapter("SHA-256", jsonObject);
        String hash = xSumAdapter.takeHashOfJsonObject();
        String shouldEqualHash = xSumAdapter.takeHashOfJsonObject();
        Assertions.assertEquals(shouldEqualHash, hash);
    }

    @Test
    void shouldExceptionWhenUnrecognizedHash() {
        boolean caught = false;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("key", "value");
            new OrgJSONXSumAdapter("Funny-Hash", jsonObject);
        } catch(XSumAdapterException xAdEx) {
            Assertions.assertEquals("Funny-Hash algorithm is not available", xAdEx.getMessage());
            caught = true;
        }
        Assertions.assertTrue(caught);
    }

    private static MessageDigest newDigestForUnitTest(String algorithmName) {
        try {
            return MessageDigest.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException nsae) {
            throw new XSumAdapterException(String.format("%s algorithm is not available", algorithmName), nsae);
        }
    }
}