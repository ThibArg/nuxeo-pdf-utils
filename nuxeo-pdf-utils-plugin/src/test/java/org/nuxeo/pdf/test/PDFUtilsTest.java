/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     thibaud
 */

package org.nuxeo.pdf.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.pdf.MergePDFsWithBlobsInputOp;
import org.nuxeo.pdf.MergePDFsWithDocsInputOp;
import org.nuxeo.pdf.PDFMerge;
import org.nuxeo.pdf.PDFUtils;
import org.nuxeo.pdf.PDFUtils.PAGE_NUMBER_POSITION;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "nuxeo-pdf-utils-plugin" })
public class PDFUtilsTest {

    private static final String THE_PDF = "files/13-pages-no-page-numbers.pdf";

    protected File pdfFile;

    protected FileBlob pdfFileBlob;

    protected static final String MERGEPDF_1 = "files/pdf-1-2pages.pdf";

    protected static final String MERGEPDF_2 = "files/pdf-2-3pages.pdf";

    protected static final String MERGEPDF_3 = "files/pdf-3-1page.pdf";

    protected static final String MERGEPDF_CHECK_PREFIX = "This is pdf ";

    protected String md5OfThePdf;

    protected ArrayList<PDDocument> createdPDDocs = new ArrayList<PDDocument>();

    protected ArrayList<File> createdTempFiles = new ArrayList<File>();

    // For visually testing the result
    public boolean kDO_LOCAL_TEST_EXPORT_DESKTOP = false;

    protected DocumentModel testDocsFolder, docMergePDF1, docMergePDF2, docMergePDF3;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    protected String getFileMd5(File inFile) throws IOException {

        String md5;

        FileInputStream fis = new FileInputStream(inFile);
        md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
        fis.close();

        return md5;
    }

    protected String getBlobMd5(Blob inBlob) throws IOException {

        String md5;

        md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(inBlob.getStream());

        return md5;
    }

    /*
     * The file must have 13 pages and no numbers at all
     */
    protected void checkPDFBeforeTest() throws IOException {

        PDDocument doc = PDDocument.load(pdfFile);
        assertNotNull(doc);
        createdPDDocs.add(doc);

        assertEquals(13, doc.getNumberOfPages());

        PDFTextStripper stripper = new PDFTextStripper();
        String allTheText = stripper.getText(doc);

        for (int i = 0; i < 10; i++) {
            assertEquals(-1, allTheText.indexOf("" + i));
        }

        doc.close();
        createdPDDocs.remove(doc);
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
        createdPDDocs.add(doc);

        String text = extractText(doc, inPageNumber, inPageNumber);
        int pos = text.indexOf("" + inExpected);
        assertTrue(inPosition + ", expecting " + inExpected + " for page "
                + inPageNumber, pos > -1);

        doc.close();
        createdPDDocs.remove(doc);
    }

    protected DocumentModel createMergePDFDocument(String inWHichOne) {

        File f = FileUtils.getResourceFileFromContext(inWHichOne);

        DocumentModel doc = coreSession.createDocumentModel(
                testDocsFolder.getPathAsString(), f.getName(), "File");
        doc.setPropertyValue("dc:title", f.getName());
        doc.setPropertyValue("file:content", new FileBlob(f));
        return coreSession.createDocument(doc);

    }

