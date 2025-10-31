# SonarCloud Issues (branch `ea`)

Total open/confirmed issues: 1628

## 1. AZk52uJNviqOMEUt0pa7

- **Rule**: `java:S1607`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MarshallableOutBuilderTest.java:116`
- **Effort**: 10min
- **Created**: 2025-09-11T17:37:22+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Either add an explanation about why this test is skipped or remove the "@Ignore" annotation.

## 2. AZhCOAu7TtJ4wK8ep7DL

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadOneTest.java:22`
- **Effort**: 1min
- **Created**: 2025-07-22T13:12:22+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'net.openhft.chronicle.bytes.MethodId'.

## 3. AZfmhMwUeoieKB1eXn7_

- **Rule**: `java:S1104`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/MapMarshaller.java:18`
- **Effort**: 10min
- **Created**: 2025-07-04T13:48:20+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Make map a static final constant or non-public and provide accessors if needed.

## 4. AZfmhMwUeoieKB1eXn8A

- **Rule**: `java:S1104`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/MapMarshaller.java:19`
- **Effort**: 10min
- **Created**: 2025-07-04T13:48:20+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Make kClass a static final constant or non-public and provide accessors if needed.

## 5. AZfmhMwUeoieKB1eXn8B

- **Rule**: `java:S1104`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/MapMarshaller.java:20`
- **Effort**: 10min
- **Created**: 2025-07-04T13:48:20+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Make vClass a static final constant or non-public and provide accessors if needed.

## 6. AZfmhMwUeoieKB1eXn8C

- **Rule**: `java:S1104`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/MapMarshaller.java:21`
- **Effort**: 10min
- **Created**: 2025-07-04T13:48:20+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Make leaf a static final constant or non-public and provide accessors if needed.

## 7. AZfmhMw0eoieKB1eXn8D

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/MethodWriterClassNameGenerator.java:3`
- **Effort**: 1min
- **Created**: 2025-07-04T13:48:20+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'net.openhft.chronicle.core.Jvm'.

## 8. AZfmhMjIeoieKB1eXn7-

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/SerializableObjectTest.java:357`
- **Effort**: 2min
- **Created**: 2025-07-04T13:48:20+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this 'public' modifier.

## 9. AZfmhMZQeoieKB1eXn78

- **Rule**: `java:S116`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2643`
- **Effort**: 2min
- **Created**: 2025-07-04T13:48:20+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this field "C" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 10. AZfmhMh-eoieKB1eXn79

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/internal/MethodWriterClassNameGeneratorTest.java:11`
- **Effort**: 1min
- **Created**: 2025-07-04T13:48:20+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'org.junit.Assume.assumeFalse'.

## 11. AZfmhMQPeoieKB1eXn77

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/CNFREOnMissingClassTest.java:8`
- **Effort**: 1min
- **Created**: 2025-07-04T13:48:20+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'org.junit.Before'.

## 12. AZfmhM41eoieKB1eXn8E

- **Rule**: `java:S1192`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:2233`
- **Effort**: 8min
- **Created**: 2025-06-16T17:58:06+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Define a constant instead of duplicating this literal "Unable to read " 3 times.

## 13. AZXyIbn3Go72-YLtJ3ho

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:2023`
- **Effort**: 5min
- **Created**: 2025-04-01T14:56:15+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 14. AZXyIbn3Go72-YLtJ3hp

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:2033`
- **Effort**: 5min
- **Created**: 2025-04-01T14:56:15+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 15. AZXyIbn3Go72-YLtJ3hq

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:2048`
- **Effort**: 5min
- **Created**: 2025-04-01T14:56:15+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 16. AZXTivR274sKGjgOChoo

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireType.java:486`
- **Effort**: 30min
- **Created**: 2025-03-26T13:40:36+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility update should be removed.

## 17. AZXTivR274sKGjgOChop

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireType.java:492`
- **Effort**: 30min
- **Created**: 2025-03-26T13:40:36+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 18. AZS37rIRIVqiQpr6kOmk

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JsonWireDoubleAndFloatSpecialValuesAcceptanceTests.java:27`
- **Effort**: 2min
- **Created**: 2025-01-08T03:49:00+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 19. AZS37qwWIVqiQpr6kOmK

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JsonWireToStringAcceptanceTest.java:34`
- **Effort**: 2min
- **Created**: 2024-12-31T14:09:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 20. AZS37qwWIVqiQpr6kOmG

- **Rule**: `java:S3008`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JsonWireToStringAcceptanceTest.java:36`
- **Effort**: 2min
- **Created**: 2024-12-31T14:09:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this field "WIRE_TYPES" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 21. AZS37qwWIVqiQpr6kOmH

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JsonWireToStringAcceptanceTest.java:40`
- **Effort**: 2min
- **Created**: 2024-12-31T14:09:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 22. AZS37qwWIVqiQpr6kOmI

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JsonWireToStringAcceptanceTest.java:50`
- **Effort**: 2min
- **Created**: 2024-12-31T14:09:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 23. AZS37qwWIVqiQpr6kOmJ

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JsonWireToStringAcceptanceTest.java:59`
- **Effort**: 2min
- **Created**: 2024-12-31T14:09:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 24. AZMw7pYbYk7po6Fb1sLe

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:170`
- **Effort**: 10min
- **Created**: 2024-11-11T16:25:03+0000
- **Assignee**: Unassigned
- **Message**:
  Do not forget to remove this deprecated code someday.

## 25. AZMw7pYbYk7po6Fb1sLf

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:170`
- **Effort**: 5min
- **Created**: 2024-11-11T16:25:03+0000
- **Assignee**: Unassigned
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 26. AZMw7pgMYk7po6Fb1sLi

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:23`
- **Effort**: 1min
- **Created**: 2024-11-11T16:25:03+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'net.openhft.chronicle.bytes.internal.BytesInternal'.

## 27. AZHf_dLdhdLM_z6jmoZm

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:886`
- **Effort**: 1min
- **Created**: 2024-09-06T06:10:58+0000
- **Assignee**: benbonavia@github
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 28. AZHf_dLdhdLM_z6jmoZl

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:884`
- **Effort**: 5min
- **Created**: 2024-09-04T13:06:34+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "multipleNonMarshallableParamTypes" which hides the field declared at line 82.

## 29. AZHf_dLdhdLM_z6jmoZi

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:900`
- **Effort**: 2min
- **Created**: 2024-09-04T13:06:34+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 30. AZHf_dLdhdLM_z6jmoZj

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:569`
- **Effort**: 1min
- **Created**: 2024-08-26T07:42:35+0000
- **Assignee**: benbonavia@github
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 31. AZHf_dLdhdLM_z6jmoZk

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:570`
- **Effort**: 1min
- **Created**: 2024-08-26T07:42:35+0000
- **Assignee**: benbonavia@github
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 32. AZDL7c0u6N0FxHM9EiI-

- **Rule**: `java:S4144`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TestJsonIssue467.java:89`
- **Effort**: 15min
- **Created**: 2024-07-09T06:52:51+0000
- **Assignee**: Unassigned
- **Message**:
  Update this method so that its implementation is not identical to "test" on line 40.

## 33. AZDL7c0u6N0FxHM9EiJA

- **Rule**: `java:S5785`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TestJsonIssue467.java:131`
- **Effort**: 2min
- **Created**: 2024-07-09T06:52:51+0000
- **Assignee**: Unassigned
- **Message**:
  Use assertEquals instead.

## 34. AZDL7c0u6N0FxHM9EiI9

- **Rule**: `java:S1488`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TestJsonIssue467.java:140`
- **Effort**: 2min
- **Created**: 2024-07-09T06:52:51+0000
- **Assignee**: Unassigned
- **Message**:
  Immediately return this expression instead of assigning it to the temporary variable "jsonWire".

## 35. AZDL7cvu6N0FxHM9EiIm

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/recursive/ReferToBaseClass.java:13`
- **Effort**: 5min
- **Created**: 2024-06-26T08:56:58+0000
- **Assignee**: david-ry4n@github
- **Message**:
  Add the "@Override" annotation above this method signature

## 36. AZBRSWoOxHjvqKrhU_oT

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/HashWire.java:664`
- **Effort**: 5min
- **Created**: 2024-06-21T13:43:55+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 37. AZBRSWzoxHjvqKrhU_oV

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:59`
- **Effort**: 10min
- **Created**: 2024-06-21T13:43:55+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Do not forget to remove this deprecated code someday.

## 38. AZBRSWzoxHjvqKrhU_oW

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:59`
- **Effort**: 5min
- **Created**: 2024-06-21T13:43:55+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 39. AZBRSWsKxHjvqKrhU_oU

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:551`
- **Effort**: 8min
- **Created**: 2024-06-21T13:43:55+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 18 to the 15 allowed.

## 40. AZBRSWa8xHjvqKrhU_oQ

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONWireTest.java:304`
- **Effort**: 5min
- **Created**: 2024-06-21T13:43:55+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 41. AZBRSWa8xHjvqKrhU_oR

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONWireTest.java:414`
- **Effort**: 5min
- **Created**: 2024-06-21T13:43:55+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 42. AZBRSWa8xHjvqKrhU_oS

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONWireTest.java:417`
- **Effort**: 5min
- **Created**: 2024-06-21T13:43:55+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 43. AZDL7c-O6N0FxHM9EiJi

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderArgumentsRecycleTest.java:392`
- **Effort**: 5min
- **Created**: 2024-06-12T13:03:40+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "a" private field.

## 44. AZDL7c-O6N0FxHM9EiJj

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderArgumentsRecycleTest.java:393`
- **Effort**: 5min
- **Created**: 2024-06-12T13:03:40+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "b" private field.

## 45. AZAWHwJZqceje68SZxir

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:1279`
- **Effort**: 5min
- **Created**: 2024-06-06T16:23:15+0000
- **Assignee**: Unassigned
- **Message**:
  Complete cases by adding the missing enum constants or add a default case to this switch.

## 46. AY_Fi6_-TbFMTgi4MX0n

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:891`
- **Effort**: 1min
- **Created**: 2024-04-29T11:31:31+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 47. AY_Fi7D7TbFMTgi4MX0p

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ValueIn.java:182`
- **Effort**: 20min
- **Created**: 2024-04-29T11:31:31+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 48. AY_Fi7D7TbFMTgi4MX0q

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ValueIn.java:182`
- **Effort**: 20min
- **Created**: 2024-04-29T11:31:31+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 49. AY_Fi7D7TbFMTgi4MX0r

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ValueIn.java:237`
- **Effort**: 20min
- **Created**: 2024-04-29T11:31:31+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 50. AY_Fi7D7TbFMTgi4MX0s

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ValueIn.java:237`
- **Effort**: 20min
- **Created**: 2024-04-29T11:31:31+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 51. AY_Fi7U0TbFMTgi4MX09

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1965`
- **Effort**: 6min
- **Created**: 2024-04-29T11:31:31+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## 52. AY_Fi62iTbFMTgi4MX0j

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWire2Test.java:679`
- **Effort**: 5min
- **Created**: 2024-04-29T11:31:31+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "bytes" which hides the field declared at line 48.

## 53. AY_Fi7DBTbFMTgi4MX0o

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:4674`
- **Effort**: 33min
- **Created**: 2024-04-29T11:31:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 43 to the 15 allowed.

## 54. AY_Fi7UETbFMTgi4MX06

- **Rule**: `java:S1185`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:1161`
- **Effort**: 2min
- **Created**: 2024-04-29T11:31:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this method to simply inherit it.

## 55. AY_Fi7UETbFMTgi4MX07

- **Rule**: `java:S1185`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:1166`
- **Effort**: 2min
- **Created**: 2024-04-29T11:31:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this method to simply inherit it.

## 56. AY_Fi7TBTbFMTgi4MX03

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1403`
- **Effort**: 20min
- **Created**: 2024-04-29T11:31:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 30 to the 15 allowed.

## 57. AY_Fi7TBTbFMTgi4MX04

- **Rule**: `java:S119`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1403`
- **Effort**: 10min
- **Created**: 2024-04-29T11:31:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this generic name to match the regular expression '^[A-Z][0-9]?$'.

## 58. AY_Fi7TBTbFMTgi4MX05

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:3050`
- **Effort**: 10min
- **Created**: 2024-04-29T11:31:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 20 to the 15 allowed.

## 59. AY_Fi7GpTbFMTgi4MX0t

- **Rule**: `java:S3358`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ValueOut.java:604`
- **Effort**: 5min
- **Created**: 2024-04-29T11:31:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Extract this nested ternary operation into an independent statement.

## 60. AY_Fi7U0TbFMTgi4MX08

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:310`
- **Effort**: 19min
- **Created**: 2024-04-29T11:31:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 29 to the 15 allowed.

## 61. AY_Fi7Q8TbFMTgi4MX0y

- **Rule**: `java:S2386`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:86`
- **Effort**: 15min
- **Created**: 2024-04-29T11:31:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Make this member "protected".

## 62. AY_Fi7Q8TbFMTgi4MX0z

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:1053`
- **Effort**: 18min
- **Created**: 2024-04-29T11:31:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 28 to the 15 allowed.

## 63. AY_Fi7Q8TbFMTgi4MX00

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:1496`
- **Effort**: 13min
- **Created**: 2024-04-29T11:31:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 23 to the 15 allowed.

## 64. AY_Fi7JATbFMTgi4MX0u

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:2333`
- **Effort**: 13min
- **Created**: 2024-04-29T11:31:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 23 to the 15 allowed.

## 65. AY_Fi6uSTbFMTgi4MX0g

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TimestampLongConverterZoneIdsTest.java:46`
- **Effort**: 5min
- **Created**: 2024-04-29T11:31:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove these unused method parameters "zoneId", "converterType".

## 66. AY-YR1rKkrh49lmvlS9X

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/Issue886Test.java:33`
- **Effort**: 5min
- **Created**: 2024-04-26T11:54:18+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove the declaration of thrown exception 'java.io.IOException', as it cannot be thrown from
  method's body.

## 67. AY77FwKNz4ht3-Lml0KY

- **Rule**: `java:S5778`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MessageHistoryTest.java:86`
- **Effort**: 5min
- **Created**: 2024-04-19T09:43:47+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor the body of this try/catch to have only one invocation possibly throwing a runtime
  exception.

## 68. AY77Fwczz4ht3-Lml0Kv

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1690`
- **Effort**: 0min
- **Created**: 2024-04-19T06:40:04+0000
- **Assignee**: chroniclekevinpowe@github
- **Message**:
  Complete the task associated to this TODO comment.

## 69. AY-YR1efkrh49lmvlS7O

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/MarshallingJSONStringTest.java:12`
- **Effort**: 1min
- **Created**: 2024-04-19T06:40:04+0000
- **Assignee**: chroniclekevinpowe@github
- **Message**:
  Remove this unused import 'org.junit.Assert.assertFalse'.

## 70. AY77FwS5z4ht3-Lml0KZ

- **Rule**: `java:S5164`
- **Severity**: MAJOR
- **Type**: BUG
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:91`
- **Effort**: 10min
- **Created**: 2024-04-12T07:58:15+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Call "remove()" on "VANILLA_MESSAGE_HISTORY_TL".

## 71. AY77FwS5z4ht3-Lml0Ka

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:627`
- **Effort**: 5min
- **Created**: 2024-04-12T07:58:15+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete cases by adding the missing enum constants or add a default case to this switch.

## 72. AZDL7ciu6N0FxHM9EiBa

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1206`
- **Effort**: 2min
- **Created**: 2024-04-03T20:28:57+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 73. AZDL7ciu6N0FxHM9EiBb

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1207`
- **Effort**: 2min
- **Created**: 2024-04-03T20:28:57+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 74. AZDL7ciu6N0FxHM9EiBc

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1208`
- **Effort**: 2min
- **Created**: 2024-04-03T20:28:57+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 75. AZDL7dCS6N0FxHM9EiJt

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:945`
- **Effort**: 2min
- **Created**: 2024-04-03T20:28:57+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 76. AZDL7dCS6N0FxHM9EiJu

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:946`
- **Effort**: 2min
- **Created**: 2024-04-03T20:28:57+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 77. AZDL7dCS6N0FxHM9EiJv

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:947`
- **Effort**: 2min
- **Created**: 2024-04-03T20:28:57+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 78. AZDL7c3a6N0FxHM9EiJL

- **Rule**: `java:S1220`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/IntConversionTest.java`
- **Effort**: 10min
- **Created**: 2024-03-28T13:52:16+0000
- **Assignee**: Unassigned
- **Message**:
  Move this file to a named package.

## 79. AZDL7dBX6N0FxHM9EiJr

- **Rule**: `java:S1220`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TestIntConversion.java`
- **Effort**: 10min
- **Created**: 2024-03-28T13:52:16+0000
- **Assignee**: Unassigned
- **Message**:
  Move this file to a named package.

## 80. AY6FVbG-5NUPA8B1oXpL

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractClassGenerator.java:46`
- **Effort**: 0min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 81. AY6FVbKB5NUPA8B1oXpZ

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:257`
- **Effort**: 0min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 82. AY6FVbJO5NUPA8B1oXpX

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1979`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 83. AY6FVbJO5NUPA8B1oXpY

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:3052`
- **Effort**: 0min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 84. AY6FVbH65NUPA8B1oXpS

- **Rule**: `java:S3008`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:130`
- **Effort**: 2min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this field "CACHED_COMPILER" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 85. AY6FVa8w5NUPA8B1oXoh

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlLogging.java:37`
- **Effort**: 0min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 86. AY6FVbE45NUPA8B1oXpC

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:771`
- **Effort**: 20min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 87. AY6FVbE45NUPA8B1oXpD

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:1165`
- **Effort**: 0min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 88. AY6FVbB_5NUPA8B1oXox

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:154`
- **Effort**: 6min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## 89. AY6FVbB_5NUPA8B1oXoy

- **Rule**: `java:S119`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:154`
- **Effort**: 10min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this generic name to match the regular expression '^[A-Z][0-9]?$'.

## 90. AZDL7c2g6N0FxHM9EiJI

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONTypesWithEnumsAndBoxedTypesTest.java:65`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "surname" private field.

## 91. AZDL7c2g6N0FxHM9EiJK

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONTypesWithEnumsAndBoxedTypesTest.java:69`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "location" private field.

## 92. AZS37rBPIVqiQpr6kOmZ

- **Rule**: `java:S1640`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONWireTest.java:321`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Convert this Map to an EnumMap.

## 93. AY-YR12Skrh49lmvlTAw

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MarshallableTest.java:127`
- **Effort**: 0min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 94. AZDL7cwH6N0FxHM9EiIr

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderBuilderExceptionHandlerTest.java:38`
- **Effort**: 2min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 95. AY-YR1xqkrh49lmvlTAB

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/SkipValueTest.java:79`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 96. AY-YR1r7krh49lmvlS9o

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:414`
- **Effort**: 0min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 97. AY-YR1r7krh49lmvlS-d

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1661`
- **Effort**: 0min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 98. AY-YR2Fnkrh49lmvlTDW

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:88`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 99. AY-YR2Fnkrh49lmvlTDY

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:110`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 100. AY-YR2Fnkrh49lmvlTDa

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:193`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 101. AY-YR2Fnkrh49lmvlTDb

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:204`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 102. AY-YR2Fnkrh49lmvlTDd

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:238`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 103. AY-YR2Fnkrh49lmvlTDe

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:256`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 104. AY-YR2Fnkrh49lmvlTDf

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:273`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 105. AY-YR2Fnkrh49lmvlTDg

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:285`
- **Effort**: 0min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 106. AY-YR2Fnkrh49lmvlTDh

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:292`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 107. AY-YR2Fnkrh49lmvlTDi

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:313`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 108. AY-YR2Fnkrh49lmvlTDj

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:323`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 109. AY-YR2Fnkrh49lmvlTDk

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:333`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 110. AY-YR2Fnkrh49lmvlTDl

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:341`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 111. AY-YR2Fnkrh49lmvlTDm

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:353`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 112. AY-YR2Fnkrh49lmvlTDn

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:361`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 113. AY-YR2Fnkrh49lmvlTDo

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:371`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 114. AY-YR2Fnkrh49lmvlTDp

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:382`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 115. AY-YR2Fnkrh49lmvlTDq

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:400`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 116. AY-YR2Fnkrh49lmvlTDr

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:421`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 117. AY-YR2Fnkrh49lmvlTDs

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:447`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 118. AY-YR2Fnkrh49lmvlTDt

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:475`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 119. AY-YR2Fnkrh49lmvlTDu

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:503`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 120. AY-YR2Fnkrh49lmvlTDv

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:530`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 121. AY-YR2Fnkrh49lmvlTDw

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:556`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 122. AY-YR2Fnkrh49lmvlTDx

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:582`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 123. AY-YR2Fnkrh49lmvlTDy

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:608`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 124. AY-YR2Fnkrh49lmvlTDz

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:633`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 125. AY-YR2Fnkrh49lmvlTD0

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:662`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 126. AY-YR2Fnkrh49lmvlTD1

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:691`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 127. AY-YR2Fnkrh49lmvlTD4

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:718`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 128. AY-YR2Fnkrh49lmvlTD5

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:732`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 129. AY-YR2Fnkrh49lmvlTD6

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:750`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 130. AY-YR2Fnkrh49lmvlTD7

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:770`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 131. AY-YR1evkrh49lmvlS7Q

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/MarshallableWithOverwriteFalseTest.java:84`
- **Effort**: 5min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 132. AZDL7cW76N0FxHM9EiAo

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/ModelKeys.java:31`
- **Effort**: 2min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 133. AZDL7cW76N0FxHM9EiAp

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/ModelKeys.java:32`
- **Effort**: 2min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 134. AZDL7cW76N0FxHM9EiAq

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/ModelKeys.java:33`
- **Effort**: 2min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 135. AZDL7cW76N0FxHM9EiAr

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/ModelKeys.java:34`
- **Effort**: 2min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 136. AZDL7cW76N0FxHM9EiAs

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/ModelKeys.java:35`
- **Effort**: 2min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 137. AZDL7cW76N0FxHM9EiAt

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/ModelKeys.java:36`
- **Effort**: 2min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 138. AZDL7cW76N0FxHM9EiAu

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/ModelKeys.java:37`
- **Effort**: 2min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 139. AZDL7cW76N0FxHM9EiAv

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/ModelKeys.java:38`
- **Effort**: 2min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 140. AZDL7cW76N0FxHM9EiAw

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/ModelKeys.java:39`
- **Effort**: 2min
- **Created**: 2024-03-27T13:30:13+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 141. AY4GzR1EJxZY3-_QQe2X

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:888`
- **Effort**: 5min
- **Created**: 2024-02-29T19:42:46+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 142. AY4GzR1EJxZY3-_QQe2Y

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:888`
- **Effort**: 0min
- **Created**: 2024-02-29T19:42:46+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 143. AY-YR1yzkrh49lmvlTAW

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireTypeConverterTest.java:65`
- **Effort**: 5min
- **Created**: 2024-02-29T19:42:46+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "json" which hides the field declared at line 10.

## 144. AZDL7chC6N0FxHM9EiBW

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/ClassAliasPool840Test.java:41`
- **Effort**: 5min
- **Created**: 2024-02-29T16:33:24+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 145. AZDL7chC6N0FxHM9EiBX

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/ClassAliasPool840Test.java:44`
- **Effort**: 5min
- **Created**: 2024-02-29T16:33:24+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 146. AY4GzRXSJxZY3-_QQe2S

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/Base32LongConverterTest.java:43`
- **Effort**: 10min
- **Created**: 2024-02-26T15:41:40+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 147. AY4GzReZJxZY3-_QQe2U

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/Base64LongConverterTest.java:46`
- **Effort**: 10min
- **Created**: 2024-02-26T15:41:40+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 148. AY4GzRh_JxZY3-_QQe2V

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/Base85LongConverterTest.java:69`
- **Effort**: 10min
- **Created**: 2024-02-26T15:41:40+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 149. AY4GzRXoJxZY3-_QQe2T

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ShortTextLongConverterTest.java:69`
- **Effort**: 10min
- **Created**: 2024-02-26T15:41:40+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 150. AY4GzRWiJxZY3-_QQe2R

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WiresTest.java:306`
- **Effort**: 10min
- **Created**: 2024-02-26T06:54:20+0000
- **Assignee**: JerryShea@github
- **Message**:
  Add at least one assertion to this test case.

## 151. AY-YR1ozkrh49lmvlS9F

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ShortTextLongConverterTest.java:59`
- **Effort**: 5min
- **Created**: 2024-02-05T13:28:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 152. AY-YR1r7krh49lmvlS9Y

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:84`
- **Effort**: 5min
- **Created**: 2024-02-05T13:24:56+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 153. AY-YR2Fnkrh49lmvlTEU

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1438`
- **Effort**: 5min
- **Created**: 2024-02-05T13:24:56+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 154. AY013RjjvDBaEBzdOpXm

- **Rule**: `java:S108`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:75`
- **Effort**: 5min
- **Created**: 2024-01-23T09:09:09+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this block of code, fill it in, or add a comment explaining why it is empty.

## 155. AY0dVYQiGh2SvXIsG6MC

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireOut.java:339`
- **Effort**: 5min
- **Created**: 2024-01-05T19:01:58+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 156. AY0dVYVoGh2SvXIsG6MD

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWriteDocumentContext.java:149`
- **Effort**: 0min
- **Created**: 2024-01-03T13:42:42+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 157. AY-YR1k-krh49lmvlS8J

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/BrokenChainTest.java:66`
- **Effort**: 1min
- **Created**: 2024-01-03T12:51:23+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this useless assignment to local variable "secondB".

## 158. AY-YR1k-krh49lmvlS8K

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/BrokenChainTest.java:66`
- **Effort**: 5min
- **Created**: 2024-01-03T12:51:23+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "secondB" local variable.

## 159. AZDL7c106N0FxHM9EiJE

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONWireTest.java:694`
- **Effort**: 5min
- **Created**: 2023-12-28T10:34:39+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "implClass" private field.

## 160. AZDL7c106N0FxHM9EiJF

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONWireTest.java:695`
- **Effort**: 5min
- **Created**: 2023-12-28T10:34:39+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "bool" private field.

## 161. AYyJ7cuhlxU_YJ4KkJIu

- **Rule**: `java:S1124`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractGeneratedMethodReader.java:48`
- **Effort**: 2min
- **Created**: 2023-12-20T08:27:36+0000
- **Assignee**: Unassigned
- **Message**:
  Reorder the modifiers to comply with the Java Language Specification.

## 162. AYyJ7cuhlxU_YJ4KkJIv

- **Rule**: `java:S1124`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractGeneratedMethodReader.java:49`
- **Effort**: 2min
- **Created**: 2023-12-20T08:27:36+0000
- **Assignee**: Unassigned
- **Message**:
  Reorder the modifiers to comply with the Java Language Specification.

## 163. AYxjK1WqroXplR8eGMdh

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/Issue751Test.java:45`
- **Effort**: 10min
- **Created**: 2023-12-13T09:27:18+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 164. AZDL7cR86N0FxHM9EiAf

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/CNFREOnMissingClassTest.java:77`
- **Effort**: 5min
- **Created**: 2023-11-24T01:30:45+0000
- **Assignee**: chroniclekevinpowe@github
- **Message**:
  Remove this unused "bothFields" private field.

## 165. AZDL7cR86N0FxHM9EiAg

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/CNFREOnMissingClassTest.java:78`
- **Effort**: 5min
- **Created**: 2023-11-24T01:30:45+0000
- **Assignee**: chroniclekevinpowe@github
- **Message**:
  Remove this unused "name" private field.

## 166. AZDL7cR86N0FxHM9EiAh

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/CNFREOnMissingClassTest.java:80`
- **Effort**: 5min
- **Created**: 2023-11-24T01:30:45+0000
- **Assignee**: chroniclekevinpowe@github
- **Message**:
  Remove this unused "engineListener" private field.

## 167. AYw0uQxlk7sghoIVrjKY

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:505`
- **Effort**: 13min
- **Created**: 2023-11-16T16:43:45+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 23 to the 15 allowed.

## 168. AYw0uQxlk7sghoIVrjKZ

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:942`
- **Effort**: 29min
- **Created**: 2023-11-16T16:43:45+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 39 to the 15 allowed.

## 169. AYw0uQxlk7sghoIVrjKT

- **Rule**: `java:S1141`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:957`
- **Effort**: 20min
- **Created**: 2023-11-16T16:43:45+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested try block into a separate method.

## 170. AYw0uQxlk7sghoIVrjKa

- **Rule**: `java:S4201`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1027`
- **Effort**: 5min
- **Created**: 2023-11-16T16:43:45+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unnecessary null check; "instanceof" returns false for nulls.

## 171. AYw0uQsMk7sghoIVrjKS

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshallerForUnexpectedFields.java:45`
- **Effort**: 27min
- **Created**: 2023-11-16T16:43:45+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 37 to the 15 allowed.

## 172. AYuVDWTV_FNI6Hwi6BFi

- **Rule**: `java:S5777`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/InvalidYamWithCommonMistakesTest.java:83`
- **Effort**: 5min
- **Created**: 2023-11-01T03:05:51+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Move assertions into separate method or use assertThrows or try-catch instead.

## 173. AYuVDWTl_FNI6Hwi6BFj

- **Rule**: `java:S5777`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/UnknownEnumTest.java:83`
- **Effort**: 5min
- **Created**: 2023-11-01T03:05:51+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Move assertions into separate method or use assertThrows or try-catch instead.

## 174. AYuVDWQs_FNI6Hwi6BFh

- **Rule**: `java:S5777`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WiresTest.java:170`
- **Effort**: 5min
- **Created**: 2023-11-01T03:05:51+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Move assertions into separate method or use assertThrows or try-catch instead.

## 175. AYuVDWOO_FNI6Hwi6BFe

- **Rule**: `java:S5777`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/VanillaMethodReaderTest.java:287`
- **Effort**: 5min
- **Created**: 2023-11-01T02:50:19+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Move assertions into separate method or use assertThrows or try-catch instead.

## 176. AZDL7c3-6N0FxHM9EiJO

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireTestCommon.java:205`
- **Effort**: 5min
- **Created**: 2023-10-30T17:56:08+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 177. AYuVDWmT_FNI6Hwi6BFn

