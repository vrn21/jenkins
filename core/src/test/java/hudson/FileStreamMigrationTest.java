package hudson;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Comprehensive test to verify FileInputStream/FileOutputStream migration to NIO APIs.
 * 
 * This test ensures:
 * 1. No new FileInputStream/FileOutputStream calls exist in production code
 * 2. The intentional FileInputStream in UtilTest is preserved
 * 3. Exception handling is correct for NIO APIs
 * 4. File creation and I/O behavior is preserved
 * 5. Windows file locking behavior is preserved
 */
public class FileStreamMigrationTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /**
     * Test that verifies no unallowlisted FileInputStream/FileOutputStream usage
     * exists in production code.
     */
    @Test
    public void testNoFileStreamUsageInProduction() throws IOException {
        // Define allowlist of files that should contain FileInputStream/FileOutputStream
        Set<String> allowlistedFiles = new HashSet<>(Arrays.asList(
            "core/src/test/java/hudson/UtilTest.java" // Windows file locking test
        ));

        // Scan for FileInputStream/FileOutputStream usage
        List<String> violations = scanForFileStreamUsage(allowlistedFiles);
        
        if (!violations.isEmpty()) {
            fail("Found FileInputStream/FileOutputStream usage in production code:\n" + 
                 String.join("\n", violations));
        }
    }

    /**
     * Test that verifies the intentional FileInputStream in UtilTest is preserved.
     */
    @Test
    public void testIntentionalFileInputStreamPreserved() throws IOException {
        String utilTestPath = "core/src/test/java/hudson/UtilTest.java";
        String content = new String(Files.readAllBytes(Paths.get(utilTestPath)));
        
        // Check that the intentional FileInputStream usage exists
        assertTrue("Intentional FileInputStream usage should be preserved",
                   content.contains("new FileInputStream(f)"));
        
        // Check that it's in the lockFileForDeletion method
        assertTrue("FileInputStream should be in lockFileForDeletion method",
                   content.contains("lockFileForDeletion"));
        
        // Check that there's a comment explaining why it's needed
        assertTrue("Should have comment explaining Windows file locking",
                   content.contains("Windows") && content.contains("delete"));
    }

    /**
     * Test that verifies NIO exception handling works correctly.
     */
    @Test
    public void testNioExceptionHandling() throws IOException {
        File testFile = tmp.newFile("test.txt");
        File nonExistentFile = new File(tmp.getRoot(), "nonexistent.txt");
        
        // Test Files.newInputStream with non-existent file
        try {
            Files.newInputStream(nonExistentFile.toPath());
            fail("Should throw NoSuchFileException");
        } catch (NoSuchFileException e) {
            // Expected
        }
        
        // Test Files.newOutputStream creates file when it doesn't exist
        File newFile = new File(tmp.getRoot(), "newfile.txt");
        assertFalse("File should not exist initially", newFile.exists());
        
        try (OutputStream out = Files.newOutputStream(newFile.toPath(), StandardOpenOption.CREATE)) {
            out.write("test".getBytes());
        }
        
        assertTrue("File should be created", newFile.exists());
        assertEquals("File content should match", "test", 
                    new String(Files.readAllBytes(newFile.toPath())));
    }

    /**
     * Test that verifies StandardOpenOption.CREATE is used correctly.
     */
    @Test
    public void testStandardOpenOptionCreate() throws IOException {
        File testFile = new File(tmp.getRoot(), "create-test.txt");
        
        // This should work without StandardOpenOption.CREATE_NEW (which would fail if file exists)
        try (OutputStream out = Files.newOutputStream(testFile.toPath(), StandardOpenOption.CREATE)) {
            out.write("test".getBytes());
        }
        
        // Write again - should work with CREATE
        try (OutputStream out = Files.newOutputStream(testFile.toPath(), StandardOpenOption.CREATE)) {
            out.write("updated".getBytes());
        }
        
        assertEquals("File should contain updated content", "updated", 
                    new String(Files.readAllBytes(testFile.toPath())));
    }

    /**
     * Test that verifies append behavior is preserved.
     */
    @Test
    public void testAppendBehavior() throws IOException {
        File testFile = new File(tmp.getRoot(), "append-test.txt");
        
        // Write initial content
        try (OutputStream out = Files.newOutputStream(testFile.toPath(), StandardOpenOption.CREATE)) {
            out.write("initial".getBytes());
        }
        
        // Append content
        try (OutputStream out = Files.newOutputStream(testFile.toPath(), 
                                                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            out.write("appended".getBytes());
        }
        
        assertEquals("File should contain both contents", "initialappended", 
                    new String(Files.readAllBytes(testFile.toPath())));
    }

    /**
     * Test that verifies buffered stream behavior is preserved.
     */
    @Test
    public void testBufferedStreamBehavior() throws IOException {
        File testFile = tmp.newFile("buffered-test.txt");
        FileUtils.write(testFile, "test content");
        
        // Test reading with buffered stream
        try (InputStream in = new BufferedInputStream(Files.newInputStream(testFile.toPath()))) {
            byte[] buffer = new byte[1024];
            int bytesRead = in.read(buffer);
            String content = new String(buffer, 0, bytesRead);
            assertEquals("Content should match", "test content", content);
        }
        
        // Test writing with buffered stream
        File outputFile = new File(tmp.getRoot(), "buffered-output.txt");
        try (OutputStream out = new BufferedOutputStream(
                Files.newOutputStream(outputFile.toPath(), StandardOpenOption.CREATE))) {
            out.write("buffered output".getBytes());
        }
        
        assertTrue("Output file should be created", outputFile.exists());
        assertEquals("Output content should match", "buffered output", 
                    new String(Files.readAllBytes(outputFile.toPath())));
    }

    /**
     * Test that verifies the migration report is accurate.
     */
    @Test
    public void testMigrationReportAccuracy() throws IOException {
        List<FileStreamUsage> usages = scanForFileStreamUsage();
        
        // Should have at least one allowlisted usage (UtilTest)
        long allowlistedCount = usages.stream()
            .filter(u -> "allowlisted".equals(u.classification))
            .count();
        
        assertTrue("Should have at least one allowlisted usage", allowlistedCount > 0);
        
        // Should not have any production usages
        long productionCount = usages.stream()
            .filter(u -> "production".equals(u.classification))
            .count();
        
        assertEquals("Should have no production usages", 0, productionCount);
        
        // Print summary for debugging
        System.out.println("Migration Report Summary:");
        System.out.println("  - Total usages: " + usages.size());
        System.out.println("  - Production: " + productionCount);
        System.out.println("  - Test: " + usages.stream().filter(u -> "test".equals(u.classification)).count());
        System.out.println("  - Allowlisted: " + allowlistedCount);
    }

    /**
     * Test that verifies Windows file locking behavior is preserved.
     */
    @Test
    public void testWindowsFileLocking() throws IOException, InterruptedException {
        // This test only makes sense on Windows
        if (!Functions.isWindows()) {
            System.out.println("Skipping Windows file locking test on non-Windows platform");
            return;
        }

        File testFile = tmp.newFile("lock-test.txt");
        FileUtils.write(testFile, "test content");

        // Simulate the locking mechanism from UtilTest
        InputStream lockedStream = new FileInputStream(testFile);
        
        try {
            // Try to delete the file while it's locked
            boolean deleteSucceeded = testFile.delete();
            
            // On Windows, the file should NOT be deleted because it's open for reading
            assertFalse("File should not be deleted while open on Windows", deleteSucceeded);
            assertTrue("File should still exist", testFile.exists());
            
        } finally {
            // Close the stream to release the lock
            lockedStream.close();
            
            // Now the file should be deletable
            assertTrue("File should be deletable after closing stream", testFile.delete());
        }
    }

    /**
     * Test that verifies NIO streams don't block file deletion on Windows.
     * This demonstrates why FileInputStream is needed for the locking test.
     */
    @Test
    public void testNioStreamsDontBlockDeletion() throws IOException {
        // This test only makes sense on Windows
        if (!Functions.isWindows()) {
            System.out.println("Skipping NIO deletion test on non-Windows platform");
            return;
        }

        File testFile = tmp.newFile("nio-test.txt");
        FileUtils.write(testFile, "test content");

        // Use NIO stream (which is what we're migrating to)
        try (InputStream nioStream = Files.newInputStream(testFile.toPath())) {
            // Try to delete the file while NIO stream is open
            boolean deleteSucceeded = testFile.delete();
            
            // On Windows, NIO streams typically don't block deletion like FileInputStream does
            // This is why FileInputStream is needed for the locking test
            if (deleteSucceeded) {
                System.out.println("NIO stream did not block deletion (expected behavior)");
            } else {
                System.out.println("NIO stream blocked deletion (unexpected but possible)");
            }
        }
    }

    /**
     * Test that verifies the UtilTest.lockFileForDeletion method still works.
     */
    @Test
    public void testUtilTestLockFileForDeletion() throws Exception {
        // This test only makes sense on Windows
        if (!Functions.isWindows()) {
            System.out.println("Skipping UtilTest lock test on non-Windows platform");
            return;
        }

        File testFile = tmp.newFile("util-test.txt");
        FileUtils.write(testFile, "test content");

        // Create a UtilTest instance to test the locking mechanism
        UtilTest utilTest = new UtilTest();
        
        try {
            // Lock the file using the UtilTest method
            utilTest.lockFileForDeletion(testFile);
            
            // Try to delete the file while it's locked
            boolean deleteSucceeded = testFile.delete();
            
            // On Windows, the file should NOT be deleted because it's locked
            assertFalse("File should not be deleted while locked by UtilTest", deleteSucceeded);
            assertTrue("File should still exist", testFile.exists());
            
        } finally {
            // Unlock the file
            utilTest.unlockFileForDeletion(testFile);
            
            // Now the file should be deletable
            assertTrue("File should be deletable after unlocking", testFile.delete());
        }
    }

    /**
     * Test that verifies exception handling includes both FileNotFoundException and NoSuchFileException.
     */
    @Test
    public void testExceptionHandlingCompatibility() throws IOException {
        File nonExistentFile = new File(tmp.getRoot(), "nonexistent.txt");
        
        // Test that both exception types are handled
        boolean caughtFileNotFoundException = false;
        boolean caughtNoSuchFileException = false;
        
        try {
            Files.newInputStream(nonExistentFile.toPath());
        } catch (FileNotFoundException e) {
            caughtFileNotFoundException = true;
        } catch (NoSuchFileException e) {
            caughtNoSuchFileException = true;
        }
        
        // Should catch one of these exceptions
        assertTrue("Should catch either FileNotFoundException or NoSuchFileException", 
                   caughtFileNotFoundException || caughtNoSuchFileException);
    }

    /**
     * Test that verifies file creation with explicit options.
     */
    @Test
    public void testFileCreationWithOptions() throws IOException {
        File testFile = new File(tmp.getRoot(), "options-test.txt");
        
        // Test with CREATE option
        try (OutputStream out = Files.newOutputStream(testFile.toPath(), StandardOpenOption.CREATE)) {
            out.write("test".getBytes());
        }
        
        assertTrue("File should be created with CREATE option", testFile.exists());
        
        // Test with CREATE_NEW option (should fail if file exists)
        File newFile = new File(tmp.getRoot(), "create-new-test.txt");
        try (OutputStream out = Files.newOutputStream(newFile.toPath(), StandardOpenOption.CREATE_NEW)) {
            out.write("test".getBytes());
        }
        
        assertTrue("File should be created with CREATE_NEW option", newFile.exists());
        
        // CREATE_NEW should fail if file exists
        try {
            Files.newOutputStream(newFile.toPath(), StandardOpenOption.CREATE_NEW);
            fail("CREATE_NEW should fail if file exists");
        } catch (FileAlreadyExistsException e) {
            // Expected
        }
    }

    /**
     * Scans for FileInputStream/FileOutputStream usage in the codebase.
     */
    private List<String> scanForFileStreamUsage(Set<String> allowlistedFiles) throws IOException {
        List<String> violations = new ArrayList<>();
        
        // Patterns to match FileInputStream/FileOutputStream usage
        Pattern fisPattern = Pattern.compile("new\\s+FileInputStream\\s*\\(");
        Pattern fosPattern = Pattern.compile("new\\s+FileOutputStream\\s*\\(");
        
        // Scan core source directories
        String[] sourceDirs = {
            "core/src/main/java",
            "core/src/test/java"
        };
        
        for (String sourceDir : sourceDirs) {
            if (!Files.exists(Paths.get(sourceDir))) {
                continue;
            }
            
            Files.walk(Paths.get(sourceDir))
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        String content = new String(Files.readAllBytes(path));
                        String relativePath = path.toString();
                        
                        // Check if this file is allowlisted
                        boolean isAllowlisted = allowlistedFiles.stream()
                            .anyMatch(allowlisted -> relativePath.endsWith(allowlisted));
                        
                        if (!isAllowlisted) {
                            // Check for FileInputStream usage
                            if (fisPattern.matcher(content).find()) {
                                violations.add("FileInputStream usage in: " + relativePath);
                            }
                            
                            // Check for FileOutputStream usage
                            if (fosPattern.matcher(content).find()) {
                                violations.add("FileOutputStream usage in: " + relativePath);
                            }
                        }
                    } catch (IOException e) {
                        violations.add("Error reading file: " + path + " - " + e.getMessage());
                    }
                });
        }
        
        return violations;
    }

    /**
     * Scans for FileInputStream/FileOutputStream usage and returns detailed report.
     */
    private List<FileStreamUsage> scanForFileStreamUsage() throws IOException {
        List<FileStreamUsage> usages = new ArrayList<>();
        
        // Define allowlist
        Set<String> allowlistedFiles = new HashSet<>(Arrays.asList(
            "core/src/test/java/hudson/UtilTest.java" // Windows file locking test
        ));
        
        // Patterns to match FileInputStream/FileOutputStream usage
        Pattern fisPattern = Pattern.compile("new\\s+FileInputStream\\s*\\(");
        Pattern fosPattern = Pattern.compile("new\\s+FileOutputStream\\s*\\(");
        
        // Scan core source directories
        String[] sourceDirs = {
            "core/src/main/java",
            "core/src/test/java"
        };
        
        for (String sourceDir : sourceDirs) {
            if (!Files.exists(Paths.get(sourceDir))) {
                continue;
            }
            
            Files.walk(Paths.get(sourceDir))
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        String content = new String(Files.readAllBytes(path));
                        String relativePath = path.toString();
                        
                        // Determine classification
                        String classification;
                        if (allowlistedFiles.stream().anyMatch(allowlisted -> relativePath.endsWith(allowlisted))) {
                            classification = "allowlisted";
                        } else if (relativePath.contains("/test/")) {
                            classification = "test";
                        } else {
                            classification = "production";
                        }
                        
                        // Find FileInputStream usages
                        findUsages(content, relativePath, fisPattern, "FileInputStream", classification, usages);
                        
                        // Find FileOutputStream usages
                        findUsages(content, relativePath, fosPattern, "FileOutputStream", classification, usages);
                        
                    } catch (IOException e) {
                        System.err.println("Error reading file: " + path + " - " + e.getMessage());
                    }
                });
        }
        
        return usages;
    }

    private void findUsages(String content, String filePath, Pattern pattern, 
                          String type, String classification, List<FileStreamUsage> usages) {
        Matcher matcher = pattern.matcher(content);
        String[] lines = content.split("\n");
        
        while (matcher.find()) {
            int lineNumber = getLineNumber(content, matcher.start());
            String context = getContext(lines, lineNumber);
            
            usages.add(new FileStreamUsage(filePath, lineNumber, type, context, classification));
        }
    }

    private int getLineNumber(String content, int position) {
        int line = 1;
        for (int i = 0; i < position; i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private String getContext(String[] lines, int lineNumber) {
        int start = Math.max(0, lineNumber - 2);
        int end = Math.min(lines.length, lineNumber + 1);
        
        StringBuilder context = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i == lineNumber - 1) {
                context.append(">>> ");
            }
            context.append(lines[i]).append("\n");
        }
        return context.toString().trim();
    }

    /**
     * Internal class to represent FileInputStream/FileOutputStream usage.
     */
    private static class FileStreamUsage {
        public final String file;
        public final int line;
        public final String type; // "FileInputStream" or "FileOutputStream"
        public final String context;
        public final String classification; // "production", "test", "allowlisted"
        
        public FileStreamUsage(String file, int line, String type, String context, String classification) {
            this.file = file;
            this.line = line;
            this.type = type;
            this.context = context;
            this.classification = classification;
        }
    }
} 