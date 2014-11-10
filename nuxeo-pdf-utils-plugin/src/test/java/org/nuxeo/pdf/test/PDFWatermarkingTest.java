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
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.util.PDFTextStripper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.pdf.PDFWatermarking;
import org.nuxeo.pdf.operations.WatermarkWithImageOp;
import org.nuxeo.pdf.operations.WatermarkWithPDFOp;
import org.nuxeo.pdf.operations.WatermarkWithTextOp;
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

    private static final String PDF_FOR_WATERMARK = "files/Nuxeo-logo-transp-Gray.pdf";

    private static final String IMAGE_FOR_WATERMARK_PNG = "files/Nuxeo-logo-transp-Gray.png";

    private static final int IMAGE_FOR_WATERMARK_PNG_WIDTH = 201;

    private static final int IMAGE_FOR_WATERMARK_PNG_HEIGHT = 78;

    private static final String IMAGE_FOR_WATERMARK_JPEG = "files/Nuxeo-logo-Gray.jpg";

    protected File pdfFile;

    protected FileBlob pdfFileBlob;

    protected File pdfFileWithImages;

    protected FileBlob pdfFileWithImagesBlob;

    protected TestUtils utils;

    // For visually testing the result
    public boolean kDO_LOCAL_TEST_EXPORT_DESKTOP = false;

    protected DocumentModel testDocsFolder, pdfForWatermarkDoc,
            pngForWatermarkDoc;

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

        pdfForWatermarkDoc = utils.createDocumentFromFile(coreSession,
                testDocsFolder, "File", PDF_FOR_WATERMARK);
        pngForWatermarkDoc = utils.createDocumentFromFile(coreSession,
                testDocsFolder, "File", IMAGE_FOR_WATERMARK_PNG);

    }

    @After
    public void cleanup() {

        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();

        utils.cleanup();
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
        if (kDO_LOCAL_TEST_EXPORT_DESKTOP) {
            utils.saveBlobOnDesktop(result, "nuxeo-pdfutils-test", "test.pdf");
        }
    }

    @Test
    public void testAddWatermarkWithDefaultValues_PDFWithImages()
            throws Exception {

        PDFWatermarking pdfw = new PDFWatermarking(pdfFileWithImagesBlob);

        String watermark = java.util.UUID.randomUUID().toString();

        pdfw.setText(watermark);
        Blob result = pdfw.watermark();

        checkHasWatermarkOnAllPages(result, watermark);
        if (kDO_LOCAL_TEST_EXPORT_DESKTOP) {
            utils.saveBlobOnDesktop(result, "nuxeo-pdfutils-test",
                    "test-images.pdf");
        }
    }

    @Test
    public void testAddWatermark_PDFWithImages() throws Exception {

        PDFWatermarking pdfw = new PDFWatermarking(pdfFileWithImagesBlob);

        String watermark = "© ACME - "
                + java.util.UUID.randomUUID().toString().substring(1, 5);

        pdfw.setText(watermark).setXPosition(100).setYPosition(100).setAlphaColor(
                0.3f).setFontSize(12f).setTextRotation(45);
        Blob result = pdfw.watermark();

        // When the text is rotated, extracting the full text form a page as is
        // done in checkHasWatermarkOnAllPages() sometime does not find the
        // value while it is here. It would require to extends PDFTextStripper
        // and override processTextPosition. So it is a kind of TODO.
        // checkHasWatermarkOnAllPages(result, watermark);
        // if(kDO_LOCAL_TEST_EXPORT_DESKTOP) {
        // utils.saveBlobOnDesktop(result, "nuxeo-pdfutils-test",
        // "test-images-watermarked-rot.pdf");
        // }
    }

    @Test
    public void testEmptyStringReturnsRawCopy() throws Exception {

        PDFWatermarking pdfw = new PDFWatermarking(pdfFileBlob);
        String originalMd5 = utils.calculateMd5(pdfFileBlob);

        Blob result = null;
        pdfw.setText("");
        result = pdfw.watermark();

        assertEquals(originalMd5, utils.calculateMd5(result));
    }

    @Test
    public void testWatermarkWithTextOperation() throws Exception {

        Properties props = new Properties();
        props.put("xPosition", "200");
        props.put("yPosition", "300");
        props.put("alphaColor", "0.9");
        props.put("invertY", "true");
        props.put("textRotation", "45");

        String watermark = "© Toto -" + java.util.UUID.randomUUID().toString();

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        ctx.setInput(pdfFileWithImagesBlob);
        chain = new OperationChain("testChain");
        chain.add(WatermarkWithTextOp.ID).set("watermark", watermark).set(
                "properties", props);
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);

        // When the text is rotated, extracting the full text form a page as is
        // done in checkHasWatermarkOnAllPages() sometime does not find the
        // value while it is here. It would require to extends PDFTextStripper
        // and override processTextPosition. So it is a kind of TODO.
        // checkHasWatermarkOnAllPages(result, watermark);
        //
        // if(kDO_LOCAL_TEST_EXPORT_DESKTOP) {
        // utils.saveBlobOnDesktop(result, "nuxeo-pdfutils-test",
        // "test-images-watermarked-rot-operation.pdf");
        // }
    }

    @Test
    public void testPdfOverlay() throws Exception {

        File overlayFile = FileUtils.getResourceFileFromContext(PDF_FOR_WATERMARK);
        FileBlob overlayBlob = new FileBlob(overlayFile);

        PDFWatermarking pdfw = new PDFWatermarking(pdfFileBlob);
        Blob result = pdfw.watermarkWithPdf(overlayBlob);

        // How to test that?
        // . . .

        if (kDO_LOCAL_TEST_EXPORT_DESKTOP) {
            utils.saveBlobOnDesktop(result, "nuxeo-pdfutils-test",
                    "test-images-withOverlayPDF.pdf");
        }
    }

    /*
     * Just checking on width/height here.
     */
    protected void checkHasImage(Blob inBlob, int inExpectedWidth,
            int inExpectedHeight) throws Exception {

        PDDocument doc = PDDocument.load(inBlob.getStream());
        utils.track(doc);

        List<?> allPages = doc.getDocumentCatalog().getAllPages();
        int max = allPages.size();
        for (int i = 1; i < max; i++) {
            PDPage page = (PDPage) allPages.get(i);

            PDResources pdResources = page.getResources();
            Map<String, PDXObject> allXObjects = pdResources.getXObjects();
            assertNotNull(allXObjects);

            boolean gotIt = false;
            for (Map.Entry<String, PDXObject> entry : allXObjects.entrySet()) {
                PDXObject xobject = entry.getValue();
                if (xobject instanceof PDXObjectImage) {
                    PDXObjectImage pdxObjectImage = (PDXObjectImage) xobject;
                    if (inExpectedWidth == pdxObjectImage.getWidth()
                            && inExpectedHeight == pdxObjectImage.getHeight()) {
                        gotIt = true;
                        break;
                    }
                }
            }
            assertTrue("Page " + i + "does not have the image", gotIt);
        }

        doc.close();
        utils.untrack(doc);
    }

    @Test
    public void testWatermarkWithImagePNG() throws Exception {

        File overlayPictureFile = FileUtils.getResourceFileFromContext(IMAGE_FOR_WATERMARK_PNG);
        FileBlob overlayPictureBlob = new FileBlob(overlayPictureFile);

        PDFWatermarking pdfw = new PDFWatermarking(pdfFileBlob);
        Blob result = pdfw.watermarkWithImage(overlayPictureBlob, 200, 200,
                0.5f);

        checkHasImage(result, IMAGE_FOR_WATERMARK_PNG_WIDTH,
                IMAGE_FOR_WATERMARK_PNG_HEIGHT);
        if (kDO_LOCAL_TEST_EXPORT_DESKTOP) {
            utils.saveBlobOnDesktop(result, "nuxeo-pdfutils-test",
                    "test-images-withOverlayPNG.pdf");
        }

    }

    @Test
    public void testWatermarkWithImagePNG_2() throws Exception {

        File overlayPictureFile = FileUtils.getResourceFileFromContext(IMAGE_FOR_WATERMARK_PNG);
        FileBlob overlayPictureBlob = new FileBlob(overlayPictureFile);

        PDFWatermarking pdfw = new PDFWatermarking(pdfFileWithImagesBlob);
        Blob result = pdfw.watermarkWithImage(overlayPictureBlob, 200, 200,
                0.5f);

        checkHasImage(result, IMAGE_FOR_WATERMARK_PNG_WIDTH,
                IMAGE_FOR_WATERMARK_PNG_HEIGHT);
        if (kDO_LOCAL_TEST_EXPORT_DESKTOP) {
            utils.saveBlobOnDesktop(result, "nuxeo-pdfutils-test",
                    "test-images-withOverlayPNG.pdf");
        }

    }

    @Test
    public void testWatermarkWithImageJPEG() throws Exception {

        File overlayPictureFile = FileUtils.getResourceFileFromContext(IMAGE_FOR_WATERMARK_JPEG);
        FileBlob overlayPictureBlob = new FileBlob(overlayPictureFile);

        PDFWatermarking pdfw = new PDFWatermarking(pdfFileBlob);
        Blob result = pdfw.watermarkWithImage(overlayPictureBlob, 200, 200, 4f);

        checkHasImage(result, IMAGE_FOR_WATERMARK_PNG_WIDTH,
                IMAGE_FOR_WATERMARK_PNG_HEIGHT);
        if (kDO_LOCAL_TEST_EXPORT_DESKTOP) {
            utils.saveBlobOnDesktop(result, "nuxeo-pdfutils-test",
                    "test-images-withOverlayJPEG.pdf");
        }

    }

    @Test
    public void testWatermarkWithImageOperation_withBlob_defaultValues()
            throws Exception {

        File overlayPictureFile = FileUtils.getResourceFileFromContext(IMAGE_FOR_WATERMARK_PNG);
        FileBlob overlayPictureBlob = new FileBlob(overlayPictureFile);

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        ctx.setInput(pdfFileWithImagesBlob);
        chain = new OperationChain("testChain");

        ctx.put("theBlobImage", overlayPictureBlob);
        chain.add(WatermarkWithImageOp.ID).set("imageContextVarName",
                "theBlobImage");

        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);

        checkHasImage(result, IMAGE_FOR_WATERMARK_PNG_WIDTH,
                IMAGE_FOR_WATERMARK_PNG_HEIGHT);
        if (kDO_LOCAL_TEST_EXPORT_DESKTOP) {
            utils.saveBlobOnDesktop(result, "nuxeo-pdfutils-test",
                    "test-images-withOverlayPNG-operation.pdf");
        }
    }

    @Test
    public void testWatermarkWithImageOperation_withBlob_customValues()
            throws Exception {

        File overlayPictureFile = FileUtils.getResourceFileFromContext(IMAGE_FOR_WATERMARK_PNG);
        FileBlob overlayPictureBlob = new FileBlob(overlayPictureFile);

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        ctx.setInput(pdfFileWithImagesBlob);
        chain = new OperationChain("testChain");

        ctx.put("theBlobImage", overlayPictureBlob);
        chain.add(WatermarkWithImageOp.ID).set("imageContextVarName",
                "theBlobImage").set("x", 200).set("y", "400").set("scale", 2.0);

        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);

        checkHasImage(result, IMAGE_FOR_WATERMARK_PNG_WIDTH,
                IMAGE_FOR_WATERMARK_PNG_HEIGHT);
        if (kDO_LOCAL_TEST_EXPORT_DESKTOP) {
            utils.saveBlobOnDesktop(result, "nuxeo-pdfutils-test",
                    "test-images-withOverlayPNG-operation.pdf");
        }
    }

    @Test
    public void testWatermarkWithImageOperation_withDocument_defaultValues()
            throws Exception {

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        ctx.setInput(pdfFileWithImagesBlob);
        chain = new OperationChain("testChain");

        chain.add(WatermarkWithImageOp.ID).set("imageDocRef",
                pngForWatermarkDoc.getId());

        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);

        if (kDO_LOCAL_TEST_EXPORT_DESKTOP) {
            utils.saveBlobOnDesktop(result, "nuxeo-pdfutils-test",
                    "test-images-withOverlayPNG-operation.pdf");
        }
    }

    @Test
    public void testWatermarkWithPdfOperation_withBlob() throws Exception {

        File overlayPdfFile = FileUtils.getResourceFileFromContext(PDF_FOR_WATERMARK);
        FileBlob overlayPdfBlob = new FileBlob(overlayPdfFile);

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        ctx.setInput(pdfFileWithImagesBlob);
        chain = new OperationChain("testChain");

        ctx.put("theBlobPdf", overlayPdfBlob);
        chain.add(WatermarkWithPDFOp.ID).set("pdfContextVarName", "theBlobPdf");

        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);

        utils.saveBlobOnDesktop(result, "nuxeo-pdfutils-test",
                "test-images-withOverlayPDF-operation.pdf");

        // checkHasImage(result, IMAGE_FOR_WATERMARK_PNG_WIDTH,
        // IMAGE_FOR_WATERMARK_PNG_HEIGHT);
        if (kDO_LOCAL_TEST_EXPORT_DESKTOP) {
            utils.saveBlobOnDesktop(result, "nuxeo-pdfutils-test",
                    "test-images-withOverlayPNG-operation.pdf");
        }
    }
}