- **Rule**: `java:S1192`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:105`
- **Effort**: 8min
- **Created**: 2023-10-30T08:55:17+0000
- **Assignee**: Unassigned
- **Message**:
  Define a constant instead of duplicating this literal "public " 3 times.

## 178. AZDL7cR86N0FxHM9EiAd

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/CNFREOnMissingClassTest.java:50`
- **Effort**: 5min
- **Created**: 2023-10-19T05:35:47+0000
- **Assignee**: chroniclekevinpowe@github
- **Message**:
  Remove this unused "bothFields" private field.

## 179. AZDL7cR86N0FxHM9EiAe

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/CNFREOnMissingClassTest.java:51`
- **Effort**: 5min
- **Created**: 2023-10-19T05:35:47+0000
- **Assignee**: chroniclekevinpowe@github
- **Message**:
  Remove this unused "name" private field.

## 180. AZMw7pkuYk7po6Fb1sLs

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryReadDocumentContext.java:24`
- **Effort**: 1min
- **Created**: 2023-10-19T02:44:13+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'net.openhft.chronicle.core.scoped.ScopedResource'.

## 181. AYtnL2gG0SA1Og_NYBdW

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireResetTest.java:113`
- **Effort**: 10min
- **Created**: 2023-10-19T02:41:39+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 182. AYtnL2yg0SA1Og_NYBd3

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:385`
- **Effort**: 20min
- **Created**: 2023-10-19T01:25:04+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 183. AYtnL2yg0SA1Og_NYBd4

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:397`
- **Effort**: 5min
- **Created**: 2023-10-19T01:25:04+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused method parameter "wireProvider".

## 184. AYtnL2yg0SA1Og_NYBd6

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:407`
- **Effort**: 20min
- **Created**: 2023-10-19T01:25:04+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 185. AYtnL2vQ0SA1Og_NYBdx

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/DynamicEnum.java:64`
- **Effort**: 5min
- **Created**: 2023-10-18T16:31:30+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add the "@Override" annotation above this method signature

## 186. AYtnL2px0SA1Og_NYBde

- **Rule**: `java:S3358`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:2015`
- **Effort**: 5min
- **Created**: 2023-10-07T00:29:20+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Extract this nested ternary operation into an independent statement.

## 187. AYtnL2VL0SA1Og_NYBdT

- **Rule**: `java:S5777`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/CNFREOnMissingClassTest.java:21`
- **Effort**: 5min
- **Created**: 2023-09-28T15:57:31+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Move assertions into separate method or use assertThrows or try-catch instead.

## 188. AZDL7cR86N0FxHM9EiAb

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/CNFREOnMissingClassTest.java:32`
- **Effort**: 5min
- **Created**: 2023-09-28T15:57:31+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "name" private field.

## 189. AZDL7cR86N0FxHM9EiAc

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/CNFREOnMissingClassTest.java:33`
- **Effort**: 5min
- **Created**: 2023-09-28T15:57:31+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "fieldOne" private field.

## 190. AY-YR1c-krh49lmvlS7I

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/CNFREOnMissingClassTest.java:46`
- **Effort**: 1min
- **Created**: 2023-09-28T15:57:31+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "simple".

## 191. AY-YR1c-krh49lmvlS7J

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/CNFREOnMissingClassTest.java:46`
- **Effort**: 5min
- **Created**: 2023-09-28T15:57:31+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "simple" local variable.

## 192. AY-YR10_krh49lmvlTAn

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireDumperRandomTest.java:50`
- **Effort**: 5min
- **Created**: 2023-08-02T14:29:39+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 193. AYnP57k3wZcRj3wwan6a

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:414`
- **Effort**: 38min
- **Created**: 2023-08-02T07:47:31+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 48 to the 15 allowed.

## 194. AY_Fi7RrTbFMTgi4MX01

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWireOut.java:60`
- **Effort**: 10min
- **Created**: 2023-06-30T12:54:51+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Do not forget to remove this deprecated code someday.

## 195. AY_Fi7RrTbFMTgi4MX02

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWireOut.java:60`
- **Effort**: 5min
- **Created**: 2023-06-30T12:54:51+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 196. AY-YR15Bkrh49lmvlTBI

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadOneTest.java:83`
- **Effort**: 5min
- **Created**: 2023-06-08T11:00:22+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove the declaration of thrown exception 'java.lang.InterruptedException', as it cannot be thrown
  from method's body.

## 197. AYi1D3RxtRNYeWN2GesO

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractGeneratedMethodReader.java:151`
- **Effort**: 8min
- **Created**: 2023-06-08T09:47:58+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 18 to the 15 allowed.

## 198. AYi1D3RxtRNYeWN2GesM

- **Rule**: `java:S2589`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractGeneratedMethodReader.java:307`
- **Effort**: 10min
- **Created**: 2023-06-08T09:47:58+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this expression which always evaluates to "true"

## 199. AY-YR1zWkrh49lmvlTAZ

- **Rule**: `java:S4144`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderDelegationTest.java:98`
- **Effort**: 15min
- **Created**: 2023-06-08T09:47:58+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Update this method so that its implementation is not identical to
  "testUnsuccessfulCallIsDelegatedTextWireScanning" on line 83.

## 200. AYi1D3MOtRNYeWN2GesH

- **Rule**: `java:S106`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:446`
- **Effort**: 10min
- **Created**: 2023-06-06T13:31:32+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this use of System.out by a logger.

## 201. AYi1D27CtRNYeWN2GesF

- **Rule**: `java:S5976`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderDelegationTest.java:76`
- **Effort**: 10min
- **Created**: 2023-06-06T13:31:32+0000
- **Assignee**: Unassigned
- **Message**:
  Replace these 4 tests with a single Parameterized one.

## 202. AYhT_8BBzouB-MG-BCtF

- **Rule**: `java:S107`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReader.java:141`
- **Effort**: 20min
- **Created**: 2023-05-25T17:40:22+0000
- **Assignee**: Unassigned
- **Message**:
  Constructor has 8 parameters, which is greater than 7 authorized.

## 203. AYhT_8BBzouB-MG-BCtG

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReader.java:808`
- **Effort**: 5min
- **Created**: 2023-05-25T17:40:22+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 204. AY-YR1zqkrh49lmvlTAe

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reordered/NestedReadSubset.java:46`
- **Effort**: 5min
- **Created**: 2023-05-19T11:18:19+0000
- **Assignee**: alamar@github
- **Message**:
  Rename "text" which hides the field declared at line 31.

## 205. AY-YR1zqkrh49lmvlTAf

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reordered/NestedReadSubset.java:47`
- **Effort**: 5min
- **Created**: 2023-05-19T11:18:19+0000
- **Assignee**: alamar@github
- **Message**:
  Rename "number" which hides the field declared at line 33.

## 206. AZDL7cVK6N0FxHM9EiAk

- **Rule**: `java:S116`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/This0AsTransientTest.java:111`
- **Effort**: 2min
- **Created**: 2023-05-19T11:07:32+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this field "this$0" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 207. AY-YR1klkrh49lmvlS8I

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/MethodReaderWithHistoryTest.java:56`
- **Effort**: 5min
- **Created**: 2023-05-11T11:40:23+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 208. AY-YR12Skrh49lmvlTAx

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MarshallableTest.java:128`
- **Effort**: 0min
- **Created**: 2023-05-11T11:39:44+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 209. AYgaTFsYUudhabb4KKU6

- **Rule**: `java:S1607`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MarshallableTest.java:130`
- **Effort**: 10min
- **Created**: 2023-05-11T11:39:44+0000
- **Assignee**: Unassigned
- **Message**:
  Either add an explanation about why this test is skipped or remove the "@Ignore" annotation.

## 210. AY-YR1_pkrh49lmvlTB2

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextDocumentTest.java:80`
- **Effort**: 0min
- **Created**: 2023-05-11T11:39:44+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 211. AYgaTFzRUudhabb4KKU8

- **Rule**: `java:S1607`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextDocumentTest.java:82`
- **Effort**: 10min
- **Created**: 2023-05-11T11:39:44+0000
- **Assignee**: Unassigned
- **Message**:
  Either add an explanation about why this test is skipped or remove the "@Ignore" annotation.

## 212. AY-YR2Cnkrh49lmvlTCN

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WriteDocumentContextTest.java:111`
- **Effort**: 0min
- **Created**: 2023-05-11T11:39:44+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 213. AYgaTF1NUudhabb4KKU9

- **Rule**: `java:S1607`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WriteDocumentContextTest.java:113`
- **Effort**: 10min
- **Created**: 2023-05-11T11:39:44+0000
- **Assignee**: Unassigned
- **Message**:
  Either add an explanation about why this test is skipped or remove the "@Ignore" annotation.

## 214. AY-YR2Cnkrh49lmvlTCO

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WriteDocumentContextTest.java:126`
- **Effort**: 0min
- **Created**: 2023-05-11T11:39:44+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 215. AYgaTF1NUudhabb4KKU-

- **Rule**: `java:S1607`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WriteDocumentContextTest.java:128`
- **Effort**: 10min
- **Created**: 2023-05-11T11:39:44+0000
- **Assignee**: Unassigned
- **Message**:
  Either add an explanation about why this test is skipped or remove the "@Ignore" annotation.

## 216. AY-YR1pbkrh49lmvlS9J

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/JSON222Test.java:81`
- **Effort**: 0min
- **Created**: 2023-05-11T11:39:44+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 217. AYgaTFfQUudhabb4KKU5

- **Rule**: `java:S1607`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/JSON222Test.java:83`
- **Effort**: 10min
- **Created**: 2023-05-11T11:39:44+0000
- **Assignee**: Unassigned
- **Message**:
  Either add an explanation about why this test is skipped or remove the "@Ignore" annotation.

## 218. AY-YR1kYkrh49lmvlS8G

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/HandleSkippedValueReadsTest.java:56`
- **Effort**: 5min
- **Created**: 2023-05-11T11:39:44+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 219. AZhCOA7hTtJ4wK8ep7DM

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReaderBuilder.java:101`
- **Effort**: 10min
- **Created**: 2023-05-05T14:54:27+0000
- **Assignee**: Unassigned
- **Message**:
  Do not forget to remove this deprecated code someday.

## 220. AZhCOA7hTtJ4wK8ep7DN

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReaderBuilder.java:101`
- **Effort**: 5min
- **Created**: 2023-05-05T14:54:27+0000
- **Assignee**: Unassigned
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 221. AZDL7cwH6N0FxHM9EiIn

- **Rule**: `java:S114`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderBuilderExceptionHandlerTest.java:59`
- **Effort**: 10min
- **Created**: 2023-05-05T14:54:27+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this interface name to match the regular expression '^[A-Z][a-zA-Z0-9]*$'.

## 222. AZDL7cwH6N0FxHM9EiIo

- **Rule**: `java:S114`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderBuilderExceptionHandlerTest.java:64`
- **Effort**: 10min
- **Created**: 2023-05-05T14:54:27+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this interface name to match the regular expression '^[A-Z][a-zA-Z0-9]*$'.

## 223. AZDL7cwH6N0FxHM9EiIp

- **Rule**: `java:S114`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderBuilderExceptionHandlerTest.java:69`
- **Effort**: 10min
- **Created**: 2023-05-05T14:54:27+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this interface name to match the regular expression '^[A-Z][a-zA-Z0-9]*$'.

## 224. AZDL7cwH6N0FxHM9EiIq

- **Rule**: `java:S114`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderBuilderExceptionHandlerTest.java:74`
- **Effort**: 10min
- **Created**: 2023-05-05T14:54:27+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this interface name to match the regular expression '^[A-Z][a-zA-Z0-9]*$'.

## 225. AZDL7cbS6N0FxHM9EiBM

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/MarshallableMethodReaderTest.java:126`
- **Effort**: 5min
- **Created**: 2023-05-05T14:54:27+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 226. AY-YR137krh49lmvlTA-

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/generated/ChronicleWireMethodExample.java:36`
- **Effort**: 1min
- **Created**: 2023-05-03T08:26:42+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 227. AY-YR137krh49lmvlTA_

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/generated/ChronicleWireMethodExample.java:41`
- **Effort**: 1min
- **Created**: 2023-05-03T08:26:42+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 228. AY6FVbEQ5NUPA8B1oXpB

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractCommonMarshallable.java:44`
- **Effort**: 20min
- **Created**: 2023-04-11T17:55:47+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 229. AY6FVbHO5NUPA8B1oXpN

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireIn.java:126`
- **Effort**: 20min
- **Created**: 2023-04-11T17:55:47+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 230. AYd7WB7wWObQ7rqx1URL

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:821`
- **Effort**: 8min
- **Created**: 2023-04-11T17:55:47+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 18 to the 15 allowed.

## 231. AYd7WB7wWObQ7rqx1URM

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:840`
- **Effort**: 5min
- **Created**: 2023-04-11T17:55:47+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 232. AZDL7czU6N0FxHM9EiI3

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/VanillaMethodWriterBuilderVerboseTypesTest.java:64`
- **Effort**: 5min
- **Created**: 2023-04-06T13:29:02+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "str" private field.

## 233. AZDL7czU6N0FxHM9EiI4

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/VanillaMethodWriterBuilderVerboseTypesTest.java:65`
- **Effort**: 5min
- **Created**: 2023-04-06T13:29:02+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "value" private field.

## 234. AYd7WB-xWObQ7rqx1URN

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodWriterBuilder.java:145`
- **Effort**: 5min
- **Created**: 2023-04-06T13:24:42+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 235. AYdIqFO9kGIZoi3kHjiM

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1198`
- **Effort**: 20min
- **Created**: 2023-04-03T19:46:12+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 30 to the 15 allowed.

## 236. AYdIqFQKkGIZoi3kHjiO

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ValueIn.java:1240`
- **Effort**: 20min
- **Created**: 2023-04-03T19:46:12+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Catch Exception instead of Throwable.

## 237. AYdIqFSXkGIZoi3kHjiP

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ValueOut.java:1194`
- **Effort**: 43min
- **Created**: 2023-04-03T19:46:12+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 53 to the 15 allowed.

## 238. AYdIqFdxkGIZoi3kHjiZ

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWireOut.java:1424`
- **Effort**: 8min
- **Created**: 2023-04-03T19:46:12+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 18 to the 15 allowed.

## 239. AYdIqFdxkGIZoi3kHjia

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWireOut.java:1482`
- **Effort**: 6min
- **Created**: 2023-04-03T19:46:12+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## 240. AYdIqE7GkGIZoi3kHjiD

- **Rule**: `java:S5778`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/streams/StreamsDemoTest.java:105`
- **Effort**: 5min
- **Created**: 2023-04-03T19:46:12+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor the body of this try/catch to have only one invocation possibly throwing a runtime
  exception.

## 241. AZDL7cxW6N0FxHM9EiIs

- **Rule**: `java:S114`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/internal/MethodWriterClassNameGeneratorTest.java:105`
- **Effort**: 10min
- **Created**: 2023-03-30T22:52:29+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this interface name to match the regular expression '^[A-Z][a-zA-Z0-9]*$'.

## 242. AY-YR1azkrh49lmvlS7F

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/serializable/SerializableWireTest.java:89`
- **Effort**: 0min
- **Created**: 2023-03-24T13:19:23+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 243. AY-YR2BKkrh49lmvlTCA

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/AbstractTimestampLongConverterJLBHBenchmark.java:50`
- **Effort**: 5min
- **Created**: 2023-02-16T23:23:19+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "jlbh" which hides the field declared at line 28.

## 244. AZDL7cek6N0FxHM9EiBS

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MicroTimestampLongConverterTest.java:29`
- **Effort**: 5min
- **Created**: 2023-02-16T23:23:19+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "TIMESTAMP_STRING_UTC" private field.

## 245. AZDL7cuR6N0FxHM9EiIk

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MilliTimestampLongConverterTest.java:29`
- **Effort**: 5min
- **Created**: 2023-02-16T23:23:19+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "TIMESTAMP_STRING_UTC" private field.

## 246. AZDL7cjj6N0FxHM9EiBu

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/NanoTimestampLongConverterTest.java:30`
- **Effort**: 5min
- **Created**: 2023-02-16T23:23:19+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "TIMESTAMP_STRING_UTC" private field.

## 247. AY-YR1pJkrh49lmvlS9H

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/Issue609Test.java:20`
- **Effort**: 1min
- **Created**: 2023-02-09T13:40:19+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'net.openhft.chronicle.bytes.Bytes'.

## 248. AYYyYdjdbIgzUz0xMioB

- **Rule**: `java:S106`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateJsonSchemaMain.java:76`
- **Effort**: 10min
- **Created**: 2023-02-08T18:54:47+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this use of System.out by a logger.

## 249. AYYyYdjdbIgzUz0xMioA

- **Rule**: `java:S1488`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateJsonSchemaMain.java:95`
- **Effort**: 2min
- **Created**: 2023-02-08T18:54:47+0000
- **Assignee**: Unassigned
- **Message**:
  Immediately return this expression instead of assigning it to the temporary variable "json".

## 250. AZDL7dBm6N0FxHM9EiJs

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42Test.java:115`
- **Effort**: 2min
- **Created**: 2023-02-02T15:10:40+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 251. AYYyYdgDbIgzUz0xMin-

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:753`
- **Effort**: 5min
- **Created**: 2023-01-31T15:21:50+0000
- **Assignee**: Unassigned
- **Message**:
  Complete cases by adding the missing enum constants or add a default case to this switch.

## 252. AYYyYdgDbIgzUz0xMin9

- **Rule**: `java:S1301`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:1052`
- **Effort**: 5min
- **Created**: 2023-01-31T15:21:50+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this "switch" statement by "if" statements to increase readability.

## 253. AYYyYdgDbIgzUz0xMin_

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:1052`
- **Effort**: 5min
- **Created**: 2023-01-31T15:21:50+0000
- **Assignee**: Unassigned
- **Message**:
  Complete cases by adding the missing enum constants or add a default case to this switch.

## 254. AYX-7D10pfy62JdiwBrf

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:665`
- **Effort**: 5min
- **Created**: 2023-01-27T16:00:38+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 255. AYTOpMmaPIXwGdUzbLyj

- **Rule**: `java:S1134`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractWire.java:583`
- **Effort**: 0min
- **Created**: 2022-12-01T11:32:12+0000
- **Assignee**: Unassigned
- **Message**:
  Take the required action to fix the issue indicated by this comment.

## 256. AYTOpMmaPIXwGdUzbLyi

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractWire.java:525`
- **Effort**: 28min
- **Created**: 2022-11-29T17:07:00+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 38 to the 15 allowed.

## 257. AYTOpMuaPIXwGdUzbLyl

- **Rule**: `java:S2065`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMessageHistory.java:72`
- **Effort**: 2min
- **Created**: 2022-11-24T13:25:08+0000
- **Assignee**: glukos@github
- **Message**:
  Remove the "transient" modifier from this field.

## 258. AYTOpMuaPIXwGdUzbLym

- **Rule**: `java:S2065`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMessageHistory.java:73`
- **Effort**: 2min
- **Created**: 2022-11-24T13:25:08+0000
- **Assignee**: glukos@github
- **Message**:
  Remove the "transient" modifier from this field.

## 259. AYTOpMrRPIXwGdUzbLyk

- **Rule**: `java:S1192`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:822`
- **Effort**: 8min
- **Created**: 2022-11-22T15:36:50+0000
- **Assignee**: Unassigned
- **Message**:
  Define a constant instead of duplicating this literal "_valueOut_" 3 times.

## 260. AY-YR18Kkrh49lmvlTBg

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MyClass.java:3`
- **Effort**: 1min
- **Created**: 2022-10-18T07:10:28+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unnecessary import: same package classes are always implicitly imported.

## 261. AY-YR18Vkrh49lmvlTBh

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MyClass3.java:3`
- **Effort**: 1min
- **Created**: 2022-10-18T07:10:28+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unnecessary import: same package classes are always implicitly imported.

## 262. AY-YR1yzkrh49lmvlTAV

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireTypeConverterTest.java:38`
- **Effort**: 5min
- **Created**: 2022-10-18T07:10:28+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.lang.Exception', as it cannot be thrown from
  method's body.

## 263. AY-YR1yzkrh49lmvlTAX

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireTypeConverterTest.java:92`
- **Effort**: 5min
- **Created**: 2022-10-18T07:10:28+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "yaml" which hides the field declared at line 13.

## 264. AYPS2yErKLimjuBI1yKV

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:1025`
- **Effort**: 5min
- **Created**: 2022-10-13T19:38:08+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 265. AY-YR2Eakrh49lmvlTCr

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:117`
- **Effort**: 5min
- **Created**: 2022-09-28T11:15:39+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 266. AYN6buNmoTzq4a-y4aWR

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:93`
- **Effort**: 5min
- **Created**: 2022-09-26T15:33:17+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 267. AYN6buG9oTzq4a-y4aV8

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:63`
- **Effort**: 5min
- **Created**: 2022-09-26T15:33:17+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 268. AYN6buMxoTzq4a-y4aWF

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWireOut.java:245`
- **Effort**: 7min
- **Created**: 2022-09-26T15:27:08+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## 269. AYN6buMxoTzq4a-y4aWG

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWireOut.java:356`
- **Effort**: 6min
- **Created**: 2022-09-26T15:27:08+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## 270. AYN6buMxoTzq4a-y4aWH

- **Rule**: `java:S3358`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWireOut.java:635`
- **Effort**: 5min
- **Created**: 2022-09-26T15:27:08+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested ternary operation into an independent statement.

## 271. AYN6buMxoTzq4a-y4aWK

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWireOut.java:1008`
- **Effort**: 9min
- **Created**: 2022-09-26T15:27:08+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## 272. AYN6buMxoTzq4a-y4aWL

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWireOut.java:1290`
- **Effort**: 5min
- **Created**: 2022-09-26T15:27:08+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "leaf" which hides the field declared at line 526.

## 273. AYN6buMxoTzq4a-y4aWM

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWireOut.java:1342`
- **Effort**: 5min
- **Created**: 2022-09-26T15:27:08+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "leaf" which hides the field declared at line 526.

## 274. AYN6buMxoTzq4a-y4aWP

- **Rule**: `java:S1155`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWireOut.java:1505`
- **Effort**: 2min
- **Created**: 2022-09-26T15:27:08+0000
- **Assignee**: Unassigned
- **Message**:
  Use isEmpty() to check whether the collection is empty or not.

## 275. AY-YR16ykrh49lmvlTBV

- **Rule**: `java:S6213`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONWireTest.java:663`
- **Effort**: 5min
- **Created**: 2022-09-12T12:09:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this variable to not match a restricted identifier.

## 276. AY-YR1nRkrh49lmvlS8S

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/MethodIdTest.java:119`
- **Effort**: 5min
- **Created**: 2022-09-09T16:51:43+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 277. AY-YR1nRkrh49lmvlS8T

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/MethodIdTest.java:137`
- **Effort**: 5min
- **Created**: 2022-09-09T16:51:43+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 278. AYMhHIQaFLjHBrxPoPTm

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:822`
- **Effort**: 1min
- **Created**: 2022-09-09T07:16:48+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 279. AYMc0Oe9n1ybGk_Xqmgf

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:824`
- **Effort**: 1min
- **Created**: 2022-09-08T11:16:05+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 280. AYMZAH_PJYm5AhkRvrec

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodWriterBuilder.java:113`
- **Effort**: 0min
- **Created**: 2022-09-07T17:29:36+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 281. AYL0pZouSxNN11n7qag9

- **Rule**: `java:S3415`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2580`
- **Effort**: 2min
- **Created**: 2022-08-31T16:03:58+0000
- **Assignee**: Unassigned
- **Message**:
  Swap these 2 arguments so they are in the correct order: expected value, actual value.

## 282. AY-YR1yYkrh49lmvlTAT

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireTest.java:1766`
- **Effort**: 5min
- **Created**: 2022-08-16T23:26:47+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "bytes" which hides the field declared at line 62.

## 283. AY6FVbDR5NUPA8B1oXo3

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/MarshallableOut.java:153`
- **Effort**: 20min
- **Created**: 2022-08-03T14:11:09+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Catch Exception instead of Throwable.

## 284. AZDL7c_j6N0FxHM9EiJm

- **Rule**: `java:S1220`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChainedMethodsTestChainedMethodsTest$1MethodReader.java`
- **Effort**: 10min
- **Created**: 2022-08-03T13:35:14+0000
- **Assignee**: Unassigned
- **Message**:
  Move this file to a named package.

## 285. AY6FVbH65NUPA8B1oXpT

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:1320`
- **Effort**: 20min
- **Created**: 2022-08-01T11:56:24+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 286. AYI2vPDGgmW8QGaja60V

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:437`
- **Effort**: 0min
- **Created**: 2022-07-25T19:01:16+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 287. AYIhxyfbgmW8QGajkaCv

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:750`
- **Effort**: 20min
- **Created**: 2022-07-21T17:20:49+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 288. AYISQwSHQRDjcrSiuUp7

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1830`
- **Effort**: 5min
- **Created**: 2022-07-18T17:02:10+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add a default case to this switch.

## 289. AZDL7czH6N0FxHM9EiI2

- **Rule**: `java:S1488`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/UpdateInterceptorReturnTypeTest.java:53`
- **Effort**: 2min
- **Created**: 2022-07-14T10:35:58+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Immediately return this expression instead of assigning it to the temporary variable "wire".

## 290. AZDL7ccG6N0FxHM9EiBO

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/ChronicleEvent.java:47`
- **Effort**: 5min
- **Created**: 2022-06-30T07:27:43+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "text3" private field.

## 291. AYGwUycNTxicQLdRiYqY

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:780`
- **Effort**: 44min
- **Created**: 2022-06-29T16:37:00+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 54 to the 15 allowed.

## 292. AY-YR2Fnkrh49lmvlTEL

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1272`
- **Effort**: 5min
- **Created**: 2022-06-29T15:29:44+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 293. AYGwFBSws3HCEZt8OsbP

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:804`
- **Effort**: 16min
- **Created**: 2022-06-29T15:27:47+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 26 to the 15 allowed.

## 294. AYGwFBSws3HCEZt8OsbR

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:845`
- **Effort**: 5min
- **Created**: 2022-06-29T15:27:47+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "className" which hides the field declared at line 131.

## 295. AZDL7c5L6N0FxHM9EiJT

- **Rule**: `java:S116`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TriviallyCopyableJLBH.java:46`
- **Effort**: 2min
- **Created**: 2022-06-29T14:56:54+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this field "CPU" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 296. AY-YR1xQkrh49lmvlTAA

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/CreateUtil.java:67`
- **Effort**: 5min
- **Created**: 2022-06-28T12:25:17+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 297. AZDL7cqj6N0FxHM9EiCG

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/reduction/CollectorTest.java:45`
- **Effort**: 5min
- **Created**: 2022-06-28T12:25:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "MARKET_DATA_SET" private field.

## 298. AZDL7crA6N0FxHM9EiCH

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/reduction/MethodWriterTest.java:41`
- **Effort**: 5min
- **Created**: 2022-06-28T12:25:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "MARKET_DATA_SET" private field.

## 299. AY-YR1wSkrh49lmvlS_2

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/reduction/StreamingDemoMain.java:51`
- **Effort**: 5min
- **Created**: 2022-06-28T12:25:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "count" local variable.

## 300. AY-YR1wSkrh49lmvlS_3

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/reduction/StreamingDemoMain.java:57`
- **Effort**: 5min
- **Created**: 2022-06-28T12:25:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "maxIndex" local variable.

## 301. AY-YR1wSkrh49lmvlS_4

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/reduction/StreamingDemoMain.java:60`
- **Effort**: 5min
- **Created**: 2022-06-28T12:25:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "listing" local variable.

## 302. AY-YR1wSkrh49lmvlS_5

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/reduction/StreamingDemoMain.java:65`
- **Effort**: 5min
- **Created**: 2022-06-28T12:25:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "symbolsStartingWithS" local variable.

## 303. AY-YR1wSkrh49lmvlS_6

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/reduction/StreamingDemoMain.java:83`
- **Effort**: 5min
- **Created**: 2022-06-28T12:25:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "latestAppleMarketData" local variable.

## 304. AY-YR1wSkrh49lmvlS_7

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/reduction/StreamingDemoMain.java:86`
- **Effort**: 5min
- **Created**: 2022-06-28T12:25:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "liveQueueBackedMap" local variable.

## 305. AY-YR1wSkrh49lmvlS_8

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/reduction/StreamingDemoMain.java:90`
- **Effort**: 5min
- **Created**: 2022-06-28T12:25:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "latestProtected" local variable.

## 306. AY-YR1wSkrh49lmvlS_9

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/reduction/StreamingDemoMain.java:115`
- **Effort**: 5min
- **Created**: 2022-06-28T12:25:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "averageApplePrice" local variable.

## 307. AY-YR1wSkrh49lmvlS_-

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/reduction/StreamingDemoMain.java:135`
- **Effort**: 5min
- **Created**: 2022-06-28T12:25:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "queueBackedMap" local variable.

## 308. AY-YR1w5krh49lmvlS__

- **Rule**: `java:S1144`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/streams/StreamsTest.java:143`
- **Effort**: 2min
- **Created**: 2022-06-28T12:25:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused private "wires" method.

## 309. AYGlj8ghWdr4r1mwtkGw

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:566`
- **Effort**: 1min
- **Created**: 2022-06-27T14:27:25+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 310. AYGlj8ghWdr4r1mwtkGx

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:877`
- **Effort**: 1min
- **Created**: 2022-06-27T14:27:25+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 311. AYGkCNBJoBDu_8sAuH1L

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:2196`
- **Effort**: 30min
- **Created**: 2022-06-27T07:20:16+0000
- **Assignee**: JerryShea@github
- **Message**:
  This accessibility bypass should be removed.

## 312. AYGkCNBJoBDu_8sAuH1M

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:2202`
- **Effort**: 30min
- **Created**: 2022-06-27T07:20:16+0000
- **Assignee**: JerryShea@github
- **Message**:
  This accessibility bypass should be removed.

