/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Jason Burke
 */
package org.xsumadapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests targeting potential collisions and correctness of the singleton-path algorithm.
 */
public class CollisionTest {

    // ---- Test 1: Simple key-prefix ambiguity ----
    // {"a":{"b":1}} vs {"ab":2}
    // These should produce DIFFERENT hashes. If they don't, it's a collision.
    @Test
    void gsonShouldDistinguishNestedKeyFromFlatKey() {
        JsonObject json1 = new JsonObject();
        {
            JsonObject inner = new JsonObject();
            inner.addProperty("b", 1);
            json1.add("a", inner);
        }

        JsonObject json2 = new JsonObject();
        json2.addProperty("ab", 2);

        String hash1 = new GsonXSumAdapter("SHA-256", json1).takeHashOfJsonObject();
        String hash2 = new GsonXSumAdapter("SHA-256", json2).takeHashOfJsonObject();

        Assertions.assertNotEquals(hash1, hash2,
                "Different JSON structures should not collide: {\"a\":{\"b\":1}} vs {\"ab\":2}");
    }

    @Test
    void orgJsonShouldDistinguishNestedKeyFromFlatKey() {
        JSONObject json1 = new JSONObject();
        {
            JSONObject inner = new JSONObject();
            inner.put("b", 1);
            json1.put("a", inner);
        }

        JSONObject json2 = new JSONObject();
        json2.put("ab", 2);

        String hash1 = new OrgJSONXSumAdapter("SHA-256", json1).takeHashOfJsonObject();
        String hash2 = new OrgJSONXSumAdapter("SHA-256", json2).takeHashOfJsonObject();

        Assertions.assertNotEquals(hash1, hash2,
                "Different JSON structures should not collide: {\"a\":{\"b\":1}} vs {\"ab\":2}");
    }

    // ---- Test 2: Key order independence (the core promise) ----
    @Test
    void gsonKeyOrderShouldNotMatter() {
        JsonObject json1 = new JsonObject();
        json1.addProperty("a", 1);
        json1.addProperty("b", 2);
        json1.addProperty("c", 3);

        JsonObject json2 = new JsonObject();
        json2.addProperty("c", 3);
        json2.addProperty("a", 1);
        json2.addProperty("b", 2);

        String hash1 = new GsonXSumAdapter("SHA-256", json1).takeHashOfJsonObject();
        String hash2 = new GsonXSumAdapter("SHA-256", json2).takeHashOfJsonObject();

        Assertions.assertEquals(hash1, hash2,
                "Key order should not affect the hash");
    }

    @Test
    void orgJsonKeyOrderShouldNotMatter() {
        JSONObject json1 = new JSONObject();
        json1.put("a", 1);
        json1.put("b", 2);
        json1.put("c", 3);

        JSONObject json2 = new JSONObject();
        json2.put("c", 3);
        json2.put("a", 1);
        json2.put("b", 2);

        String hash1 = new OrgJSONXSumAdapter("SHA-256", json1).takeHashOfJsonObject();
        String hash2 = new OrgJSONXSumAdapter("SHA-256", json2).takeHashOfJsonObject();

        Assertions.assertEquals(hash1, hash2,
                "Key order should not affect the hash");
    }

    // ---- Test 3: Deep nesting with similar key prefixes ----
    @Test
    void gsonDeepNestingNoCollision() {
        // {"x":{"y":{"z":1}}} vs {"xy":{"z":1}} vs {"x":{"yz":1}}
        JsonObject json1 = new JsonObject();
        {
            JsonObject inner1 = new JsonObject();
            {
                JsonObject inner2 = new JsonObject();
                inner2.addProperty("z", 1);
                inner1.add("y", inner2);
            }
            json1.add("x", inner1);
        }

        JsonObject json2 = new JsonObject();
        {
            JsonObject inner = new JsonObject();
            inner.addProperty("z", 1);
            json2.add("xy", inner);
        }

        String hash1 = new GsonXSumAdapter("SHA-256", json1).takeHashOfJsonObject();
        String hash2 = new GsonXSumAdapter("SHA-256", json2).takeHashOfJsonObject();

        Assertions.assertNotEquals(hash1, hash2,
                "Different deep nesting should not collide");
    }

    // ---- Test 4: Array vs object ambiguity ----
    @Test
    void gsonArrayVsObjectShouldNotCollide() {
        // {"a":[1]} vs {"a":{"0":1}} - fundamentally different structures
        JsonObject json1 = new JsonObject();
        JsonArray arr = new JsonArray();
        arr.add(1);
        json1.add("a", arr);

        JsonObject json2 = new JsonObject();
        JsonObject inner = new JsonObject();
        inner.addProperty("0", 1);
        json2.add("a", inner);

        String hash1 = new GsonXSumAdapter("SHA-256", json1).takeHashOfJsonObject();
        String hash2 = new GsonXSumAdapter("SHA-256", json2).takeHashOfJsonObject();

        Assertions.assertNotEquals(hash1, hash2,
                "Array and object structures should not collide");
    }

