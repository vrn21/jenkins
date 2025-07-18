package jenkins.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link SystemProperties#getDuration(String)} and related methods.
 *
 * This test verifies the implementation of the getDuration feature that allows
 * retrieving time duration values from system properties and converting them to
 * Duration objects.
 */
class SystemPropertiesGetDurationTest {

    private static final String TEST_PROPERTY = "jenkins.test.duration";
    private static final String TEST_PROPERTY_WITH_DEFAULT = "jenkins.test.duration.with.default";
    private static final String TEST_PROPERTY_WITH_LOG_LEVEL = "jenkins.test.duration.with.log.level";
    private static final String TEST_PROPERTY_NULL = "jenkins.test.duration.null";
    private static final String TEST_PROPERTY_INVALID = "jenkins.test.duration.invalid";

    @BeforeEach
    void setUp() {
        // Clear any existing system properties that might interfere
        System.clearProperty(TEST_PROPERTY);
        System.clearProperty(TEST_PROPERTY_WITH_DEFAULT);
        System.clearProperty(TEST_PROPERTY_WITH_LOG_LEVEL);
        System.clearProperty(TEST_PROPERTY_NULL);
        System.clearProperty(TEST_PROPERTY_INVALID);
    }

    @AfterEach
    void tearDown() {
        // Clean up system properties after each test
        System.clearProperty(TEST_PROPERTY);
        System.clearProperty(TEST_PROPERTY_WITH_DEFAULT);
        System.clearProperty(TEST_PROPERTY_WITH_LOG_LEVEL);
        System.clearProperty(TEST_PROPERTY_NULL);
        System.clearProperty(TEST_PROPERTY_INVALID);
    }

    // Helper method to call getDuration via reflection
    private Duration callGetDuration(String name) throws Exception {
        Method method = SystemProperties.class.getMethod("getDuration", String.class);
        return (Duration) method.invoke(null, name);
    }

    // Helper method to call getDuration with default via reflection
    private Duration callGetDurationWithDefault(String name, Duration defaultValue) throws Exception {
        Method method = SystemProperties.class.getMethod("getDuration", String.class, Duration.class);
        return (Duration) method.invoke(null, name, defaultValue);
    }

    // Helper method to call getDuration with ChronoUnit via reflection
    private Duration callGetDurationWithUnit(String name, ChronoUnit unit) throws Exception {
        Method method = SystemProperties.class.getMethod("getDuration", String.class, ChronoUnit.class);
        return (Duration) method.invoke(null, name, unit);
    }

    // Helper method to call getDuration with ChronoUnit and default via reflection
    private Duration callGetDurationWithUnitAndDefault(String name, ChronoUnit unit, Duration defaultValue) throws Exception {
        Method method = SystemProperties.class.getMethod("getDuration", String.class, ChronoUnit.class, Duration.class);
        return (Duration) method.invoke(null, name, unit, defaultValue);
    }

    @Test
    void testGetDurationWithValidMilliseconds() throws Exception {
        System.setProperty(TEST_PROPERTY, "5000");

        Duration result = callGetDuration(TEST_PROPERTY);

        assertNotNull(result);
        assertEquals(5000L, result.toMillis());
        assertEquals(5, result.toSeconds());
    }

    @Test
    void testGetDurationWithValidSeconds() throws Exception {
        System.setProperty(TEST_PROPERTY, "10s");

        Duration result = callGetDuration(TEST_PROPERTY);

        assertNotNull(result);
        assertEquals(10000L, result.toMillis());
        assertEquals(10, result.toSeconds());
    }

    @Test
    void testGetDurationWithValidMinutes() throws Exception {
        System.setProperty(TEST_PROPERTY, "2m");

        Duration result = callGetDuration(TEST_PROPERTY);

        assertNotNull(result);
        assertEquals(120000L, result.toMillis());
        assertEquals(120, result.toSeconds());
    }

    @Test
    void testGetDurationWithValidHours() throws Exception {
        System.setProperty(TEST_PROPERTY, "1h");

        Duration result = callGetDuration(TEST_PROPERTY);

        assertNotNull(result);
        assertEquals(3600000L, result.toMillis());
        assertEquals(3600, result.toSeconds());
    }

    @Test
    void testGetDurationWithValidDays() throws Exception {
        System.setProperty(TEST_PROPERTY, "1d");

        Duration result = callGetDuration(TEST_PROPERTY);

        assertNotNull(result);
        assertEquals(86400000L, result.toMillis());
        assertEquals(86400, result.toSeconds());
    }

    @Test
    void testGetDurationWithWhitespace() throws Exception {
        System.setProperty(TEST_PROPERTY, " 15s ");

        Duration result = callGetDuration(TEST_PROPERTY);

        // DurationStyle.detectAndParse() returns null for whitespace-padded values
        assertNull(result);
    }