## 313. AYGCIs-WC--Kxxm5P9g0

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:755`
- **Effort**: 0min
- **Created**: 2022-06-20T17:21:41+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 314. AYGCIs-WC--Kxxm5P9gz

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:763`
- **Effort**: 9min
- **Created**: 2022-06-20T17:21:41+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## 315. AYGCIs-WC--Kxxm5P9gw

- **Rule**: `java:S1121`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:774`
- **Effort**: 5min
- **Created**: 2022-06-20T17:21:41+0000
- **Assignee**: Unassigned
- **Message**:
  Extract the assignment out of this expression.

## 316. AY-YR1r7krh49lmvlS-7

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2478`
- **Effort**: 5min
- **Created**: 2022-06-20T13:03:16+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 317. AY-YR1r7krh49lmvlS-8

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2511`
- **Effort**: 5min
- **Created**: 2022-06-20T13:03:16+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 318. AY-YR2Fnkrh49lmvlTEr

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1904`
- **Effort**: 5min
- **Created**: 2022-06-20T13:03:16+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 319. AYF_0q6IZ12bDu_Jdwt0

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:818`
- **Effort**: 1min
- **Created**: 2022-06-20T06:34:55+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 320. AYF_0q6IZ12bDu_Jdwt1

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:835`
- **Effort**: 1min
- **Created**: 2022-06-20T06:34:55+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 321. AYF_0q6IZ12bDu_Jdwt2

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:847`
- **Effort**: 1min
- **Created**: 2022-06-20T06:34:55+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 322. AYF_0q6IZ12bDu_Jdwt3

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:859`
- **Effort**: 1min
- **Created**: 2022-06-20T06:34:55+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 323. AYF_0rE_Z12bDu_Jdwt7

- **Rule**: `java:S112`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextMethodWriterInvocationHandler.java:145`
- **Effort**: 20min
- **Created**: 2022-06-20T06:34:55+0000
- **Assignee**: Unassigned
- **Message**:
  Define and throw a dedicated exception instead of using a generic one.

## 324. AY-YR2Fnkrh49lmvlTEC

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:913`
- **Effort**: 5min
- **Created**: 2022-06-15T15:36:30+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 325. AY-YR2Fnkrh49lmvlTED

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:950`
- **Effort**: 5min
- **Created**: 2022-06-15T15:36:30+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 326. AY-YR2Fnkrh49lmvlTEb

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1611`
- **Effort**: 5min
- **Created**: 2022-06-15T15:36:30+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 327. AY-YR2Fnkrh49lmvlTEg

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1721`
- **Effort**: 5min
- **Created**: 2022-06-15T15:36:30+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.io.IOException', as it cannot be thrown from
  method's body.

## 328. AYFcQm8rdpFp6A2xXzAG

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1779`
- **Effort**: 20min
- **Created**: 2022-06-13T08:50:18+0000
- **Assignee**: JerryShea@github
- **Message**:
  Remove usage of generic wildcard type.

## 329. AYFcQm8rdpFp6A2xXzAD

- **Rule**: `java:S1066`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:2948`
- **Effort**: 5min
- **Created**: 2022-06-13T08:50:18+0000
- **Assignee**: Unassigned
- **Message**:
  Merge this if statement with the enclosing one.

## 330. AYFcQm8rdpFp6A2xXzAE

- **Rule**: `java:S1066`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:2968`
- **Effort**: 5min
- **Created**: 2022-06-13T08:50:18+0000
- **Assignee**: Unassigned
- **Message**:
  Merge this if statement with the enclosing one.

## 331. AYFcQnGvdpFp6A2xXzAU

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:819`
- **Effort**: 1min
- **Created**: 2022-06-13T08:50:18+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 332. AYFcQnM9dpFp6A2xXzAj

- **Rule**: `java:S112`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:756`
- **Effort**: 20min
- **Created**: 2022-06-13T08:50:18+0000
- **Assignee**: Unassigned
- **Message**:
  Define and throw a dedicated exception instead of using a generic one.

## 333. AYFcQnM9dpFp6A2xXzAr

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1754`
- **Effort**: 30min
- **Created**: 2022-06-13T08:50:18+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 334. AYFcQnM9dpFp6A2xXzAs

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1759`
- **Effort**: 30min
- **Created**: 2022-06-13T08:50:18+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 335. AYFcQnM9dpFp6A2xXzAk

- **Rule**: `java:S1066`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1777`
- **Effort**: 5min
- **Created**: 2022-06-13T08:50:18+0000
- **Assignee**: Unassigned
- **Message**:
  Merge this if statement with the enclosing one.

## 336. AYFNgmbCvKVKX6weDQYe

- **Rule**: `java:S1192`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:823`
- **Effort**: 10min
- **Created**: 2022-06-10T12:06:07+0000
- **Assignee**: Unassigned
- **Message**:
  Define a constant instead of duplicating this literal "private final %s %sConverter =
  ObjectUtils.newInstance(%s.class); " 4 times.

## 337. AYFNgmbCvKVKX6weDQYh

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:823`
- **Effort**: 1min
- **Created**: 2022-06-10T12:06:07+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 338. AYFNgmbCvKVKX6weDQYi

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:826`
- **Effort**: 1min
- **Created**: 2022-06-10T12:06:07+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 339. AYFNgmbCvKVKX6weDQYj

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:838`
- **Effort**: 1min
- **Created**: 2022-06-10T12:06:07+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 340. AYFNgmbCvKVKX6weDQYk

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:841`
- **Effort**: 1min
- **Created**: 2022-06-10T12:06:07+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 341. AYFNgmbCvKVKX6weDQYl

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:850`
- **Effort**: 1min
- **Created**: 2022-06-10T12:06:07+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 342. AYFNgmbCvKVKX6weDQYm

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:853`
- **Effort**: 1min
- **Created**: 2022-06-10T12:06:07+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 343. AYFcQm2OdpFp6A2xXzAA

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/converter/PowerOfTwoLongConverter.java:182`
- **Effort**: 5min
- **Created**: 2022-06-10T11:14:47+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 344. AYFcQm5WdpFp6A2xXzAC

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/converter/SymbolsLongConverter.java:189`
- **Effort**: 5min
- **Created**: 2022-06-10T11:14:47+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 345. AZS37q6nIVqiQpr6kOmR

- **Rule**: `java:S1612`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderDelegationTest.java:279`
- **Effort**: 2min
- **Created**: 2022-05-31T11:58:25+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this lambda with method reference 'myInterface::myCall'.

## 346. AZS37q6nIVqiQpr6kOmT

- **Rule**: `java:S1612`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderDelegationTest.java:344`
- **Effort**: 2min
- **Created**: 2022-05-31T11:58:25+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this lambda with method reference 'myInterface::myCall'.

## 347. AY-YR1zWkrh49lmvlTAc

- **Rule**: `java:S1611`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderDelegationTest.java:388`
- **Effort**: 2min
- **Created**: 2022-05-31T11:58:25+0000
- **Assignee**: JerryShea@github
- **Message**:
  Remove the parentheses around the "l" parameter

## 348. AY-YR1zWkrh49lmvlTAd

- **Rule**: `java:S1611`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderDelegationTest.java:392`
- **Effort**: 2min
- **Created**: 2022-05-31T11:58:25+0000
- **Assignee**: JerryShea@github
- **Message**:
  Remove the parentheses around the "l" parameter

## 349. AZS37q6nIVqiQpr6kOmV

- **Rule**: `java:S1612`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderDelegationTest.java:392`
- **Effort**: 2min
- **Created**: 2022-05-31T11:58:25+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this lambda with method reference 'myInterface::myCall'.

## 350. AYDMCktgYgbWbEj7VDgk

- **Rule**: `java:S108`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:189`
- **Effort**: 5min
- **Created**: 2022-05-16T08:44:01+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this block of code, fill it in, or add a comment explaining why it is empty.

## 351. AZS37q6nIVqiQpr6kOmS

- **Rule**: `java:S1612`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderDelegationTest.java:282`
- **Effort**: 2min
- **Created**: 2022-05-11T22:07:36+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this lambda with method reference 'reader::readOne'.

## 352. AZS37q6nIVqiQpr6kOmU

- **Rule**: `java:S1612`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderDelegationTest.java:350`
- **Effort**: 2min
- **Created**: 2022-05-11T22:07:36+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this lambda with method reference 'reader::readOne'.

## 353. AZS37q6nIVqiQpr6kOmW

- **Rule**: `java:S1612`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderDelegationTest.java:398`
- **Effort**: 2min
- **Created**: 2022-05-11T22:07:36+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this lambda with method reference 'reader::readOne'.

## 354. AYCuhKMmr5ABGvPcHeoI

- **Rule**: `java:S5785`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/JSONWithAMapTest.java:109`
- **Effort**: 2min
- **Created**: 2022-05-10T15:02:48+0000
- **Assignee**: Unassigned
- **Message**:
  Use assertEquals instead.

## 355. AYCs9tWSWFYurb1OW3yR

- **Rule**: `java:S5785`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TestJsonIssue467.java:82`
- **Effort**: 2min
- **Created**: 2022-05-10T07:00:56+0000
- **Assignee**: Unassigned
- **Message**:
  Use assertEquals instead.

## 356. AY-YR1r7krh49lmvlS-L

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1240`
- **Effort**: 5min
- **Created**: 2022-05-09T14:15:25+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 357. AY-YR1r7krh49lmvlS-Q

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1390`
- **Effort**: 5min
- **Created**: 2022-05-09T14:15:25+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 358. AY-YR1r7krh49lmvlS-S

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1453`
- **Effort**: 5min
- **Created**: 2022-05-09T14:15:25+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 359. AY-YR1r7krh49lmvlS-U

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1506`
- **Effort**: 5min
- **Created**: 2022-05-09T14:15:25+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 360. AY-YR1r7krh49lmvlS-o

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1849`
- **Effort**: 5min
- **Created**: 2022-05-09T14:15:25+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 361. AYCmHIrrONEMdKy3uS6b

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/LongArrayValueBitSet.java:568`
- **Effort**: 0min
- **Created**: 2022-05-08T19:05:14+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 362. AYCmHIhaONEMdKy3uS6W

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:699`
- **Effort**: 10min
- **Created**: 2022-05-08T14:25:51+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 20 to the 15 allowed.

## 363. AY-YR2Bokrh49lmvlTCJ

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MarshallableOutBuilderTest.java:167`
- **Effort**: 5min
- **Created**: 2022-05-08T10:25:53+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.lang.InterruptedException', as it cannot be thrown
  from method's body.

## 364. AYCjF6A_n6UwdMIzgcGW

- **Rule**: `java:S116`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireType.java:62`
- **Effort**: 2min
- **Created**: 2022-05-08T09:54:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this field "TEXT_AS_YAML" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 365. AYCP0PxFCzIktaRa5UEB

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:572`
- **Effort**: 6min
- **Created**: 2022-05-04T16:04:11+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## 366. AYCP0PxFCzIktaRa5UEA

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:715`
- **Effort**: 5min
- **Created**: 2022-05-04T16:04:11+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 367. AY-YR16ykrh49lmvlTBR

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONWireTest.java:62`
- **Effort**: 5min
- **Created**: 2022-05-04T08:10:48+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 368. AY-YR16ykrh49lmvlTBS

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONWireTest.java:282`
- **Effort**: 5min
- **Created**: 2022-05-04T08:10:48+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 369. AYCMoy9sDcVGm2iIoLgj

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/fieldinfo/UnsafeFieldInfo.java:29`
- **Effort**: 30min
- **Created**: 2022-05-04T00:18:22+0000
- **Assignee**: nicktindall@github
- **Message**:
  Override the "equals" method in this class.

## 370. AYCMoy9tDcVGm2iIoLgk

- **Rule**: `java:S2065`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/fieldinfo/UnsafeFieldInfo.java:34`
- **Effort**: 2min
- **Created**: 2022-05-04T00:18:22+0000
- **Assignee**: nicktindall@github
- **Message**:
  Remove the "transient" modifier from this field.

## 371. AYFOAp9V_DYcfppPYyW1

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/extractor/DocumentExtractorUtil.java:70`
- **Effort**: 20min
- **Created**: 2022-04-29T13:49:21+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 372. AYFOApvQ_DYcfppPYyW0

- **Rule**: `java:S2925`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/domestic/streaming/reduction/StreamingDemoMain.java:145`
- **Effort**: 20min
- **Created**: 2022-04-29T13:49:21+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this use of "Thread.sleep()".

## 373. AZMw7pYbYk7po6Fb1sLg

- **Rule**: `java:S1905`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:3336`
- **Effort**: 5min
- **Created**: 2022-04-14T00:41:16+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unnecessary cast to "Bytes".

## 374. AYAq7B7SlbqSPYHOMc4p

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ReadAnyWire.java:158`
- **Effort**: 20min
- **Created**: 2022-04-14T00:41:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 375. AYAq7B5elbqSPYHOMc4o

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ValueIn.java:139`
- **Effort**: 20min
- **Created**: 2022-04-14T00:41:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 376. AYAq7CD2lbqSPYHOMc4s

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:1696`
- **Effort**: 20min
- **Created**: 2022-04-14T00:41:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 377. AY-YR1r7krh49lmvlS-K

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1239`
- **Effort**: 5min
- **Created**: 2022-04-14T00:41:16+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "bytes" which hides the field declared at line 69.

## 378. AY-YR1r7krh49lmvlS-P

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1389`
- **Effort**: 5min
- **Created**: 2022-04-14T00:41:16+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "bytes" which hides the field declared at line 69.

## 379. AY-YR1r7krh49lmvlS-R

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1452`
- **Effort**: 5min
- **Created**: 2022-04-14T00:41:16+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "bytes" which hides the field declared at line 69.

## 380. AY-YR1r7krh49lmvlS-T

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1505`
- **Effort**: 5min
- **Created**: 2022-04-14T00:41:16+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "bytes" which hides the field declared at line 69.

## 381. AY-YR1r7krh49lmvlS-Z

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1624`
- **Effort**: 5min
- **Created**: 2022-04-14T00:41:16+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "bytes" which hides the field declared at line 69.

## 382. AY-YR1r7krh49lmvlS-b

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1642`
- **Effort**: 5min
- **Created**: 2022-04-14T00:41:16+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "bytes" which hides the field declared at line 69.

## 383. AZDL7ciu6N0FxHM9EiBj

- **Rule**: `java:S116`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2641`
- **Effort**: 2min
- **Created**: 2022-04-14T00:41:16+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this field "A" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 384. AZDL7ciu6N0FxHM9EiBk

- **Rule**: `java:S116`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2642`
- **Effort**: 2min
- **Created**: 2022-04-14T00:41:16+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this field "B" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 385. AZDL7ciu6N0FxHM9EiBm

- **Rule**: `java:S116`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2644`
- **Effort**: 2min
- **Created**: 2022-04-14T00:41:16+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this field "D" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 386. AYCjMGOQPOFyXWRWiuqu

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/FileMarshallableOut.java:128`
- **Effort**: 30min
- **Created**: 2022-04-13T07:47:55+0000
- **Assignee**: Unassigned
- **Message**:
  Override the "equals" method in this class.

## 387. AYCjMGT7POFyXWRWiuq4

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireDumper.java:191`
- **Effort**: 33min
- **Created**: 2022-04-13T05:34:51+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 43 to the 15 allowed.

## 388. AYAI-FvijowxqCVRytF6

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ValueOut.java:1199`
- **Effort**: 5min
- **Created**: 2022-04-08T11:38:08+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 389. AY_Fi6ydTbFMTgi4MX0i

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireAgitatorTest.java:26`
- **Effort**: 1min
- **Created**: 2022-04-06T01:19:07+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'java.util.Map'.

## 390. AX_-y_IhhnkG7wYsp1B5

- **Rule**: `java:S5778`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/UnknownEnumTest.java:77`
- **Effort**: 5min
- **Created**: 2022-04-06T01:19:07+0000
- **Assignee**: nicktindall@github
- **Message**:
  Refactor the code of the lambda to have only one invocation possibly throwing a runtime exception.

## 391. AZDL7cpX6N0FxHM9EiCD

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/UnknownEnumTest.java:121`
- **Effort**: 2min
- **Created**: 2022-04-06T01:19:07+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 392. AZDL7cpX6N0FxHM9EiCE

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/UnknownEnumTest.java:122`
- **Effort**: 2min
- **Created**: 2022-04-06T01:19:07+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 393. AY-YR2BYkrh49lmvlTCB

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlSpecificationTest.java:43`
- **Effort**: 5min
- **Created**: 2022-03-23T18:14:43+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 394. AY-YR2BYkrh49lmvlTCC

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlSpecificationTest.java:80`
- **Effort**: 0min
- **Created**: 2022-03-23T18:14:43+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 395. AY-YR2BYkrh49lmvlTCG

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlSpecificationTest.java:86`
- **Effort**: 0min
- **Created**: 2022-03-23T18:14:43+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 396. AY-YR2Fnkrh49lmvlTEB

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:901`
- **Effort**: 5min
- **Created**: 2022-03-23T18:14:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 397. AZDL7dA-6N0FxHM9EiJq

- **Rule**: `java:S3008`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/KubernetesYamlTest.java:36`
- **Effort**: 2min
- **Created**: 2022-03-23T12:23:56+0000
- **Assignee**: alamar@github
- **Message**:
  Rename this field "DIR" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 398. AZS37rJDIVqiQpr6kOml

- **Rule**: `java:S2093`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/KubernetesYamlTest.java:47`
- **Effort**: 15min
- **Created**: 2022-03-23T12:23:56+0000
- **Assignee**: Unassigned
- **Message**:
  Change this "try" to a try-with-resources.

## 399. AY-YR2Fnkrh49lmvlTEA

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:877`
- **Effort**: 5min
- **Created**: 2022-03-23T12:23:56+0000
- **Assignee**: alamar@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 400. AY-YR2Fnkrh49lmvlTEq

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1890`
- **Effort**: 5min
- **Created**: 2022-03-23T12:23:56+0000
- **Assignee**: alamar@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 401. AY-YR1sykrh49lmvlS_C

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/examples/MessageRoutingExample.java:78`
- **Effort**: 5min
- **Created**: 2022-03-16T10:25:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 402. AY-YR1sykrh49lmvlS_D

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/examples/MessageRoutingExample.java:95`
- **Effort**: 5min
- **Created**: 2022-03-15T16:45:14+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 403. AZDL7cWn6N0FxHM9EiAn

- **Rule**: `java:S3008`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/ByteArrayResuseTest.java:115`
- **Effort**: 2min
- **Created**: 2022-03-09T10:49:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this field "SELF_DESCRIBING" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 404. AY6FVbFY5NUPA8B1oXpE

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractWire.java:652`
- **Effort**: 10min
- **Created**: 2022-03-04T10:49:54+0000
- **Assignee**: Unassigned
- **Message**:
  Do not forget to remove this deprecated code someday.

## 405. AY6FVbFY5NUPA8B1oXpF

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractWire.java:652`
- **Effort**: 5min
- **Created**: 2022-03-04T10:49:54+0000
- **Assignee**: Unassigned
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 406. AY-YR1xqkrh49lmvlTAC

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/SkipValueTest.java:83`
- **Effort**: 5min
- **Created**: 2022-03-04T09:11:12+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 407. AY-YR1xqkrh49lmvlTAD

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/SkipValueTest.java:105`
- **Effort**: 5min
- **Created**: 2022-03-04T09:08:03+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 408. AY-YR1xqkrh49lmvlTAE

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/SkipValueTest.java:116`
- **Effort**: 5min
- **Created**: 2022-03-04T09:08:03+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 409. AY-YR1xqkrh49lmvlTAF

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/SkipValueTest.java:126`
- **Effort**: 5min
- **Created**: 2022-03-04T09:08:03+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 410. AY-YR1xqkrh49lmvlTAG

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/SkipValueTest.java:128`
- **Effort**: 5min
- **Created**: 2022-03-04T09:08:03+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 411. AY-YR1xqkrh49lmvlTAH

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/SkipValueTest.java:132`
- **Effort**: 5min
- **Created**: 2022-03-04T09:08:03+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 412. AY-YR1xqkrh49lmvlTAI

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/SkipValueTest.java:134`
- **Effort**: 5min
- **Created**: 2022-03-04T09:08:03+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 413. AY-YR1kYkrh49lmvlS8H

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/HandleSkippedValueReadsTest.java:55`
- **Effort**: 0min
- **Created**: 2022-03-03T17:35:38+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 414. AX9P8SkkFfFFIRtc3qFq

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:542`
- **Effort**: 34min
- **Created**: 2022-03-02T17:50:03+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 44 to the 15 allowed.

## 415. AX9P8SkkFfFFIRtc3qFr

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:572`
- **Effort**: 1min
- **Created**: 2022-03-02T17:50:03+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 416. AX9P8SqCFfFFIRtc3qF-

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReader.java:418`
- **Effort**: 16min
- **Created**: 2022-03-02T17:50:03+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 26 to the 15 allowed.

## 417. AX9P8SkkFfFFIRtc3qFn

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:202`
- **Effort**: 22min
- **Created**: 2022-03-02T17:12:01+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 32 to the 15 allowed.

## 418. AX9P8SkkFfFFIRtc3qFo

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:269`
- **Effort**: 1min
- **Created**: 2022-03-02T17:12:01+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 419. AX9P8SkkFfFFIRtc3qFp

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:323`
- **Effort**: 1min
- **Created**: 2022-03-02T17:12:01+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 420. AX9P8SqCFfFFIRtc3qGB

- **Rule**: `java:S107`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReader.java:418`
- **Effort**: 20min
- **Created**: 2022-02-25T11:38:26+0000
- **Assignee**: Unassigned
- **Message**:
  Method has 11 parameters, which is greater than 7 authorized.

## 421. AX9P8SqCFfFFIRtc3qF_

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReader.java:530`
- **Effort**: 11min
- **Created**: 2022-02-25T11:38:26+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 21 to the 15 allowed.

## 422. AX9P8SqCFfFFIRtc3qGA

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReader.java:706`
- **Effort**: 7min
- **Created**: 2022-02-25T11:38:26+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## 423. AY-YR1-Gkrh49lmvlTBu

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChronicleBitSetTest.java:88`
- **Effort**: 5min
- **Created**: 2022-02-24T09:36:17+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused method parameter "bit".

## 424. AZDL7c5z6N0FxHM9EiJU

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChronicleBitSetTest.java:88`
- **Effort**: 5min
- **Created**: 2022-02-24T09:36:17+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 425. AX8r_qT619JBuIecpRnW

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:1122`
- **Effort**: 5min
- **Created**: 2022-02-16T15:17:43+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 426. AX8r_qQL19JBuIecpRnM

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:640`
- **Effort**: 17min
- **Created**: 2022-02-16T14:14:40+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## 427. AX8r_qQL19JBuIecpRnN

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:900`
- **Effort**: 1min
- **Created**: 2022-02-16T14:14:40+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 428. AY-YR15rkrh49lmvlTBK

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/EgMain.java:42`
- **Effort**: 5min
- **Created**: 2022-02-15T19:06:58+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 429. AX8r_qQ119JBuIecpRnO

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/LongValueBitSet.java:424`
- **Effort**: 5min
- **Created**: 2022-01-27T17:42:48+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 430. AY-YR1pbkrh49lmvlS9I

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/JSON222Test.java:50`
- **Effort**: 5min
- **Created**: 2022-01-26T22:39:09+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused method parameter "fileName".

## 431. AZDL7ckF6N0FxHM9EiBv

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/examples/WireExamples1.java:55`
- **Effort**: 5min
- **Created**: 2021-12-20T11:51:57+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "number" private field.

## 432. AZDL7ckF6N0FxHM9EiBw

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/examples/WireExamples1.java:56`
- **Effort**: 5min
- **Created**: 2021-12-20T11:51:57+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "driver" private field.

## 433. AX8r_qLO19JBuIecpRnE

- **Rule**: `java:S2696`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ValueOut.java:1352`
- **Effort**: 20min
- **Created**: 2021-12-13T17:28:33+0000
- **Assignee**: Unassigned
- **Message**:
  Make the enclosing method "static" or remove this set.

## 434. AX8r_qUe19JBuIecpRnZ

- **Rule**: `java:S1141`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1399`
- **Effort**: 20min
- **Created**: 2021-11-29T15:52:15+0000
- **Assignee**: minborg@github
- **Message**:
  Extract this nested try block into a separate method.

## 435. AZDL7czo6N0FxHM9EiI5

- **Rule**: `java:S1124`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/AbstractUntypedFieldTest.java:105`
- **Effort**: 2min
- **Created**: 2021-11-29T15:52:15+0000
- **Assignee**: Unassigned
- **Message**:
  Reorder the modifiers to comply with the Java Language Specification.

## 436. AX8r_qRx19JBuIecpRnT

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:1406`
- **Effort**: 9min
- **Created**: 2021-11-25T12:37:05+0000
- **Assignee**: minborg@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## 437. AX8r_qRx19JBuIecpRnU

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:1447`
- **Effort**: 0min
- **Created**: 2021-11-25T12:37:05+0000
- **Assignee**: minborg@github
- **Message**:
  Complete the task associated to this TODO comment.

## 438. AX8r_qRx19JBuIecpRnV

- **Rule**: `java:S1479`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:1497`
- **Effort**: 30min
- **Created**: 2021-11-23T08:20:39+0000
- **Assignee**: minborg@github
- **Message**:
  Reduce the number of non-empty switch cases from 31 to at most 30.

## 439. AY-YR13kkrh49lmvlTA7

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/SerializableObjectTest.java:228`
- **Effort**: 5min
- **Created**: 2021-11-23T08:20:39+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 440. AY-YR13kkrh49lmvlTA8

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/SerializableObjectTest.java:237`
- **Effort**: 5min
- **Created**: 2021-11-23T08:20:39+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 441. AX8r_qUe19JBuIecpRnY

- **Rule**: `java:S1168`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:376`
- **Effort**: 30min
- **Created**: 2021-11-11T11:02:38+0000
- **Assignee**: Unassigned
- **Message**:
  Return an empty array instead of null.

## 442. AX8r_qFh19JBuIecpRm9

- **Rule**: `java:S5785`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/map/MapCustomTest.java:53`
- **Effort**: 2min
- **Created**: 2021-11-11T11:02:38+0000
- **Assignee**: Unassigned
- **Message**:
  Use assertEquals instead.

## 443. AY-YR19Kkrh49lmvlTBm

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/map/MapCustomTest.java:69`
- **Effort**: 5min
- **Created**: 2021-11-11T11:02:38+0000
- **Assignee**: alamar@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 444. AZDL7c4o6N0FxHM9EiJP

- **Rule**: `java:S119`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/map/MapCustomTest.java:112`
- **Effort**: 10min
- **Created**: 2021-11-11T11:02:38+0000
- **Assignee**: alamar@github
- **Message**:
  Rename this generic name to match the regular expression '^[A-Z][0-9]?$'.

## 445. AZDL7c4o6N0FxHM9EiJQ

- **Rule**: `java:S119`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/map/MapCustomTest.java:119`
- **Effort**: 10min
- **Created**: 2021-11-11T11:02:38+0000
- **Assignee**: alamar@github
- **Message**:
  Rename this generic name to match the regular expression '^[A-Z][0-9]?$'.

## 446. AZDL7c4o6N0FxHM9EiJR

- **Rule**: `java:S119`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/map/MapCustomTest.java:119`
- **Effort**: 10min
- **Created**: 2021-11-11T11:02:38+0000
- **Assignee**: alamar@github
- **Message**:
  Rename this generic name to match the regular expression '^[A-Z][0-9]?$'.

## 447. AX8r_qPz19JBuIecpRnK

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractWire.java:412`
- **Effort**: 5min
- **Created**: 2021-11-08T10:13:08+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 448. AY-YR1qjkrh49lmvlS9V

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/Issue327Test.java:88`
- **Effort**: 5min
- **Created**: 2021-10-21T08:21:52+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 449. AXzV0RAuKyg4rf3RvPqn

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:1026`
- **Effort**: 0min
- **Created**: 2021-10-18T10:12:26+0000
- **Assignee**: minborg@github
- **Message**:
  Complete the task associated to this TODO comment.

## 450. AXzV0RAuKyg4rf3RvPqo

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:1028`
- **Effort**: 5min
- **Created**: 2021-10-18T10:12:26+0000
- **Assignee**: minborg@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 451. AZDL7cjI6N0FxHM9EiBr

- **Rule**: `java:S116`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONWireMiscTest.java:44`
- **Effort**: 2min
- **Created**: 2021-10-18T10:12:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this field "TEXT" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 452. AZDL7cjI6N0FxHM9EiBs

- **Rule**: `java:S1124`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONWireMiscTest.java:167`
- **Effort**: 2min
- **Created**: 2021-10-18T10:12:26+0000
- **Assignee**: Unassigned
- **Message**:
  Reorder the modifiers to comply with the Java Language Specification.

## 453. AZDL7cjI6N0FxHM9EiBt

- **Rule**: `java:S1124`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONWireMiscTest.java:186`
- **Effort**: 2min
- **Created**: 2021-10-18T10:12:26+0000
- **Assignee**: Unassigned
- **Message**:
  Reorder the modifiers to comply with the Java Language Specification.

## 454. AY-YR2Eukrh49lmvlTDS

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONTypesWithMapsTest.java:95`
- **Effort**: 5min
- **Created**: 2021-10-14T10:33:21+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 455. AZDL7c2g6N0FxHM9EiJJ

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/JSONTypesWithEnumsAndBoxedTypesTest.java:68`
- **Effort**: 5min
- **Created**: 2021-10-13T16:38:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "car" private field.

## 456. AY-YR1q4krh49lmvlS9W

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/JSON322Test.java:104`
- **Effort**: 5min
- **Created**: 2021-10-13T11:25:13+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 457. AZDL7chX6N0FxHM9EiBY

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/JSON322Test.java:70`
- **Effort**: 5min
- **Created**: 2021-10-11T13:50:12+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 458. AXzV0Q8pKyg4rf3RvPnA

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ChronicleBitSet.java:173`
- **Effort**: 5min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 459. AXzV0Q9DKyg4rf3RvPno

- **Rule**: `java:S2065`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/LongArrayValueBitSet.java:48`
- **Effort**: 2min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the "transient" modifier from this field.

## 460. AXzV0Q9DKyg4rf3RvPnt

- **Rule**: `java:S2589`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/LongArrayValueBitSet.java:221`
- **Effort**: 10min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this expression which always evaluates to "true"