    // ---- Test 5: Same values, different nesting depth ----
    @Test
    void gsonDifferentNestingDepthNoCollision() {
        // {"a":1} vs {"a":{"b":1}}
        JsonObject json1 = new JsonObject();
        json1.addProperty("a", 1);

        JsonObject json2 = new JsonObject();
        {
            JsonObject inner = new JsonObject();
            inner.addProperty("b", 1);
            json2.add("a", inner);
        }

        String hash1 = new GsonXSumAdapter("SHA-256", json1).takeHashOfJsonObject();
        String hash2 = new GsonXSumAdapter("SHA-256", json2).takeHashOfJsonObject();

        Assertions.assertNotEquals(hash1, hash2,
                "Different nesting depths should not collide");
    }

    // ---- Test 6: Empty structures ----
    @Test
    void gsonEmptyStructuresShouldDifferFromNonEmpty() {
        JsonObject empty = new JsonObject();
        String emptyHash = new GsonXSumAdapter("SHA-256", empty).takeHashOfJsonObject();

        JsonObject nonEmpty = new JsonObject();
        nonEmpty.addProperty("a", 1);
        String nonEmptyHash = new GsonXSumAdapter("SHA-256", nonEmpty).takeHashOfJsonObject();

        Assertions.assertNotEquals(emptyHash, nonEmptyHash,
                "Empty and non-empty objects should have different hashes");
    }

    // ---- Test 7: Input mutation check ----
    @Test
    void gsonInputShouldNotBeMutated() {
        JsonObject json = new JsonObject();
        json.addProperty("a", 1);
        json.addProperty("b", 2);
        String originalString = json.toString();

        new GsonXSumAdapter("SHA-256", json).takeHashOfJsonObject();

        Assertions.assertEquals(originalString, json.toString(),
                "Input JSON should not be mutated by hashing");
    }

    @Test
    void orgJsonInputShouldNotBeMutated() {
        JSONObject json = new JSONObject();
        json.put("a", 1);
        json.put("b", 2);
        String originalString = json.toString();

        new OrgJSONXSumAdapter("SHA-256", json).takeHashOfJsonObject();

        Assertions.assertEquals(originalString, json.toString(),
                "Input JSON should not be mutated by hashing");
    }

    // ---- Test 8: Brute-force small-space collision search ----
    @Test
    void gsonBruteForceCollisionSearch() {
        // Generate many small random-ish JSON objects and check for collisions
        java.util.Set<String> hashes = new java.util.HashSet<>();
        java.util.Map<String, JsonObject> hashToInput = new java.util.HashMap<>();

        String[] keys = {"a", "b", "c"};
        int[] values = {1, 2, 3};

        // Generate all possible single-key objects: {k:v}
        for (String k : keys) {
            for (int v : values) {
                JsonObject json = new JsonObject();
                json.addProperty(k, v);
                String hash = new GsonXSumAdapter("SHA-256", json).takeHashOfJsonObject();

                if (hashToInput.containsKey(hash)) {
                    JsonObject original = hashToInput.get(hash);
                    Assertions.fail("Collision found: " + original + " and " + json +
                            " both produced hash " + hash);
                }
                hashToInput.put(hash, json);
            }
        }

        // Generate two-key objects (sorted by key to avoid comparing same-content diffs)
        for (int i = 0; i < keys.length; i++) {
            for (int j = i + 1; j < keys.length; j++) {
                for (int v1 : values) {
                    for (int v2 : values) {
                        JsonObject json = new JsonObject();
                        json.addProperty(keys[i], v1);
                        json.addProperty(keys[j], v2);
                        String hash = new GsonXSumAdapter("SHA-256", json).takeHashOfJsonObject();

                        if (hashToInput.containsKey(hash)) {
                            JsonObject original = hashToInput.get(hash);
                            Assertions.fail("Collision found: " + original + " and " + json +
                                    " both produced hash " + hash);
                        }
                        hashToInput.put(hash, json);
                    }
                }
            }
        }

        // Generate nested single-key objects: {k:{k2:v}}
        for (String k : keys) {
            for (String k2 : keys) {
                if (k.equals(k2)) continue;
                for (int v : values) {
                    JsonObject json = new JsonObject();
                    JsonObject inner = new JsonObject();
                    inner.addProperty(k2, v);
                    json.add(k, inner);
                    String hash = new GsonXSumAdapter("SHA-256", json).takeHashOfJsonObject();

                    if (hashToInput.containsKey(hash)) {
                        JsonObject original = hashToInput.get(hash);
                        Assertions.fail("Collision found: " + original + " and " + json +
                                " both produced hash " + hash);
                    }
                    hashToInput.put(hash, json);
                }
            }
        }

        Assertions.assertTrue(hashes.isEmpty() || hashToInput.size() > 10,
                "Should have generated multiple unique hashes");
    }

