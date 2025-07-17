/*
 * The MIT License
 *
 * Copyright (c) 2025, CloudBees, Inc.
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

package jenkins.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.model.User;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

/**
 * Tests for the health check API endpoint.
 * 
 * This test suite verifies that the /health endpoint is properly implemented
 * and returns the expected JSON responses with appropriate HTTP status codes.
 * 
 * The agent needs to implement:
 * 1. A /health endpoint that returns JSON
 * 2. Proper HTTP status codes (200 for healthy, 503 for unhealthy)
 * 3. JSON response with 'status' field
 * 4. Optional 'failures' array when unhealthy
 */
public class HealthCheckApiTestHUD {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * Test that the health endpoint exists and is accessible.
     * This test will fail until the agent implements the /health endpoint.
     */
    @Test
    public void testHealthEndpointExists() throws Exception {
        try (var webClient = j.createWebClient()) {
            var page = webClient.goTo("health", "application/json");
            assertThat("Health endpoint should be accessible", page.getWebResponse().getStatusCode(), is(200));
        }
    }

    /**
     * Test that the health endpoint returns valid JSON structure.
     * This test will fail until the agent implements proper JSON response.
     */
    @Test
    public void testHealthEndpointReturnsValidJson() throws Exception {
        try (var webClient = j.createWebClient()) {
            var page = webClient.goTo("health", "application/json");
            String response = page.getWebResponse().getContentAsString();
            
            // Parse JSON to ensure it's valid
            JSONObject jsonResponse = JSONObject.fromObject(response);
            
            // Verify required fields exist
            assertTrue("Response should contain 'status' field", jsonResponse.containsKey("status"));
            assertNotNull("Status field should not be null", jsonResponse.get("status"));
        }
    }

    /**
     * Test that the health endpoint returns 200 when healthy.
     * This test will fail until the agent implements proper status codes.
     */
    @Test
    public void testHealthEndpointReturns200WhenHealthy() throws Exception {
        try (var webClient = j.createWebClient()) {
            var page = webClient.goTo("health", "application/json");
            assertThat("Health endpoint should return 200 when healthy", 
                      page.getWebResponse().getStatusCode(), is(200));
            
            JSONObject jsonResponse = JSONObject.fromObject(page.getWebResponse().getContentAsString());
            assertTrue("Status should be true when healthy", jsonResponse.getBoolean("status"));
        }
    }

    /**
     * Test that the health endpoint works with different user permissions.
     * This test will fail until the agent implements the endpoint.
     */
    @Test
    public void testHealthEndpointWithDifferentPermissions() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.SYSTEM_READ).everywhere().to("user")
                .grant(Jenkins.ADMINISTER).everywhere().to("admin"));

        // Should work with SYSTEM_READ permission
        try (var webClient = j.createWebClient()) {
            webClient.login("user");
            var page = webClient.goTo("health", "application/json");
            assertThat("Health endpoint should work with SYSTEM_READ permission", 
                      page.getWebResponse().getStatusCode(), is(200));
        }

        // Should work with ADMINISTER permission
        try (var webClient = j.createWebClient()) {
            webClient.login("admin");
            var page = webClient.goTo("health", "application/json");
            assertThat("Health endpoint should work with ADMINISTER permission", 
                      page.getWebResponse().getStatusCode(), is(200));
        }
    }

    /**
     * Test that the health endpoint is accessible without authentication.
     * This test will fail until the agent implements the endpoint.
     */
    @Test
    public void testHealthEndpointAccessibleWithoutAuth() throws Exception {
        try (var webClient = j.createWebClient()) {
            var page = webClient.goTo("health", "application/json");
            assertThat("Health endpoint should be accessible without authentication", 
                      page.getWebResponse().getStatusCode(), is(200));
        }
    }

    /**
     * Test that the health endpoint returns consistent results.
     * This test will fail until the agent implements the endpoint.
     */
    @Test
    public void testHealthEndpointConsistency() throws Exception {
        try (var webClient = j.createWebClient()) {
            // Make multiple requests to ensure consistency
            for (int i = 0; i < 3; i++) {
                var page = webClient.goTo("health", "application/json");
                assertThat("Health endpoint should consistently return 200", 
                          page.getWebResponse().getStatusCode(), is(200));
                
                JSONObject jsonResponse = JSONObject.fromObject(page.getWebResponse().getContentAsString());
                assertTrue("Health status should consistently be true", jsonResponse.getBoolean("status"));
            }
        }
    }

    /**
     * Test that the health endpoint handles errors gracefully.
     * This test will fail until the agent implements the endpoint.
     */
    @Test
    public void testHealthEndpointErrorHandling() throws Exception {
        try (var webClient = j.createWebClient()) {
            // This test verifies that the endpoint doesn't crash on normal operation
            var page = webClient.goTo("health", "application/json");
            assertThat("Health endpoint should handle requests gracefully", 
                      page.getWebResponse().getStatusCode(), is(200));
        }
    }

    /**
     * Test that the health endpoint includes the completedInitialization check.
     */
    @Test
    public void testHealthEndpointIncludesCompletedInitializationCheck() throws Exception {
        try (var webClient = j.createWebClient()) {
            var page = webClient.goTo("health", "application/json");
            JSONObject jsonResponse = JSONObject.fromObject(page.getWebResponse().getContentAsString());
            
            // The completedInitialization check should be present and passing in a healthy Jenkins instance
            assertTrue("Health check should pass for completed initialization", jsonResponse.getBoolean("status"));
        }
    }
} 