## 461. AXzV0Q9DKyg4rf3RvPnp

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/LongArrayValueBitSet.java:331`
- **Effort**: 5min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 462. AXzV0Q9DKyg4rf3RvPnq

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/LongArrayValueBitSet.java:850`
- **Effort**: 20min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 463. AY-YR1-Gkrh49lmvlTBt

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChronicleBitSetTest.java:25`
- **Effort**: 5min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 464. AZS37rELIVqiQpr6kOma

- **Rule**: `java:S2140`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChronicleBitSetTest.java:122`
- **Effort**: 5min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Use "java.util.Random.nextInt()" instead.

## 465. AZS37rELIVqiQpr6kOmb

- **Rule**: `java:S1940`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChronicleBitSetTest.java:457`
- **Effort**: 2min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Use the opposite operator ("!=") instead.

## 466. AZS37rELIVqiQpr6kOmc

- **Rule**: `java:S2178`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChronicleBitSetTest.java:457`
- **Effort**: 5min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Correct this "&" to "&&".

## 467. AZS37rELIVqiQpr6kOmd

- **Rule**: `java:S1940`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChronicleBitSetTest.java:494`
- **Effort**: 2min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Use the opposite operator ("!=") instead.

## 468. AZS37rELIVqiQpr6kOme

- **Rule**: `java:S2178`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChronicleBitSetTest.java:494`
- **Effort**: 5min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Correct this "&" to "&&".

## 469. AZS37rELIVqiQpr6kOmf

- **Rule**: `java:S1940`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChronicleBitSetTest.java:567`
- **Effort**: 2min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Use the opposite operator ("!=") instead.

## 470. AZS37rELIVqiQpr6kOmg

- **Rule**: `java:S2178`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChronicleBitSetTest.java:567`
- **Effort**: 5min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Correct this "|" to "||".

## 471. AZS37rELIVqiQpr6kOmh

- **Rule**: `java:S1940`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChronicleBitSetTest.java:603`
- **Effort**: 2min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Use the opposite operator ("!=") instead.

## 472. AY-YR1-Gkrh49lmvlTBv

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChronicleBitSetTest.java:875`
- **Effort**: 5min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 473. AY-YR1-Gkrh49lmvlTBw

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ChronicleBitSetTest.java:1101`
- **Effort**: 5min
- **Created**: 2021-10-11T08:56:19+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 474. AY-YR16Ikrh49lmvlTBO

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireTests.java:139`
- **Effort**: 5min
- **Created**: 2021-09-21T23:17:53+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 475. AXzV0Q7_Kyg4rf3RvPmP

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:215`
- **Effort**: 57min
- **Created**: 2021-08-27T14:59:56+0000
- **Assignee**: minborg@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 67 to the 15 allowed.

## 476. AXzV0Q7_Kyg4rf3RvPmW

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:534`
- **Effort**: 5min
- **Created**: 2021-08-27T14:59:56+0000
- **Assignee**: minborg@github
- **Message**:
  Rename "temp" which hides the field declared at line 72.

## 477. AXzV0Q5iKyg4rf3RvPlU

- **Rule**: `java:S1488`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:1898`
- **Effort**: 2min
- **Created**: 2021-08-27T14:59:56+0000
- **Assignee**: minborg@github
- **Message**:
  Immediately return this expression instead of assigning it to the temporary variable "type".

## 478. AXzV0Q4aKyg4rf3RvPkL

- **Rule**: `java:S5261`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ValueOut.java:1136`
- **Effort**: 1min
- **Created**: 2021-08-27T14:02:35+0000
- **Assignee**: minborg@github
- **Message**:
  Add explicit curly braces to avoid dangling else.

## 479. AXzV0Q5HKyg4rf3RvPkb

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireInternal.java:177`
- **Effort**: 0min
- **Created**: 2021-08-27T14:02:35+0000
- **Assignee**: minborg@github
- **Message**:
  Complete the task associated to this TODO comment.

## 480. AXzV0Q5HKyg4rf3RvPkc

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireInternal.java:237`
- **Effort**: 13min
- **Created**: 2021-08-27T14:02:35+0000
- **Assignee**: minborg@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 23 to the 15 allowed.

## 481. AXzV0Q4oKyg4rf3RvPkT

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ReadAnyWire.java:149`
- **Effort**: 5min
- **Created**: 2021-08-27T12:32:02+0000
- **Assignee**: minborg@github
- **Message**:
  Rename "wire" which hides the field declared at line 99.

## 482. AXzV0Q4KKyg4rf3RvPkH

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/SelfDescribingTriviallyCopyable.java:82`
- **Effort**: 12min
- **Created**: 2021-08-27T12:32:02+0000
- **Assignee**: minborg@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 22 to the 15 allowed.

## 483. AXzV0Q_uKyg4rf3RvPpr

- **Rule**: `java:S1192`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1440`
- **Effort**: 8min
- **Created**: 2021-08-27T12:32:02+0000
- **Assignee**: minborg@github
- **Message**:
  Define a constant instead of duplicating this literal "!null" 3 times.

## 484. AXzV0Q8zKyg4rf3RvPnM

- **Rule**: `java:S1124`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:148`
- **Effort**: 2min
- **Created**: 2021-08-27T11:53:39+0000
- **Assignee**: minborg@github
- **Message**:
  Reorder the modifiers to comply with the Java Language Specification.

## 485. AXzV0Q8zKyg4rf3RvPnl

- **Rule**: `java:S107`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:206`
- **Effort**: 20min
- **Created**: 2021-08-27T11:53:39+0000
- **Assignee**: minborg@github
- **Message**:
  Method has 9 parameters, which is greater than 7 authorized.

## 486. AXzV0Q8zKyg4rf3RvPnN

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:398`
- **Effort**: 40min
- **Created**: 2021-08-27T11:53:39+0000
- **Assignee**: minborg@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 50 to the 15 allowed.

## 487. AXzV0Q8IKyg4rf3RvPmo

- **Rule**: `java:S5261`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWireCode.java:310`
- **Effort**: 1min
- **Created**: 2021-08-27T11:15:31+0000
- **Assignee**: minborg@github
- **Message**:
  Add explicit curly braces to avoid dangling else.

## 488. AXzV0Q7nKyg4rf3RvPmF

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodBridge.java:164`
- **Effort**: 30min
- **Created**: 2021-08-27T11:15:31+0000
- **Assignee**: minborg@github
- **Message**:
  Override the "equals" method in this class.

## 489. AXzV0Q8zKyg4rf3RvPnS

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:613`
- **Effort**: 1min
- **Created**: 2021-08-23T13:38:41+0000
- **Assignee**: Unassigned
- **Message**:
  Format specifiers should be used instead of string concatenation.

## 490. AXzV0Q6uKyg4rf3RvPl2

- **Rule**: `java:S5261`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/RawWire.java:354`
- **Effort**: 1min
- **Created**: 2021-08-23T13:38:41+0000
- **Assignee**: Unassigned
- **Message**:
  Add explicit curly braces to avoid dangling else.

## 491. AXzV0Q17Kyg4rf3RvPhx

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:683`
- **Effort**: 1min
- **Created**: 2021-08-10T00:02:42+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 492. AXzV0Q17Kyg4rf3RvPhy

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:685`
- **Effort**: 1min
- **Created**: 2021-08-10T00:02:42+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 493. AXzV0Q-lKyg4rf3RvPoZ

- **Rule**: `java:S3358`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:978`
- **Effort**: 5min
- **Created**: 2021-07-28T16:49:14+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested ternary operation into an independent statement.

## 494. AYAIIzmx3Of7v0ssWkQn

- **Rule**: `java:S1141`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReader.java:323`
- **Effort**: 20min
- **Created**: 2021-07-20T04:22:30+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested try block into a separate method.

## 495. AY-YR2Aqkrh49lmvlTB9

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ThrowableTest.java:47`
- **Effort**: 5min
- **Created**: 2021-07-14T09:15:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 496. AXzV0Q_uKyg4rf3RvPpW

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1544`
- **Effort**: 0min
- **Created**: 2021-07-12T08:52:54+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 497. AY-YR1r7krh49lmvlS-6

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2449`
- **Effort**: 5min
- **Created**: 2021-07-09T12:18:53+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 498. AY-YR1r7krh49lmvlS-4

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2376`
- **Effort**: 5min
- **Created**: 2021-07-09T12:15:22+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 499. AY-YR1r7krh49lmvlS-5

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2431`
- **Effort**: 5min
- **Created**: 2021-07-09T12:15:22+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 500. AXzV0Q5yKyg4rf3RvPlZ

- **Rule**: `java:S3358`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireDumper.java:231`
- **Effort**: 5min
- **Created**: 2021-06-29T09:39:10+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested ternary operation into an independent statement.

## 501. AXzV0Q5yKyg4rf3RvPla

- **Rule**: `java:S3358`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireDumper.java:232`
- **Effort**: 5min
- **Created**: 2021-06-29T09:39:10+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested ternary operation into an independent statement.

## 502. AY-YR2Eakrh49lmvlTDQ

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:197`
- **Effort**: 5min
- **Created**: 2021-06-28T11:49:39+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 503. AY-YR2Eakrh49lmvlTDR

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:197`
- **Effort**: 5min
- **Created**: 2021-06-28T11:49:39+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 504. AZDL7c3-6N0FxHM9EiJN

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireTestCommon.java:189`
- **Effort**: 5min
- **Created**: 2021-06-28T11:49:39+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 505. AZDL7cz66N0FxHM9EiI6

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TimestampLongConverterZoneIdsTest.java:82`
- **Effort**: 2min
- **Created**: 2021-06-18T06:30:56+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 506. AZDL7cz66N0FxHM9EiI7

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TimestampLongConverterZoneIdsTest.java:87`
- **Effort**: 2min
- **Created**: 2021-06-18T06:30:56+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 507. AZDL7cz66N0FxHM9EiI8

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TimestampLongConverterZoneIdsTest.java:92`
- **Effort**: 2min
- **Created**: 2021-06-18T06:30:56+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 508. AXzV0Q4KKyg4rf3RvPkC

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/SelfDescribingTriviallyCopyable.java:37`
- **Effort**: 30min
- **Created**: 2021-05-13T15:43:54+0000
- **Assignee**: Unassigned
- **Message**:
  Override the "equals" method in this class.

## 509. AZDL7ch76N0FxHM9EiBZ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/Issue277Test.java:51`
- **Effort**: 2min
- **Created**: 2021-05-07T14:55:54+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 510. AXzV0Q-QKyg4rf3RvPoN

- **Rule**: `java:S1141`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshallerForUnexpectedFields.java:78`
- **Effort**: 20min
- **Created**: 2021-04-21T17:33:28+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested try block into a separate method.

## 511. AY-YR1vGkrh49lmvlS_u

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/bytesmarshallable/PerfRegressionHolder.java:82`
- **Effort**: 5min
- **Created**: 2021-04-16T14:37:46+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "mapped" which hides the field declared at line 70.

## 512. AY-YR1vckrh49lmvlS_v

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/bytesmarshallable/PerfRegressionTest.java:44`
- **Effort**: 5min
- **Created**: 2021-04-16T14:37:46+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 513. AZDL7cot6N0FxHM9EiB-

- **Rule**: `java:S1488`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/bytesmarshallable/PerfRegressionTest.java:135`
- **Effort**: 2min
- **Created**: 2021-04-16T14:37:46+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Immediately return this expression instead of assigning it to the temporary variable "process".

## 514. AY-YR1vckrh49lmvlS_w

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/bytesmarshallable/PerfRegressionTest.java:163`
- **Effort**: 5min
- **Created**: 2021-04-16T14:37:46+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 515. AXzV0QuyKyg4rf3RvPg4

- **Rule**: `java:S5976`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/EmbeddedBytesMarshallableTest.java:126`
- **Effort**: 10min
- **Created**: 2021-03-23T11:04:48+0000
- **Assignee**: Unassigned
- **Message**:
  Replace these 3 tests with a single Parameterized one.

## 516. AXzV0Q4KKyg4rf3RvPkD

- **Rule**: `java:S2065`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/SelfDescribingTriviallyCopyable.java:41`
- **Effort**: 2min
- **Created**: 2021-03-17T13:06:42+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the "transient" modifier from this field.

## 517. AXzV0Q4KKyg4rf3RvPkE

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/SelfDescribingTriviallyCopyable.java:48`
- **Effort**: 5min
- **Created**: 2021-03-17T13:06:42+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 518. AXzV0Q4KKyg4rf3RvPkF

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/SelfDescribingTriviallyCopyable.java:55`
- **Effort**: 5min
- **Created**: 2021-03-17T13:06:42+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 519. AXzV0Q4KKyg4rf3RvPkG

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/SelfDescribingTriviallyCopyable.java:62`
- **Effort**: 5min
- **Created**: 2021-03-17T13:06:42+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 520. AZDL7cyc6N0FxHM9EiIv

- **Rule**: `java:S3008`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TestLongConversion.java:32`
- **Effort**: 2min
- **Created**: 2021-03-12T13:15:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this field "SEPARATOR" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 521. AXzV0Q-_Kyg4rf3RvPov

- **Rule**: `java:S1121`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/FromStringInterner.java:115`
- **Effort**: 5min
- **Created**: 2021-02-04T10:21:11+0000
- **Assignee**: Unassigned
- **Message**:
  Extract the assignment out of this expression.

## 522. AXzV0Q-wKyg4rf3RvPoo

- **Rule**: `java:S2065`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMessageHistory.java:76`
- **Effort**: 2min
- **Created**: 2021-01-20T17:14:42+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the "transient" modifier from this field.

## 523. AXzV0Q17Kyg4rf3RvPiE

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:889`
- **Effort**: 1min
- **Created**: 2021-01-18T01:18:33+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 524. AZS37q0NIVqiQpr6kOmL

- **Rule**: `java:S1640`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2397`
- **Effort**: 5min
- **Created**: 2020-12-10T13:27:25+0000
- **Assignee**: Unassigned
- **Message**:
  Convert this Map to an EnumMap.

## 525. AY-YR1r7krh49lmvlS-s

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1984`
- **Effort**: 5min
- **Created**: 2020-12-10T13:05:35+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.io.IOException', as it cannot be thrown from
  method's body.

## 526. AY-YR1r7krh49lmvlS-t

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1990`
- **Effort**: 5min
- **Created**: 2020-12-10T13:05:35+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.io.IOException', as it cannot be thrown from
  method's body.

## 527. AY-YR15Qkrh49lmvlTBJ

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/Base64LongConverterTest.java:36`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 528. AY-YR2DHkrh49lmvlTCQ

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/Base85LongConverterTest.java:56`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 529. AY-YR2DYkrh49lmvlTCX

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWire2Test.java:296`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 530. AY-YR2DYkrh49lmvlTCZ

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWire2Test.java:709`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 531. AY-YR2DYkrh49lmvlTCa

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWire2Test.java:725`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 532. AY-YR2Eakrh49lmvlTCm

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:84`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 533. AY-YR2Eakrh49lmvlTCn

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:95`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 534. AY-YR2Eakrh49lmvlTCs

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:119`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 535. AY-YR2Eakrh49lmvlTCt

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:125`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 536. AY-YR2Eakrh49lmvlTCx

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:146`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 537. AY-YR1yYkrh49lmvlTAM

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireTest.java:1114`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 538. AY-YR1yYkrh49lmvlTAQ

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireTest.java:1557`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 539. AY-YR1yYkrh49lmvlTAR

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireTest.java:1566`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 540. AY-YR1yYkrh49lmvlTAS

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireTest.java:1617`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 541. AY-YR2Dxkrh49lmvlTCf

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireWithMappedBytesTest.java:96`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 542. AY-YR2Dxkrh49lmvlTCg

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireWithMappedBytesTest.java:100`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 543. AY-YR17jkrh49lmvlTBZ

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/CSVBytesMarshallableTest.java:107`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 544. AY-YR151krh49lmvlTBL

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ForwardAndBackwardCompatibilityMarshallableTest.java:74`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 545. AY-YR151krh49lmvlTBM

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ForwardAndBackwardCompatibilityMarshallableTest.java:98`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 546. AY-YR151krh49lmvlTBN

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ForwardAndBackwardCompatibilityMarshallableTest.java:133`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 547. AY-YR2A5krh49lmvlTB-

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ForwardAndBackwardCompatibilityTest.java:68`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 548. AY-YR2A5krh49lmvlTB_

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ForwardAndBackwardCompatibilityTest.java:102`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 549. AY-YR12Skrh49lmvlTAv

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MarshallableTest.java:109`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 550. AY-YR18vkrh49lmvlTBj

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/PrimArraysTest.java:106`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 551. AY-YR16Zkrh49lmvlTBP

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/PrimitiveTypeWrappersTest.java:67`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 552. AY-YR16Zkrh49lmvlTBQ

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/PrimitiveTypeWrappersTest.java:84`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 553. AY-YR14Okrh49lmvlTBC

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RFCExamplesTest.java:64`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 554. AY-YR14Okrh49lmvlTBE

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RFCExamplesTest.java:200`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 555. AY-YR2CAkrh49lmvlTCK

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RawWireTest.java:561`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 556. AY-YR1nkkrh49lmvlS8U

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:74`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 557. AY-YR1nkkrh49lmvlS8V

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:94`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 558. AY-YR1nkkrh49lmvlS8W

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:119`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 559. AY-YR1nkkrh49lmvlS8X

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:148`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 560. AY-YR1nkkrh49lmvlS8Y

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:152`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 561. AY-YR1nkkrh49lmvlS8a

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:172`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 562. AY-YR1nkkrh49lmvlS8b

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:176`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 563. AY-YR1nkkrh49lmvlS8c

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:205`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 564. AY-YR1nkkrh49lmvlS8d

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:209`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 565. AY-YR1nkkrh49lmvlS8f

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:230`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 566. AY-YR1nkkrh49lmvlS8g

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:234`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 567. AY-YR1nkkrh49lmvlS8j

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:267`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 568. AY-YR1nkkrh49lmvlS8l

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:270`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 569. AY-YR1nkkrh49lmvlS8n

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:291`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 570. AY-YR1nkkrh49lmvlS8p

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:294`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 571. AY-YR1nkkrh49lmvlS8q

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:334`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 572. AY-YR1nkkrh49lmvlS8r

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:338`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 573. AY-YR1nkkrh49lmvlS8t

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:360`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 574. AY-YR1nkkrh49lmvlS8u

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:364`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 575. AY-YR1nkkrh49lmvlS8v

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:403`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 576. AY-YR1nkkrh49lmvlS8x

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:454`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 577. AY-YR1nkkrh49lmvlS81

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:515`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 578. AY-YR1nkkrh49lmvlS83

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:520`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 579. AY-YR1nkkrh49lmvlS85

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:542`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 580. AY-YR1nkkrh49lmvlS87

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:547`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 581. AY-YR1-0krh49lmvlTBy

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmePojoTest.java:44`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 582. AY-YR101krh49lmvlTAk

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextCompatibilityTest.java:101`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 583. AY-YR101krh49lmvlTAm

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextCompatibilityTest.java:112`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 584. AY-YR17Ekrh49lmvlTBW

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireCompatibilityTest.java:42`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 585. AY-YR1r7krh49lmvlS9a

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:119`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 586. AY-YR1r7krh49lmvlS9g

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:252`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 587. AY-YR1r7krh49lmvlS9i

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:327`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 588. AY-YR1r7krh49lmvlS-k

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1747`
- **Effort**: 0min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 589. AY-YR1r7krh49lmvlS-1

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2283`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 590. AY-YR171krh49lmvlTBb

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/UnicodeStringTest.java:112`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 591. AY-YR17_krh49lmvlTBd

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/UsingTestMarshallableTest.java:56`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 592. AY-YR17_krh49lmvlTBf

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/UsingTestMarshallableTest.java:95`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 593. AY-YR143krh49lmvlTBG

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ValueOutTest.java:67`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 594. AY-YR143krh49lmvlTBH

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ValueOutTest.java:91`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 595. AY-YR1_fkrh49lmvlTB1

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireSerializedLambdaTest.java:77`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 596. AY-YR13Mkrh49lmvlTA2

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireToOutputStreamTest.java:93`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 597. AY-YR13Mkrh49lmvlTA3

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireToOutputStreamTest.java:105`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 598. AY-YR13Mkrh49lmvlTA4

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireToOutputStreamTest.java:126`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 599. AY-YR2Cdkrh49lmvlTCM

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WordsLongConverterTest.java:113`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 600. AY-YR2Fnkrh49lmvlTDZ

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:116`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 601. AY-YR2Fnkrh49lmvlTDc

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:221`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 602. AY-YR2Fnkrh49lmvlTEF

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:982`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 603. AY-YR2AOkrh49lmvlTB7

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/compact/DotNetTest.java:52`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 604. AY-YR1p-krh49lmvlS9R

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/JSON222IndividualTest.java:110`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 605. AY-YR1pbkrh49lmvlS9M

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/JSON222Test.java:134`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 606. AY-YR1qakrh49lmvlS9U

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/WireBug37Test.java:67`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 607. AY-YR1pykrh49lmvlS9O

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/WireBug38Test.java:67`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 608. AY-YR1o_krh49lmvlS9G

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/WireBug39Test.java:72`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 609. AY-YR1cxkrh49lmvlS7H

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/MarshallableWireTest.java:108`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 610. AY-YR1evkrh49lmvlS7P

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/MarshallableWithOverwriteFalseTest.java:62`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 611. AY-YR1llkrh49lmvlS8L

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/MethodWriter2Test.java:44`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 612. AY-YR1z_krh49lmvlTAg

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reordered/ReorderedTest.java:119`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 613. AY-YR1gHkrh49lmvlS7X

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/NestedClassTest.java:112`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 614. AY-YR1fukrh49lmvlS7W

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/WireCollectionTest.java:100`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 615. AY-YR1azkrh49lmvlS7G

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/serializable/SerializableWireTest.java:98`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 616. AY-YR12zkrh49lmvlTAz

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/type/conversions/binary/ConventionsTest.java:46`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 617. AY-YR12zkrh49lmvlTA0

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/type/conversions/binary/ConventionsTest.java:71`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:59+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 618. AXzV0Q6aKyg4rf3RvPlk

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReader.java:206`
- **Effort**: 15min
- **Created**: 2020-12-03T00:32:58+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 25 to the 15 allowed.

## 619. AXzV0Q6aKyg4rf3RvPlu

- **Rule**: `java:S1141`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReader.java:243`
- **Effort**: 20min
- **Created**: 2020-12-03T00:32:58+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested try block into a separate method.

## 620. AY-YR1zWkrh49lmvlTAb

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderDelegationTest.java:285`
- **Effort**: 0min
- **Created**: 2020-12-03T00:32:58+0000
- **Assignee**: JerryShea@github
- **Message**:
  Complete the task associated to this TODO comment.

## 621. AY-YR1mOkrh49lmvlS8P

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/GenerateMethodWriterInheritanceTest.java:103`
- **Effort**: 0min
- **Created**: 2020-12-02T00:52:23+0000
- **Assignee**: JerryShea@github
- **Message**:
  Complete the task associated to this TODO comment.

## 622. AXzV0Q-wKyg4rf3RvPoq

- **Rule**: `java:S1192`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMessageHistory.java:228`
- **Effort**: 8min
- **Created**: 2020-11-10T16:04:09+0000
- **Assignee**: Unassigned
- **Message**:
  Define a constant instead of duplicating this literal "sources" 3 times.

## 623. AXzV0Q-wKyg4rf3RvPor

- **Rule**: `java:S1192`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMessageHistory.java:230`
- **Effort**: 8min
- **Created**: 2020-11-10T16:04:09+0000
- **Assignee**: Unassigned
- **Message**:
  Define a constant instead of duplicating this literal "timings" 3 times.

## 624. AY-YR14Okrh49lmvlTBD

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RFCExamplesTest.java:156`
- **Effort**: 5min
- **Created**: 2020-11-05T18:19:49+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 625. AXzV0RA5Kyg4rf3RvPq2

- **Rule**: `java:S3358`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:630`
- **Effort**: 5min
- **Created**: 2020-11-05T10:38:36+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested ternary operation into an independent statement.

## 626. AXzV0RA5Kyg4rf3RvPq4

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:677`
- **Effort**: 30min
- **Created**: 2020-11-05T10:38:36+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 627. AXzV0RA5Kyg4rf3RvPq5

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:679`
- **Effort**: 30min
- **Created**: 2020-11-05T10:38:36+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 628. AXzV0RA5Kyg4rf3RvPq6

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:681`
- **Effort**: 30min
- **Created**: 2020-11-05T10:38:36+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 629. AXzV0RAIKyg4rf3RvPqI

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Marshallable.java:303`
- **Effort**: 5min
- **Created**: 2020-11-05T10:09:33+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 630. AXzV0Q3rKyg4rf3RvPj6

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/CSVWire.java:232`
- **Effort**: 18min
- **Created**: 2020-11-02T10:30:36+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 28 to the 15 allowed.

## 631. AXzV0Q9xKyg4rf3RvPn4

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodDelegate.java:60`
- **Effort**: 5min
- **Created**: 2020-10-28T10:29:55+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 632. AXzV0Q35Kyg4rf3RvPj_

- **Rule**: `java:S119`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/MethodDelegate.java:29`
- **Effort**: 10min
- **Created**: 2020-10-28T10:29:55+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this generic name to match the regular expression '^[A-Z][0-9]?$'.

## 633. AY-YR1zWkrh49lmvlTAY

- **Rule**: `java:S4144`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderDelegationTest.java:91`
- **Effort**: 15min
- **Created**: 2020-10-23T13:22:15+0000
- **Assignee**: Unassigned
- **Message**:
  Update this method so that its implementation is not identical to
  "testUnsuccessfulCallIsDelegatedTextWire" on line 76.

## 634. AXzV0RAkKyg4rf3RvPqc

- **Rule**: `java:S1192`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter2.java:72`
- **Effort**: 10min
- **Created**: 2020-10-23T13:19:20+0000
- **Assignee**: Unassigned
- **Message**:
  Define a constant instead of duplicating this literal "public " 4 times.

## 635. AXzV0RAkKyg4rf3RvPqb

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter2.java:364`
- **Effort**: 30min
- **Created**: 2020-10-23T13:19:20+0000
- **Assignee**: Unassigned
- **Message**:
  Override the "equals" method in this class.

## 636. AXzV0Q6HKyg4rf3RvPli

- **Rule**: `java:S119`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/utils/SourceCodeFormatter.java:208`
- **Effort**: 10min
- **Created**: 2020-10-23T13:19:20+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this generic name to match the regular expression '^[A-Z][0-9]?$'.

## 637. AY6FVbG-5NUPA8B1oXpM

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractClassGenerator.java:145`
- **Effort**: 20min
- **Created**: 2020-10-21T08:11:22+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 638. AXzV0Q8zKyg4rf3RvPng

- **Rule**: `java:S1192`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:370`
- **Effort**: 10min
- **Created**: 2020-10-19T11:44:03+0000
- **Assignee**: Unassigned
- **Message**:
  Define a constant instead of duplicating this literal "final " 4 times.

## 639. AXzV0Q8zKyg4rf3RvPnW

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:742`
- **Effort**: 1min
- **Created**: 2020-10-19T11:44:03+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 640. AZDL7c3w6N0FxHM9EiJM

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireResetTest.java:140`
- **Effort**: 5min
- **Created**: 2020-10-16T14:21:49+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 641. AXzV0Q17Kyg4rf3RvPhv

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:591`
- **Effort**: 1min
- **Created**: 2020-10-15T16:49:20+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 642. AXzV0Q17Kyg4rf3RvPhu

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:582`
- **Effort**: 1min
- **Created**: 2020-10-14T17:05:45+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 643. AXzV0Q17Kyg4rf3RvPh1

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:743`
- **Effort**: 1min
- **Created**: 2020-10-14T17:05:45+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 644. AZDL7dAg6N0FxHM9EiJn

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderInterceptorReturnsTest.java:278`
- **Effort**: 5min
- **Created**: 2020-10-14T17:05:45+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 645. AZDL7dAg6N0FxHM9EiJo

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderInterceptorReturnsTest.java:283`
- **Effort**: 5min
- **Created**: 2020-10-14T17:05:45+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 646. AZDL7dAg6N0FxHM9EiJp

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MethodReaderInterceptorReturnsTest.java:319`
- **Effort**: 5min
- **Created**: 2020-10-14T17:05:45+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 647. AXzV0Q6aKyg4rf3RvPlm

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReader.java:270`
- **Effort**: 5min
- **Created**: 2020-10-12T09:12:33+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 648. AXzV0Q17Kyg4rf3RvPiH

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:482`
- **Effort**: 1h
- **Created**: 2020-10-09T10:35:42+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 649. AXzV0Q8zKyg4rf3RvPni

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:482`
- **Effort**: 40min
- **Created**: 2020-10-08T14:09:08+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 650. AXzV0Q17Kyg4rf3RvPhk

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:192`
- **Effort**: 20min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 651. AXzV0Q17Kyg4rf3RvPhl

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:243`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 652. AXzV0Q17Kyg4rf3RvPhm

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:263`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 653. AXzV0Q17Kyg4rf3RvPhn

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:272`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 654. AXzV0Q17Kyg4rf3RvPho

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:327`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 655. AXzV0Q17Kyg4rf3RvPhp

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:329`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 656. AXzV0Q17Kyg4rf3RvPhs

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:556`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 657. AXzV0Q17Kyg4rf3RvPhw

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:609`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 658. AXzV0Q17Kyg4rf3RvPiI

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:788`
- **Effort**: 20min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 659. AXzV0Q17Kyg4rf3RvPh3

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:814`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 660. AXzV0Q17Kyg4rf3RvPh4

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:828`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 661. AXzV0Q17Kyg4rf3RvPh5

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:831`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 662. AXzV0Q17Kyg4rf3RvPh6

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:843`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 663. AXzV0Q17Kyg4rf3RvPh9

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:855`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 664. AXzV0Q17Kyg4rf3RvPh7

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:862`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 665. AXzV0Q17Kyg4rf3RvPh8

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:865`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 666. AXzV0Q17Kyg4rf3RvPiA

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:867`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 667. AXzV0Q17Kyg4rf3RvPiB

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:871`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 668. AXzV0Q17Kyg4rf3RvPiC

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:874`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 669. AXzV0Q17Kyg4rf3RvPiD

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodReader.java:880`
- **Effort**: 1min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 670. AXzV0Q2jKyg4rf3RvPih

