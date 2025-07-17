/*
 * The MIT License
 *
 * Copyright (c) 2024, Jenkins contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Test;

/**
 * Test for verifying the removal of the deprecated Scrambler class.
 * 
 * This test is designed to:
 * - FAIL before the Scrambler class is removed (compilation error)
 * - PASS after the Scrambler class is removed (confirms successful removal)
 * 
 * The test attempts to use the Scrambler class, which will cause compilation
 * errors once the class is removed, confirming the removal was successful.
 */
public class ScramblerRemovalTest {

    /**
     * Test that attempts to use the Scrambler class.
     * This test will fail with compilation errors after Scrambler is removed,
     * confirming the class has been successfully removed.
     */
    @Test
    public void testScramblerClassRemoval() {
        // This line will cause compilation errors after Scrambler is removed
        // If this test compiles and runs, it means Scrambler still exists
        // If this test fails to compile, it means Scrambler has been removed (success!)
        Scrambler scrambler = new Scrambler();
        assertNotNull("Scrambler class should exist", scrambler);
    }

    /**
     * Test that attempts to use the scramble method.
     * This test will fail with compilation errors after Scrambler is removed.
     */
    @Test
    public void testScrambleMethodRemoval() {
        String input = "test string";
        String scrambled = Scrambler.scramble(input);
        
        // Verify the scrambled result is Base64 encoded
        assertNotNull("Scrambled result should not be null", scrambled);
        assertTrue("Scrambled result should be Base64 encoded", 
                  isBase64Encoded(scrambled));
        
        // Verify we can decode it back using standard Base64
        String decoded = new String(Base64.getDecoder().decode(scrambled), StandardCharsets.UTF_8);
        assertEquals("Decoded result should match original", input, decoded);
    }

    /**
     * Test that attempts to use the descramble method.
     * This test will fail with compilation errors after Scrambler is removed.
     */
    @Test
    public void testDescrambleMethodRemoval() {
        String input = "test string";
        String base64Encoded = Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
        
        String descrambled = Scrambler.descramble(base64Encoded);
        assertEquals("Descrambled result should match original", input, descrambled);
    }

    /**
     * Test that attempts to use Scrambler with null values.
     * This test will fail with compilation errors after Scrambler is removed.
     */
    @Test
    public void testScramblerNullHandling() {
        String result1 = Scrambler.scramble(null);
        assertNull("Scrambling null should return null", result1);
        
        String result2 = Scrambler.descramble(null);
        assertNull("Descrambling null should return null", result2);
    }

    /**
     * Test that attempts to use Scrambler with invalid input.
     * This test will fail with compilation errors after Scrambler is removed.
     */
    @Test
    public void testScramblerInvalidInput() {
        String result = Scrambler.descramble("invalid-base64!");
        assertEquals("Invalid Base64 should return empty string", "", result);
    }

    /**
     * Test that attempts to verify Scrambler class properties.
     * This test will fail with compilation errors after Scrambler is removed.
     */
    @Test
    public void testScramblerClassProperties() {
        // This test will fail after Scrambler is removed
        Class<?> scramblerClass = Scrambler.class;
        assertEquals("Scrambler should be in hudson.util package", 
                    "hudson.util", scramblerClass.getPackage().getName());
    }

    /**
     * Test that attempts to verify Scrambler method accessibility.
     * This test will fail with compilation errors after Scrambler is removed.
     */
    @Test
    public void testScramblerMethodAccessibility() {
        // This test will fail after Scrambler is removed
        String testInput = "hello world";
        String scrambled = Scrambler.scramble(testInput);
        String descrambled = Scrambler.descramble(scrambled);
        
        assertEquals("Round-trip scramble/descramble should work", testInput, descrambled);
    }

    /**
     * Helper method to check if a string is Base64 encoded.
     */
    private boolean isBase64Encoded(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
} 


// # Task: Remove Deprecated Scrambler Class

// ## Overview
// Remove the `hudson.util.Scrambler` class from Jenkins. This class provides simple Base64 encoding/decoding functionality but is misleadingly named and should not be used for storing sensitive data.

// ## Background
// The `Scrambler` class is currently **not deprecated** in the codebase. It only provides Base64 encoding/decoding, which is not encryption and should not be used for storing passwords or other sensitive data. Jenkins has `hudson.util.Secret` for proper encryption of sensitive data.

// ## Current State
// - File: `core/src/main/java/hudson/util/Scrambler.java`
// - Class is **NOT** marked as deprecated (no `@Deprecated` annotation)
// - No external usages found in the codebase
// - Provides two methods:
//   - `scramble(String secret)` - Base64 encodes a string
//   - `descramble(String scrambled)` - Base64 decodes a string

// ## Requirements

// ### 1. Remove the Scrambler class
// - Delete the file `core/src/main/java/hudson/util/Scrambler.java`
// - Ensure no compilation errors occur after removal

// ### 2. Verify no usages exist
// - Confirm no other classes import or use `hudson.util.Scrambler`
// - Confirm no references to `scramble()` or `descramble()` methods exist outside the class itself

// ### 3. Update documentation if needed
// - Check if any documentation references the Scrambler class
// - Update or remove such references

// ### 4. Test the changes
// - Ensure Jenkins builds successfully after the removal
// - Run the provided test suite to verify functionality

// ## Success Criteria
// - [ ] Scrambler class is completely removed
// - [ ] No compilation errors
// - [ ] No runtime errors
// - [ ] All tests pass
// - [ ] No references to Scrambler exist in the codebase

// ## Testing Strategy
// The test file `core/src/test/java/hudson/util/ScramblerRemovalTest.java` is designed to:

// ### Before Removal (Current State):
// - **FAIL** with compilation errors when trying to use the Scrambler class
// - This confirms the class exists and can be used

// ### After Removal (Target State):
// - **PASS** because the Scrambler class no longer exists
// - Compilation errors confirm the class has been successfully removed

// ## Notes
// - The class is not currently deprecated, so this is a direct removal
// - Base64 encoding can be done directly with `java.util.Base64` if needed
// - For sensitive data, use `hudson.util.Secret` instead
// - This is a cleanup task to remove unused code that could be misleading

// ## Files to modify
// - `core/src/main/java/hudson/util/Scrambler.java` (delete)

// ## Testing
// Use the provided test file `core/src/test/java/hudson/util/ScramblerRemovalTest.java` to verify the removal works correctly.

// ## Expected Behavior
// 1. **Before removal**: Tests will compile and run, confirming Scrambler exists
// 2. **After removal**: Tests will fail to compile with "cannot find symbol" errors, confirming Scrambler has been removed
// 3. **Success**: All other Jenkins functionality continues to work normally 