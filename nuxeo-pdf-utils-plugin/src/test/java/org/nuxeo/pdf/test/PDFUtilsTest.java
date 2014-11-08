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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
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
import org.nuxeo.pdf.PDFUtils;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "nuxeo-pdf-utils-plugin" })
public class PDFUtilsTest {

    // WARNING: If you change this pdf, a lot of tests will fail (count pages,
    // text in the pdf, ...)
    private static final String THE_PDF = "files/13-pages-no-page-numbers.pdf";

    protected File pdfFile;

    protected FileBlob pdfFileBlob;

    TestUtils utils;

    // For visually testing the result
    public boolean kDO_LOCAL_TEST_EXPORT_DESKTOP = false;

    protected DocumentModel testDocsFolder, pdfDocModel;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    @Before
    public void setup() throws Exception {

        utils = new TestUtils();

        assertNotNull(coreSession);
        assertNotNull(automationService);

        testDocsFolder = coreSession.createDocumentModel("/", "test-pictures",
                "Folder");
        testDocsFolder.setPropertyValue("dc:title", "test-pdfutils");
        testDocsFolder = coreSession.createDocument(testDocsFolder);
        testDocsFolder = coreSession.saveDocument(testDocsFolder);
        assertNotNull(testDocsFolder);

        pdfFile = FileUtils.getResourceFileFromContext(THE_PDF);
        assertNotNull(pdfFile);
        pdfFileBlob = new FileBlob(pdfFile);
        assertNotNull(pdfFileBlob);

        pdfDocModel = coreSession.createDocumentModel(
                testDocsFolder.getPathAsString(), pdfFile.getName(), "File");
        pdfDocModel.setPropertyValue("dc:title", pdfFile.getName());
        pdfDocModel.setPropertyValue("file:content", pdfFileBlob);
        pdfDocModel = coreSession.createDocument(pdfDocModel);
        pdfDocModel = coreSession.saveDocument(pdfDocModel);
        assertNotNull(pdfDocModel);
    }

    @After
    public void cleanup() {

        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();

        utils.cleanup();
    }

    @Test
    public void test_hex255ToRGB() throws Exception {

        int[] rgb;

        rgb = PDFUtils.hex255ToRGB("#000000");
        assertEquals(3, rgb.length);
        assertEquals(0, rgb[0]);
        assertEquals(0, rgb[1]);
        assertEquals(0, rgb[2]);

        rgb = PDFUtils.hex255ToRGB("0xffFfFf");
        assertEquals(255, rgb[0]);
        assertEquals(255, rgb[1]);
        assertEquals(255, rgb[2]);

        rgb = PDFUtils.hex255ToRGB("123456");
        assertEquals(18, rgb[0]);
        assertEquals(52, rgb[1]);
        assertEquals(86, rgb[2]);

        rgb = PDFUtils.hex255ToRGB("");
        assertEquals(0, rgb[0]);
        assertEquals(0, rgb[1]);
        assertEquals(0, rgb[2]);
    }

    @Test
    public void test_saveInTempFile() throws Exception {

        PDDocument doc = PDDocument.load(pdfFile);
        utils.track(doc);

        FileBlob fb = PDFUtils.saveInTempFile(doc);
        assertNotNull(fb);
        assertEquals("application/pdf", fb.getMimeType());

        doc.close();
        utils.untrack(doc);
    }

    @Test
    public void test_checkXPath() throws Exception {

        assertEquals("file:content", PDFUtils.checkXPath(null));
        assertEquals("file:content", PDFUtils.checkXPath(""));
        assertEquals("myschema:myfield",
                PDFUtils.checkXPath("myschema:myfield"));
    }

    @Test
    public void test_setInfos() throws Exception {

        PDDocument doc = PDDocument.load(pdfFile);
        utils.track(doc);

        PDDocumentInformation docInfoOriginal = doc.getDocumentInformation();
        // Check original document has the expected values
        assertEquals("Untitled 3", docInfoOriginal.getTitle());
        assertNull(docInfoOriginal.getSubject());
        assertNull(docInfoOriginal.getAuthor());
        // Now, modify
        // First, actually, don't modify
        PDFUtils.setInfos(doc, null, "", null);
        PDDocumentInformation newDocInfo = doc.getDocumentInformation();
        assertEquals(docInfoOriginal.getTitle(), newDocInfo.getTitle());
        assertEquals(docInfoOriginal.getSubject(), newDocInfo.getSubject());
        assertEquals(docInfoOriginal.getAuthor(), newDocInfo.getAuthor());
        // Now, modify
        PDFUtils.setInfos(doc, "The Title", "The Subject", "The Author");
        newDocInfo = doc.getDocumentInformation();
        assertEquals("The Title", newDocInfo.getTitle());
        assertEquals("The Subject", newDocInfo.getSubject());
        assertEquals("The Author", newDocInfo.getAuthor());

        doc.close();
        utils.untrack(doc);
    }
}
