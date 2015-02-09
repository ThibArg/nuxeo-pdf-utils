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
 *     Thiabud Arguillere
 */
package org.nuxeo.pdf;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Extract pages from a PDF
 *
 * @since 5.9.5
 */
public class PDFTextExtractor {

    private static Log log = LogFactory.getLog(PDFTextExtractor.class);

    protected Blob pdfBlob;

    protected String extractedAllAsString = null;

    private static final String END_OF_LINE = "\n";

    public PDFTextExtractor(Blob inBlob) {

        pdfBlob = inBlob;
    }

    /**
     * Constructor with a <code>DocumentModel</code>. Default value for
     * <code>inXPath</code> (if passed <code>null</code> or "", if
     * <code>file:content</code>.
     *
     * @param inDoc
     * @param inXPath
     */
    public PDFTextExtractor(DocumentModel inDoc, String inXPath) {

        if (inXPath == null || inXPath.isEmpty()) {
            inXPath = "file:content";
        }
        pdfBlob = (Blob) inDoc.getPropertyValue(inXPath);
    }

    public String getAllExtractedLines() throws IOException {

        PDDocument pdfDoc = null;
        PDFTextStripper stripper = new PDFTextStripper();

        if (extractedAllAsString == null) {
            try {
                pdfDoc = PDDocument.load(pdfBlob.getStream());
                extractedAllAsString = stripper.getText(pdfDoc);

            } catch (IOException e) {
                throw new ClientException(e);
            } finally {
                if (pdfDoc != null) {
                    try {
                        pdfDoc.close();
                    } catch (IOException e) {
                        log.error("Error closing the PDDocument", e);
                    }
                }

            }
        }
        return extractedAllAsString;
    }

    public String extractLineOf(String inString) throws IOException {
        String extractedLine = null;
        int lineBegining = getAllExtractedLines().indexOf(inString);
        int lineEnd;
        if (lineBegining != -1) {
            lineEnd = getAllExtractedLines().indexOf(END_OF_LINE, lineBegining);
            extractedLine = getAllExtractedLines().substring(lineBegining, lineEnd).trim();
        }

        return extractedLine;
    }

    public String extractLastPartOfLine(String string) throws IOException {
        String extractedLine = null;
        extractedLine = extractLineOf(string);
        if (extractedLine != null) {
            extractedLine = extractedLine.substring(string.length(), extractedLine.length());
        }
        return extractedLine;
    }

}
