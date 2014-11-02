/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thibaud Arguillere
 */

package org.nuxeo.pdf.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.pdf.PDFInfo;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "nuxeo-pdf-utils-plugin" })
public class PDFInfoTest {

    private static final String THE_PDF = "files/13-pages-no-page-numbers.pdf";

    private static final String NOT_A_PDF = "files/Travel-3.jpg";

    protected File pdfFile;

    protected FileBlob pdfFileBlob;

    protected ArrayList<PDDocument> createdPDDocs = new ArrayList<PDDocument>();

    protected ArrayList<File> createdTempFiles = new ArrayList<File>();

    // For visually testing the result
    public boolean kDO_LOCAL_TEST_EXPORT_DESKTOP = false;

    protected DocumentModel testDocsFolder;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    @Before
    public void setup() throws IOException {

        assertNotNull(coreSession);
        assertNotNull(automationService);

        testDocsFolder = coreSession.createDocumentModel("/", "test-pictures",
                "Folder");
        testDocsFolder.setPropertyValue("dc:title", "test-pdfutils");
        testDocsFolder = coreSession.createDocument(testDocsFolder);
        testDocsFolder = coreSession.saveDocument(testDocsFolder);

        pdfFile = FileUtils.getResourceFileFromContext(THE_PDF);
        pdfFileBlob = new FileBlob(pdfFile);
    }

    @After
    public void cleanup() {

        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();

        try {
            for (PDDocument pdfDoc : createdPDDocs) {
                pdfDoc.close();
            }

            for (File f : createdTempFiles) {
                f.delete();
            }
        } catch (Exception e) {
            // Nothing
        }
    }

    @Test
    public void testPDFInfo() throws IOException {

        PDFInfo info = new PDFInfo(pdfFileBlob, true);
        assertNotNull(info);

        HashMap<String, String> values = info.toHashMap();
        assertNotNull(values);

        // check some values
        assertEquals("1.3", values.get("PDF version"));
        assertEquals(67122, Long.valueOf(values.get("File size")).longValue());
        assertEquals("SinglePage", values.get("Page Layout"));
        assertEquals("TextEdit", values.get("Content creator"));

    }

    @Test
    public void testPDFInfoShouldFailOnNonPDFBlob() throws IOException {

        File f = FileUtils.getResourceFileFromContext(NOT_A_PDF);
        FileBlob fb = new FileBlob(f);

        try {
            PDFInfo info = new PDFInfo(fb, true);
            assertTrue("Parsing the non-pdf file should have failed", false);
        } catch (Exception e) {
            // All good
        }
    }
}
