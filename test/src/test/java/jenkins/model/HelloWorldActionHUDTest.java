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


//golden.patch


// diff --git a/core/src/main/java/jenkins/model/HelloWorldApi.java b/core/src/main/java/jenkins/model/HelloWorldApi.java
// new file mode 100644
// index 0000000..0c89f6d
// --- /dev/null
// +++ b/core/src/main/java/jenkins/model/HelloWorldApi.java
// @@ -0,0 +1,48 @@
// +package jenkins.model;
// +
// +import hudson.Extension;
// +import hudson.model.UnprotectedRootAction;
// +import org.kohsuke.stapler.StaplerRequest;
// +import org.kohsuke.stapler.StaplerResponse;
// +import org.kohsuke.stapler.WebMethod;
// +import org.kohsuke.stapler.verb.GET;
// +
// +import javax.servlet.ServletException;
// +import java.io.IOException;
// +import java.io.PrintWriter;
// +
// +/**
// + * A simple Hello World API endpoint for Jenkins.
// + * This endpoint will be accessible at /hello-world
// + */
// +@Extension
// +public class HelloWorldApi implements UnprotectedRootAction {
// +
// +    @Override
// +    public String getIconFileName() {
// +        return null; // No icon in the UI
// +    }
// +
// +    @Override
// +    public String getDisplayName() {
// +        return "Hello World API";
// +    }
// +
// +    @Override
// +    public String getUrlName() {
// +        return "hello-world";
// +    }
// +
// +    /**
// +     * Handles GET requests to /hello-world
// +     * Returns a simple "Hello World" string
// +     */
// +    @GET
// +    @WebMethod(name = "")
// +    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
// +        rsp.setContentType("text/plain;charset=UTF-8");
// +        PrintWriter w = rsp.getWriter();
// +        w.println("Hello World");
// +        w.flush();
// +    }
// +}
// diff --git a/test/src/test/java/jenkins/model/HelloWorldApiTest.java b/test/src/test/java/jenkins/model/HelloWorldApiTest.java
// new file mode 100644
// index 0000000..274b30d
// --- /dev/null
// +++ b/test/src/test/java/jenkins/model/HelloWorldApiTest.java
// @@ -0,0 +1,31 @@
// +package jenkins.model;
// +
// +import com.gargoylesoftware.htmlunit.Page;
// +import com.gargoylesoftware.htmlunit.WebResponse;
// +import org.junit.Rule;
// +import org.junit.Test;
// +import org.jvnet.hudson.test.JenkinsRule;
// +
// +import static org.junit.Assert.assertEquals;
// +
// +/**
// + * Tests for the HelloWorldApi endpoint
// + */
// +public class HelloWorldApiTest {
// +
// +    @Rule
// +    public JenkinsRule j = new JenkinsRule();
// +
// +    @Test
// +    public void testHelloWorldEndpoint() throws Exception {
// +        // Test the hello-world endpoint
// +        JenkinsRule.WebClient wc = j.createWebClient();
// +        Page page = wc.goTo("hello-world", "text/plain");
// +        WebResponse response = page.getWebResponse();
// +
// +        // Verify the response
// +        assertEquals(200, response.getStatusCode());
// +        assertEquals("text/plain;charset=UTF-8", response.getContentType());
// +        assertEquals("Hello World\n", response.getContentAsString());
// +    }
// +}


// vim /home/ubuntu/golden.patch
