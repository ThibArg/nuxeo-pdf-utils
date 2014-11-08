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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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

    // WARNING: If you change this pdf, a lot of tests will fail (count pages,
    // text in the pdf, ...)
    private static final String THE_PDF = "files/13-pages-no-page-numbers.pdf";

    private static final String NOT_A_PDF = "files/Travel-3.jpg";

    protected static final String ENCRYPTED_PDF = "files/13-pages-no-page-numbers-encrypted-pwd-nuxeo.pdf";

    protected static final String ENCRYPTED_PDF_PWD = "nuxeo";

    protected static final String PDF_WITH_XMP = "files/XMP-Embedding.pdf";

    protected File pdfFile;

    protected FileBlob pdfFileBlob;

    // For visually testing the result
    public boolean kDO_LOCAL_TEST_EXPORT_DESKTOP = false;

    protected DocumentModel testDocsFolder, pdfDocModel;

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
        assertEquals("Mac OS X 10.10 Quartz PDFContext",
                values.get("PDF producer"));

    }

    @Test
    public void testPDFInfoShouldFailOnEncryptedPDFAndBadPassword()
            throws Exception {

        File f = FileUtils.getResourceFileFromContext(ENCRYPTED_PDF);
        FileBlob fb = new FileBlob(f);

        try {
            PDFInfo info = new PDFInfo(fb, "toto");
            info.run();
            assertTrue(
                    "Parsing the file with a wrong password should have failed",
                    false);
        } catch (Exception e) {
            // this error comes from CryptographyException
            // Warning: Maybe if PDFBox version change, the message changes too.
            assertTrue(e.getMessage().indexOf("CryptographyException") > -1);
            assertTrue(e.getMessage().indexOf(
                    "The supplied password does not match") > -1);
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

    @Test
    public void testInfoToField() throws Exception {

        PDFInfo info = new PDFInfo(pdfDocModel);

        // Let's put things here and there in dublincore as placeholder. Real
        // life should have dedicated schema, likely
        HashMap<String, String> mapping = new HashMap<String, String>();
        mapping.put("dc:coverage", "PDF version");
        mapping.put("dc:description", "Page count");
        mapping.put("dc:expired", "Creation date");// a date
        mapping.put("dc:format", "Page layout");
        mapping.put("dc:issued", "Modification date");// a date
        mapping.put("dc:language", "Title");
        mapping.put("dc:nature", "Author");
        mapping.put("dc:publisher", "Subject");
        mapping.put("dc:rights", "PDF producer");
        mapping.put("dc:source", "Content creator");

        // We don't save the document, just check the fileds
        DocumentModel result = info.toFields(pdfDocModel, mapping, false, null);

        // PDF Version
        assertEquals("1.3", result.getPropertyValue("dc:coverage"));
        // Page Count
        assertEquals("13", result.getPropertyValue("dc:description"));
        // Page layout
        assertEquals("SinglePage", result.getPropertyValue("dc:format"));
        // Title
        assertEquals("Untitled 3", result.getPropertyValue("dc:language"));
        // Author
        assertEquals("", result.getPropertyValue("dc:nature"));
        // Subject
        assertEquals("", result.getPropertyValue("dc:publisher"));
        // PDF producer
        assertEquals("Mac OS X 10.9.5 Quartz PDFContext",
                result.getPropertyValue("dc:rights"));
        // Content creator
        assertEquals("TextEdit", result.getPropertyValue("dc:source"));

        // Check dates. This pdf has creation date == modif. date == 2014-10-22
        // 20:00:00
        GregorianCalendar expectedDate = new GregorianCalendar(2014, 9, 22, 20,
                0, 0);
        Calendar cal = (Calendar) result.getPropertyValue("dc:expired");// Creation
                                                                        // date
        assertEquals(expectedDate, cal);
        cal = (Calendar) result.getPropertyValue("dc:issued");// Modification
                                                              // date
        assertEquals(expectedDate, cal);

    }
}