    @Before
    public void setup() throws IOException {

        assertNotNull(coreSession);
        assertNotNull(automationService);

        testDocsFolder = coreSession.createDocumentModel("/",
                "test-pictures", "Folder");
        testDocsFolder.setPropertyValue("dc:title", "test-pdfutils");
        testDocsFolder = coreSession.createDocument(testDocsFolder);
        testDocsFolder = coreSession.saveDocument(testDocsFolder);

        docMergePDF1 = createMergePDFDocument(MERGEPDF_1);
        docMergePDF2 = createMergePDFDocument(MERGEPDF_2);
        docMergePDF3 = createMergePDFDocument(MERGEPDF_3);


        pdfFile = FileUtils.getResourceFileFromContext(THE_PDF);
        md5OfThePdf = getFileMd5(pdfFile);
        pdfFileBlob = new FileBlob(pdfFile);
        checkPDFBeforeTest();
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

        blobResult = PDFUtils.addPageNumbers(pdfFileBlob, inStartAtPage,
                inStartAtNumber, inFontName, inFontSize, inHex255Color,
                inPosition);
        assertNotNull(blobResult);
        assertNotSame(md5OfThePdf, getBlobMd5(blobResult));

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

    /*
     * We check the pdf is in the correct order. Each pdf file used has a text
     * telling us where it should have been merged
     */
    protected void checkMergedPDF(Blob inBlob, boolean jutsFirst2Pages) throws IOException {

        File tempFile = File.createTempFile("testmergepdf", ".pdf");
        createdTempFiles.add(tempFile);
        inBlob.transferTo(tempFile);

        PDDocument doc = PDDocument.load(tempFile);
        assertNotNull(doc);
        createdPDDocs.add(doc);

        // 2 + 3 + 1
        if(jutsFirst2Pages) {
            assertEquals(5, doc.getNumberOfPages());
        } else {
            assertEquals(6, doc.getNumberOfPages());
        }

        String txt;
        txt = extractText(doc, 1, 1);
        assertTrue(txt.indexOf(MERGEPDF_CHECK_PREFIX + "1") > -1);

        txt = extractText(doc, 3, 3);
        assertTrue(txt.indexOf(MERGEPDF_CHECK_PREFIX + "2") > -1);

        if(!jutsFirst2Pages) {
            txt = extractText(doc, 6, 6);
            assertTrue(txt.indexOf(MERGEPDF_CHECK_PREFIX + "3") > -1);
        }

        doc.close();
        createdPDDocs.remove(doc);

        tempFile.delete();
        createdTempFiles.remove(tempFile);

    }

    /*
     * Test PDFMerge constructor with simple blobs
     */
    @Test
    public void testMergePDFs_ConstructorSimpleBlobs() throws Exception {

        FileBlob fb;

        FileBlob first = new FileBlob(
                FileUtils.getResourceFileFromContext(MERGEPDF_1));
        PDFMerge pdfm = new PDFMerge(first);

        fb = new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_2));
        pdfm.addBlob(fb);
        fb = new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_3));
        pdfm.addBlob(fb);

        Blob result = pdfm.merge("merged1.pdf");
        assertNotNull(result);

        checkMergedPDF(result, false);
    }

    /*
     * Test PDFMerge constructor with BlobList
     */
    @Test
    public void testMergePDFs_ConstructorBlobList() throws Exception {

        BlobList bl = new BlobList();

        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_1)));
        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_2)));
        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_3)));

        PDFMerge pdfm = new PDFMerge(bl);

        Blob result = pdfm.merge("merged2.pdf");
        assertNotNull(result);

        checkMergedPDF(result, false);

    }

    @Test
    public void testMergePDFs_ConstructorSimpleDoc() throws Exception {

        PDFMerge pdfm = new PDFMerge(docMergePDF1, null);
        pdfm.addBlob(docMergePDF2, null);
        pdfm.addBlob(docMergePDF3, "");

        Blob result = pdfm.merge("merged1.pdf");
        assertNotNull(result);

        checkMergedPDF(result, false);
    }

    @Test
    public void testMergePDFs_ConstructorDocList() throws Exception {

        DocumentModelList docList = new DocumentModelListImpl();

        docList.add(docMergePDF1);
        docList.add(docMergePDF2);
        docList.add(docMergePDF3);

        PDFMerge pdfm = new PDFMerge(docList, null);

        Blob result = pdfm.merge("merged1.pdf");
        assertNotNull(result);

        checkMergedPDF(result, false);
    }

    @Test
    public void testMergePDFsBlobOperation_blobInput() throws Exception {

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        // Test with blobs
        FileBlob fb = new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_1));
        ctx.setInput(fb);
        chain = new OperationChain("testChain");

        ctx.put("varBlobToAppend", new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_2)));
        String theFileName = "theMergedFile.pdf";
        chain.add(MergePDFsWithBlobsInputOp.ID)
                .set("toAppendVarName", "varBlobToAppend")
                .set("fileName", theFileName);

        Blob result = (Blob) automationService.run(ctx,  chain);
        assertNotNull(result);
        checkMergedPDF(result, true);
        assertEquals(theFileName, result.getFilename());

    }

    @Test
    public void testMergePDFsBlobOperation_blobListInput() throws Exception {

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        BlobList bl = new BlobList();
        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_1)));
        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_2)));

        ctx.setInput(bl);
        chain = new OperationChain("testChain");

        ctx.put("varBlobToAppend", new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_3)));
        String theFileName = "theMergedFile.pdf";
        chain.add(MergePDFsWithBlobsInputOp.ID)
                .set("toAppendVarName", "varBlobToAppend")
                .set("fileName", theFileName);

        Blob result = (Blob) automationService.run(ctx,  chain);
        assertNotNull(result);
        checkMergedPDF(result, false);
        assertEquals(theFileName, result.getFilename());

    }

    @Test
    public void testMergePDFsBlobOperation_docInput() throws Exception {

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        ctx.setInput(docMergePDF1);
        chain = new OperationChain("testChain");

        ctx.put("varBlobToAppend", new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_2)));
        String theFileName = "theMergedFile.pdf";
        chain.add(MergePDFsWithDocsInputOp.ID)
                .set("toAppendVarName", "varBlobToAppend")
                .set("fileName", theFileName);

        Blob result = (Blob) automationService.run(ctx,  chain);
        assertNotNull(result);
        checkMergedPDF(result, true);
        assertEquals(theFileName, result.getFilename());

    }

    @Test
    public void testMergePDFsBlobOperation_docListInput() throws Exception {

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        DocumentModelList docList = new DocumentModelListImpl();

        docList.add(docMergePDF1);
        docList.add(docMergePDF2);
        ctx.setInput(docList);
        chain = new OperationChain("testChain");

        ctx.put("varBlobToAppend", new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_3)));
        String theFileName = "theMergedFile.pdf";
        chain.add(MergePDFsWithDocsInputOp.ID)
                .set("toAppendVarName", "varBlobToAppend")
                .set("fileName", theFileName);

        Blob result = (Blob) automationService.run(ctx,  chain);
        assertNotNull(result);
        checkMergedPDF(result, false);
        assertEquals(theFileName, result.getFilename());

    }

    @Test
    public void testMergePDFsBlobOperation_OtherTests() throws Exception {

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        FileBlob fb = new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_1));
        BlobList bl = new BlobList();
        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_2)));
        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(MERGEPDF_3)));

        ctx.setInput(fb);
        chain = new OperationChain("testChain");
        ctx.put("varBlobListToAppend", bl);
        String theFileName = "theMergedFile.pdf";
        chain.add(MergePDFsWithBlobsInputOp.ID)
                .set("toAppendListVarName", "varBlobListToAppend")
                .set("fileName", theFileName);

        Blob result = (Blob) automationService.run(ctx,  chain);
        assertNotNull(result);
        checkMergedPDF(result, false);
        assertEquals(theFileName, result.getFilename());

    }
}
