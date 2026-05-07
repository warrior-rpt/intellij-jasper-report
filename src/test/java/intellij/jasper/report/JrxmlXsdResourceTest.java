/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 warrior-rpt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package intellij.jasper.report;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import intellij.jasper.report.lang.jrxml.autocomplete.JrxmlSchemaProvider;

import java.net.URL;

/**
 * Regression tests for the "xsd not found" bug (fixed in v1.0.3).
 *
 * <p>Root cause: {@code VirtualFileManager.findFileByUrl(resource.toExternalForm())} failed to
 * resolve {@code jar:file://...} URLs produced by {@code Class.getResource()}. Fixed by switching
 * to {@code VfsUtil.findFileByURL(URL)}.
 *
 * <p>These tests will catch any future regression where:
 * <ul>
 *   <li>The XSD is moved or removed from the classpath resource path.</li>
 *   <li>Someone reverts to {@code VirtualFileManager} which cannot handle jar:// URLs.</li>
 * </ul>
 */
public class JrxmlXsdResourceTest extends BasePlatformTestCase {

    private static final String XSD_RESOURCE_PATH = "/intellij/jasper/report/xsd/jasperreport.xsd";

    /**
     * Verifies the jasperreport.xsd is bundled at the expected classpath location.
     * If this fails, the XSD was moved or deleted from src/main/resources.
     */
    public void testXsdResourceExistsOnClasspath() {
        URL resource = JrxmlSchemaProvider.class.getResource(XSD_RESOURCE_PATH);
        assertNotNull(
                "jasperreport.xsd must be present on the classpath at " + XSD_RESOURCE_PATH
                        + " — it may have been moved or excluded from the JAR",
                resource
        );
    }

    /**
     * Verifies that {@code VfsUtil.findFileByURL(URL)} can resolve the XSD to a VirtualFile.
     *
     * <p>This is the direct regression test for the v1.0.3 fix. Before the fix,
     * {@code VirtualFileManager.findFileByUrl(resource.toExternalForm())} returned null for
     * jar:file:// URLs, causing the "xsd not found" error in the IDE log.
     */
    public void testVfsUtilResolvesXsdUrl() {
        URL resource = JrxmlSchemaProvider.class.getResource(XSD_RESOURCE_PATH);
        assertNotNull("XSD resource URL is null — run testXsdResourceExistsOnClasspath first", resource);

        VirtualFile vf = VfsUtil.findFileByURL(resource);
        assertNotNull(
                "VfsUtil.findFileByURL() returned null for '"
                        + resource + "' — the 'xsd not found' bug has regressed. "
                        + "Ensure VfsUtil is used (not VirtualFileManager.findFileByUrl) in JrxmlSchemaProvider.",
                vf
        );
    }

    /**
     * Verifies the XSD resolves to a readable, non-empty file.
     * Guards against a corrupted or zero-byte resource being bundled.
     */
    public void testXsdVirtualFileIsReadable() {
        URL resource = JrxmlSchemaProvider.class.getResource(XSD_RESOURCE_PATH);
        assertNotNull(resource);

        VirtualFile vf = VfsUtil.findFileByURL(resource);
        assertNotNull(vf);

        assertTrue("jasperreport.xsd VirtualFile is not valid", vf.isValid());
        assertFalse("jasperreport.xsd must not be a directory", vf.isDirectory());
        assertTrue("jasperreport.xsd appears to be empty (0 bytes)", vf.getLength() > 0);
    }
}
