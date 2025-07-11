package jenkins.model;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class HelloWorldActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testHelloWorldEndpoint() throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();
        String response = wc.goTo("hello-world", "text/plain")
                           .getWebResponse()
                           .getContentAsString();
        assertEquals("Hello world", response.strip());
    }
}