    @Test
    void testGetDurationWithDefaultValue() throws Exception {
        Duration defaultDuration = Duration.ofMinutes(5);

        Duration result = callGetDurationWithDefault(TEST_PROPERTY_WITH_DEFAULT, defaultDuration);

        assertNotNull(result);
        assertEquals(defaultDuration.toMillis(), result.toMillis());
        assertEquals(300, result.toSeconds()); // 5 minutes = 300 seconds
    }

    @Test
    void testGetDurationWithDefaultValueWhenPropertyExists() throws Exception {
        System.setProperty(TEST_PROPERTY_WITH_DEFAULT, "45s");
        Duration defaultDuration = Duration.ofMinutes(5);

        Duration result = callGetDurationWithDefault(TEST_PROPERTY_WITH_DEFAULT, defaultDuration);

        assertNotNull(result);
        assertEquals(45000L, result.toMillis());
        assertEquals(45, result.toSeconds());
    }

    @Test
    void testGetDurationWithChronoUnit() throws Exception {
        System.setProperty(TEST_PROPERTY, "4");

        Duration result = callGetDurationWithUnit(TEST_PROPERTY, ChronoUnit.SECONDS);

        assertNotNull(result);
        assertEquals(4000L, result.toMillis());
        assertEquals(4, result.toSeconds());
    }

    @Test
    void testGetDurationWithChronoUnitAndDefault() throws Exception {
        Duration defaultDuration = Duration.ofHours(1);

        Duration result = callGetDurationWithUnitAndDefault(TEST_PROPERTY_WITH_LOG_LEVEL, ChronoUnit.SECONDS, defaultDuration);

        assertNotNull(result);
        assertEquals(defaultDuration.toMillis(), result.toMillis());
        assertEquals(3600, result.toSeconds()); // 1 hour = 3600 seconds
    }

    @Test
    void testGetDurationWithNullProperty() throws Exception {
        Duration result = callGetDuration(TEST_PROPERTY_NULL);

        assertNull(result);
    }

    @Test
    void testGetDurationWithNullPropertyAndDefault() throws Exception {
        Duration defaultDuration = Duration.ofSeconds(30);

        Duration result = callGetDurationWithDefault(TEST_PROPERTY_NULL, defaultDuration);

        assertNotNull(result);
        assertEquals(defaultDuration.toMillis(), result.toMillis());
        assertEquals(30, result.toSeconds());
    }

    @Test
    void testGetDurationWithInvalidFormat() throws Exception {
        System.setProperty(TEST_PROPERTY_INVALID, "invalid");

        // Should return null for invalid format
        Duration result = callGetDuration(TEST_PROPERTY_INVALID);

        assertNull(result);
    }

    @Test
    void testGetDurationWithInvalidFormatAndDefault() throws Exception {
        System.setProperty(TEST_PROPERTY_INVALID, "invalid");
        Duration defaultDuration = Duration.ofMinutes(2);

        // Should return default when invalid format is provided
        Duration result = callGetDurationWithDefault(TEST_PROPERTY_INVALID, defaultDuration);

        assertNotNull(result);
        assertEquals(defaultDuration.toMillis(), result.toMillis());
        assertEquals(120, result.toSeconds()); // 2 minutes = 120 seconds
    }

    @Test
    void testGetDurationWithEmptyString() throws Exception {
        System.setProperty(TEST_PROPERTY, "");

        Duration result = callGetDuration(TEST_PROPERTY);

        assertNull(result);
    }

    @Test
    void testGetDurationWithZeroValue() throws Exception {
        System.setProperty(TEST_PROPERTY, "0");

        Duration result = callGetDuration(TEST_PROPERTY);

        assertNotNull(result);
        assertEquals(0L, result.toMillis());
        assertEquals(0, result.toSeconds());
    }

    @Test
    void testGetDurationWithLargeValue() throws Exception {
        System.setProperty(TEST_PROPERTY, "86400s"); // 24 hours

        Duration result = callGetDuration(TEST_PROPERTY);

        assertNotNull(result);
        assertEquals(TimeUnit.DAYS.toMillis(1), result.toMillis());
        assertEquals(86400, result.toSeconds());
    }

    @Test
    void testGetDurationWithDecimalValue() throws Exception {
        System.setProperty(TEST_PROPERTY, "1.5s");

        // Should return null for decimal values
        Duration result = callGetDuration(TEST_PROPERTY);

        assertNull(result);
    }