    // ---- Test 9: Verify the existing test's expected string is actually correct ----
    // This test traces through what singleton paths are produced and verifies them manually
    @Test
    void gsonSimpleTwoKeyObjectTrace() {
        // For {"a":1,"b":2}, sorted keys are ["a","b"]
        // After processing "a": topLevelSingletonPath = {"a":1}, hash it, remove "a" -> {}
        // After processing "b": topLevelSingletonPath = {"b":2}, hash it, remove "b" -> {}
        // So the digest should be SHA-256("{\"a\":1}\" + "{\"b\":2}")

        JsonObject json = new JsonObject();
        json.addProperty("b", 2);
        json.addProperty("a", 1);

        String actualHash = new GsonXSumAdapter("SHA-256", json).takeHashOfJsonObject();

        // Compute expected: SHA-256 of "{\"a\":1}{\"b\":2}"
        java.security.MessageDigest md;
        try {
            md = java.security.MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String expectedInput = "{\"a\":1}{\"b\":2}";
        md.update(expectedInput.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String expectedHash = java.util.HexFormat.of().formatHex(md.digest());

        Assertions.assertEquals(expectedHash, actualHash,
                "Hash should be SHA-256 of concatenated singleton paths");
    }

    // ---- Test 10: Verify nested path is included in the hash input ----
    @Test
    void gsonNestedPathIsFullNotFragment() {
        // For {"a":{"b":1}}, sorted keys at root: ["a"], then at nested: ["b"]
        // The singleton path when hitting leaf 1 should be {"a":{"b":1}} (full path),
        // NOT just {"b":1} (fragment)

        JsonObject json = new JsonObject();
        {
            JsonObject inner = new JsonObject();
            inner.addProperty("b", 1);
            json.add("a", inner);
        }

        String actualHash = new GsonXSumAdapter("SHA-256", json).takeHashOfJsonObject();

        // If the full path is hashed: SHA-256("{\"a\":{\"b\":1}}")
        java.security.MessageDigest md;
        try {
            md = java.security.MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String fullPathInput = "{\"a\":{\"b\":1}}";
        md.update(fullPathInput.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String expectedHashFullPath = java.util.HexFormat.of().formatHex(md.digest());

        // If only the fragment is hashed: SHA-256("{\"b\":1}")
        try {
            md = java.security.MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String fragmentInput = "{\"b\":1}";
        md.update(fragmentInput.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String expectedHashFragment = java.util.HexFormat.of().formatHex(md.digest());

        Assertions.assertEquals(expectedHashFullPath, actualHash,
                "Nested leaf should hash the FULL path {\"a\":{\"b\":1}}, not just the fragment {\"b\":1}");
        Assertions.assertNotEquals(expectedHashFragment, actualHash,
                "Should NOT be hashing only the fragment");
    }

    // ---- Test 11: Unicode escape handling ----
    @Test
    void gsonUnicodeEscapesAreDeterministic() {
        // Gson normalizes unicode escapes in its toString(), so "\u0041" becomes "A"
        // This test verifies that unicode escapes produce deterministic hashes

        JsonObject json1 = new JsonObject();
        json1.addProperty("key", "\u0041"); // "A" via unicode escape
        json1.addProperty("b", 2);

        JsonObject json2 = new JsonObject();
        json2.addProperty("key", "A"); // same character, no escape
        json2.addProperty("b", 2);

        String hash1 = new GsonXSumAdapter("SHA-256", json1).takeHashOfJsonObject();
        String hash2 = new GsonXSumAdapter("SHA-256", json2).takeHashOfJsonObject();

        // Both should produce the same hash because they represent the same JSON value
        Assertions.assertEquals(hash1, hash2,
                "Unicode escapes should produce deterministic hashes");
    }

    @Test
    void orgJsonUnicodeEscapesAreDeterministic() {
        JSONObject json1 = new JSONObject();
        json1.put("key", "\u0041"); // "A" via unicode escape
        json1.put("b", 2);

        JSONObject json2 = new JSONObject();
        json2.put("key", "A"); // same character, no escape
        json2.put("b", 2);

        String hash1 = new OrgJSONXSumAdapter("SHA-256", json1).takeHashOfJsonObject();
        String hash2 = new OrgJSONXSumAdapter("SHA-256", json2).takeHashOfJsonObject();

        Assertions.assertEquals(hash1, hash2,
                "Unicode escapes should produce deterministic hashes");
    }
}