- **Rule**: `java:S1141`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReaderBuilder.java:250`
- **Effort**: 20min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested try block into a separate method.

## 671. AXzV0Q2jKyg4rf3RvPif

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReaderBuilder.java:260`
- **Effort**: 20min
- **Created**: 2020-10-06T11:55:49+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 672. AY83hwBeugVfbMlTVkap

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/DynamicEnum.java:33`
- **Effort**: 10min
- **Created**: 2020-10-02T12:50:13+0000
- **Assignee**: Unassigned
- **Message**:
  Do not forget to remove this deprecated code someday.

## 673. AY83hwBeugVfbMlTVkaq

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/DynamicEnum.java:33`
- **Effort**: 5min
- **Created**: 2020-10-02T12:50:13+0000
- **Assignee**: Unassigned
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 674. AXzV0Q24Kyg4rf3RvPjd

- **Rule**: `java:S1192`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:2602`
- **Effort**: 8min
- **Created**: 2020-09-29T18:04:10+0000
- **Assignee**: Unassigned
- **Message**:
  Define a constant instead of duplicating this literal "Document length %,d out of 32-bit int range."
  3 times.

## 675. AXzV0Q-lKyg4rf3RvPoQ

- **Rule**: `java:S3008`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:124`
- **Effort**: 2min
- **Created**: 2020-09-29T18:04:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this field "GENERATE_TUPLES" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 676. AXzV0Q-lKyg4rf3RvPoi

- **Rule**: `java:S1104`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:124`
- **Effort**: 10min
- **Created**: 2020-09-29T18:04:10+0000
- **Assignee**: Unassigned
- **Message**:
  Make GENERATE_TUPLES a static final constant or non-public and provide accessors if needed.

## 677. AXzV0Q-lKyg4rf3RvPoj

- **Rule**: `java:S1444`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:124`
- **Effort**: 20min
- **Created**: 2020-09-29T18:04:10+0000
- **Assignee**: Unassigned
- **Message**:
  Make this "public static GENERATE_TUPLES" field final

## 678. AXzV0Q_uKyg4rf3RvPpU

- **Rule**: `java:S119`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1504`
- **Effort**: 10min
- **Created**: 2020-09-23T08:11:40+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this generic name to match the regular expression '^[A-Z][0-9]?$'.

## 679. AY6FVbGJ5NUPA8B1oXpK

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:530`
- **Effort**: 20min
- **Created**: 2020-09-16T21:34:03+0000
- **Assignee**: JerryShea@github
- **Message**:
  Catch Exception instead of Throwable.

## 680. AXzV0Q8zKyg4rf3RvPnQ

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:583`
- **Effort**: 1min
- **Created**: 2020-09-16T12:13:11+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 681. AXzV0Q8zKyg4rf3RvPnT

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:622`
- **Effort**: 1min
- **Created**: 2020-09-16T12:13:11+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 682. AXzV0RA5Kyg4rf3RvPrY

- **Rule**: `java:S1066`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1709`
- **Effort**: 5min
- **Created**: 2020-09-14T17:15:57+0000
- **Assignee**: Unassigned
- **Message**:
  Merge this if statement with the enclosing one.

## 683. AXzV0RA5Kyg4rf3RvPqy

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:235`
- **Effort**: 5min
- **Created**: 2020-09-11T07:27:38+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 684. AY-YR1l0krh49lmvlS8O

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/MethodWriterTest.java:242`
- **Effort**: 5min
- **Created**: 2020-09-10T21:26:02+0000
- **Assignee**: JerryShea@github
- **Message**:
  Remove this unused method parameter "byteShort".

## 685. AXzV0RA5Kyg4rf3RvPqz

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:247`
- **Effort**: 30min
- **Created**: 2020-09-08T07:30:00+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 686. AXzV0RA5Kyg4rf3RvPq0

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:248`
- **Effort**: 30min
- **Created**: 2020-09-08T07:30:00+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 687. AXzV0Q49Kyg4rf3RvPkW

- **Rule**: `java:S1948`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/SerializationStrategies.java:224`
- **Effort**: 30min
- **Created**: 2020-09-08T06:40:05+0000
- **Assignee**: Unassigned
- **Message**:
  Make non-static "ordinal" transient or serializable.

## 688. AXzV0Q49Kyg4rf3RvPkX

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/SerializationStrategies.java:287`
- **Effort**: 30min
- **Created**: 2020-09-08T06:40:05+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 689. AXzV0RA5Kyg4rf3RvPrW

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:217`
- **Effort**: 40min
- **Created**: 2020-09-08T06:40:05+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 690. AXzV0Q6aKyg4rf3RvPlt

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReader.java:426`
- **Effort**: 1h40min
- **Created**: 2020-09-03T22:06:08+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 691. AXzV0Q_uKyg4rf3RvPpu

- **Rule**: `java:S1192`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:67`
- **Effort**: 12min
- **Created**: 2020-09-01T11:14:40+0000
- **Assignee**: Unassigned
- **Message**:
  Define a constant instead of duplicating this literal "type " 5 times.

## 692. AXzV0Q-lKyg4rf3RvPoT

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:109`
- **Effort**: 30min
- **Created**: 2020-09-01T08:17:44+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility update should be removed.

## 693. AY-YR1l0krh49lmvlS8M

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/MethodWriterTest.java:53`
- **Effort**: 5min
- **Created**: 2020-08-31T05:31:01+0000
- **Assignee**: JerryShea@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 694. AXzV0Q87Kyg4rf3RvPnm

- **Rule**: `java:S114`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ServicesTimestampLongConverter.java:171`
- **Effort**: 10min
- **Created**: 2020-08-25T23:47:25+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this interface name to match the regular expression '^[A-Z][a-zA-Z0-9]*$'.

## 695. AXzV0Q8zKyg4rf3RvPnh

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:439`
- **Effort**: 20min
- **Created**: 2020-08-20T13:03:26+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 696. AZDL7cdZ6N0FxHM9EiBQ

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/MethodWriterProxyTest.java:55`
- **Effort**: 5min
- **Created**: 2020-08-14T22:27:36+0000
- **Assignee**: JerryShea@github
- **Message**:
  Add the "@Override" annotation above this method signature

## 697. AZDL7cdZ6N0FxHM9EiBR

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/MethodWriterProxyTest.java:63`
- **Effort**: 5min
- **Created**: 2020-08-14T22:21:21+0000
- **Assignee**: JerryShea@github
- **Message**:
  Add the "@Override" annotation above this method signature

## 698. AXzV0Q8zKyg4rf3RvPnR

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:609`
- **Effort**: 1min
- **Created**: 2020-08-14T06:49:25+0000
- **Assignee**: Unassigned
- **Message**:
  %n should be used in place of \n to produce the platform-specific line separator.

## 699. AXzV0Q8zKyg4rf3RvPnX

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:786`
- **Effort**: 1min
- **Created**: 2020-08-13T10:55:32+0000
- **Assignee**: Unassigned
- **Message**:
  Format specifiers should be used instead of string concatenation.

## 700. AY6FVbDR5NUPA8B1oXo2

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/MarshallableOut.java:132`
- **Effort**: 20min
- **Created**: 2020-08-08T09:17:07+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 701. AY-YR1n4krh49lmvlS89

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MicroTimestampLongConverterTest.java:43`
- **Effort**: 5min
- **Created**: 2020-08-06T12:39:07+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 702. AY-YR1m2krh49lmvlS8Q

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/ClusterCommand.java:49`
- **Effort**: 5min
- **Created**: 2020-07-09T14:47:12+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 703. AY-YR1m2krh49lmvlS8R

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/ClusterCommand.java:61`
- **Effort**: 5min
- **Created**: 2020-07-09T14:47:12+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 704. AXzV0Q_KKyg4rf3RvPow

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GeneratedProxyClass.java:54`
- **Effort**: 7min
- **Created**: 2020-07-03T14:53:35+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## 705. AXzV0RA5Kyg4rf3RvPrV

- **Rule**: `java:S1121`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1584`
- **Effort**: 5min
- **Created**: 2020-07-03T14:53:35+0000
- **Assignee**: Unassigned
- **Message**:
  Extract the assignment out of this expression.

## 706. AXzV0Q7eKyg4rf3RvPmD

- **Rule**: `java:S1141`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodWriterBuilder.java:293`
- **Effort**: 20min
- **Created**: 2020-07-02T14:36:54+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested try block into a separate method.

## 707. AY-YR2DYkrh49lmvlTCb

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWire2Test.java:903`
- **Effort**: 5min
- **Created**: 2020-07-01T11:41:03+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 708. AY-YR1r7krh49lmvlS9Z

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:109`
- **Effort**: 5min
- **Created**: 2020-07-01T11:25:07+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 709. AY-YR1r7krh49lmvlS-M

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1254`
- **Effort**: 5min
- **Created**: 2020-07-01T11:25:07+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 710. AY-YR2DYkrh49lmvlTCe

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWire2Test.java:1044`
- **Effort**: 5min
- **Created**: 2020-06-26T12:51:40+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 711. AZDL7cT26N0FxHM9EiAi

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/NullFieldMarshallingTest.java:46`
- **Effort**: 5min
- **Created**: 2020-06-25T19:19:03+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 712. AXzV0Q8aKyg4rf3RvPmt

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractWire.java:237`
- **Effort**: 5min
- **Created**: 2020-06-23T12:53:49+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 713. AXzV0Q8aKyg4rf3RvPmu

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractWire.java:243`
- **Effort**: 5min
- **Created**: 2020-06-23T12:53:49+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 714. AXzV0Q2OKyg4rf3RvPiQ

- **Rule**: `java:S1192`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlLogging.java:38`
- **Effort**: 10min
- **Created**: 2020-05-31T15:14:52+0000
- **Assignee**: Unassigned
- **Message**:
  Define a constant instead of duplicating this literal "yaml.logging" 4 times.

## 715. AXzV0Q8zKyg4rf3RvPnY

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:787`
- **Effort**: 1min
- **Created**: 2020-05-10T16:20:21+0000
- **Assignee**: Unassigned
- **Message**:
  Format specifiers should be used instead of string concatenation.

## 716. AXzV0Q8zKyg4rf3RvPnZ

- **Rule**: `java:S3457`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:790`
- **Effort**: 1min
- **Created**: 2020-05-10T16:20:21+0000
- **Assignee**: Unassigned
- **Message**:
  Format specifiers should be used instead of string concatenation.

## 717. AXzV0Q8zKyg4rf3RvPnP

- **Rule**: `java:S106`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:513`
- **Effort**: 10min
- **Created**: 2020-05-10T15:48:05+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this use of System.out by a logger.

## 718. AXzV0Q8zKyg4rf3RvPnk

- **Rule**: `java:S107`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GenerateMethodWriter.java:167`
- **Effort**: 20min
- **Created**: 2020-05-10T13:34:33+0000
- **Assignee**: Unassigned
- **Message**:
  Constructor has 10 parameters, which is greater than 7 authorized.

## 719. AY6FVbEB5NUPA8B1oXpA

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodWriterBuilder.java:308`
- **Effort**: 20min
- **Created**: 2020-05-10T13:34:33+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 720. AY6FVbDg5NUPA8B1oXo8

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextMethodWriterInvocationHandler.java:85`
- **Effort**: 20min
- **Created**: 2020-05-05T14:44:15+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 721. AY-YR19Akrh49lmvlTBk

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/map/MapWireTest.java:70`
- **Effort**: 5min
- **Created**: 2020-05-05T14:44:15+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 722. AY-YR2CKkrh49lmvlTCL

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireResourcesTest.java:181`
- **Effort**: 5min
- **Created**: 2020-04-21T15:49:12+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.io.StreamCorruptedException', as it cannot be
  thrown from method's body.

## 723. AXzV0Q5hKyg4rf3RvPks

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:373`
- **Effort**: 0min
- **Created**: 2020-04-03T13:38:08+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 724. AXzV0RA5Kyg4rf3RvPrX

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:522`
- **Effort**: 10min
- **Created**: 2020-04-02T07:38:20+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 725. AXzV0Q5iKyg4rf3RvPlP

- **Rule**: `java:S1066`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:1955`
- **Effort**: 5min
- **Created**: 2020-04-01T15:42:16+0000
- **Assignee**: Unassigned
- **Message**:
  Merge this if statement with the enclosing one.

## 726. AXzV0Q_uKyg4rf3RvPpB

- **Rule**: `java:S6035`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:79`
- **Effort**: 5min
- **Created**: 2020-03-31T16:16:34+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this alternation with a character class.

## 727. AXzV0Q7_Kyg4rf3RvPme

- **Rule**: `java:S1121`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:1178`
- **Effort**: 5min
- **Created**: 2020-03-20T16:16:43+0000
- **Assignee**: Unassigned
- **Message**:
  Extract the assignment out of this expression.

## 728. AXzV0Q7_Kyg4rf3RvPma

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:1196`
- **Effort**: 30min
- **Created**: 2020-03-20T16:16:43+0000
- **Assignee**: Unassigned
- **Message**:
  Override the "equals" method in this class.

## 729. AXzV0Q5iKyg4rf3RvPlN

- **Rule**: `java:S1066`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:1069`
- **Effort**: 5min
- **Created**: 2020-03-20T16:16:43+0000
- **Assignee**: Unassigned
- **Message**:
  Merge this if statement with the enclosing one.

## 730. AXzV0Q5iKyg4rf3RvPk6

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:1843`
- **Effort**: 5min
- **Created**: 2020-03-13T18:59:09+0000
- **Assignee**: Unassigned
- **Message**:
  Complete cases by adding the missing enum constants or add a default case to this switch.

## 731. AY-YR2Fnkrh49lmvlTEJ

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1198`
- **Effort**: 5min
- **Created**: 2020-03-13T18:59:09+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 732. AXzV0Q_fKyg4rf3RvPo7

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireParser.java:100`
- **Effort**: 20min
- **Created**: 2020-03-10T14:25:10+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 733. AXzV0Q5iKyg4rf3RvPk5

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:1362`
- **Effort**: 0min
- **Created**: 2020-03-03T15:11:54+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 734. AZDL7cbr6N0FxHM9EiBN

- **Rule**: `java:S1124`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/method/VanillaMethodReaderHierarchyTest.java:152`
- **Effort**: 2min
- **Created**: 2020-01-14T04:11:14+0000
- **Assignee**: JerryShea@github
- **Message**:
  Reorder the modifiers to comply with the Java Language Specification.

## 735. AXzV0Q7_Kyg4rf3RvPmX

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:769`
- **Effort**: 6min
- **Created**: 2020-01-07T16:56:54+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## 736. AXzV0Q5iKyg4rf3RvPlR

- **Rule**: `java:S1301`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:1896`
- **Effort**: 5min
- **Created**: 2020-01-07T16:56:54+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this "switch" statement by "if" statements to increase readability.

## 737. AXzV0Q5iKyg4rf3RvPk7

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:1906`
- **Effort**: 5min
- **Created**: 2020-01-07T16:56:54+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 738. AXzV0Q5iKyg4rf3RvPlO

- **Rule**: `java:S1066`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:1954`
- **Effort**: 5min
- **Created**: 2020-01-07T16:56:54+0000
- **Assignee**: Unassigned
- **Message**:
  Merge this if statement with the enclosing one.

## 739. AXzV0Q5iKyg4rf3RvPlC

- **Rule**: `java:S3358`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:2445`
- **Effort**: 5min
- **Created**: 2020-01-07T16:56:54+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested ternary operation into an independent statement.

## 740. AXzV0Q5iKyg4rf3RvPlG

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:168`
- **Effort**: 10min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 741. AXzV0Q5iKyg4rf3RvPlH

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:212`
- **Effort**: 10min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 742. AXzV0Q5iKyg4rf3RvPlM

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:213`
- **Effort**: 10min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 743. AXzV0Q5iKyg4rf3RvPlJ

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:219`
- **Effort**: 10min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 744. AXzV0Q5iKyg4rf3RvPlK

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:220`
- **Effort**: 10min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 745. AXzV0Q5iKyg4rf3RvPlL

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:221`
- **Effort**: 10min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 746. AXzV0Q5iKyg4rf3RvPlI

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:222`
- **Effort**: 10min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 747. AXzV0Q5iKyg4rf3RvPk3

- **Rule**: `java:S4524`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:1245`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Move this default to the end of the switch.

## 748. AXzV0Q5iKyg4rf3RvPlF

- **Rule**: `java:S1121`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlWire.java:1719`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Extract the assignment out of this expression.

## 749. AY-YR2Fnkrh49lmvlTDX

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:100`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 750. AY-YR2Fnkrh49lmvlTD3

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:697`
- **Effort**: 0min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 751. AY-YR2Fnkrh49lmvlTD2

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:698`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 752. AY-YR2Fnkrh49lmvlTD8

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:815`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 753. AY-YR2Fnkrh49lmvlTD9

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:827`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 754. AY-YR2Fnkrh49lmvlTD-

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:839`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 755. AY-YR2Fnkrh49lmvlTD_

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:859`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 756. AY-YR2Fnkrh49lmvlTEE

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:974`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 757. AY-YR2Fnkrh49lmvlTEG

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:993`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 758. AY-YR2Fnkrh49lmvlTEH

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1040`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 759. AY-YR2Fnkrh49lmvlTEI

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1144`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 760. AY-YR2Fnkrh49lmvlTEK

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1228`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 761. AY-YR2Fnkrh49lmvlTEM

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1298`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 762. AY-YR2Fnkrh49lmvlTEN

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1314`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 763. AY-YR2Fnkrh49lmvlTEO

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1343`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 764. AY-YR2Fnkrh49lmvlTEP

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1362`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 765. AY-YR2Fnkrh49lmvlTEQ

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1376`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 766. AY-YR2Fnkrh49lmvlTER

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1390`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 767. AY-YR2Fnkrh49lmvlTES

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1411`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 768. AY-YR2Fnkrh49lmvlTET

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1419`
- **Effort**: 0min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 769. AY-YR2Fnkrh49lmvlTEV

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1447`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 770. AY-YR2Fnkrh49lmvlTEW

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1455`
- **Effort**: 0min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 771. AY-YR2Fnkrh49lmvlTEX

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1469`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 772. AY-YR2Fnkrh49lmvlTEY

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1561`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 773. AY-YR2Fnkrh49lmvlTEZ

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1577`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 774. AY-YR2Fnkrh49lmvlTEa

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1597`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 775. AZDL7dCS6N0FxHM9EiJw

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1613`
- **Effort**: 2min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 776. AZDL7dCS6N0FxHM9EiJx

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1623`
- **Effort**: 2min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 777. AY-YR2Fnkrh49lmvlTEc

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1633`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 778. AY-YR2Fnkrh49lmvlTEd

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1655`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 779. AY-YR2Fnkrh49lmvlTEe

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1691`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 780. AY-YR2Fnkrh49lmvlTEf

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1714`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.io.IOException', as it cannot be thrown from
  method's body.

## 781. AY-YR2Fnkrh49lmvlTEh

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1738`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 782. AY-YR2Fnkrh49lmvlTEi

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1749`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 783. AY-YR2Fnkrh49lmvlTEj

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1767`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 784. AY-YR2Fnkrh49lmvlTEk

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1787`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 785. AY-YR2Fnkrh49lmvlTEl

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1818`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 786. AY-YR2Fnkrh49lmvlTEo

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1830`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 787. AY-YR2Fnkrh49lmvlTEn

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1840`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 788. AY-YR2Fnkrh49lmvlTEm

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1851`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 789. AY-YR2Fnkrh49lmvlTEp

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:1865`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 790. AZDL7dCS6N0FxHM9EiJ0

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:2054`
- **Effort**: 2min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 791. AZDL7dCS6N0FxHM9EiJy

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:2054`
- **Effort**: 2min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 792. AZDL7dCS6N0FxHM9EiJz

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:2054`
- **Effort**: 2min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 793. AY-YR2Fnkrh49lmvlTEs

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlWireTest.java:2068`
- **Effort**: 5min
- **Created**: 2020-01-03T13:38:14+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 794. AY-YR1-okrh49lmvlTBx

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlTokeniserTest.java:39`
- **Effort**: 5min
- **Created**: 2020-01-02T08:33:06+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 795. AXzV0Q7_Kyg4rf3RvPmV

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:527`
- **Effort**: 12min
- **Created**: 2020-01-02T08:13:20+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 22 to the 15 allowed.

## 796. AXzV0Q7_Kyg4rf3RvPmi

- **Rule**: `java:S1066`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:561`
- **Effort**: 5min
- **Created**: 2020-01-02T08:13:20+0000
- **Assignee**: Unassigned
- **Message**:
  Merge this if statement with the enclosing one.

## 797. AXzV0Q7_Kyg4rf3RvPmY

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:773`
- **Effort**: 5min
- **Created**: 2020-01-01T19:42:47+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 798. AXzV0Q7_Kyg4rf3RvPmc

- **Rule**: `java:S1121`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:200`
- **Effort**: 5min
- **Created**: 2020-01-01T17:29:59+0000
- **Assignee**: Unassigned
- **Message**:
  Extract the assignment out of this expression.

## 799. AXzV0Q7_Kyg4rf3RvPmd

- **Rule**: `java:S1121`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:203`
- **Effort**: 5min
- **Created**: 2020-01-01T17:29:59+0000
- **Assignee**: Unassigned
- **Message**:
  Extract the assignment out of this expression.

## 800. AXzV0Q7_Kyg4rf3RvPmS

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:236`
- **Effort**: 5min
- **Created**: 2020-01-01T17:29:59+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 801. AXzV0Q7_Kyg4rf3RvPmh

- **Rule**: `java:S1066`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlTokeniser.java:296`
- **Effort**: 5min
- **Created**: 2020-01-01T17:29:59+0000
- **Assignee**: Unassigned
- **Message**:
  Merge this if statement with the enclosing one.

## 802. AXzV0Q_KKyg4rf3RvPox

- **Rule**: `java:S106`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GeneratedProxyClass.java:129`
- **Effort**: 10min
- **Created**: 2019-12-23T12:08:22+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this use of System.out by a logger.

## 803. AXzV0Q6aKyg4rf3RvPll

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReader.java:206`
- **Effort**: 5min
- **Created**: 2019-12-16T11:19:49+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused method parameter "name".

## 804. AXzV0Q6aKyg4rf3RvPlv

- **Rule**: `java:S107`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodReader.java:206`
- **Effort**: 20min
- **Created**: 2019-12-16T11:19:49+0000
- **Assignee**: Unassigned
- **Message**:
  Method has 9 parameters, which is greater than 7 authorized.

## 805. AXzV0Q-wKyg4rf3RvPon

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMessageHistory.java:38`
- **Effort**: 30min
- **Created**: 2019-12-04T09:44:37+0000
- **Assignee**: Unassigned
- **Message**:
  Override the "equals" method in this class.

## 806. AXzV0Q5PKyg4rf3RvPkj

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/MethodWireKey.java:31`
- **Effort**: 30min
- **Created**: 2019-12-04T09:23:33+0000
- **Assignee**: Unassigned
- **Message**:
  Override the "equals" method in this class.

## 807. AZDL7cV36N0FxHM9EiAm

- **Rule**: `java:S108`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/B2Class.java:47`
- **Effort**: 5min
- **Created**: 2019-12-04T05:50:41+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this block of code, fill it in, or add a comment explaining why it is empty.

## 808. AXzV0Q-lKyg4rf3RvPof

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:1791`
- **Effort**: 35min
- **Created**: 2019-12-04T05:28:28+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 45 to the 15 allowed.

## 809. AXzV0Q-lKyg4rf3RvPog

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wires.java:1793`
- **Effort**: 5min
- **Created**: 2019-12-04T05:28:28+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 810. AY-YR1dykrh49lmvlS7M

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/UnknownDatatimeTest.java:26`
- **Effort**: 5min
- **Created**: 2019-11-29T11:14:28+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.io.IOException', as it cannot be thrown from
  method's body.

## 811. AZDL7c6G6N0FxHM9EiJV

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/OutOfOrderTest.java:32`
- **Effort**: 2min
- **Created**: 2019-08-06T16:26:59+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 812. AZDL7c6G6N0FxHM9EiJW

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/OutOfOrderTest.java:33`
- **Effort**: 2min
- **Created**: 2019-08-06T16:26:59+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 813. AZDL7c6G6N0FxHM9EiJX

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/OutOfOrderTest.java:34`
- **Effort**: 2min
- **Created**: 2019-08-06T16:26:59+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 814. AZDL7c6G6N0FxHM9EiJY

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/OutOfOrderTest.java:35`
- **Effort**: 2min
- **Created**: 2019-08-06T16:26:59+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 815. AY-YR1r7krh49lmvlS-9

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2614`
- **Effort**: 5min
- **Created**: 2019-05-23T10:46:30+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 816. AXzV0RA5Kyg4rf3RvPq7

- **Rule**: `java:S108`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:928`
- **Effort**: 5min
- **Created**: 2019-05-22T19:53:14+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this block of code, fill it in, or add a comment explaining why it is empty.

## 817. AXzV0Q_KKyg4rf3RvPoy

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GeneratedProxyClass.java:89`
- **Effort**: 20min
- **Created**: 2019-04-06T13:09:54+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 818. AY-YR1r7krh49lmvlS9c

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:142`
- **Effort**: 5min
- **Created**: 2019-03-22T12:19:00+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 819. AX8r_qAI19JBuIecpRm2

- **Rule**: `java:S5777`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MarshallableTest.java:50`
- **Effort**: 5min
- **Created**: 2019-02-11T22:34:18+0000
- **Assignee**: Unassigned
- **Message**:
  Move assertions into separate method or use assertThrows or try-catch instead.

## 820. AX8r_qAI19JBuIecpRm3

- **Rule**: `java:S5777`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/MarshallableTest.java:58`
- **Effort**: 5min
- **Created**: 2019-02-11T22:34:18+0000
- **Assignee**: Unassigned
- **Message**:
  Move assertions into separate method or use assertThrows or try-catch instead.

## 821. AY6FVbEB5NUPA8B1oXo_

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/VanillaMethodWriterBuilder.java:258`
- **Effort**: 20min
- **Created**: 2019-02-01T17:48:14+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 822. AY6FVbIo5NUPA8B1oXpU

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/GeneratedProxyClass.java:134`
- **Effort**: 20min
- **Created**: 2019-02-01T09:25:08+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 823. AY-YR13Mkrh49lmvlTA5

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireToOutputStreamTest.java:147`
- **Effort**: 5min
- **Created**: 2019-01-24T19:04:10+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 824. AZS37q_iIVqiQpr6kOmY

- **Rule**: `java:S2093`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/IgnoreHighOrderBitsTest.java:40`
- **Effort**: 15min
- **Created**: 2018-10-24T11:33:45+0000
- **Assignee**: Unassigned
- **Message**:
  Change this "try" to a try-with-resources.

## 825. AXzV0Q9eKyg4rf3RvPny

- **Rule**: `java:S2065`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/LongValueBitSet.java:48`
- **Effort**: 2min
- **Created**: 2018-10-16T09:27:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the "transient" modifier from this field.

## 826. AXzV0RA5Kyg4rf3RvPq-

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1305`
- **Effort**: 30min
- **Created**: 2018-10-15T11:25:19+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 827. AXzV0RA5Kyg4rf3RvPq_

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1343`
- **Effort**: 30min
- **Created**: 2018-10-15T08:38:47+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 828. AXzV0Q9eKyg4rf3RvPnz

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/LongValueBitSet.java:369`
- **Effort**: 5min
- **Created**: 2018-10-10T11:19:04+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 829. AXzV0Q9eKyg4rf3RvPn2

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/LongValueBitSet.java:959`
- **Effort**: 20min
- **Created**: 2018-10-10T11:19:04+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 830. AY-YR1r7krh49lmvlS9b

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:133`
- **Effort**: 5min
- **Created**: 2018-09-03T15:48:35+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 831. AY-YR1r7krh49lmvlS-z

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2268`
- **Effort**: 5min
- **Created**: 2018-08-23T14:50:16+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 832. AY-YR1r7krh49lmvlS-0

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2282`
- **Effort**: 5min
- **Created**: 2018-08-23T14:50:16+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 833. AZDL7cu36N0FxHM9EiIl

- **Rule**: `java:S3008`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlSpecTest.java:29`
- **Effort**: 2min
- **Created**: 2018-07-24T16:10:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this field "DIR" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 834. AXzV0Q2HKyg4rf3RvPiJ

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BitSetUtil.java:86`
- **Effort**: 30min
- **Created**: 2018-07-24T13:07:35+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 835. AXzV0Q2HKyg4rf3RvPiK

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BitSetUtil.java:87`
- **Effort**: 30min
- **Created**: 2018-07-24T13:07:35+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 836. AXzV0Q2HKyg4rf3RvPiL

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BitSetUtil.java:88`
- **Effort**: 30min
- **Created**: 2018-07-24T13:07:35+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 837. AY-YR1yYkrh49lmvlTAP

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireTest.java:1543`
- **Effort**: 5min
- **Created**: 2018-07-02T09:30:05+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 838. AY-YR171krh49lmvlTBa

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/UnicodeStringTest.java:65`
- **Effort**: 1min
- **Created**: 2018-06-29T11:06:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "a".

## 839. AXzV0Q8aKyg4rf3RvPmz

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/AbstractWire.java:359`
- **Effort**: 20min
- **Created**: 2018-06-14T13:23:04+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 840. AY-YR1r7krh49lmvlS9l

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:378`
- **Effort**: 5min
- **Created**: 2018-05-16T13:32:29+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 841. AY-YR1r7krh49lmvlS9m

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:400`
- **Effort**: 5min
- **Created**: 2018-05-16T13:32:29+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 842. AY-YR1r7krh49lmvlS9n

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:415`
- **Effort**: 5min
- **Created**: 2018-05-16T13:32:29+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 843. AX8r_qFu19JBuIecpRm_

- **Rule**: `java:S5777`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireAgitatorTest.java:42`
- **Effort**: 5min
- **Created**: 2018-05-10T07:22:35+0000
- **Assignee**: Unassigned
- **Message**:
  Move assertions into separate method or use assertThrows or try-catch instead.

