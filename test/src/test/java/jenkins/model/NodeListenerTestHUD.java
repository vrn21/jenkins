package jenkins.model;

import hudson.model.Node;
import hudson.slaves.DumbSlave;
import hudson.slaves.OfflineCause;
import jenkins.model.NodeListener;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Test to verify that Jenkins.updateNode() fires NodeListener.onUpdated events.
 * 
 * This test checks whether the issue where Jenkins.updateNode() persisted a node's 
 * configuration but did not fire NodeListener.onUpdated events is solved.
 * 
 * BEFORE FIX: This test should FAIL (onUpdated not called)
 * AFTER FIX: This test should PASS (onUpdated is called)
 */
public class NodeListenerTestHUD {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testUpdateNodeFiresOnUpdatedEvent() throws Exception {
        // Create a node
        DumbSlave node = j.createOnlineSlave();
        
        // Reset the listener counter
        TestNodeListener.resetCounter();
        
        // Update the node configuration (this should trigger updateNode internally)
        OfflineCause cause = new OfflineCause.ByCLI("test cause");
        node.toComputer().setTemporaryOfflineCause(cause);
        
        // Check if onUpdated was called
        // BEFORE FIX: This should FAIL (counter = 0)
        // AFTER FIX: This should PASS (counter = 1)
        assertEquals("NodeListener.onUpdated should have been called", 1, TestNodeListener.getUpdatedCounter());
        
        // Verify the node was actually updated
        assertEquals("Node should have the offline cause", cause, node.toComputer().getOfflineCause());
        assertTrue("Node should be temporarily offline", node.toComputer().isTemporarilyOffline());
    }

    @Test
    public void testUpdateNodeWithLabelChangeFiresOnUpdatedEvent() throws Exception {
        // Create a node
        DumbSlave node = j.createOnlineSlave();
        String originalLabel = node.getLabelString();
        
        // Reset the listener counter
        TestNodeListener.resetCounter();
        
        // Update the node label and call updateNode explicitly
        node.setLabelString("updated-label");
        Jenkins.get().updateNode(node);
        
        // Check if onUpdated was called
        // BEFORE FIX: This should FAIL (counter = 0)
        // AFTER FIX: This should PASS (counter = 1)
        assertEquals("NodeListener.onUpdated should have been called", 1, TestNodeListener.getUpdatedCounter());
        
        // Verify the node was actually updated
        assertEquals("Node label should be updated", "updated-label", node.getLabelString());
    }

    @Test
    public void testUpdateNodeConsistencyWithOtherOperations() throws Exception {
        // Test that updateNode behaves consistently with addNode and replaceNode
        
        // Create nodes
        DumbSlave node1 = j.createOnlineSlave();
        DumbSlave node2 = j.createOnlineSlave();
        DumbSlave node3 = j.createSlave();
        
        // Test addNode with a NEW node (should fire onCreated)
        TestNodeListener.resetCounter();
        DumbSlave newNode = new DumbSlave("new-node", "temp", j.createComputerLauncher(null));
        j.jenkins.addNode(newNode); // This should fire onCreated since it's a new node
        int addNodeCount = TestNodeListener.getCreatedCounter();
        
        // Test updateNode (should fire onUpdated after fix)
        TestNodeListener.resetCounter();
        node2.setLabelString("consistency test");
        Jenkins.get().updateNode(node2);
        int updateNodeCount = TestNodeListener.getUpdatedCounter();
        
        // Test replaceNode (should fire onUpdated)
        TestNodeListener.resetCounter();
        j.jenkins.getNodesObject().replaceNode(node2, node3);
        int replaceNodeCount = TestNodeListener.getUpdatedCounter();
        
        // Check consistency
        // BEFORE FIX: addNodeCount=1, updateNodeCount=0, replaceNodeCount=1
        // AFTER FIX: all should be 1
        assertEquals("addNode should fire onCreated for new node", 1, addNodeCount);
        assertEquals("updateNode should fire onUpdated", 1, updateNodeCount);
        assertEquals("replaceNode should fire onUpdated", 1, replaceNodeCount);
    }

    /**
     * Test NodeListener implementation to track onUpdated calls
     */
    @TestExtension
    public static class TestNodeListener extends NodeListener {
        private static final AtomicInteger createdCounter = new AtomicInteger(0);
        private static final AtomicInteger updatedCounter = new AtomicInteger(0);
        
        @Override
        protected void onCreated(Node node) {
            createdCounter.incrementAndGet();
        }
        
        @Override
        protected void onUpdated(Node oldOne, Node newOne) {
            updatedCounter.incrementAndGet();
        }
        
        public static int getCreatedCounter() {
            return createdCounter.get();
        }
        
        public static int getUpdatedCounter() {
            return updatedCounter.get();
        }
        
        public static int getTotalCounter() {
            return createdCounter.get() + updatedCounter.get();
        }
        
        public static void resetCounter() {
            createdCounter.set(0);
            updatedCounter.set(0);
        }
    }
} 
