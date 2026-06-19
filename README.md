# ShaXSumDriver

**A Driver Class For Taking A Deterministic Message Digest Of A JSON**

Basically, if you have two JSON strings, `{"key1": "value1", "key2": "value2"}` and `{"key2":      "value2","key1":"value1"     }`, they ought to leave the same message digest (regardless of if it is a sha256, shake, blake, etc.) even if a JSON library would otherwise convert them to unequal string objects.

This project attempts to fix that.  It also attempts to create a framework around which others can create JSON ShaXSum drivers of their own.  The name "driver" derives its inspiration from JDBC.  In the same way JDBC has different jars called "drivers" which allow connecting to different databases.  This project encourages different "driver" classes around which different JSON processing libraries can have a way to take a message digest and have the result come out deterministically.