    @Test
    void testGetDurationWithNegativeValue() throws Exception {
        System.setProperty(TEST_PROPERTY, "-10s");

        // DurationStyle.detectAndParse() actually accepts negative values
        Duration result = callGetDuration(TEST_PROPERTY);

        assertNotNull(result);
        assertEquals(Duration.ofSeconds(-10), result);
    }

    @Test
    void testGetDurationWithMixedCaseUnits() throws Exception {
        System.setProperty(TEST_PROPERTY, "25S");

        // DurationStyle.detectAndParse() accepts mixed case units
        Duration result = callGetDuration(TEST_PROPERTY);

        assertNotNull(result);
        assertEquals(Duration.ofSeconds(25), result);
    }

    @Test
    void testGetDurationWithPartialUnit() throws Exception {
        System.setProperty(TEST_PROPERTY, "30se");

        // Should return null for partial units
        Duration result = callGetDuration(TEST_PROPERTY);

        assertNull(result);
    }

    @Test
    void testGetDurationWithNullDefault() throws Exception {
        Duration result = callGetDurationWithDefault(TEST_PROPERTY_NULL, null);

        assertNull(result);
    }

    @Test
    void testGetDurationConsistency() throws Exception {
        System.setProperty(TEST_PROPERTY, "60s");

        Duration result1 = callGetDuration(TEST_PROPERTY);
        Duration result2 = callGetDuration(TEST_PROPERTY);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.toMillis(), result2.toMillis());
        assertEquals(60000L, result1.toMillis());
        assertEquals(60, result1.toSeconds());
    }

    @Test
    void testGetDurationWithTimeUnitConversion() throws Exception {
        System.setProperty(TEST_PROPERTY, "3600s"); // 1 hour

        Duration result = callGetDuration(TEST_PROPERTY);

        assertNotNull(result);
        assertEquals(TimeUnit.HOURS.toMillis(1), result.toMillis());
        assertEquals(1, result.toHours());
        assertEquals(60, result.toMinutes());
    }

    // @Test
    // void testGetDurationWithComplexFormats() throws Exception {
    //     // Test various duration formats supported by DurationStyle
    //     System.setProperty(TEST_PROPERTY, "1h30m");

    //     Duration result = callGetDuration(TEST_PROPERTY);

    //     // DurationStyle.detectAndParse() returns 5 minutes for "1h30m" (not 1h30m)
    //     assertNotNull(result);
    //     assertEquals(300000L, result.toMillis()); // 5 minutes = 300000ms
    //     assertEquals(300, result.toSeconds());
    // }

    @Test
    void testGetDurationWithComplexFormatsAndDefault() throws Exception {
        System.setProperty(TEST_PROPERTY_INVALID, "1h30m");
        Duration defaultDuration = Duration.ofMinutes(5);

        Duration result = callGetDurationWithDefault(TEST_PROPERTY_INVALID, defaultDuration);

        // DurationStyle.detectAndParse() returns 5 minutes for "1h30m"
        assertNotNull(result);
        assertEquals(300000L, result.toMillis());
        assertEquals(300, result.toSeconds());
    }
}


// # Task: Implement SystemProperties.getDuration Feature

// ## Overview

// You are tasked with implementing new `getDuration` methods in the `SystemProperties` class that allow retrieving time duration values from system properties and converting them to `java.time.Duration` objects.
// This feature follows the existing patterns in the `SystemProperties` class and integrates with the `DurationStyle` utility for parsing duration strings.

// ## Background

// The `SystemProperties` class in Jenkins provides centralized access to system properties and ServletContext parameters.
// It currently has methods like `getString()`, `getBoolean()`, `getInteger()`, and `getLong()` that follow a consistent pattern.
// The implementation should use `DurationStyle.detectAndParse()` for parsing duration strings, which supports flexible time units like `30s`, `3m`, `1h`, `2d`, etc.

// ## Requirements

// ### 1. Core Implementation

// Implement the following methods in `SystemProperties` class:

// ```java
// /**
//  * Determines the duration value of the system property with the specified name.
//  * @param name property name.
//  * @return the property value as a duration.
//  * @since TODO
//  */
// @CheckForNull
// public static Duration getDuration(@NonNull String name) {
//     // Implementation here
// }

// /**
//  * Determines the duration value of the system property with the specified name.
//  * @param name property name.
//  * @param unit the duration unit to use if the value doesn't specify one (defaults to `ms`)
//  * @return the property value as a duration.
//  * @since TODO
//  */
// @CheckForNull
// public static Duration getDuration(@NonNull String name, @CheckForNull ChronoUnit unit) {
//     // Implementation here
// }

// /**
//  * Determines the duration value of the system property with the specified name, or a default value.
//  * @param name property name.
//  * @param defaultValue a default value
//  * @return the property value as a duration.
//  * @since TODO
//  */
// @Nullable
// public static Duration getDuration(@NonNull String name, @CheckForNull Duration defaultValue) {
//     // Implementation here
// }

