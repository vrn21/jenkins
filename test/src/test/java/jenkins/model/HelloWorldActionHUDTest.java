package jenkins.model;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class HelloWorldActionHUDTest {

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

// /HelloWorldAction
// package jenkins.model;

// import hudson.Extension;
// import hudson.model.UnprotectedRootAction;
// import org.kohsuke.stapler.HttpResponse;
// import org.kohsuke.stapler.HttpResponses;

// @Extension
// public class HelloWorldAction implements UnprotectedRootAction {

//     @Override
//     public String getUrlName() {
//         return "hello-world";
//     }

//     @Override
//     public String getIconFileName() {
//         return null;
//     }

//     @Override
//     public String getDisplayName() {
//         return null;
//     }

//     public HttpResponse doIndex() {
//         return HttpResponses.plainText("Hello world");
//     }
// }

// ran using
// mvn clean install -pl core -am
// mvn test -Dtest=HelloWorldActionHUDTest -Dcheckstyle.skip=true -Dmaven.checkstyle.skip=true -Dspotless.check.skip=true -Dyarn.lint.skip=true
