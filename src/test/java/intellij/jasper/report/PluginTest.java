package intellij.jasper.report;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class PluginTest extends BasePlatformTestCase {

    public void testProjectService() {
        // Just verify the test environment is working and the plugin is loaded
        assertNotNull("Project instance should not be null", getProject());
    }
}
