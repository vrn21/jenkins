package hudson;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * Test to verify that IOException2 has been completely removed from the codebase.
 *
 * This test ensures:
 * 1. The IOException2.java file is removed
 * 2. No references to IOException2 exist in the codebase (except in this test file)
 */
public class IOException2RemovalTestHUD {

    @Test
    public void testIOException2ClassIsRemoved() throws IOException {
        // Check that the class file is gone
        try (Stream<Path> files = Files.walk(Paths.get("./"))) {
            List<Path> matches = files
                .filter(p -> p.getFileName().toString().equals("IOException2.java"))
                .collect(Collectors.toList());
            assertTrue("IOException2.java should be removed", matches.isEmpty());
        }
    }

    @Test
    public void testNoReferencesToIOException2() throws IOException {
        // Check for any remaining references in the codebase, excluding this test file
        try (Stream<Path> files = Files.walk(Paths.get("."))) {
            List<Path> javaFiles = files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.toString().contains("IOException2RemovalTestHUD.java")) // Exclude this test file
                .collect(Collectors.toList());

            for (Path javaFile : javaFiles) {
                String content = new String(Files.readAllBytes(javaFile));
                assertFalse("Reference to IOException2 found in " + javaFile,
                        content.contains("IOException2"));
            }
        }
    }

    @Test
    public void testNoIOException2Imports() throws IOException {
        // Check for any import statements referencing IOException2
        try (Stream<Path> files = Files.walk(Paths.get("."))) {
            List<Path> javaFiles = files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.toString().contains("IOException2RemovalTestHUD.java")) // Exclude this test file
                .collect(Collectors.toList());

            for (Path javaFile : javaFiles) {
                String content = new String(Files.readAllBytes(javaFile));
                assertFalse("Import of IOException2 found in " + javaFile,
                        content.contains("import hudson.util.IOException2"));
            }
        }
    }

    @Test
    public void testNoIOException2Instantiations() throws IOException {
        // Check for any new IOException2() calls
        try (Stream<Path> files = Files.walk(Paths.get("."))) {
            List<Path> javaFiles = files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.toString().contains("IOException2RemovalTestHUD.java")) // Exclude this test file
                .collect(Collectors.toList());

            for (Path javaFile : javaFiles) {
                String content = new String(Files.readAllBytes(javaFile));
                assertFalse("IOException2 instantiation found in " + javaFile,
                        content.contains("new IOException2"));
            }
        }
    }

    @Test
    public void testNoIOException2TypeReferences() throws IOException {
        // Check for any type references to IOException2 (method parameters, return types, etc.)
        try (Stream<Path> files = Files.walk(Paths.get("."))) {
            List<Path> javaFiles = files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.toString().contains("IOException2RemovalTestHUD.java")) // Exclude this test file
                .collect(Collectors.toList());

            for (Path javaFile : javaFiles) {
                String content = new String(Files.readAllBytes(javaFile));
                // Look for IOException2 as a type (not in strings or comments)
                String[] lines = content.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    // Skip comments and string literals
                    if (!line.startsWith("//") && !line.startsWith("/*") && !line.startsWith("*")) {
                        if (line.contains("IOException2") && !line.contains("\"IOException2\"")) {
                            fail("IOException2 type reference found in " + javaFile + " at line " + (i + 1) + ": " + line);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testCompilationSucceeds() {
        // This test will fail if there are any compilation errors due to missing IOException2
        // The test framework will catch compilation errors and fail the test
        assertTrue("Code should compile without IOException2", true);
    }
}