## 844. AZDL7cUs6N0FxHM9EiAj

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/MarshallableWithOverwriteFalseTest.java:81`
- **Effort**: 5min
- **Created**: 2018-04-21T09:49:25+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 845. AZS37rE6IVqiQpr6kOmj

- **Rule**: `java:S1640`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireDumperTest.java:40`
- **Effort**: 5min
- **Created**: 2018-01-16T10:05:20+0000
- **Assignee**: Unassigned
- **Message**:
  Convert this Map to an EnumMap.

## 846. AZS37rE6IVqiQpr6kOmi

- **Rule**: `java:S1640`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireDumperTest.java:39`
- **Effort**: 5min
- **Created**: 2018-01-15T15:08:07+0000
- **Assignee**: Unassigned
- **Message**:
  Convert this Map to an EnumMap.

## 847. AY-YR1_Bkrh49lmvlTB0

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireDumperTest.java:43`
- **Effort**: 5min
- **Created**: 2018-01-15T15:08:07+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused method parameter "name".

## 848. AXzV0Q4SKyg4rf3RvPkI

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryMethodWriterInvocationHandler.java:89`
- **Effort**: 20min
- **Created**: 2017-12-14T05:40:16+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 849. AY6FVbDR5NUPA8B1oXo1

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/MarshallableOut.java:110`
- **Effort**: 20min
- **Created**: 2017-12-14T05:40:16+0000
- **Assignee**: JerryShea@github
- **Message**:
  Catch Exception instead of Throwable.

## 850. AY6FVbDR5NUPA8B1oXo4

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/MarshallableOut.java:172`
- **Effort**: 20min
- **Created**: 2017-12-14T05:40:16+0000
- **Assignee**: JerryShea@github
- **Message**:
  Catch Exception instead of Throwable.

## 851. AY6FVbDR5NUPA8B1oXo5

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/MarshallableOut.java:197`
- **Effort**: 20min
- **Created**: 2017-12-14T05:40:16+0000
- **Assignee**: JerryShea@github
- **Message**:
  Catch Exception instead of Throwable.

## 852. AY6FVbDR5NUPA8B1oXo6

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/MarshallableOut.java:216`
- **Effort**: 20min
- **Created**: 2017-12-14T05:40:16+0000
- **Assignee**: JerryShea@github
- **Message**:
  Catch Exception instead of Throwable.

## 853. AY6FVbDR5NUPA8B1oXo7

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/MarshallableOut.java:240`
- **Effort**: 20min
- **Created**: 2017-12-14T05:40:16+0000
- **Assignee**: JerryShea@github
- **Message**:
  Catch Exception instead of Throwable.

## 854. AXzV0Q23Kyg4rf3RvPjO

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:3718`
- **Effort**: 5min
- **Created**: 2017-11-28T18:41:49+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 855. AXzV0Q23Kyg4rf3RvPjK

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:4556`
- **Effort**: 5min
- **Created**: 2017-11-28T18:41:49+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 856. AXzV0Q6_Kyg4rf3RvPl6

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ParameterHolderSequenceWriter.java:76`
- **Effort**: 5min
- **Created**: 2017-11-20T20:11:07+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "methodId" which hides the field declared at line 57.

## 857. AXzV0Q64Kyg4rf3RvPl5

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireCommon.java:72`
- **Effort**: 20min
- **Created**: 2017-11-20T20:11:07+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 858. AZDL7co86N0FxHM9EiB_

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/bytesmarshallable/BytesMarshallableTest.java:87`
- **Effort**: 5min
- **Created**: 2017-10-24T11:00:46+0000
- **Assignee**: Unassigned
- **Message**:
  Complete cases by adding the missing enum constants or add a default case to this switch.

## 859. AZDL7co86N0FxHM9EiCA

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/bytesmarshallable/BytesMarshallableTest.java:135`
- **Effort**: 5min
- **Created**: 2017-10-24T11:00:46+0000
- **Assignee**: Unassigned
- **Message**:
  Complete cases by adding the missing enum constants or add a default case to this switch.

## 860. AXzV0RA5Kyg4rf3RvPqx

- **Rule**: `java:S112`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:953`
- **Effort**: 20min
- **Created**: 2017-10-05T12:16:55+0000
- **Assignee**: Unassigned
- **Message**:
  Define and throw a dedicated exception instead of using a generic one.

## 861. AXzV0RA5Kyg4rf3RvPrE

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1875`
- **Effort**: 30min
- **Created**: 2017-10-05T12:16:55+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 862. AXzV0RA5Kyg4rf3RvPrF

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1881`
- **Effort**: 30min
- **Created**: 2017-10-05T12:16:55+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 863. AXzV0RA5Kyg4rf3RvPrG

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1894`
- **Effort**: 30min
- **Created**: 2017-10-05T12:16:55+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 864. AXzV0RA5Kyg4rf3RvPrH

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1900`
- **Effort**: 30min
- **Created**: 2017-10-05T12:16:55+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 865. AXzV0Q23Kyg4rf3RvPiz

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1437`
- **Effort**: 0min
- **Created**: 2017-10-02T22:32:56+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 866. AXzV0Q23Kyg4rf3RvPi4

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1517`
- **Effort**: 0min
- **Created**: 2017-10-02T22:32:56+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 867. AXzV0Q23Kyg4rf3RvPjY

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:4737`
- **Effort**: 5min
- **Created**: 2017-10-02T22:32:56+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 868. AXzV0Q23Kyg4rf3RvPjX

- **Rule**: `java:S1871`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:4917`
- **Effort**: 10min
- **Created**: 2017-10-02T22:32:56+0000
- **Assignee**: Unassigned
- **Message**:
  This case's code block is the same as the block for the case on line 4914.

## 869. AXzV0Q23Kyg4rf3RvPjW

- **Rule**: `java:S1871`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:4920`
- **Effort**: 10min
- **Created**: 2017-10-02T22:32:56+0000
- **Assignee**: Unassigned
- **Message**:
  This case's code block is the same as the block for the case on line 4914.

## 870. AXzV0Q23Kyg4rf3RvPjZ

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:4944`
- **Effort**: 5min
- **Created**: 2017-10-02T22:32:56+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 871. AXzV0Q_uKyg4rf3RvPpF

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:516`
- **Effort**: 0min
- **Created**: 2017-10-02T22:31:13+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 872. AZS37q4XIVqiQpr6kOmQ

- **Rule**: `java:S1640`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/CopyTest.java:139`
- **Effort**: 5min
- **Created**: 2017-10-02T22:31:13+0000
- **Assignee**: Unassigned
- **Message**:
  Convert this Map to an EnumMap.

## 873. AXzV0Q_uKyg4rf3RvPpG

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:614`
- **Effort**: 10min
- **Created**: 2017-08-19T02:54:17+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 20 to the 15 allowed.

## 874. AZDL7ciu6N0FxHM9EiBq

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2772`
- **Effort**: 5min
- **Created**: 2017-08-18T12:56:39+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 875. AXzV0Q2XKyg4rf3RvPiW

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireType.java:582`
- **Effort**: 7min
- **Created**: 2017-08-18T12:21:01+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## 876. AXzV0RA5Kyg4rf3RvPrK

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:2061`
- **Effort**: 30min
- **Created**: 2017-08-08T15:38:32+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 877. AXzV0RA5Kyg4rf3RvPrL

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:2067`
- **Effort**: 30min
- **Created**: 2017-08-08T15:38:32+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 878. AXzV0RA5Kyg4rf3RvPrR

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:2304`
- **Effort**: 30min
- **Created**: 2017-08-08T15:38:32+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 879. AXzV0RA5Kyg4rf3RvPrS

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:2311`
- **Effort**: 30min
- **Created**: 2017-08-08T15:38:32+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 880. AY-YR1yYkrh49lmvlTAN

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireTest.java:1280`
- **Effort**: 5min
- **Created**: 2017-08-08T15:38:32+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 881. AXzV0Q2XKyg4rf3RvPiV

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireType.java:533`
- **Effort**: 5min
- **Created**: 2017-07-31T17:16:09+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 882. AXzV0Q_uKyg4rf3RvPp6

- **Rule**: `java:S1301`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:2746`
- **Effort**: 5min
- **Created**: 2017-07-31T14:17:59+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this "switch" statement by "if" statements to increase readability.

## 883. AXzV0Q_uKyg4rf3RvPpj

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:2746`
- **Effort**: 5min
- **Created**: 2017-07-31T14:17:59+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 884. AXzV0Q_uKyg4rf3RvPpk

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:2789`
- **Effort**: 5min
- **Created**: 2017-07-31T14:17:59+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 885. AXzV0RA5Kyg4rf3RvPrA

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1417`
- **Effort**: 30min
- **Created**: 2017-07-31T14:17:59+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 886. AXzV0RA5Kyg4rf3RvPrB

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1425`
- **Effort**: 30min
- **Created**: 2017-07-31T14:17:59+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 887. AZDL7cpX6N0FxHM9EiCB

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/UnknownEnumTest.java:115`
- **Effort**: 2min
- **Created**: 2017-07-28T16:41:03+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 888. AZDL7cpX6N0FxHM9EiCC

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/UnknownEnumTest.java:116`
- **Effort**: 2min
- **Created**: 2017-07-28T16:41:03+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 889. AXzV0RASKyg4rf3RvPqM

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/VanillaFieldInfo.java:34`
- **Effort**: 30min
- **Created**: 2017-07-24T17:04:44+0000
- **Assignee**: Unassigned
- **Message**:
  Override the "equals" method in this class.

## 890. AY-YR118krh49lmvlTAt

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireStringInternerTest.java:82`
- **Effort**: 5min
- **Created**: 2017-07-18T11:19:31+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.lang.Exception', as it cannot be thrown from
  method's body.

## 891. AZS37q8xIVqiQpr6kOmX

- **Rule**: `java:S1612`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireStringInternerTest.java:179`
- **Effort**: 2min
- **Created**: 2017-07-18T11:19:31+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this lambda with method reference 'BufferUnderflowException.class::isInstance'.

## 892. AY-YR118krh49lmvlTAu

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireStringInternerTest.java:191`
- **Effort**: 5min
- **Created**: 2017-07-18T11:19:31+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.io.IOException', as it cannot be thrown from
  constructor's body.

## 893. AXzV0Q23Kyg4rf3RvPjC

- **Rule**: `java:S1905`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:2973`
- **Effort**: 5min
- **Created**: 2017-07-07T11:14:33+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unnecessary cast to "double".

## 894. AXzV0RA5Kyg4rf3RvPrJ

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1985`
- **Effort**: 30min
- **Created**: 2017-07-06T16:53:14+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 895. AY-YR1oKkrh49lmvlS9A

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WiresTest.java:140`
- **Effort**: 5min
- **Created**: 2017-06-18T15:58:21+0000
- **Assignee**: JerryShea@github
- **Message**:
  Rename "container1" which hides the field declared at line 46.

## 896. AY-YR1oKkrh49lmvlS9B

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WiresTest.java:143`
- **Effort**: 5min
- **Created**: 2017-06-18T15:58:21+0000
- **Assignee**: JerryShea@github
- **Message**:
  Rename "container2" which hides the field declared at line 47.

## 897. AY-YR1oKkrh49lmvlS8-

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WiresTest.java:120`
- **Effort**: 5min
- **Created**: 2017-06-15T10:12:52+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "container1" which hides the field declared at line 46.

## 898. AY-YR1oKkrh49lmvlS8_

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WiresTest.java:124`
- **Effort**: 5min
- **Created**: 2017-06-15T10:12:52+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "container2" which hides the field declared at line 47.

## 899. AY-YR1r7krh49lmvlS-3

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2312`
- **Effort**: 5min
- **Created**: 2017-06-06T14:01:56+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 900. AY-YR1r7krh49lmvlS-y

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2252`
- **Effort**: 5min
- **Created**: 2017-06-04T12:59:31+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 901. AY-YR1r7krh49lmvlS-2

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2297`
- **Effort**: 5min
- **Created**: 2017-06-04T12:59:31+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 902. AZDL7ciu6N0FxHM9EiBf

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2218`
- **Effort**: 5min
- **Created**: 2017-06-02T19:57:09+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 903. AZS37q0NIVqiQpr6kOmM

- **Rule**: `java:S1643`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2220`
- **Effort**: 10min
- **Created**: 2017-06-02T19:57:09+0000
- **Assignee**: Unassigned
- **Message**:
  Use a StringBuilder instead.

## 904. AZS37q0NIVqiQpr6kOmN

- **Rule**: `java:S1643`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2224`
- **Effort**: 10min
- **Created**: 2017-06-02T19:57:09+0000
- **Assignee**: Unassigned
- **Message**:
  Use a StringBuilder instead.

## 905. AZS37q0NIVqiQpr6kOmO

- **Rule**: `java:S1643`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2230`
- **Effort**: 10min
- **Created**: 2017-06-02T19:57:09+0000
- **Assignee**: Unassigned
- **Message**:
  Use a StringBuilder instead.

## 906. AZS37q0NIVqiQpr6kOmP

- **Rule**: `java:S1643`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2237`
- **Effort**: 10min
- **Created**: 2017-06-02T19:57:09+0000
- **Assignee**: Unassigned
- **Message**:
  Use a StringBuilder instead.

## 907. AY-YR1r7krh49lmvlS-H

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1148`
- **Effort**: 5min
- **Created**: 2017-05-31T16:26:07+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 908. AY-YR1r7krh49lmvlS-I

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1169`
- **Effort**: 5min
- **Created**: 2017-05-31T16:26:07+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 909. AY-YR1r7krh49lmvlS-J

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1211`
- **Effort**: 5min
- **Created**: 2017-05-31T16:26:07+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 910. AZDL7ciu6N0FxHM9EiBn

- **Rule**: `java:S116`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2665`
- **Effort**: 2min
- **Created**: 2017-05-31T16:26:07+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this field "A" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 911. AZDL7ciu6N0FxHM9EiBo

- **Rule**: `java:S116`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2666`
- **Effort**: 2min
- **Created**: 2017-05-31T16:26:07+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this field "B" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 912. AZDL7ciu6N0FxHM9EiBp

- **Rule**: `java:S116`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2667`
- **Effort**: 2min
- **Created**: 2017-05-31T16:26:07+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this field "C" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 913. AXzV0Q_uKyg4rf3RvPo_

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:77`
- **Effort**: 5min
- **Created**: 2017-05-30T11:33:17+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 914. AXzV0Q_uKyg4rf3RvPpA

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:78`
- **Effort**: 5min
- **Created**: 2017-05-30T11:33:17+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 915. AY-YR101krh49lmvlTAl

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextCompatibilityTest.java:110`
- **Effort**: 5min
- **Created**: 2017-05-24T04:32:00+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "o" local variable.

## 916. AZMw7pkuYk7po6Fb1sLr

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryReadDocumentContext.java:22`
- **Effort**: 1min
- **Created**: 2017-05-17T17:32:44+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'net.openhft.chronicle.core.Jvm'.

## 917. AXzV0Q_WKyg4rf3RvPo5

- **Rule**: `java:S1104`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryReadDocumentContext.java:38`
- **Effort**: 10min
- **Created**: 2017-05-17T17:32:44+0000
- **Assignee**: Unassigned
- **Message**:
  Make start a static final constant or non-public and provide accessors if needed.

## 918. AXzV0Q_WKyg4rf3RvPo6

- **Rule**: `java:S1104`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryReadDocumentContext.java:39`
- **Effort**: 10min
- **Created**: 2017-05-17T17:32:44+0000
- **Assignee**: Unassigned
- **Message**:
  Make lastStart a static final constant or non-public and provide accessors if needed.

## 919. AZMw7pkuYk7po6Fb1sLp

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryReadDocumentContext.java:67`
- **Effort**: 10min
- **Created**: 2017-05-17T17:32:44+0000
- **Assignee**: Unassigned
- **Message**:
  Do not forget to remove this deprecated code someday.

## 920. AZMw7pkuYk7po6Fb1sLq

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryReadDocumentContext.java:67`
- **Effort**: 5min
- **Created**: 2017-05-17T17:32:44+0000
- **Assignee**: Unassigned
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 921. AXzV0Q55Kyg4rf3RvPlc

- **Rule**: `java:S1659`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextReadDocumentContext.java:42`
- **Effort**: 2min
- **Created**: 2017-05-17T16:59:03+0000
- **Assignee**: Unassigned
- **Message**:
  Declare "notComplete" on a separate line.

## 922. AXzV0Q55Kyg4rf3RvPld

- **Rule**: `java:S1659`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextReadDocumentContext.java:48`
- **Effort**: 2min
- **Created**: 2017-05-17T16:59:03+0000
- **Assignee**: Unassigned
- **Message**:
  Declare "readLimit" on a separate line.

## 923. AXzV0Q55Kyg4rf3RvPle

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextReadDocumentContext.java:132`
- **Effort**: 5min
- **Created**: 2017-05-17T16:59:03+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "readLimit" which hides the field declared at line 48.

## 924. AXzV0Q55Kyg4rf3RvPlf

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextReadDocumentContext.java:133`
- **Effort**: 5min
- **Created**: 2017-05-17T16:59:03+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "readPosition" which hides the field declared at line 48.

## 925. AXzV0Q03Kyg4rf3RvPhX

- **Rule**: `java:S2924`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ProjectTest.java:41`
- **Effort**: 5min
- **Created**: 2017-05-16T15:49:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "TestName".

## 926. AXzV0Q3rKyg4rf3RvPj9

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/CSVWire.java:136`
- **Effort**: 20min
- **Created**: 2017-05-16T14:25:12+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 927. AY-YR1eMkrh49lmvlS7N

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/EnumWireTest.java:95`
- **Effort**: 5min
- **Created**: 2017-04-20T08:51:34+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 928. AXzV0Q23Kyg4rf3RvPjL

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:4238`
- **Effort**: 0min
- **Created**: 2017-03-05T15:43:25+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 929. AXzV0Q23Kyg4rf3RvPjI

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:3646`
- **Effort**: 5min
- **Created**: 2017-02-14T11:36:15+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 930. AY-YR1r7krh49lmvlS-G

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1123`
- **Effort**: 5min
- **Created**: 2017-01-26T14:41:06+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 931. AY-YR2DYkrh49lmvlTCW

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWire2Test.java:205`
- **Effort**: 5min
- **Created**: 2017-01-10T14:23:05+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 932. AY-YR1r7krh49lmvlS9f

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:246`
- **Effort**: 5min
- **Created**: 2017-01-10T14:23:05+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 933. AXzV0RASKyg4rf3RvPqU

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/VanillaFieldInfo.java:110`
- **Effort**: 30min
- **Created**: 2017-01-10T12:39:44+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 934. AXzV0RASKyg4rf3RvPqV

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/VanillaFieldInfo.java:119`
- **Effort**: 30min
- **Created**: 2017-01-10T12:39:44+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 935. AXzV0RASKyg4rf3RvPqW

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/VanillaFieldInfo.java:128`
- **Effort**: 30min
- **Created**: 2017-01-10T12:39:44+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 936. AXzV0RASKyg4rf3RvPqX

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/VanillaFieldInfo.java:137`
- **Effort**: 30min
- **Created**: 2017-01-10T12:39:44+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 937. AY-YR1r7krh49lmvlS-x

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2079`
- **Effort**: 5min
- **Created**: 2016-12-30T11:29:02+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 938. AXzV0RAIKyg4rf3RvPqC

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Marshallable.java:58`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 939. AXzV0RAIKyg4rf3RvPqJ

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Marshallable.java:58`
- **Effort**: 2min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 940. AXzV0Q_uKyg4rf3RvPpV

- **Rule**: `java:S119`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1525`
- **Effort**: 10min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this generic name to match the regular expression '^[A-Z][0-9]?$'.

## 941. AXzV0Q2OKyg4rf3RvPiN

- **Rule**: `java:S3066`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlLogging.java:70`
- **Effort**: 20min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Lower the visibility of this setter or remove it altogether.

## 942. AY-YR17jkrh49lmvlTBY

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/CSVBytesMarshallableTest.java:89`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused method parameter "binary".

## 943. AY-YR1nkkrh49lmvlS8i

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:269`
- **Effort**: 1min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "data2".

## 944. AY-YR1nkkrh49lmvlS8k

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:269`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "data2" local variable.

## 945. AY-YR1nkkrh49lmvlS8h

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:293`
- **Effort**: 1min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "data3".

## 946. AY-YR1nkkrh49lmvlS8o

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:293`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "data3" local variable.

## 947. AY-YR1nkkrh49lmvlS82

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:519`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "data2" local variable.

## 948. AY-YR1nkkrh49lmvlS8z

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:519`
- **Effort**: 1min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "data2".

## 949. AY-YR1nkkrh49lmvlS80

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:546`
- **Effort**: 1min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "data2".

## 950. AY-YR1nkkrh49lmvlS86

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:546`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "data2" local variable.

## 951. AXzV0Qt6Kyg4rf3RvPg0

- **Rule**: `java:S5976`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/StrangeTextCombinationTest.java:77`
- **Effort**: 10min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Replace these 9 tests with a single Parameterized one.

## 952. AY-YR1r7krh49lmvlS9j

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:338`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 953. AY-YR1r7krh49lmvlS9k

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:356`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 954. AY-YR1r7krh49lmvlS9p

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:422`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 955. AY-YR1r7krh49lmvlS9q

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:447`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 956. AY-YR1r7krh49lmvlS9r

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:463`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 957. AY-YR1r7krh49lmvlS9s

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:479`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 958. AY-YR1r7krh49lmvlS9t

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:491`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 959. AY-YR1r7krh49lmvlS9u

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:509`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 960. AY-YR1r7krh49lmvlS9v

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:521`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 961. AY-YR1r7krh49lmvlS9w

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:535`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 962. AY-YR1r7krh49lmvlS9x

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:549`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 963. AY-YR1r7krh49lmvlS9y

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:571`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 964. AY-YR1r7krh49lmvlS9z

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:594`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 965. AY-YR1r7krh49lmvlS90

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:624`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 966. AY-YR1r7krh49lmvlS91

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:654`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 967. AY-YR1r7krh49lmvlS92

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:684`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 968. AY-YR1r7krh49lmvlS93

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:714`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 969. AY-YR1r7krh49lmvlS94

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:744`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 970. AY-YR1r7krh49lmvlS95

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:774`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 971. AY-YR1r7krh49lmvlS96

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:804`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 972. AY-YR1r7krh49lmvlS97

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:834`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 973. AY-YR1r7krh49lmvlS98

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:871`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 974. AY-YR1r7krh49lmvlS99

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:902`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 975. AY-YR1r7krh49lmvlS-A

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:973`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 976. AY-YR1r7krh49lmvlS-B

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:989`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 977. AY-YR1r7krh49lmvlS-C

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1009`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 978. AY-YR1r7krh49lmvlS-D

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1033`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 979. AY-YR1r7krh49lmvlS-E

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1085`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 980. AY-YR1r7krh49lmvlS-F

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1104`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 981. AY-YR1r7krh49lmvlS-N

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1299`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 982. AY-YR1r7krh49lmvlS-O

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1355`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 983. AY-YR1r7krh49lmvlS-V

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1534`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 984. AY-YR1r7krh49lmvlS-W

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1554`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 985. AY-YR1r7krh49lmvlS-X

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1591`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 986. AY-YR1r7krh49lmvlS-Y

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1615`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 987. AY-YR1r7krh49lmvlS-a

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1633`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 988. AY-YR1r7krh49lmvlS-c

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1651`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 989. AY-YR1r7krh49lmvlS-e

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1679`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 990. AY-YR1r7krh49lmvlS-g

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1707`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 991. AY-YR1r7krh49lmvlS-i

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1737`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 992. AY-YR1r7krh49lmvlS-l

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1795`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 993. AY-YR1r7krh49lmvlS-m

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1814`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 994. AY-YR1r7krh49lmvlS-n

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1834`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 995. AY-YR1r7krh49lmvlS-p

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1877`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 996. AY-YR1r7krh49lmvlS-q

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1908`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 997. AY-YR1r7krh49lmvlS-r

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1951`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 998. AY-YR1r7krh49lmvlS-u

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2006`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 999. AY-YR1r7krh49lmvlS-v

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2023`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 1000. AY-YR1r7krh49lmvlS-w

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2051`
- **Effort**: 5min
- **Created**: 2016-12-28T18:30:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 1001. AXzV0Q2XKyg4rf3RvPiS

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireType.java:688`
- **Effort**: 5min
- **Created**: 2016-12-20T14:16:46+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 1002. AY-YR1r7krh49lmvlS9h

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:310`
- **Effort**: 5min
- **Created**: 2016-12-15T16:46:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "wire" which hides the field declared at line 68.

## 1003. AXzV0RA5Kyg4rf3RvPq3

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:655`
- **Effort**: 30min
- **Created**: 2016-12-05T20:16:54+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 1004. AXzV0Q_uKyg4rf3RvPo9

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:75`
- **Effort**: 5min
- **Created**: 2016-11-04T14:30:02+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1005. AXzV0Q_uKyg4rf3RvPo-

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:76`
- **Effort**: 5min
- **Created**: 2016-11-04T14:30:02+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1006. AXzV0RASKyg4rf3RvPqN

- **Rule**: `java:S2065`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/VanillaFieldInfo.java:37`
- **Effort**: 2min
- **Created**: 2016-11-01T15:38:42+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the "transient" modifier from this field.

## 1007. AXzV0RASKyg4rf3RvPqT

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/internal/VanillaFieldInfo.java:101`
- **Effort**: 30min
- **Created**: 2016-11-01T15:38:42+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 1008. AY-YR1p-krh49lmvlS9P

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/JSON222IndividualTest.java:108`
- **Effort**: 1min
- **Created**: 2016-10-31T14:48:22+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "o".

## 1009. AY-YR1p-krh49lmvlS9Q

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/JSON222IndividualTest.java:108`
- **Effort**: 5min
- **Created**: 2016-10-31T14:48:22+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "o" local variable.

## 1010. AY-YR1pbkrh49lmvlS9K

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/JSON222Test.java:94`
- **Effort**: 5min
- **Created**: 2016-10-31T14:05:45+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1011. AY-YR1pbkrh49lmvlS9L

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/JSON222Test.java:116`
- **Effort**: 5min
- **Created**: 2016-10-31T14:05:45+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1012. AY-YR1pbkrh49lmvlS9N

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/issue/JSON222Test.java:152`
- **Effort**: 5min
- **Created**: 2016-10-31T14:05:45+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1013. AY-YR11Wkrh49lmvlTAo

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/DefaultMarshallerTest.java:41`
- **Effort**: 1min
- **Created**: 2016-10-20T04:36:58+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "dmOuterClass".

## 1014. AY-YR11Wkrh49lmvlTAp

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/DefaultMarshallerTest.java:41`
- **Effort**: 5min
- **Created**: 2016-10-20T04:36:58+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "dmOuterClass" local variable.

## 1015. AXzV0RAIKyg4rf3RvPqH

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Marshallable.java:294`
- **Effort**: 5min
- **Created**: 2016-10-18T15:55:01+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 1016. AXzV0RA5Kyg4rf3RvPrC

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1680`
- **Effort**: 30min
- **Created**: 2016-10-18T15:55:01+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 1017. AXzV0RA5Kyg4rf3RvPrD

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:1691`
- **Effort**: 30min
- **Created**: 2016-10-18T15:55:01+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 1018. AY-YR2DYkrh49lmvlTCc

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWire2Test.java:1043`
- **Effort**: 1min
- **Created**: 2016-07-23T13:40:45+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "size".

## 1019. AY-YR2DYkrh49lmvlTCd

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWire2Test.java:1043`
- **Effort**: 5min
- **Created**: 2016-07-23T13:40:45+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "size" local variable.

## 1020. AXzV0Q5yKyg4rf3RvPlX

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireDumper.java:160`
- **Effort**: 20min
- **Created**: 2016-07-09T10:13:57+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 1021. AXzV0Q5HKyg4rf3RvPka

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireInternal.java:173`
- **Effort**: 5min
- **Created**: 2016-07-07T16:34:36+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1022. AY_Fi6wkTbFMTgi4MX0h

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/EnumTest.java:23`
- **Effort**: 1min
- **Created**: 2016-06-24T14:42:31+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'net.openhft.chronicle.core.util.ReadResolvable'.

## 1023. AZDL7c2S6N0FxHM9EiJG

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/EnumTest.java:91`
- **Effort**: 5min
- **Created**: 2016-06-24T14:42:31+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 1024. AZDL7c2S6N0FxHM9EiJH

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/EnumTest.java:96`
- **Effort**: 5min
- **Created**: 2016-06-24T14:42:31+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 1025. AXzV0Q6uKyg4rf3RvPl3

- **Rule**: `java:S1192`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/RawWire.java:630`
- **Effort**: 10min
- **Created**: 2016-06-20T15:34:58+0000
- **Assignee**: Unassigned
- **Message**:
  Define a constant instead of duplicating this literal "Document length %,d out of 32-bit int range."
  4 times.

## 1026. AY-YR2BYkrh49lmvlTCD

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlSpecificationTest.java:82`
- **Effort**: 0min
- **Created**: 2016-06-11T16:13:57+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1027. AY-YR2BYkrh49lmvlTCE

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlSpecificationTest.java:84`
- **Effort**: 0min
- **Created**: 2016-06-11T16:13:57+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1028. AY-YR2BYkrh49lmvlTCF

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlSpecificationTest.java:85`
- **Effort**: 0min
- **Created**: 2016-06-11T16:13:57+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1029. AZMw7pYbYk7po6Fb1sLh

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:51`
- **Effort**: 1min
- **Created**: 2016-05-26T21:51:30+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'java.util.Arrays'.

## 1030. AXzV0Q24Kyg4rf3RvPjc

