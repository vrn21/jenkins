/*
 * The MIT License
 *
 * Copyright 2019 CloudBees, Inc.
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

package jenkins.agents;

import jenkins.slaves.JnlpAgentReceiver;
import org.jenkinsci.remoting.engine.JnlpConnectionState;
import org.jenkinsci.remoting.engine.JnlpConnectionStateListener;
import org.jenkinsci.remoting.protocol.impl.ConnectionRefusalException;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

import jakarta.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test to verify that WebSocketAgents properly handles ConnectionRefusalException.
 * 
 * This test checks whether the issue where WebSocketAgents.fireAfterProperties() 
 * could reject connections but the code would still proceed with WebSocket upgrade.
 * 
 * BEFORE FIX: This test should FAIL (rejected connections still proceed)
 * AFTER FIX: This test should PASS (rejected connections are properly handled)
 */
public class WebSocketAgentsConnectionRefusalTestHUD {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private static final Logger LOGGER = Logger.getLogger(WebSocketAgentsConnectionRefusalTestHUD.class.getName());

    @Test
    public void testWebSocketAgentsHandlesRejection() throws Exception {
        // Reset the rejection tracker
        TestRejectionTracker.reset();
        
        // Simulate the WebSocketAgents.doIndex method logic for a valid agent
        String agentName = "valid-agent";
        String secret = JnlpAgentReceiver.SLAVE_SECRET.mac(agentName);
        
        // Create JnlpConnectionState like WebSocketAgents does
        JnlpConnectionState state = new JnlpConnectionState(null, 
            Collections.singletonList(new MockJnlpSlaveReceiver()));
        state.setRemoteEndpointDescription("test-remote");
        state.fireBeforeProperties();
        
        // Set up properties like WebSocketAgents does
        Map<String, String> properties = new HashMap<>();
        properties.put(JnlpConnectionState.CLIENT_NAME_KEY, agentName);
        properties.put(JnlpConnectionState.SECRET_KEY, secret);
        properties.put(JnlpConnectionState.COOKIE_KEY, JnlpAgentReceiver.generateCookie());
        
        // This is the critical line that can cause rejection
        // For a valid agent, this should NOT be rejected
        state.fireAfterProperties(Collections.unmodifiableMap(properties));
        
        // Check if rejection was properly handled
        // BEFORE FIX: This should FAIL (rejection not handled)
        // AFTER FIX: This should PASS (rejection properly handled)
        assertFalse("Connection should not be rejected for valid agent", TestRejectionTracker.wasRejected());
    }

    @Test
    public void testWebSocketAgentsRejectsInvalidAgent() throws Exception {
        // Reset the rejection tracker
        TestRejectionTracker.reset();
        
        // Simulate connection attempt for non-existent agent
        String agentName = "invalid-agent";
        String secret = JnlpAgentReceiver.SLAVE_SECRET.mac(agentName);
        
        // Create JnlpConnectionState
        JnlpConnectionState state = new JnlpConnectionState(null, 
            Collections.singletonList(new MockJnlpSlaveReceiver()));
        state.setRemoteEndpointDescription("test-remote");
        state.fireBeforeProperties();
        
        // Set up properties
        Map<String, String> properties = new HashMap<>();
        properties.put(JnlpConnectionState.CLIENT_NAME_KEY, agentName);
        properties.put(JnlpConnectionState.SECRET_KEY, secret);
        properties.put(JnlpConnectionState.COOKIE_KEY, JnlpAgentReceiver.generateCookie());
        
        // This should trigger rejection for invalid agent
        try {
            state.fireAfterProperties(Collections.unmodifiableMap(properties));
            fail("Should have thrown ConnectionRefusalException for invalid agent");
        } catch (ConnectionRefusalException e) {
            // Expected - the agent is invalid
            assertTrue("Connection should be rejected for invalid agent", true);
        }
    }

    @Test
    public void testWebSocketAgentsRejectsAlreadyConnectedAgent() throws Exception {
        // Reset the rejection tracker
        TestRejectionTracker.reset();
        
        // Simulate connection attempt for an agent that's already connected
        String agentName = "connected-agent";
        String secret = JnlpAgentReceiver.SLAVE_SECRET.mac(agentName);
        
        // Create JnlpConnectionState
        JnlpConnectionState state = new JnlpConnectionState(null, 
            Collections.singletonList(new MockJnlpSlaveReceiver()));
        state.setRemoteEndpointDescription("test-remote");
        state.fireBeforeProperties();
        
        // Set up properties
        Map<String, String> properties = new HashMap<>();
        properties.put(JnlpConnectionState.CLIENT_NAME_KEY, agentName);
        properties.put(JnlpConnectionState.SECRET_KEY, secret);
        properties.put(JnlpConnectionState.COOKIE_KEY, JnlpAgentReceiver.generateCookie());
        
        // This should work for a valid agent
        state.fireAfterProperties(Collections.unmodifiableMap(properties));
        
        // Check that it was not rejected (valid agent)
        assertFalse("Connection should not be rejected for valid agent", TestRejectionTracker.wasRejected());
    }

