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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "nuxeo-pdf-utils-plugin" })
public class PDFInfoTest {

    private static final String THE_PDF = "files/13-pages-no-page-numbers.pdf";

    private static final String NOT_A_PDF = "files/Travel-3.jpg";

    protected static final String ENCRYPTED_PDF = "files/13-pages-no-page-numbers-encrypted-pwd-nuxeo.pdf";

    protected static final String ENCRYPTED_PDF_PWD = "nuxeo";

    protected static final String PDF_WITH_XMP = "files/XMP-Embedding.pdf";

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
    public void setup() throws Exception {

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
    public void testPDFInfo() throws Exception {

        PDFInfo info = new PDFInfo(pdfFileBlob);
        assertNotNull(info);

        info.run();
        HashMap<String, String> values = info.toHashMap();
        assertNotNull(values);

        // check some values
        assertEquals("1.3", values.get("PDF version"));
        assertEquals(67122, Long.valueOf(values.get("File size")).longValue());
        assertEquals("SinglePage", values.get("Page layout"));
        assertEquals("TextEdit", values.get("Content creator"));

    }

    @Test
    public void testPDFInfoShouldFailOnNonPDFBlob() throws Exception {

        File f = FileUtils.getResourceFileFromContext(NOT_A_PDF);
        FileBlob fb = new FileBlob(f);

        try {
            PDFInfo info = new PDFInfo(fb);
            info.run();
            assertTrue("Parsing the non-pdf file should have failed", false);
        } catch (Exception e) {
            // All good
        }
    }

    @Test
    public void testPDFInfoOnEncryptedPDF() throws Exception {

        File f = FileUtils.getResourceFileFromContext(ENCRYPTED_PDF);
        FileBlob fb = new FileBlob(f);

        PDFInfo info = new PDFInfo(fb, ENCRYPTED_PDF_PWD);
        assertNotNull(info);

        info.run();
        HashMap<String, String> values = info.toHashMap();
        assertNotNull(values);

        // check some values
        assertEquals("true", values.get("Encrypted"));
        assertEquals("1.4", values.get("PDF version"));
        assertEquals(67218, Long.valueOf(values.get("File size")).longValue());
        assertEquals("SinglePage", values.get("Page layout"));
        assertEquals("TextEdit", values.get("Content creator"));
        assertEquals("Mac OS X 10.10 Quartz PDFContext", values.get("PDF producer"));

    }

    @Test
    public void testPDFInfoShouldFailOnEncryptedPDFAndBadPassword() throws Exception {

        File f = FileUtils.getResourceFileFromContext(ENCRYPTED_PDF);
        FileBlob fb = new FileBlob(f);

        try {
            PDFInfo info = new PDFInfo(fb, "toto");
            info.run();
            assertTrue("Parsing the file with a wrong password should have failed", false);
        } catch (Exception e) {
            // this error comes from CryptographyException
            // Warning: Maybe if PDFBox version  change, the message changes too.
            assertTrue(e.getMessage().indexOf("CryptographyException") > -1);
            assertTrue(e.getMessage().indexOf("The supplied password does not match") > -1);
        }
    }

    @Test
    public void testPDFInfoGetXMP() throws Exception {

        File f = FileUtils.getResourceFileFromContext(PDF_WITH_XMP);
        FileBlob fb = new FileBlob(f);

        PDFInfo info = new PDFInfo(fb);
        assertNotNull(info);

        info.setParseWithXMP(true);
        info.run();
        String xmp = info.getXmp();
        assertNotNull(xmp);

        // We check we have a valid xml String
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xmp));
        Document doc = dBuilder.parse(is);
        assertEquals("rdf:RDF", doc.getDocumentElement().getNodeName());

    }
}
