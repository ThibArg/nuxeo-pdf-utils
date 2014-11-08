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
import org.apache.pdfbox.exceptions.COSVisitorException;
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
import org.nuxeo.pdf.PDFPageNumbering.PAGE_NUMBER_POSITION;
import org.nuxeo.pdf.PDFPageNumbering;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "nuxeo-pdf-utils-plugin" })
public class PDFPageNumberingTest {

    private static final String THE_PDF = "files/13-pages-no-page-numbers.pdf";

    protected File pdfFile;

    protected FileBlob pdfFileBlob;

    protected String md5OfThePdf;

    protected TestUtils utils;

    // For visually testing the result
    public boolean kDO_LOCAL_TEST_EXPORT_DESKTOP = false;

    protected DocumentModel testDocsFolder;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    /*
     * The file must have 13 pages and no numbers at all
     */
    protected void checkPDFBeforeTest() throws IOException {

        PDDocument doc = PDDocument.load(pdfFile);
        assertNotNull(doc);
        utils.track(doc);

        assertEquals(13, doc.getNumberOfPages());

        PDFTextStripper stripper = new PDFTextStripper();
        String allTheText = stripper.getText(doc);

        for (int i = 0; i < 10; i++) {
            assertEquals(-1, allTheText.indexOf("" + i));
        }

        doc.close();
        utils.untrack(doc);
    }

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

    protected void checkHasNumberInPage(File inDoc, int inExpected,
            int inPageNumber, String inPosition) throws IOException {

        PDDocument doc = PDDocument.load(inDoc);
        assertNotNull(doc);
        utils.track(doc);

        String text = extractText(doc, inPageNumber, inPageNumber);
        int pos = text.indexOf("" + inExpected);
        assertTrue(inPosition + ", expecting " + inExpected + " for page "
                + inPageNumber, pos > -1);

        doc.close();
        utils.untrack(doc);
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
        md5OfThePdf = utils.calculateMd5(pdfFile);
        pdfFileBlob = new FileBlob(pdfFile);
        checkPDFBeforeTest();
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

    protected void testWithTheseParameters(int inStartAtPage,
            int inStartAtNumber, String inFontName, float inFontSize,
            String inHex255Color, PAGE_NUMBER_POSITION inPosition)
            throws COSVisitorException, IOException {

        Blob blobResult;

        PDFPageNumbering pn = new PDFPageNumbering(pdfFileBlob);
        blobResult = pn.addPageNumbers(inStartAtPage, inStartAtNumber,
                inFontName, inFontSize, inHex255Color, inPosition);
        assertNotNull(blobResult);
        assertNotSame(md5OfThePdf, utils.calculateMd5(blobResult));

        File tempFile = File.createTempFile("pdfutils-", ".pdf");
        blobResult.transferTo(tempFile);
        checkHasNumberInPage(tempFile, inStartAtNumber, inStartAtPage,
                inPosition.toString());
        tempFile.delete();

        // THIS IS ONLY FOR LOCAL TESTING
        if (kDO_LOCAL_TEST_EXPORT_DESKTOP) {
            if (inPosition == null) {
                inPosition = PAGE_NUMBER_POSITION.BOTTOM_RIGHT;
            }
            String strPosition;
            switch (inPosition) {
            case BOTTOM_LEFT:
                strPosition = "bottom-left";
                break;

            case BOTTOM_CENTER:
                strPosition = "bottom-center";
                break;

            case TOP_LEFT:
                strPosition = "top-left";
                break;

            case TOP_CENTER:
                strPosition = "top-center";
                break;

            case TOP_RIGHT:
                strPosition = "top-right";
                break;

            // Bottom-right is the default
            default:
                strPosition = "bottom-right";
                break;
            }
            saveBlobOnDesktop(blobResult, strPosition + ".pdf");
        }

    }

    @Test
    public void testAddPageNumbers() throws COSVisitorException, IOException {

        // We try misc positions, start pages and page numbers
        testWithTheseParameters(1, 1, null, 0, "ff0000",
                PAGE_NUMBER_POSITION.BOTTOM_RIGHT);
        testWithTheseParameters(5, 3, null, 0, "00ff00",
                PAGE_NUMBER_POSITION.BOTTOM_CENTER);
        testWithTheseParameters(10, 10, null, 0, "0000ff",
                PAGE_NUMBER_POSITION.BOTTOM_LEFT);
        testWithTheseParameters(1, 150, null, 0, "#FF0000",
                PAGE_NUMBER_POSITION.TOP_RIGHT);
        testWithTheseParameters(1, 1, null, 0, "0x0000ff",
                PAGE_NUMBER_POSITION.TOP_CENTER);
        testWithTheseParameters(1, 1, null, 0, "",
                PAGE_NUMBER_POSITION.TOP_LEFT);

    }
}
