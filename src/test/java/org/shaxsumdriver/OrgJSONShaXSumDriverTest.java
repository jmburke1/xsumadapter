/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Jason Burke
 */
package org.shaxsumdriver;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class OrgJSONShaXSumDriverTest {

    @Test
    void shouldTakeShaDeterministically() {
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
        ShaXSumDriver shaXSumDriver = new OrgJSONShaXSumDriver("SHA-256", testJson);
        String actualSha = shaXSumDriver.takeShaOfJsonObject();
        System.out.println("Actual Sha: " + actualSha);
        String expectedShaBasedOnThis =
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
        System.out.println("************************");
        System.out.println(testJson);
        System.out.println("************************");
        MessageDigest expectedMessageDigest = newDigestForUnitTest("SHA-256");
        expectedMessageDigest.update(expectedShaBasedOnThis.getBytes(StandardCharsets.UTF_8));
        String expectedSha = HexFormat.of().formatHex(expectedMessageDigest.digest());
        Assertions.assertEquals(expectedSha, actualSha);
    }

    @Test
    void shouldReturnCachedWhenTryToComputeTwice() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", "value");
        ShaXSumDriver shaXSumDriver = new OrgJSONShaXSumDriver("SHA-256", jsonObject);
        String sha = shaXSumDriver.takeShaOfJsonObject();
        String shouldEqualSha = shaXSumDriver.takeShaOfJsonObject();
        Assertions.assertEquals(shouldEqualSha, sha);
    }

    @Test
    void shouldExceptionWhenUnrecognizedHash() {
        boolean caught = false;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("key", "value");
            new OrgJSONShaXSumDriver("Funny-Hash", jsonObject);
        } catch(ShaXSumDriverException shxDex) {
            Assertions.assertEquals("Funny-Hash algorithm is not available", shxDex.getMessage());
            caught = true;
        }
        Assertions.assertTrue(caught);
    }

    private static MessageDigest newDigestForUnitTest(String algorithmName) {
        try {
            return MessageDigest.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException nsae) {
            throw new ShaXSumDriverException(String.format("%s algorithm is not available", algorithmName), nsae);
        }
    }
}