- **Rule**: `java:S3398`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1438`
- **Effort**: 5min
- **Created**: 2016-05-15T11:25:59+0000
- **Assignee**: Unassigned
- **Message**:
  Move this method into "BinaryValueIn".

## 1031. AXzV0Q_uKyg4rf3RvPpc

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1965`
- **Effort**: 5min
- **Created**: 2016-05-15T11:25:59+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1032. AY-YR10bkrh49lmvlTAi

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextBinaryWireTest.java:139`
- **Effort**: 0min
- **Created**: 2016-05-15T11:25:59+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1033. AY-YR19Akrh49lmvlTBl

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/map/MapWireTest.java:102`
- **Effort**: 5min
- **Created**: 2016-05-13T16:17:43+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1034. AXzV0Q23Kyg4rf3RvPir

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:973`
- **Effort**: 5min
- **Created**: 2016-05-11T22:04:37+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1035. AXzV0Q23Kyg4rf3RvPis

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1122`
- **Effort**: 5min
- **Created**: 2016-05-11T22:04:37+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1036. AXzV0Q_uKyg4rf3RvPpI

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:666`
- **Effort**: 5min
- **Created**: 2016-05-11T22:04:37+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1037. AXzV0Q_uKyg4rf3RvPpy

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1252`
- **Effort**: 20min
- **Created**: 2016-05-11T22:04:37+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 1038. AXzV0Q23Kyg4rf3RvPjD

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:3164`
- **Effort**: 5min
- **Created**: 2016-05-10T15:56:23+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1039. AXzV0RA5Kyg4rf3RvPrN

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:2089`
- **Effort**: 30min
- **Created**: 2016-05-10T15:56:23+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 1040. AXzV0RA5Kyg4rf3RvPrQ

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:2220`
- **Effort**: 30min
- **Created**: 2016-05-10T15:56:23+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 1041. AXzV0RA5Kyg4rf3RvPrU

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:2330`
- **Effort**: 30min
- **Created**: 2016-05-10T15:56:23+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 1042. AY-YR1dXkrh49lmvlS7K

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/marshallable/ScalarValues.java:62`
- **Effort**: 5min
- **Created**: 2016-05-10T15:56:23+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1043. AXzV0Q6jKyg4rf3RvPlx

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireObjectInput.java:88`
- **Effort**: 5min
- **Created**: 2016-05-09T11:19:27+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 1044. AXzV0RA5Kyg4rf3RvPrO

- **Rule**: `java:S1185`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:2109`
- **Effort**: 2min
- **Created**: 2016-05-08T17:19:05+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this method to simply inherit it.

## 1045. AXzV0RA5Kyg4rf3RvPrP

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:2214`
- **Effort**: 30min
- **Created**: 2016-05-08T17:19:05+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 1046. AZMw7pgfYk7po6Fb1sLj

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWireCode.java:57`
- **Effort**: 10min
- **Created**: 2016-05-08T06:41:16+0000
- **Assignee**: Unassigned
- **Message**:
  Do not forget to remove this deprecated code someday.

## 1047. AZMw7pgfYk7po6Fb1sLk

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWireCode.java:57`
- **Effort**: 5min
- **Created**: 2016-05-08T06:41:16+0000
- **Assignee**: Unassigned
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 1048. AZMw7pgfYk7po6Fb1sLl

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWireCode.java:63`
- **Effort**: 10min
- **Created**: 2016-05-08T06:41:16+0000
- **Assignee**: Unassigned
- **Message**:
  Do not forget to remove this deprecated code someday.

## 1049. AZMw7pgfYk7po6Fb1sLm

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWireCode.java:63`
- **Effort**: 5min
- **Created**: 2016-05-08T06:41:16+0000
- **Assignee**: Unassigned
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 1050. AZMw7pgfYk7po6Fb1sLn

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWireCode.java:69`
- **Effort**: 10min
- **Created**: 2016-05-08T06:41:16+0000
- **Assignee**: Unassigned
- **Message**:
  Do not forget to remove this deprecated code someday.

## 1051. AZMw7pgfYk7po6Fb1sLo

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWireCode.java:69`
- **Effort**: 5min
- **Created**: 2016-05-08T06:41:16+0000
- **Assignee**: Unassigned
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 1052. AXzV0Q8IKyg4rf3RvPmm

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWireCode.java:75`
- **Effort**: 5min
- **Created**: 2016-05-08T06:41:16+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1053. AXzV0Q8IKyg4rf3RvPmn

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWireCode.java:181`
- **Effort**: 5min
- **Created**: 2016-05-08T06:41:16+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1054. AXzV0Q8IKyg4rf3RvPmk

- **Rule**: `java:S2386`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWireCode.java:293`
- **Effort**: 15min
- **Created**: 2016-05-08T06:41:16+0000
- **Assignee**: Unassigned
- **Message**:
  Make this member "protected".

## 1055. AZDL7c6w6N0FxHM9EiJZ

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/NestedMapsTest.java:77`
- **Effort**: 5min
- **Created**: 2016-04-21T10:43:22+0000
- **Assignee**: Unassigned
- **Message**:
  Complete cases by adding the missing enum constants or add a default case to this switch.

## 1056. AZDL7c6w6N0FxHM9EiJa

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/NestedMapsTest.java:217`
- **Effort**: 5min
- **Created**: 2016-04-21T10:43:22+0000
- **Assignee**: Unassigned
- **Message**:
  Complete cases by adding the missing enum constants or add a default case to this switch.

## 1057. AY-YR1z_krh49lmvlTAh

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reordered/ReorderedTest.java:172`
- **Effort**: 5min
- **Created**: 2016-04-17T16:35:11+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1058. AXzV0RAuKyg4rf3RvPqi

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:932`
- **Effort**: 5min
- **Created**: 2016-04-15T10:43:13+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 1059. AY-YR11ykrh49lmvlTAs

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireTextBugTest.java:23`
- **Effort**: 5min
- **Created**: 2016-04-09T21:59:28+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1060. AXzV0Q2OKyg4rf3RvPiR

- **Rule**: `java:S1444`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlLogging.java:34`
- **Effort**: 20min
- **Created**: 2016-04-09T17:38:18+0000
- **Assignee**: Unassigned
- **Message**:
  Make this "public static title" field final

## 1061. AXzV0Q4aKyg4rf3RvPkO

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ValueOut.java:1509`
- **Effort**: 5min
- **Created**: 2016-04-09T11:09:09+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1062. AY-YR2Eakrh49lmvlTCp

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:104`
- **Effort**: 5min
- **Created**: 2016-04-02T19:38:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused method parameter "t".

## 1063. AXzV0Q4aKyg4rf3RvPkJ

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/ValueOut.java:443`
- **Effort**: 5min
- **Created**: 2016-03-29T09:57:10+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 1064. AY-YR1-0krh49lmvlTBz

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmePojoTest.java:66`
- **Effort**: 5min
- **Created**: 2016-03-17T17:01:09+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.io.IOException', as it cannot be thrown from
  method's body.

## 1065. AXzV0RAIKyg4rf3RvPqD

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Marshallable.java:69`
- **Effort**: 5min
- **Created**: 2016-03-16T12:14:47+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 1066. AXzV0RAIKyg4rf3RvPqK

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Marshallable.java:69`
- **Effort**: 2min
- **Created**: 2016-03-16T12:14:47+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 1067. AXzV0RAIKyg4rf3RvPqE

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Marshallable.java:79`
- **Effort**: 5min
- **Created**: 2016-03-16T12:14:47+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 1068. AXzV0RAIKyg4rf3RvPqL

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Marshallable.java:79`
- **Effort**: 2min
- **Created**: 2016-03-16T12:14:47+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 1069. AXzV0RA5Kyg4rf3RvPrM

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:2079`
- **Effort**: 30min
- **Created**: 2016-03-16T12:14:47+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 1070. AXzV0RA5Kyg4rf3RvPrT

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireMarshaller.java:2325`
- **Effort**: 30min
- **Created**: 2016-03-16T12:14:47+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 1071. AY6FVbCw5NUPA8B1oXo0

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Demarshallable.java:79`
- **Effort**: 20min
- **Created**: 2016-03-07T17:11:36+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 1072. AY-YR1fukrh49lmvlS7V

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/WireCollectionTest.java:52`
- **Effort**: 5min
- **Created**: 2016-02-13T13:21:31+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1073. AY-YR1fbkrh49lmvlS7R

- **Rule**: `java:S1611`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/WireUtils.java:62`
- **Effort**: 2min
- **Created**: 2016-02-13T08:22:04+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the parentheses around the "i" parameter

## 1074. AY-YR1fbkrh49lmvlS7S

- **Rule**: `java:S1611`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/WireUtils.java:68`
- **Effort**: 2min
- **Created**: 2016-02-13T08:22:04+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the parentheses around the "i" parameter

## 1075. AY-YR1fbkrh49lmvlS7T

- **Rule**: `java:S1611`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/WireUtils.java:71`
- **Effort**: 2min
- **Created**: 2016-02-13T08:22:04+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the parentheses around the "k" parameter

## 1076. AY-YR1fbkrh49lmvlS7U

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/reuse/WireUtils.java:133`
- **Effort**: 5min
- **Created**: 2016-02-13T08:22:04+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1077. AXzV0Q2XKyg4rf3RvPib

- **Rule**: `java:S4042`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireType.java:629`
- **Effort**: 10min
- **Created**: 2016-02-10T20:36:20+0000
- **Assignee**: Unassigned
- **Message**:
  Use "java.nio.file.Files#delete" here for better messages on error conditions.

## 1078. AY-YR1_8krh49lmvlTB4

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireTypeTest.java:101`
- **Effort**: 5min
- **Created**: 2016-02-10T17:39:26+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1079. AXzV0Q3UKyg4rf3RvPj1

- **Rule**: `java:S5411`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/HashWire.java:377`
- **Effort**: 5min
- **Created**: 2016-02-05T19:40:31+0000
- **Assignee**: Unassigned
- **Message**:
  Use a primitive boolean expression here.

## 1080. AXzV0Q_uKyg4rf3RvPpf

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:2497`
- **Effort**: 5min
- **Created**: 2016-02-04T21:06:47+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1081. AXzV0Q_uKyg4rf3RvPph

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:2564`
- **Effort**: 5min
- **Created**: 2016-02-04T21:06:47+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1082. AXzV0Q_uKyg4rf3RvPpl

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:2856`
- **Effort**: 5min
- **Created**: 2016-02-04T21:06:47+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1083. AXzV0Q23Kyg4rf3RvPjU

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:4849`
- **Effort**: 5min
- **Created**: 2016-02-02T12:22:50+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1084. AXzV0Q23Kyg4rf3RvPjV

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:4876`
- **Effort**: 5min
- **Created**: 2016-02-02T12:22:50+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1085. AXzV0Q3EKyg4rf3RvPjt

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/QueryWire.java:383`
- **Effort**: 5min
- **Created**: 2016-02-02T01:51:26+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 1086. AXzV0Q3EKyg4rf3RvPju

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/QueryWire.java:387`
- **Effort**: 5min
- **Created**: 2016-02-02T01:51:26+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 1087. AXzV0RAuKyg4rf3RvPqh

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:918`
- **Effort**: 5min
- **Created**: 2016-02-02T01:05:28+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 1088. AXzV0RAuKyg4rf3RvPqj

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:937`
- **Effort**: 5min
- **Created**: 2016-02-02T01:05:28+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 1089. AXzV0RAuKyg4rf3RvPqk

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:941`
- **Effort**: 5min
- **Created**: 2016-02-02T01:05:28+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 1090. AZDL7ctD6N0FxHM9EiIh

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireTest.java:1778`
- **Effort**: 2min
- **Created**: 2016-01-29T14:45:01+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1091. AZDL7ctD6N0FxHM9EiIi

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireTest.java:1778`
- **Effort**: 2min
- **Created**: 2016-01-29T14:45:01+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1092. AZDL7ctD6N0FxHM9EiIj

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireTest.java:1778`
- **Effort**: 2min
- **Created**: 2016-01-29T14:45:01+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1093. AZDL7c--6N0FxHM9EiJk

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWire2Test.java:600`
- **Effort**: 2min
- **Created**: 2016-01-27T11:21:34+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 1094. AZDL7c--6N0FxHM9EiJl

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWire2Test.java:612`
- **Effort**: 2min
- **Created**: 2016-01-27T11:21:34+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 1095. AZDL7ciu6N0FxHM9EiBd

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1850`
- **Effort**: 2min
- **Created**: 2016-01-27T11:21:34+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 1096. AZDL7ciu6N0FxHM9EiBe

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1866`
- **Effort**: 2min
- **Created**: 2016-01-27T11:21:34+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 1097. AXzV0Q-IKyg4rf3RvPoK

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireIn.java:228`
- **Effort**: 0min
- **Created**: 2015-12-24T18:38:43+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1098. AY-YR1nkkrh49lmvlS84

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:527`
- **Effort**: 5min
- **Created**: 2015-10-13T18:18:02+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 1099. AY-YR1nkkrh49lmvlS88

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:559`
- **Effort**: 5min
- **Created**: 2015-10-13T18:18:02+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1100. AZDL7csQ6N0FxHM9EiCI

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:26`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1101. AZDL7csQ6N0FxHM9EiCJ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:27`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1102. AZDL7csQ6N0FxHM9EiCK

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:28`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1103. AZDL7csQ6N0FxHM9EiCL

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:29`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1104. AZDL7csQ6N0FxHM9EiCM

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:30`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1105. AZDL7csQ6N0FxHM9EiCN

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:31`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1106. AZDL7csQ6N0FxHM9EiCO

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:32`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1107. AZDL7csQ6N0FxHM9EiCP

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:33`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1108. AZDL7csQ6N0FxHM9EiCQ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:34`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1109. AZDL7csQ6N0FxHM9EiCR

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:35`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1110. AZDL7csQ6N0FxHM9EiCS

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:36`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1111. AZDL7csQ6N0FxHM9EiCT

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:37`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1112. AZDL7csQ6N0FxHM9EiCU

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:38`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1113. AZDL7csQ6N0FxHM9EiCV

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:39`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1114. AZDL7csQ6N0FxHM9EiCW

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:40`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1115. AZDL7csQ6N0FxHM9EiCX

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:41`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1116. AZDL7csQ6N0FxHM9EiCY

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:42`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1117. AZDL7csQ6N0FxHM9EiCZ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:43`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1118. AZDL7csQ6N0FxHM9EiCa

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:44`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1119. AZDL7csQ6N0FxHM9EiCb

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:45`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1120. AZDL7csQ6N0FxHM9EiCc

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:46`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1121. AZDL7csQ6N0FxHM9EiCd

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:47`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1122. AZDL7csQ6N0FxHM9EiCe

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:48`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1123. AZDL7csQ6N0FxHM9EiCf

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:49`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1124. AZDL7csQ6N0FxHM9EiCg

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:50`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1125. AZDL7csQ6N0FxHM9EiCh

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:51`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1126. AZDL7csQ6N0FxHM9EiCi

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:52`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1127. AZDL7csQ6N0FxHM9EiCj

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:53`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1128. AZDL7csQ6N0FxHM9EiCk

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:54`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1129. AZDL7csQ6N0FxHM9EiCl

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:55`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1130. AZDL7csQ6N0FxHM9EiCm

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:56`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1131. AZDL7csQ6N0FxHM9EiCn

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:57`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1132. AZDL7csQ6N0FxHM9EiCo

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:58`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1133. AZDL7csQ6N0FxHM9EiCp

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:59`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1134. AZDL7csQ6N0FxHM9EiCq

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:60`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1135. AZDL7csQ6N0FxHM9EiCr

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:61`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1136. AZDL7csQ6N0FxHM9EiCs

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:62`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1137. AZDL7csQ6N0FxHM9EiCt

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:63`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1138. AZDL7csQ6N0FxHM9EiCu

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:64`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1139. AZDL7csQ6N0FxHM9EiCv

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:65`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1140. AZDL7csQ6N0FxHM9EiCw

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:66`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1141. AZDL7csQ6N0FxHM9EiCx

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:67`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1142. AZDL7csQ6N0FxHM9EiCy

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:68`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1143. AZDL7csQ6N0FxHM9EiCz

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:69`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1144. AZDL7csQ6N0FxHM9EiC0

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:70`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1145. AZDL7csQ6N0FxHM9EiC1

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:71`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1146. AZDL7csQ6N0FxHM9EiC2

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:72`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1147. AZDL7csQ6N0FxHM9EiC3

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:73`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1148. AZDL7csQ6N0FxHM9EiC4

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:74`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1149. AZDL7csQ6N0FxHM9EiC5

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:75`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1150. AZDL7csQ6N0FxHM9EiC6

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:76`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1151. AZDL7csQ6N0FxHM9EiC7

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:77`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1152. AZDL7csQ6N0FxHM9EiC8

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:78`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1153. AZDL7csQ6N0FxHM9EiC9

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:79`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1154. AZDL7csQ6N0FxHM9EiC-

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:80`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1155. AZDL7csQ6N0FxHM9EiC_

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:81`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1156. AZDL7csQ6N0FxHM9EiDA

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:82`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1157. AZDL7csQ6N0FxHM9EiDB

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:83`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1158. AZDL7csQ6N0FxHM9EiDC

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:84`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1159. AZDL7csQ6N0FxHM9EiDD

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:85`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1160. AZDL7csQ6N0FxHM9EiDE

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:86`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1161. AZDL7csQ6N0FxHM9EiDF

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:87`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1162. AZDL7csQ6N0FxHM9EiDG

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:88`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1163. AZDL7csQ6N0FxHM9EiDH

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:89`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1164. AZDL7csQ6N0FxHM9EiDI

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:90`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1165. AZDL7csQ6N0FxHM9EiDJ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:91`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1166. AZDL7csQ6N0FxHM9EiDK

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:92`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1167. AZDL7csQ6N0FxHM9EiDL

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:93`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1168. AZDL7csQ6N0FxHM9EiDM

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:94`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1169. AZDL7csQ6N0FxHM9EiDN

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:95`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1170. AZDL7csQ6N0FxHM9EiDO

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:96`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1171. AZDL7csQ6N0FxHM9EiDP

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:97`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1172. AZDL7csQ6N0FxHM9EiDQ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:98`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1173. AZDL7csQ6N0FxHM9EiDR

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:99`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1174. AZDL7csQ6N0FxHM9EiDS

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:100`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1175. AZDL7csQ6N0FxHM9EiDT

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:101`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1176. AZDL7csQ6N0FxHM9EiDU

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:102`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1177. AZDL7csQ6N0FxHM9EiDV

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:103`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1178. AZDL7csQ6N0FxHM9EiDW

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:104`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1179. AZDL7csQ6N0FxHM9EiDX

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:105`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1180. AZDL7csQ6N0FxHM9EiDY

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:106`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1181. AZDL7csQ6N0FxHM9EiDZ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:107`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1182. AZDL7csQ6N0FxHM9EiDa

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:108`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1183. AZDL7csQ6N0FxHM9EiDb

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:109`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1184. AZDL7csQ6N0FxHM9EiDc

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:110`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1185. AZDL7csQ6N0FxHM9EiDd

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:111`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1186. AZDL7csQ6N0FxHM9EiDe

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:112`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1187. AZDL7csQ6N0FxHM9EiDf

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:113`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1188. AZDL7csQ6N0FxHM9EiDg

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:114`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1189. AZDL7csQ6N0FxHM9EiDh

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:115`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1190. AZDL7csQ6N0FxHM9EiDi

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:116`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1191. AZDL7csQ6N0FxHM9EiDj

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:117`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1192. AZDL7csQ6N0FxHM9EiDk

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:118`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1193. AZDL7csQ6N0FxHM9EiDl

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:119`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1194. AZDL7csQ6N0FxHM9EiDm

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:120`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1195. AZDL7csQ6N0FxHM9EiDn

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:121`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1196. AZDL7csQ6N0FxHM9EiDo

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:122`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1197. AZDL7csQ6N0FxHM9EiDp

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:123`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1198. AZDL7csQ6N0FxHM9EiDq

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:124`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1199. AZDL7csQ6N0FxHM9EiDr

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:125`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1200. AZDL7csQ6N0FxHM9EiDs

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:126`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1201. AZDL7csQ6N0FxHM9EiDt

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:127`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1202. AZDL7csQ6N0FxHM9EiDu

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:128`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1203. AZDL7csQ6N0FxHM9EiDv

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:129`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1204. AZDL7csQ6N0FxHM9EiDw

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:130`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1205. AZDL7csQ6N0FxHM9EiDx

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:131`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1206. AZDL7csQ6N0FxHM9EiDy

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:132`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1207. AZDL7csQ6N0FxHM9EiDz

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:133`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1208. AZDL7csQ6N0FxHM9EiD0

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:134`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1209. AZDL7csQ6N0FxHM9EiD1

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:135`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1210. AZDL7csQ6N0FxHM9EiD2

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:136`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1211. AZDL7csQ6N0FxHM9EiD3

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:137`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1212. AZDL7csQ6N0FxHM9EiD4

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:138`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1213. AZDL7csQ6N0FxHM9EiD5

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:139`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1214. AZDL7csQ6N0FxHM9EiD6

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:140`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1215. AZDL7csQ6N0FxHM9EiD7

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:141`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1216. AZDL7csQ6N0FxHM9EiD8

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:142`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1217. AZDL7csQ6N0FxHM9EiD9

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:143`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1218. AZDL7csQ6N0FxHM9EiD-

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:144`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1219. AZDL7csQ6N0FxHM9EiD_

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:145`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1220. AZDL7csQ6N0FxHM9EiEA

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:146`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1221. AZDL7csQ6N0FxHM9EiEB

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:147`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1222. AZDL7csQ6N0FxHM9EiEC

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:148`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1223. AZDL7csQ6N0FxHM9EiED

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:149`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1224. AZDL7csQ6N0FxHM9EiEE

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:150`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1225. AZDL7csQ6N0FxHM9EiEF

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:151`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1226. AZDL7csQ6N0FxHM9EiEG

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:152`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1227. AZDL7csQ6N0FxHM9EiEH

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:153`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1228. AZDL7csQ6N0FxHM9EiEI

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:154`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1229. AZDL7csQ6N0FxHM9EiEJ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:155`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1230. AZDL7csQ6N0FxHM9EiEK

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:156`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1231. AZDL7csQ6N0FxHM9EiEL

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:157`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1232. AZDL7csQ6N0FxHM9EiEM

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:158`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1233. AZDL7csQ6N0FxHM9EiEN

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:159`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1234. AZDL7csQ6N0FxHM9EiEO

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:160`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1235. AZDL7csQ6N0FxHM9EiEP

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:161`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1236. AZDL7csQ6N0FxHM9EiEQ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:162`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1237. AZDL7csQ6N0FxHM9EiER

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:163`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1238. AZDL7csQ6N0FxHM9EiES

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:164`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1239. AZDL7csQ6N0FxHM9EiET

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:165`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1240. AZDL7csQ6N0FxHM9EiEU

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:166`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1241. AZDL7csQ6N0FxHM9EiEV

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:167`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1242. AZDL7csQ6N0FxHM9EiEW

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:168`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1243. AZDL7csQ6N0FxHM9EiEX

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:169`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1244. AZDL7csQ6N0FxHM9EiEY

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:170`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1245. AZDL7csQ6N0FxHM9EiEZ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:171`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1246. AZDL7csQ6N0FxHM9EiEa

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:172`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1247. AZDL7csQ6N0FxHM9EiEb

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:173`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1248. AZDL7csQ6N0FxHM9EiEc

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:174`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1249. AZDL7csQ6N0FxHM9EiEd

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:175`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1250. AZDL7csQ6N0FxHM9EiEe

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:176`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1251. AZDL7csQ6N0FxHM9EiEf

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:177`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1252. AZDL7csQ6N0FxHM9EiEg

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:178`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1253. AZDL7csQ6N0FxHM9EiEh

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:179`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1254. AZDL7csQ6N0FxHM9EiEi

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:180`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1255. AZDL7csQ6N0FxHM9EiEj

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:181`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1256. AZDL7csQ6N0FxHM9EiEk

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:182`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1257. AZDL7csQ6N0FxHM9EiEl

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:183`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1258. AZDL7csQ6N0FxHM9EiEm

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:184`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1259. AZDL7csQ6N0FxHM9EiEn

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:185`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1260. AZDL7csQ6N0FxHM9EiEo

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:186`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1261. AZDL7csQ6N0FxHM9EiEp

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:187`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1262. AZDL7csQ6N0FxHM9EiEq

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:188`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1263. AZDL7csQ6N0FxHM9EiEr

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:189`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1264. AZDL7csQ6N0FxHM9EiEs

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:190`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1265. AZDL7csQ6N0FxHM9EiEt

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:191`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1266. AZDL7csQ6N0FxHM9EiEu

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:192`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1267. AZDL7csQ6N0FxHM9EiEv

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:193`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1268. AZDL7csQ6N0FxHM9EiEw

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:194`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1269. AZDL7csQ6N0FxHM9EiEx

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:195`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1270. AZDL7csQ6N0FxHM9EiEy

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:196`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1271. AZDL7csQ6N0FxHM9EiEz

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:197`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1272. AZDL7csQ6N0FxHM9EiE0

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:198`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1273. AZDL7csQ6N0FxHM9EiE1

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:199`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1274. AZDL7csQ6N0FxHM9EiE2

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:200`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1275. AZDL7csQ6N0FxHM9EiE3

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:201`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1276. AZDL7csQ6N0FxHM9EiE4

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:202`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1277. AZDL7csQ6N0FxHM9EiE5

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:203`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1278. AZDL7csQ6N0FxHM9EiE6

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:204`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1279. AZDL7csQ6N0FxHM9EiE7

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:205`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1280. AZDL7csQ6N0FxHM9EiE8

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:206`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1281. AZDL7csQ6N0FxHM9EiE9

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:207`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1282. AZDL7csQ6N0FxHM9EiE-

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:208`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1283. AZDL7csQ6N0FxHM9EiE_

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:209`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1284. AZDL7csQ6N0FxHM9EiFA

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:210`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1285. AZDL7csQ6N0FxHM9EiFB

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:211`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1286. AZDL7csQ6N0FxHM9EiFC

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:212`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1287. AZDL7csQ6N0FxHM9EiFD

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:213`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1288. AZDL7csQ6N0FxHM9EiFE

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:214`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1289. AZDL7csQ6N0FxHM9EiFF

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:215`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1290. AZDL7csQ6N0FxHM9EiFG

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:216`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1291. AZDL7csQ6N0FxHM9EiFH

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:217`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1292. AZDL7csQ6N0FxHM9EiFI

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:218`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1293. AZDL7csQ6N0FxHM9EiFJ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:219`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1294. AZDL7csQ6N0FxHM9EiFK

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:220`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1295. AZDL7csQ6N0FxHM9EiFL

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:221`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1296. AZDL7csQ6N0FxHM9EiFM

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:222`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1297. AZDL7csQ6N0FxHM9EiFN

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:223`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1298. AZDL7csQ6N0FxHM9EiFO

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:224`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1299. AZDL7csQ6N0FxHM9EiFP

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:225`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1300. AZDL7csQ6N0FxHM9EiFQ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:226`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1301. AZDL7csQ6N0FxHM9EiFR

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:227`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1302. AZDL7csQ6N0FxHM9EiFS

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:228`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1303. AZDL7csQ6N0FxHM9EiFT

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:229`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1304. AZDL7csQ6N0FxHM9EiFU

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:230`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1305. AZDL7csQ6N0FxHM9EiFV

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:231`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1306. AZDL7csQ6N0FxHM9EiFW

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:232`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1307. AZDL7csQ6N0FxHM9EiFX

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:233`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1308. AZDL7csQ6N0FxHM9EiFY

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:234`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1309. AZDL7csQ6N0FxHM9EiFZ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:235`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1310. AZDL7csQ6N0FxHM9EiFa

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:236`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1311. AZDL7csQ6N0FxHM9EiFb

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:237`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1312. AZDL7csQ6N0FxHM9EiFc

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:238`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1313. AZDL7csQ6N0FxHM9EiFd

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:239`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1314. AZDL7csQ6N0FxHM9EiFe

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:240`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1315. AZDL7csQ6N0FxHM9EiFf

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:241`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1316. AZDL7csQ6N0FxHM9EiFg

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:242`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1317. AZDL7csQ6N0FxHM9EiFh

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:243`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1318. AZDL7csQ6N0FxHM9EiFi

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:244`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1319. AZDL7csQ6N0FxHM9EiFj

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:245`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1320. AZDL7csQ6N0FxHM9EiFk

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:246`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1321. AZDL7csQ6N0FxHM9EiFl

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:247`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1322. AZDL7csQ6N0FxHM9EiFm

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:248`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1323. AZDL7csQ6N0FxHM9EiFn

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:249`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1324. AZDL7csQ6N0FxHM9EiFo

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:250`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1325. AZDL7csQ6N0FxHM9EiFp

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:251`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1326. AZDL7csQ6N0FxHM9EiFq

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:252`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1327. AZDL7csQ6N0FxHM9EiFr

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:253`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1328. AZDL7csQ6N0FxHM9EiFs

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:254`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1329. AZDL7csQ6N0FxHM9EiFt

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:255`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1330. AZDL7csQ6N0FxHM9EiFu

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:256`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1331. AZDL7csQ6N0FxHM9EiFv

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:257`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1332. AZDL7csQ6N0FxHM9EiFw

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:258`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1333. AZDL7csQ6N0FxHM9EiFx

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:259`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1334. AZDL7csQ6N0FxHM9EiFy

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:260`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1335. AZDL7csQ6N0FxHM9EiFz

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:261`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1336. AZDL7csQ6N0FxHM9EiF0

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:262`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1337. AZDL7csQ6N0FxHM9EiF1

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:263`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1338. AZDL7csQ6N0FxHM9EiF2

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:264`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1339. AZDL7csQ6N0FxHM9EiF3

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:265`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1340. AZDL7csQ6N0FxHM9EiF4

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:266`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1341. AZDL7csQ6N0FxHM9EiF5

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:267`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1342. AZDL7csQ6N0FxHM9EiF6

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:268`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1343. AZDL7csQ6N0FxHM9EiF7

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:269`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1344. AZDL7csQ6N0FxHM9EiF8

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:270`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1345. AZDL7csQ6N0FxHM9EiF9

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:271`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1346. AZDL7csQ6N0FxHM9EiF-

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:272`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1347. AZDL7csQ6N0FxHM9EiF_

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:273`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1348. AZDL7csQ6N0FxHM9EiGA

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:274`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1349. AZDL7csQ6N0FxHM9EiGB

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:275`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1350. AZDL7csQ6N0FxHM9EiGC

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:276`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1351. AZDL7csQ6N0FxHM9EiGD

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:277`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1352. AZDL7csQ6N0FxHM9EiGE

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:278`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1353. AZDL7csQ6N0FxHM9EiGF

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:279`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1354. AZDL7csQ6N0FxHM9EiGG

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:280`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1355. AZDL7csQ6N0FxHM9EiGH

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:281`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1356. AZDL7csQ6N0FxHM9EiGI

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:282`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1357. AZDL7csQ6N0FxHM9EiGJ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:283`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1358. AZDL7csQ6N0FxHM9EiGK

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:284`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1359. AZDL7csQ6N0FxHM9EiGL

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:285`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1360. AZDL7csQ6N0FxHM9EiGM

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:286`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1361. AZDL7csQ6N0FxHM9EiGN

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:287`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1362. AZDL7csQ6N0FxHM9EiGO

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:288`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1363. AZDL7csQ6N0FxHM9EiGP

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:289`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1364. AZDL7csQ6N0FxHM9EiGQ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:290`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1365. AZDL7csQ6N0FxHM9EiGR

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:291`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1366. AZDL7csQ6N0FxHM9EiGS

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:292`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1367. AZDL7csQ6N0FxHM9EiGT

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:293`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1368. AZDL7csQ6N0FxHM9EiGU

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:294`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1369. AZDL7csQ6N0FxHM9EiGV

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:295`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1370. AZDL7csQ6N0FxHM9EiGW

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:296`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1371. AZDL7csQ6N0FxHM9EiGX

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:297`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1372. AZDL7csQ6N0FxHM9EiGY

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:298`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1373. AZDL7csQ6N0FxHM9EiGZ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:299`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1374. AZDL7csQ6N0FxHM9EiGa

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:300`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1375. AZDL7csQ6N0FxHM9EiGb

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:301`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1376. AZDL7csQ6N0FxHM9EiGc

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:302`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1377. AZDL7csQ6N0FxHM9EiGd

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:303`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1378. AZDL7csQ6N0FxHM9EiGe

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:304`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1379. AZDL7csQ6N0FxHM9EiGf

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:305`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1380. AZDL7csQ6N0FxHM9EiGg

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:306`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1381. AZDL7csQ6N0FxHM9EiGh

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:307`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1382. AZDL7csQ6N0FxHM9EiGi

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:308`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1383. AZDL7csQ6N0FxHM9EiGj

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:309`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1384. AZDL7csQ6N0FxHM9EiGk

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:310`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1385. AZDL7csQ6N0FxHM9EiGl

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:311`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1386. AZDL7csQ6N0FxHM9EiGm

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:312`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1387. AZDL7csQ6N0FxHM9EiGn

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:313`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1388. AZDL7csQ6N0FxHM9EiGo

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:314`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1389. AZDL7csQ6N0FxHM9EiGp

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:315`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1390. AZDL7csQ6N0FxHM9EiGq

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:316`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1391. AZDL7csQ6N0FxHM9EiGr

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:317`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1392. AZDL7csQ6N0FxHM9EiGs

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:318`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1393. AZDL7csQ6N0FxHM9EiGt

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:319`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1394. AZDL7csQ6N0FxHM9EiGu

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:320`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1395. AZDL7csQ6N0FxHM9EiGv

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:321`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1396. AZDL7csQ6N0FxHM9EiGw

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:322`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1397. AZDL7csQ6N0FxHM9EiGx

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:323`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1398. AZDL7csQ6N0FxHM9EiGy

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:324`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1399. AZDL7csQ6N0FxHM9EiGz

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:325`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1400. AZDL7csQ6N0FxHM9EiG0

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:326`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1401. AZDL7csQ6N0FxHM9EiG1

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:327`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1402. AZDL7csQ6N0FxHM9EiG2

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:328`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1403. AZDL7csQ6N0FxHM9EiG3

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:329`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1404. AZDL7csQ6N0FxHM9EiG4

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:330`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1405. AZDL7csQ6N0FxHM9EiG5

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:331`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1406. AZDL7csQ6N0FxHM9EiG6

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:332`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1407. AZDL7csQ6N0FxHM9EiG7

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:333`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1408. AZDL7csQ6N0FxHM9EiG8

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:334`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1409. AZDL7csQ6N0FxHM9EiG9

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:335`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1410. AZDL7csQ6N0FxHM9EiG-

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:336`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1411. AZDL7csQ6N0FxHM9EiG_

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:337`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1412. AZDL7csQ6N0FxHM9EiHA

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:338`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1413. AZDL7csQ6N0FxHM9EiHB

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:339`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1414. AZDL7csQ6N0FxHM9EiHC

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:340`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1415. AZDL7csQ6N0FxHM9EiHD

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:341`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1416. AZDL7csQ6N0FxHM9EiHE

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:342`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1417. AZDL7csQ6N0FxHM9EiHF

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:343`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1418. AZDL7csQ6N0FxHM9EiHG

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:344`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1419. AZDL7csQ6N0FxHM9EiHH

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:345`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1420. AZDL7csQ6N0FxHM9EiHI

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:346`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1421. AZDL7csQ6N0FxHM9EiHJ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:347`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1422. AZDL7csQ6N0FxHM9EiHK

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:348`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1423. AZDL7csQ6N0FxHM9EiHL

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:349`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1424. AZDL7csQ6N0FxHM9EiHM

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:350`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1425. AZDL7csQ6N0FxHM9EiHN

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:351`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1426. AZDL7csQ6N0FxHM9EiHO

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:352`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1427. AZDL7csQ6N0FxHM9EiHP

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:353`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1428. AZDL7csQ6N0FxHM9EiHQ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:354`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1429. AZDL7csQ6N0FxHM9EiHR

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:355`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1430. AZDL7csQ6N0FxHM9EiHS

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:356`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1431. AZDL7csQ6N0FxHM9EiHT

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:357`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1432. AZDL7csQ6N0FxHM9EiHU

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:358`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1433. AZDL7csQ6N0FxHM9EiHV

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:359`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1434. AZDL7csQ6N0FxHM9EiHW

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:360`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1435. AZDL7csQ6N0FxHM9EiHX

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:361`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1436. AZDL7csQ6N0FxHM9EiHY

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:362`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1437. AZDL7csQ6N0FxHM9EiHZ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:363`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1438. AZDL7csQ6N0FxHM9EiHa

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:364`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1439. AZDL7csQ6N0FxHM9EiHb

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:365`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1440. AZDL7csQ6N0FxHM9EiHc

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:366`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1441. AZDL7csQ6N0FxHM9EiHd

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:367`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1442. AZDL7csQ6N0FxHM9EiHe

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:368`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1443. AZDL7csQ6N0FxHM9EiHf

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:369`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1444. AZDL7csQ6N0FxHM9EiHg

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:370`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1445. AZDL7csQ6N0FxHM9EiHh

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:371`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1446. AZDL7csQ6N0FxHM9EiHi

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:372`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1447. AZDL7csQ6N0FxHM9EiHj

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:373`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1448. AZDL7csQ6N0FxHM9EiHk

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:374`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1449. AZDL7csQ6N0FxHM9EiHl

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:375`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1450. AZDL7csQ6N0FxHM9EiHm

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:376`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1451. AZDL7csQ6N0FxHM9EiHn

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:377`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1452. AZDL7csQ6N0FxHM9EiHo

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:378`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1453. AZDL7csQ6N0FxHM9EiHp

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:379`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1454. AZDL7csQ6N0FxHM9EiHq

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:380`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1455. AZDL7csQ6N0FxHM9EiHr

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:381`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1456. AZDL7csQ6N0FxHM9EiHs

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:382`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1457. AZDL7csQ6N0FxHM9EiHt

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:383`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1458. AZDL7csQ6N0FxHM9EiHu

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:384`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1459. AZDL7csQ6N0FxHM9EiHv

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:385`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1460. AZDL7csQ6N0FxHM9EiHw

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:386`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1461. AZDL7csQ6N0FxHM9EiHx

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:387`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1462. AZDL7csQ6N0FxHM9EiHy

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:388`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1463. AZDL7csQ6N0FxHM9EiHz

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:389`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1464. AZDL7csQ6N0FxHM9EiH0

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:390`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1465. AZDL7csQ6N0FxHM9EiH1

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:391`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1466. AZDL7csQ6N0FxHM9EiH2

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:392`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1467. AZDL7csQ6N0FxHM9EiH3

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:393`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1468. AZDL7csQ6N0FxHM9EiH4

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:394`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1469. AZDL7csQ6N0FxHM9EiH5

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:395`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1470. AZDL7csQ6N0FxHM9EiH6

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:396`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1471. AZDL7csQ6N0FxHM9EiH7

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:397`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1472. AZDL7csQ6N0FxHM9EiH8

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:398`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1473. AZDL7csQ6N0FxHM9EiH9

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:399`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1474. AZDL7csQ6N0FxHM9EiH-

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:400`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1475. AZDL7csQ6N0FxHM9EiH_

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:401`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1476. AZDL7csQ6N0FxHM9EiIA

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:402`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1477. AZDL7csQ6N0FxHM9EiIB

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:403`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1478. AZDL7csQ6N0FxHM9EiIC

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:404`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1479. AZDL7csQ6N0FxHM9EiID

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:405`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1480. AZDL7csQ6N0FxHM9EiIE

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:406`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1481. AZDL7csQ6N0FxHM9EiIF

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:407`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1482. AZDL7csQ6N0FxHM9EiIG

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:408`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1483. AZDL7csQ6N0FxHM9EiIH

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:409`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1484. AZDL7csQ6N0FxHM9EiII

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:410`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1485. AZDL7csQ6N0FxHM9EiIJ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:411`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1486. AZDL7csQ6N0FxHM9EiIK

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:412`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1487. AZDL7csQ6N0FxHM9EiIL

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:413`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1488. AZDL7csQ6N0FxHM9EiIM

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:414`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1489. AZDL7csQ6N0FxHM9EiIN

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:415`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1490. AZDL7csQ6N0FxHM9EiIO

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:416`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1491. AZDL7csQ6N0FxHM9EiIP

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:417`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1492. AZDL7csQ6N0FxHM9EiIQ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:418`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1493. AZDL7csQ6N0FxHM9EiIR

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:419`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1494. AZDL7csQ6N0FxHM9EiIS

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:420`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1495. AZDL7csQ6N0FxHM9EiIT

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:421`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1496. AZDL7csQ6N0FxHM9EiIU

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:422`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1497. AZDL7csQ6N0FxHM9EiIV

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:423`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1498. AZDL7csQ6N0FxHM9EiIW

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:424`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1499. AZDL7csQ6N0FxHM9EiIX

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:425`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1500. AZDL7csQ6N0FxHM9EiIY

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:426`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1501. AZDL7csQ6N0FxHM9EiIZ

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:427`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1502. AZDL7csQ6N0FxHM9EiIa

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:428`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1503. AZDL7csQ6N0FxHM9EiIb

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:429`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1504. AZDL7csQ6N0FxHM9EiIc

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/FIX42.java:430`
- **Effort**: 2min
- **Created**: 2015-10-06T16:52:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1505. AXzV0QxUKyg4rf3RvPhF

- **Rule**: `java:S2924`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/WireTests.java:55`
- **Effort**: 5min
- **Created**: 2015-10-05T15:31:00+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "TestName".