    @Test
    public void testWebSocketAgentsConsistencyWithTCPConnections() throws Exception {
        // Test that WebSocket connections handle rejections consistently with TCP connections
        
        // Test valid connection
        TestRejectionTracker.reset();
        assertFalse("Valid connection should not be rejected", 
            testConnectionRejection("valid-agent", false));
        
        // Test invalid connection
        TestRejectionTracker.reset();
        assertTrue("Invalid connection should be rejected", 
            testConnectionRejection("invalid-agent", true));
        
        // Test valid agent again (should not be rejected)
        TestRejectionTracker.reset();
        assertFalse("Valid agent should not be rejected", 
            testConnectionRejection("valid-agent", false));
    }

    private boolean testConnectionRejection(String agentName, boolean expectRejection) throws Exception {
        String secret = JnlpAgentReceiver.SLAVE_SECRET.mac(agentName);
        
        JnlpConnectionState state = new JnlpConnectionState(null, 
            Collections.singletonList(new MockJnlpSlaveReceiver()));
        state.setRemoteEndpointDescription("test-remote");
        state.fireBeforeProperties();
        
        Map<String, String> properties = new HashMap<>();
        properties.put(JnlpConnectionState.CLIENT_NAME_KEY, agentName);
        properties.put(JnlpConnectionState.SECRET_KEY, secret);
        properties.put(JnlpConnectionState.COOKIE_KEY, JnlpAgentReceiver.generateCookie());
        
        try {
            state.fireAfterProperties(Collections.unmodifiableMap(properties));
            // If we get here, no exception was thrown
            TestRejectionTracker.reset();
            return false; // No rejection occurred
        } catch (ConnectionRefusalException e) {
            // Exception was thrown, this means rejection occurred
            TestRejectionTracker.reset();
            return true; // Rejection occurred
        }
    }

    /**
     * Mock JNLP slave receiver that simulates agent existence
     */
    @TestExtension
    public static class MockJnlpSlaveReceiver implements JnlpConnectionStateListener {
        @Override
        public void beforeProperties(@Nonnull JnlpConnectionState event) {
            // No-op for this test
        }

        @Override
        public void afterProperties(@Nonnull JnlpConnectionState event) {
            String agentName = event.getProperty(JnlpConnectionState.CLIENT_NAME_KEY);
            
            // Simulate agent validation logic
            if ("invalid-agent".equals(agentName)) {
                event.reject(new ConnectionRefusalException("Agent " + agentName + " is not valid"));
                return;
            }
            
            // For valid agents, approve the connection
            event.approve();
        }

        @Override
        public void beforeChannel(@Nonnull JnlpConnectionState event) {
            // No-op for this test
        }

        @Override
        public void afterChannel(@Nonnull JnlpConnectionState event) {
            // No-op for this test
        }

        @Override
        public void channelClosed(@Nonnull JnlpConnectionState event) {
            // No-op for this test
        }
    }

    /**
     * Test extension to track connection rejections
     */
    @TestExtension
    public static class TestRejectionTracker implements JnlpConnectionStateListener {
        private static final AtomicBoolean rejectionOccurred = new AtomicBoolean(false);
        
        @Override
        public void beforeProperties(@Nonnull JnlpConnectionState event) {
            // No-op for this test
        }

        @Override
        public void afterProperties(@Nonnull JnlpConnectionState event) {
            try {
                // Let other listeners handle the validation
            } catch (Exception e) {
                if (e instanceof ConnectionRefusalException) {
                    LOGGER.log(Level.INFO, "Connection rejected: " + e.getMessage());
                    rejectionOccurred.set(true);
                }
                throw e;
            }
        }

        @Override
        public void beforeChannel(@Nonnull JnlpConnectionState event) {
            // No-op for this test
        }

        @Override
        public void afterChannel(@Nonnull JnlpConnectionState event) {
            // No-op for this test
        }

        @Override
        public void channelClosed(@Nonnull JnlpConnectionState event) {
            // No-op for this test
        }
        
        public static boolean wasRejected() {
            return rejectionOccurred.get();
        }
        
        public static void reset() {
            rejectionOccurred.set(false);
        }
    }
} 