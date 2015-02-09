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
 *     Frederic Vadon
 */

package org.nuxeo.pdf.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.pdf.PDFTextExtractor;
import org.nuxeo.pdf.operations.ExtractTextFromPDFOp;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "nuxeo-pdf-utils-plugin" })
public class PDFTextExtractorTest {

    private static final String THE_PDF = "files/fakecontract.pdf";

    protected File pdfFile;

    protected FileBlob pdfFileBlob;

    TestUtils utils;

    protected DocumentModel testDocsFolder;

    protected DocumentModel testDoc;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    /*
     * The file must have 13 pages
     */
    protected void checkPDFBeforeTest() throws IOException {

        PDDocument doc = PDDocument.load(pdfFile);
        assertNotNull(doc);
        utils.track(doc);

        assertEquals(6, doc.getNumberOfPages());

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
        pdfFileBlob = new FileBlob(pdfFile);

        testDoc = utils.createDocumentFromFile(coreSession, testDocsFolder,
                "File", THE_PDF);

        checkPDFBeforeTest();
    }

    @After
    public void cleanup() {

        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();

        utils.cleanup();
    }

    @Test
    public void testExtractText() throws IOException {
        PDFTextExtractor textExtractor = new PDFTextExtractor(pdfFileBlob);
        String extractedText = textExtractor.getAllExtractedLines();
        assertNotNull(extractedText);
        String extractedLine = textExtractor.extractLineOf("Contract Number: ");
        assertEquals("Contract Number: 123456789", extractedLine);
        extractedLine = textExtractor.extractLineOf("Toto");
        assertNull(extractedLine);
        extractedLine = textExtractor.extractLineOf("13.1");
        assertNotNull(extractedLine);

        extractedLine = textExtractor.extractLastPartOfLine("Contract Number: ");
        assertEquals("123456789", extractedLine);
    }

    @Test
    public void testExtractTextOperation() throws Exception {

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        ctx.setInput(testDoc);

        chain = new OperationChain("testChain");
        chain.add(ExtractTextFromPDFOp.ID).set("save", true).set("targetxpath",
                "dc:description").set("patterntofind", "Contract Number: ").set(
                "removepatternfromresult", false);
        DocumentModel documentModified = (DocumentModel) automationService.run(ctx, chain);
        assertEquals("Contract Number: 123456789", documentModified.getPropertyValue("dc:description"));

        chain = new OperationChain("testChain");
        chain.add(ExtractTextFromPDFOp.ID).set("save", true).set("targetxpath",
                "dc:description").set("patterntofind", "toto").set(
                "removepatternfromresult", false);
        documentModified = (DocumentModel) automationService.run(ctx, chain);
        assertNull(documentModified.getPropertyValue("dc:description"));

        chain = new OperationChain("testChain");
        chain.add(ExtractTextFromPDFOp.ID).set("save", true).set("targetxpath",
                "dc:description").set("patterntofind", "Contract Number: ").set(
                "removepatternfromresult", true);
        documentModified = (DocumentModel) automationService.run(ctx, chain);
        assertEquals("123456789", documentModified.getPropertyValue("dc:description"));



    }

}
