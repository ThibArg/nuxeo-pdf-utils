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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.nuxeo.ecm.core.api.Blob;

/**
 *
 *
 * @since TODO
 */
public class TestUtils {

    protected ArrayList<PDDocument> createdPDDocs = new ArrayList<PDDocument>();

    protected ArrayList<File> createdTempFiles = new ArrayList<File>();

    public TestUtils() {

    }

    public void track(PDDocument inPdfDoc) {
        createdPDDocs.add(inPdfDoc);
    }

    public void untrack(PDDocument inPdfDoc) {
        createdPDDocs.remove(inPdfDoc);
    }

    public void track(File inFile) {
        createdTempFiles.add(inFile);
    }

    public void untrack(File inFile) {
        createdTempFiles.remove(inFile);
    }

    public void cleanup() {

        for (PDDocument pdfDoc : createdPDDocs) {
            try {
                pdfDoc.close();
            } catch (Exception e) {
                // Nothing
            }
        }
        createdPDDocs = new ArrayList<PDDocument>();

        for (File f : createdTempFiles) {
            try {
                f.delete();
            } catch (Exception e) {
                // Nothing
            }
        }
        createdTempFiles = new ArrayList<File>();
    }

    /*
     * Utility. Extract the text in the given page(s)
     */
    protected String extractText(PDDocument inDoc, int startPage, int inEndPage)
            throws IOException {

        String txt = "";

        inEndPage = inEndPage < startPage ? startPage : inEndPage;

        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(startPage);
        stripper.setEndPage(inEndPage);
        txt = stripper.getText(inDoc);

        return txt;
    }

    public String calculateMd5(File inFile) throws IOException {

        String md5;

        FileInputStream fis = new FileInputStream(inFile);
        md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
        fis.close();

        return md5;
    }

    public String calculateMd5(Blob inBlob) throws IOException {

        String md5;

        md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(inBlob.getStream());

        return md5;
    }

    /*
     * This one is for local test with human checking :-). Requires inFolderName to exist on your Desktop
     */
    protected void saveBlobOnDesktop(Blob inBlob, String inFolderName, String inFileName)
            throws IOException {
        File destFile = new File(System.getProperty("user.home"),
                "Desktop/" + inFolderName + "/" + inFileName);
        inBlob.transferTo(destFile);
    }
}
