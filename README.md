# ShaXSumDriver

**A Driver Class For Taking A Deterministic Message Digest Of A JSON**

Basically, if you have two JSON strings, `{"key1": "value1", "key2": "value2"}` and `{"key2":      "value2","key1":"value1"     }`, they ought to leave the same message digest (regardless of if it is a sha256, shake, blake, etc.) even if a JSON library would otherwise convert them to unequal string objects.

This project attempts to fix that. 