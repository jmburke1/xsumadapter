/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Jason Burke
 */
package org.shaxsumdriver;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;

public class ShaXSumDriverTest {

    @Test
    void shouldTakeShaDeterministically() {
        // Build a complex nested JSON with various types
        JsonObject testJson = new JsonObject();
        testJson.addProperty("name", "John Doe");
        testJson.addProperty("emptyString", "");
        testJson.addProperty("age", 30);
        testJson.addProperty("isActive", true);
        testJson.addProperty("hadPremiumMembership", false);
        testJson.add("astrologicalSign", JsonNull.INSTANCE);
        JsonObject address;
        {
            address = new JsonObject();
            address.addProperty("street", "123 Main St");
            address.addProperty("city", "New York");
            address.addProperty("zipCode", "10001");
        }
        testJson.add("address", address);
        JsonArray hobbies;
        {
            hobbies = new JsonArray();
            hobbies.add("reading");
            hobbies.add("coding");
            JsonArray codingTypes;
            {
                codingTypes = new JsonArray();
                codingTypes.add("functional");
                codingTypes.add("objectOriented");
            }
            hobbies.add(codingTypes);
            hobbies.add("");
            hobbies.add(42);
        }
        testJson.add("hobbies", hobbies);
        JsonArray contacts;
        {
            contacts = new JsonArray();
            JsonObject contact1;
            {
                contact1 = new JsonObject();
                contact1.addProperty("type", "email");
                contact1.addProperty("value", "john@example.com");
            }
            JsonObject contact2;
            {
                contact2 = new JsonObject();
                contact2.add("value", JsonNull.INSTANCE);
                contact2.addProperty("type", "phone");
            }
            contacts.add(contact2);
            contacts.add(contact1);
        }
        testJson.add("contacts", contacts);
        testJson.add("affinity", new JsonObject());
        testJson.add("adjunct", new JsonArray());
        ShaXSumDriver shaXSumDriver = new ShaXSumDriver("SHA-256", testJson);
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
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("key", "value");
        ShaXSumDriver shaXSumDriver = new ShaXSumDriver("SHA-256", jsonObject);
        String sha = shaXSumDriver.takeShaOfJsonObject();
        String shouldEqualSha = shaXSumDriver.takeShaOfJsonObject();
        Assertions.assertEquals(shouldEqualSha, sha);
    }

    @Test
    void shouldExceptionWhenUnrecognizedHash() {
        boolean caught = false;
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("key", "value");
            new ShaXSumDriver("Funny-Hash", jsonObject);
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
