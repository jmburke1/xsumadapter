# XSumAdapter

**A Deterministic Message Digest Adapter For JSON Objects**

If you have two JSON strings that represent the same data - for example, `{"key1": "value1", "key2": "value2"}` and `{"key2": "value2", "key1": "value1"}` - they should produce the same message digest, regardless of which hash algorithm you use (SHA-256, SHA-3, BLAKE, etc.) or how a JSON library internally represents them.

This project provides that guarantee.

## How It Works

The algorithm walks the JSON tree depth-first, sorting keys at each level to ensure deterministic traversal order. As it walks, it maintains a **"singleton path"** - a mirror structure containing exactly one element per nesting level, representing the current position in the input tree.

When a leaf is reached (a primitive value, empty object, or empty array), the full singleton path from root to that leaf is serialized as minified JSON and fed into the message digest. After processing each branch, the singleton path is restored by removing the processed element - maintaining the one-element-per-level invariant for the next sibling.

The result: a sequence of canonical path fragments whose concatenation uniquely encodes the JSON structure regardless of key insertion order or whitespace differences.

## Architecture

Inspired by JDBC's driver model, this project provides an **adapter** pattern for different JSON libraries. Each adapter (e.g., `GsonXSumAdapter`, `OrgJSONXSumAdapter`) implements the library-specific operations needed to traverse and serialize its native JSON types, while `GenericRecursiveXSumAdapter` handles the shared traversal logic.  It also provides an XSumAdapter interface for those real edge cases where it is necessary to include a JSON library that, for whatever reason, doesn't cooperate with the mutate model implied by the singleton path.

## Usage

```java
JsonObject json = new JsonObject();
json.addProperty("name", "John");
json.addProperty("age", 30);

String hash = new GsonXSumAdapter("SHA-256", json).takeHashOfJsonObject();
```
