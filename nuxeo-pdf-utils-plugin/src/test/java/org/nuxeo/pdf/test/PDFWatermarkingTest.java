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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.pdf.PDFWatermarking;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "nuxeo-pdf-utils-plugin" })
public class PDFWatermarkingTest {

    private static final String THE_PDF = "files/13-pages-no-page-numbers.pdf";

    private static final String PDF_WITH_IMAGES = "files/With-pictures.pdf";

    protected File pdfFile;

    protected FileBlob pdfFileBlob;

    protected File pdfFileWithImages;

    protected FileBlob pdfFileWithImagesBlob;

    protected TestUtils utils;

    // For visually testing the result
    public boolean kDO_LOCAL_TEST_EXPORT_DESKTOP = false;

    protected DocumentModel testDocsFolder;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    /*
     * Utility. Extract the text in the given page(s)
     */
    protected String extractText(PDDocument inDoc, int startPage, int inEndPage)
            throws IOException {

        String txt = "";

        inEndPage = inEndPage < startPage ? startPage : startPage;

        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(startPage);
        stripper.setEndPage(inEndPage);
        txt = stripper.getText(inDoc);

        return txt;
    }

    @Before
    public void setup() throws IOException {

        utils = new TestUtils();

        assertNotNull(coreSession);
        assertNotNull(automationService);

        testDocsFolder = coreSession.createDocumentModel("/", "test-pictures",
                "Folder");
        testDocsFolder.setPropertyValue("dc:title", "test-pdfutils");
        testDocsFolder = coreSession.createDocument(testDocsFolder);
        testDocsFolder = coreSession.saveDocument(testDocsFolder);

        pdfFile = FileUtils.getResourceFileFromContext(THE_PDF);
        pdfFileBlob = new FileBlob(pdfFile);

        pdfFileWithImages = FileUtils.getResourceFileFromContext(PDF_WITH_IMAGES);
        pdfFileWithImagesBlob = new FileBlob(pdfFileWithImages);

    }

    @After
    public void cleanup() {

        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();

        utils.cleanup();
    }

    /*
     * This one is for local test with human checking :-)
     */
    protected void saveBlobOnDesktop(Blob inBlob, String inFileName)
            throws IOException {
        File destFile = new File(System.getProperty("user.home"),
                "Desktop/tests-add-page-numbers/" + inFileName);
        inBlob.transferTo(destFile);
    }

    protected void checkHasWatermarkOnAllPages(Blob inBlob, String inWatermark)
            throws Exception {

        PDDocument doc = PDDocument.load(inBlob.getStream());
        utils.track(doc);

        int count = doc.getNumberOfPages();
        for (int i = 1; i <= count; i++) {
            String txt = utils.extractText(doc, i, i);
            int pos = txt.indexOf(inWatermark);
            assertTrue("for page " + i + ", found pos is " + pos, pos > -1);
        }

        doc.close();
        utils.untrack(doc);
    }

    @Test
    public void testAddWatermarkWithDefaultValues_SimplePDF() throws Exception {

        PDFWatermarking pdfw = new PDFWatermarking(pdfFileBlob);

        String watermark = java.util.UUID.randomUUID().toString();

        pdfw.setText(watermark);
        Blob result = pdfw.watermark();

        checkHasWatermarkOnAllPages(result, watermark);
        // saveBlobOnDesktop(result, "test.pdf");
    }

    @Test
    public void testAddWatermarkWithDefaultValues_PDFWithImages()
            throws Exception {

        PDFWatermarking pdfw = new PDFWatermarking(pdfFileWithImagesBlob);

        String watermark = java.util.UUID.randomUUID().toString();

        pdfw.setText(watermark);
        Blob result = pdfw.watermark();

        checkHasWatermarkOnAllPages(result, watermark);
        // saveBlobOnDesktop(result, "test-images.pdf");
    }

    @Test
    public void testAddWatermark_PDFWithImages() throws Exception {

        PDFWatermarking pdfw = new PDFWatermarking(pdfFileWithImagesBlob);

        String watermark = java.util.UUID.randomUUID().toString().substring(1, 5);

        pdfw.setText(watermark).setXPosition(100).setYPosition(100).setAlphaColor(
                0.3f).setFontSize(12f).setTextRotation(45);
        Blob result = pdfw.watermark();

        //checkHasWatermarkOnAllPages(result, watermark);
        //saveBlobOnDesktop(result, "test-images.pdf");
    }

    @Test
    public void testShouldFailOnEmptyString() throws Exception {

        PDFWatermarking pdfw = new PDFWatermarking(pdfFileBlob);

        Blob result = null;

        pdfw.setText("");
        try {
            result = pdfw.watermark();
            assertFalse("An error should have been raised for an empty string",
                    true);
        } catch (Exception e) {
            // All good
        }

        pdfw.setText(null);
        try {
            result = pdfw.watermark();
            assertFalse("An error should have been raised for a null string",
                    true);
        } catch (Exception e) {
            // All good
        }
    }
}