## 1506. AXzV0Q5HKyg4rf3RvPkd

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireInternal.java:276`
- **Effort**: 5min
- **Created**: 2015-09-01T08:16:52+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1507. AXzV0Q5HKyg4rf3RvPke

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireInternal.java:386`
- **Effort**: 30min
- **Created**: 2015-09-01T08:16:52+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 1508. AXzV0Q5HKyg4rf3RvPkf

- **Rule**: `java:S3012`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireInternal.java:417`
- **Effort**: 5min
- **Created**: 2015-09-01T08:16:52+0000
- **Assignee**: Unassigned
- **Message**:
  Use "Arrays.copyOf", "Arrays.asList", "Collections.addAll" or "System.arraycopy" instead.

## 1509. AXzV0Q5HKyg4rf3RvPkg

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireInternal.java:422`
- **Effort**: 30min
- **Created**: 2015-09-01T08:16:52+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 1510. AXzV0Q5HKyg4rf3RvPkh

- **Rule**: `java:S3358`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireInternal.java:439`
- **Effort**: 5min
- **Created**: 2015-09-01T08:16:52+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested ternary operation into an independent statement.

## 1511. AXzV0Q23Kyg4rf3RvPjJ

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:3669`
- **Effort**: 0min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1512. AXzV0Q_uKyg4rf3RvPpv

- **Rule**: `java:S1121`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:2135`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Extract the assignment out of this expression.

## 1513. AXzV0Q_uKyg4rf3RvPpw

- **Rule**: `java:S1121`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:2145`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Extract the assignment out of this expression.

## 1514. AY-YR2Eakrh49lmvlTCy

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:182`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1515. AY-YR2Eakrh49lmvlTCz

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:182`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1516. AY-YR2Eakrh49lmvlTC0

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:183`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1517. AY-YR2Eakrh49lmvlTC1

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:183`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1518. AY-YR2Eakrh49lmvlTC2

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:184`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1519. AY-YR2Eakrh49lmvlTC3

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:184`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1520. AY-YR2Eakrh49lmvlTC4

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:185`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1521. AY-YR2Eakrh49lmvlTC5

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:185`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1522. AY-YR2Eakrh49lmvlTC6

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:186`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1523. AY-YR2Eakrh49lmvlTC7

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:186`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1524. AY-YR2Eakrh49lmvlTC8

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:187`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1525. AY-YR2Eakrh49lmvlTC9

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:187`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1526. AY-YR2Eakrh49lmvlTC-

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:188`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "t" which hides the field declared at line 155.

## 1527. AY-YR2Eakrh49lmvlTC_

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:188`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1528. AY-YR2Eakrh49lmvlTDA

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:189`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1529. AY-YR2Eakrh49lmvlTDB

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:189`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1530. AY-YR2Eakrh49lmvlTDC

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:190`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1531. AY-YR2Eakrh49lmvlTDD

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:190`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1532. AY-YR2Eakrh49lmvlTDE

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:191`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1533. AY-YR2Eakrh49lmvlTDF

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:191`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1534. AY-YR2Eakrh49lmvlTDG

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:192`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1535. AY-YR2Eakrh49lmvlTDH

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:192`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1536. AY-YR2Eakrh49lmvlTDI

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:193`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1537. AY-YR2Eakrh49lmvlTDJ

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:193`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1538. AY-YR2Eakrh49lmvlTDK

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:194`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1539. AY-YR2Eakrh49lmvlTDL

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:194`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1540. AY-YR2Eakrh49lmvlTDM

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:195`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1541. AY-YR2Eakrh49lmvlTDN

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:195`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1542. AY-YR2Eakrh49lmvlTDO

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:196`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "o" which hides the field declared at line 155.

## 1543. AY-YR2Eakrh49lmvlTDP

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:196`
- **Effort**: 5min
- **Created**: 2015-08-31T12:16:58+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "x" which hides the field declared at line 155.

## 1544. AXzV0Q3rKyg4rf3RvPj8

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/CSVWire.java:299`
- **Effort**: 5min
- **Created**: 2015-08-27T11:46:04+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1545. AY_Fi7QYTbFMTgi4MX0x

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/Wire.java:25`
- **Effort**: 1min
- **Created**: 2015-08-27T11:46:04+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'java.io.IOException'.

## 1546. AXzV0Q_uKyg4rf3RvPp0

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:256`
- **Effort**: 10min
- **Created**: 2015-08-25T14:02:03+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 1547. AXzV0Q_uKyg4rf3RvPp5

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:257`
- **Effort**: 10min
- **Created**: 2015-08-25T14:02:03+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 1548. AXzV0Q_uKyg4rf3RvPp2

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:261`
- **Effort**: 10min
- **Created**: 2015-08-25T14:02:03+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 1549. AXzV0Q_uKyg4rf3RvPp3

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:262`
- **Effort**: 10min
- **Created**: 2015-08-25T14:02:03+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 1550. AXzV0Q_uKyg4rf3RvPp4

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:263`
- **Effort**: 10min
- **Created**: 2015-08-25T14:02:03+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 1551. AXzV0Q_uKyg4rf3RvPp1

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:264`
- **Effort**: 10min
- **Created**: 2015-08-25T14:02:03+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 1552. AXzV0Q_uKyg4rf3RvPpx

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1216`
- **Effort**: 20min
- **Created**: 2015-08-25T14:02:03+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 1553. AY-YR2BYkrh49lmvlTCH

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlSpecificationTest.java:145`
- **Effort**: 5min
- **Created**: 2015-08-25T14:02:03+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1554. AY-YR2BYkrh49lmvlTCI

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/YamlSpecificationTest.java:156`
- **Effort**: 5min
- **Created**: 2015-08-25T14:02:03+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1555. AXzV0Q_uKyg4rf3RvPpm

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:2915`
- **Effort**: 5min
- **Created**: 2015-08-25T10:00:03+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1556. AXzV0RAuKyg4rf3RvPql

- **Rule**: `java:S1186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/JSONWire.java:955`
- **Effort**: 5min
- **Created**: 2015-08-16T20:24:08+0000
- **Assignee**: Unassigned
- **Message**:
  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or
  complete the implementation.

## 1557. AY-YR1nkkrh49lmvlS8Z

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:163`
- **Effort**: 5min
- **Created**: 2015-08-15T12:13:40+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1558. AY-YR1nkkrh49lmvlS8e

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:215`
- **Effort**: 5min
- **Created**: 2015-08-15T12:13:40+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1559. AY-YR1nkkrh49lmvlS8m

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:276`
- **Effort**: 5min
- **Created**: 2015-08-15T12:13:40+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1560. AY-YR1nkkrh49lmvlS8s

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:350`
- **Effort**: 5min
- **Created**: 2015-08-15T12:13:40+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1561. AY-YR1nkkrh49lmvlS8w

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:419`
- **Effort**: 5min
- **Created**: 2015-08-15T12:13:40+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1562. AY-YR1nkkrh49lmvlS8y

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/ReadmeChapter1Test.java:469`
- **Effort**: 5min
- **Created**: 2015-08-15T12:13:40+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 1563. AY-YR1r7krh49lmvlS-f

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1689`
- **Effort**: 0min
- **Created**: 2015-08-05T13:25:36+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1564. AY-YR1r7krh49lmvlS-h

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1717`
- **Effort**: 0min
- **Created**: 2015-08-05T13:25:36+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1565. AXzV0QtnKyg4rf3RvPgy

- **Rule**: `java:S1607`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1735`
- **Effort**: 10min
- **Created**: 2015-08-05T13:25:36+0000
- **Assignee**: Unassigned
- **Message**:
  Either add an explanation about why this test is skipped or remove the "@Ignore" annotation.

## 1566. AY-YR1r7krh49lmvlS-j

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:1748`
- **Effort**: 5min
- **Created**: 2015-08-05T13:25:36+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1567. AXzV0Q2OKyg4rf3RvPiM

- **Rule**: `java:S3066`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/YamlLogging.java:61`
- **Effort**: 20min
- **Created**: 2015-07-23T14:10:32+0000
- **Assignee**: Unassigned
- **Message**:
  Lower the visibility of this setter or remove it altogether.

## 1568. AXzV0Q23Kyg4rf3RvPi0

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1439`
- **Effort**: 0min
- **Created**: 2015-07-14T08:03:41+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1569. AXzV0Q23Kyg4rf3RvPi1

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1446`
- **Effort**: 5min
- **Created**: 2015-07-14T08:03:41+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1570. AXzV0Q23Kyg4rf3RvPi5

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1479`
- **Effort**: 5min
- **Created**: 2015-07-14T08:03:41+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1571. AXzV0Q23Kyg4rf3RvPi-

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1803`
- **Effort**: 5min
- **Created**: 2015-07-08T16:14:53+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1572. AXzV0Q23Kyg4rf3RvPjQ

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:4680`
- **Effort**: 5min
- **Created**: 2015-07-07T18:48:58+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1573. AXzV0Q23Kyg4rf3RvPjS

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:4682`
- **Effort**: 5min
- **Created**: 2015-07-07T18:48:58+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1574. AXzV0Q23Kyg4rf3RvPjT

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:4909`
- **Effort**: 5min
- **Created**: 2015-07-07T18:48:58+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1575. AY-YR17_krh49lmvlTBc

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/UsingTestMarshallableTest.java:54`
- **Effort**: 5min
- **Created**: 2015-07-06T08:53:25+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1576. AY-YR17_krh49lmvlTBe

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/UsingTestMarshallableTest.java:67`
- **Effort**: 5min
- **Created**: 2015-07-06T08:53:25+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1577. AXzV0Q64Kyg4rf3RvPl4

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/WireCommon.java:65`
- **Effort**: 20min
- **Created**: 2015-06-30T19:16:56+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 1578. AXzV0Q_uKyg4rf3RvPpd

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:2318`
- **Effort**: 5min
- **Created**: 2015-06-21T21:41:36+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1579. AXzV0Q_uKyg4rf3RvPpg

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:2528`
- **Effort**: 5min
- **Created**: 2015-06-21T21:41:36+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1580. AZDL7ciu6N0FxHM9EiBg

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2590`
- **Effort**: 2min
- **Created**: 2015-06-21T21:41:36+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1581. AZDL7ciu6N0FxHM9EiBh

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2590`
- **Effort**: 2min
- **Created**: 2015-06-21T21:41:36+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1582. AZDL7ciu6N0FxHM9EiBi

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:2590`
- **Effort**: 2min
- **Created**: 2015-06-21T21:41:36+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1583. AXzV0Q_uKyg4rf3RvPp8

- **Rule**: `java:S1163`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:2330`
- **Effort**: 30min
- **Created**: 2015-06-21T11:25:59+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this code to not throw exceptions in finally blocks.

## 1584. AZDL7c9l6N0FxHM9EiJf

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RawWireTest.java:613`
- **Effort**: 2min
- **Created**: 2015-06-21T11:25:59+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1585. AZDL7c9l6N0FxHM9EiJg

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RawWireTest.java:613`
- **Effort**: 2min
- **Created**: 2015-06-21T11:25:59+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1586. AZDL7c9l6N0FxHM9EiJh

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RawWireTest.java:613`
- **Effort**: 2min
- **Created**: 2015-06-21T11:25:59+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1587. AXzV0Q_uKyg4rf3RvPpb

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1650`
- **Effort**: 0min
- **Created**: 2015-06-19T14:32:12+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1588. AXzV0Q_uKyg4rf3RvPpE

- **Rule**: `java:S119`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:205`
- **Effort**: 10min
- **Created**: 2015-06-15T13:26:55+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this generic name to match the regular expression '^[A-Z][0-9]?$'.

## 1589. AXzV0Q_uKyg4rf3RvPpz

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:212`
- **Effort**: 10min
- **Created**: 2015-06-15T13:26:55+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 1590. AY-YR2Eakrh49lmvlTCv

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:134`
- **Effort**: 5min
- **Created**: 2015-06-09T18:31:23+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused method parameter "t".

## 1591. AZDL7cxv6N0FxHM9EiIu

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/type/conversions/binary/ConventionsTest.java:113`
- **Effort**: 2min
- **Created**: 2015-05-22T17:30:59+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 1592. AZDL7cxv6N0FxHM9EiIt

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/type/conversions/binary/ConventionsTest.java:48`
- **Effort**: 2min
- **Created**: 2015-05-22T15:41:46+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 1593. AXzV0Q23Kyg4rf3RvPjN

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:4523`
- **Effort**: 5min
- **Created**: 2015-05-21T10:39:56+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1594. AY-YR1r7krh49lmvlS9_

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:912`
- **Effort**: 0min
- **Created**: 2015-05-15T05:52:04+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1595. AY-YR1r7krh49lmvlS9-

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextWireTest.java:913`
- **Effort**: 5min
- **Created**: 2015-05-15T05:52:04+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1596. AXzV0Q23Kyg4rf3RvPiy

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1410`
- **Effort**: 5min
- **Created**: 2015-04-17T18:49:33+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1597. AXzV0Q23Kyg4rf3RvPi6

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1574`
- **Effort**: 5min
- **Created**: 2015-04-17T18:49:33+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1598. AXzV0Q24Kyg4rf3RvPjh

- **Rule**: `java:S1301`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1574`
- **Effort**: 5min
- **Created**: 2015-04-17T18:49:33+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this "switch" statement by "if" statements to increase readability.

## 1599. AXzV0Q23Kyg4rf3RvPi_

- **Rule**: `java:S3358`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:2018`
- **Effort**: 5min
- **Created**: 2015-04-17T18:49:33+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested ternary operation into an independent statement.

## 1600. AXzV0Q23Kyg4rf3RvPjG

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:3551`
- **Effort**: 0min
- **Created**: 2015-04-17T18:49:33+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1601. AZDL7cy56N0FxHM9EiI0

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RFCExamplesTest.java:231`
- **Effort**: 2min
- **Created**: 2015-04-17T18:49:33+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1602. AZDL7cy56N0FxHM9EiI1

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RFCExamplesTest.java:231`
- **Effort**: 2min
- **Created**: 2015-04-17T18:49:33+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1603. AZDL7cy56N0FxHM9EiIw

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RFCExamplesTest.java:231`
- **Effort**: 2min
- **Created**: 2015-04-17T18:49:33+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1604. AZDL7cy56N0FxHM9EiIx

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RFCExamplesTest.java:231`
- **Effort**: 2min
- **Created**: 2015-04-17T18:49:33+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1605. AZDL7cy56N0FxHM9EiIy

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RFCExamplesTest.java:231`
- **Effort**: 2min
- **Created**: 2015-04-17T18:49:33+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1606. AZDL7cy56N0FxHM9EiIz

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RFCExamplesTest.java:231`
- **Effort**: 2min
- **Created**: 2015-04-17T18:49:33+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1607. AZDL7c7r6N0FxHM9EiJd

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextDocumentTest.java:90`
- **Effort**: 2min
- **Created**: 2015-04-13T20:24:05+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1608. AZDL7c7r6N0FxHM9EiJe

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextDocumentTest.java:91`
- **Effort**: 2min
- **Created**: 2015-04-13T20:24:05+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1609. AZDL7c7r6N0FxHM9EiJb

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextDocumentTest.java:88`
- **Effort**: 2min
- **Created**: 2015-04-02T08:56:41+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1610. AZDL7c7r6N0FxHM9EiJc

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/TextDocumentTest.java:89`
- **Effort**: 2min
- **Created**: 2015-04-02T08:56:41+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 1611. AY-YR14Okrh49lmvlTBB

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RFCExamplesTest.java:58`
- **Effort**: 5min
- **Created**: 2015-03-17T10:04:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1612. AY-YR14Okrh49lmvlTBA

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/RFCExamplesTest.java:36`
- **Effort**: 5min
- **Created**: 2015-03-17T04:07:40+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1613. AXzV0Q23Kyg4rf3RvPim

- **Rule**: `java:S1119`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:416`
- **Effort**: 30min
- **Created**: 2015-03-13T18:58:26+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code to remove this label and the need for it.

## 1614. AXzV0Q23Kyg4rf3RvPio

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:435`
- **Effort**: 5min
- **Created**: 2015-03-13T18:58:26+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1615. AXzV0Q_uKyg4rf3RvPpZ

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1617`
- **Effort**: 0min
- **Created**: 2015-03-04T21:38:12+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1616. AY-YR1yYkrh49lmvlTAO

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWireTest.java:1308`
- **Effort**: 5min
- **Created**: 2015-02-26T17:38:41+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 1617. AXzV0Q_uKyg4rf3RvPpY

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/TextWire.java:1583`
- **Effort**: 0min
- **Created**: 2015-02-26T11:29:56+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 1618. AXzV0Q23Kyg4rf3RvPin

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:417`
- **Effort**: 5min
- **Created**: 2015-02-04T12:17:02+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1619. AXzV0Q23Kyg4rf3RvPiw

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1374`
- **Effort**: 5min
- **Created**: 2015-01-19T17:29:24+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1620. AXzV0Q24Kyg4rf3RvPjg

- **Rule**: `java:S1301`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1374`
- **Effort**: 5min
- **Created**: 2015-01-19T17:29:24+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this "switch" statement by "if" statements to increase readability.

## 1621. AY-YR2Eakrh49lmvlTCl

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:83`
- **Effort**: 5min
- **Created**: 2015-01-19T17:29:24+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.io.StreamCorruptedException', as it cannot be
  thrown from method's body.

## 1622. AY-YR2Eakrh49lmvlTCo

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:105`
- **Effort**: 1min
- **Created**: 2015-01-19T17:29:24+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "start".

## 1623. AY-YR2Eakrh49lmvlTCq

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:105`
- **Effort**: 5min
- **Created**: 2015-01-19T17:29:24+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "start" local variable.

## 1624. AY-YR2Eakrh49lmvlTCu

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:145`
- **Effort**: 1min
- **Created**: 2015-01-19T17:29:24+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "rate".

## 1625. AY-YR2Eakrh49lmvlTCw

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/test/java/net/openhft/chronicle/wire/BinaryWirePerfTest.java:145`
- **Effort**: 5min
- **Created**: 2015-01-19T17:29:24+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "rate" local variable.

## 1626. AXzV0Q23Kyg4rf3RvPi7

- **Rule**: `java:S108`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1749`
- **Effort**: 5min
- **Created**: 2015-01-16T19:10:02+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this block of code, fill it in, or add a comment explaining why it is empty.

## 1627. AXzV0Q23Kyg4rf3RvPiv

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1372`
- **Effort**: 5min
- **Created**: 2015-01-16T15:57:43+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 1628. AXzV0Q23Kyg4rf3RvPi3

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Wire:src/main/java/net/openhft/chronicle/wire/BinaryWire.java:1523`
- **Effort**: 5min
- **Created**: 2015-01-16T14:54:54+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.