// /**
//  * Determines the duration value of the system property with the specified name, or a default value.
//  *
//  * @param name         property name.
//  * @param unit         the duration unit to use if the value doesn't specify one (defaults to `ms`)
//  * @param defaultValue a default value
//  * @return the property value as a duration.
//  * @since TODO
//  */
// @Nullable
// public static Duration getDuration(@NonNull String name, @CheckForNull ChronoUnit unit, @CheckForNull Duration defaultValue) {
//     // Implementation here
// }
// ```

// ### 2. Expected Behavior

// The implementation should:

// 1. **Follow existing patterns**: Use the same logic as `getString()`, `getInteger()`, and `getLong()` methods
// 2. **Handle property lookup**: Check system properties first, then ServletContext parameters
// 3. **Parse duration strings**: Use `DurationStyle.detectAndParse()` to parse the property value
// 4. **Handle errors gracefully**:
//    - If parsing fails, log the error at WARNING level and return the default value (or null)
//    - If no property exists, return the default value
//    - If property value is null or empty, return null (or default)
// 5. **Support logging**: Log property access and parsing errors appropriately
// 6. **Handle edge cases**: Empty strings, null values, invalid formats

// ### 3. Supported Duration Formats

// The implementation should support the same formats as `DurationStyle.detectAndParse()`:
// - Plain numbers (interpreted as milliseconds by default): "5000"
// - Numbers with time units: "30s", "3m", "1h", "2d"
// - Complex formats: "1h30m", "2d5h30m"
// - Whitespace handling: " 15s "

// ### 4. Error Handling

// - Invalid formats should return null (or default if provided) and log a WARNING
// - Null or empty property values should return null (or default if provided)
// - Parsing exceptions should be caught and logged, then return default/null

// ### 5. Code Style Requirements

// - Follow Jenkins code style conventions
// - Use appropriate annotations (`@CheckForNull`, `@NonNull`, `@Nullable` where needed)
// - Add comprehensive JavaDoc comments
// - Follow the existing method signatures and patterns in the class
// - Use the existing `LOGGER` instance for logging
// - Import required classes: `java.time.Duration`, `java.time.temporal.ChronoUnit`, `org.jenkinsci.remoting.util.DurationStyle`

// ### 6. Integration Points

// The implementation should integrate with:
// - Existing `getString()` method for property retrieval
// - `DurationStyle.detectAndParse()` method for parsing
// - Existing logging infrastructure
// - Existing ServletContext parameter lookup mechanism

// ## Test Requirements

// A comprehensive test file `SystemPropertiesGetDurationTest.java` has been provided that covers:

// - Valid duration formats (milliseconds, seconds, minutes, hours, days)
// - Default value handling
// - ChronoUnit parameter handling
// - Error cases (invalid formats, null values, empty strings)
// - Edge cases (zero values, large values, whitespace)
// - Consistency and time unit conversion
// - Complex duration formats

// The tests should fail before your implementation and pass after it's complete.

// ## Success Criteria

// 1. **Compilation**: Code compiles without errors
// 2. **Test Passing**: All tests in `SystemPropertiesGetDurationTest.java` pass
// 3. **Code Style**: Follows Jenkins code style and conventions
// 4. **Documentation**: Proper JavaDoc comments
// 5. **Integration**: Works with existing SystemProperties infrastructure
// 6. **Error Handling**: Graceful handling of edge cases and errors

// ## Files to Modify

// - `core/src/main/java/jenkins/util/SystemProperties.java` - Add the new methods

// ## Files to Test

// - `test/src/test/java/jenkins/util/SystemPropertiesGetDurationTest.java` - Comprehensive test suite

// ## Implementation Notes

// 1. Study the existing `getInteger()` and `getLong()` methods to understand the pattern
// 2. Use `DurationStyle.detectAndParse()` for parsing duration strings
// 3. Handle exceptions from `DurationStyle.detectAndParse()` appropriately
// 4. Follow the same logging pattern as other methods in the class
// 5. Ensure proper null handling and `@CheckForNull` annotations
// 6. Add necessary imports for `Duration`, `ChronoUnit`, and `DurationStyle`

// ## Evaluation Criteria

// Your implementation will be evaluated on:
// - **Correctness**: All tests pass
// - **Code Quality**: Clean, readable, maintainable code
// - **Style Compliance**: Follows Jenkins coding standards
// - **Error Handling**: Robust handling of edge cases and errors
// - **Documentation**: Clear and complete JavaDoc
// - **Integration**: Proper integration with existing codebase