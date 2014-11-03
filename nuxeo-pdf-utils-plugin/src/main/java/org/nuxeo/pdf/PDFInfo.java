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
package org.nuxeo.pdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.BadSecurityHandlerException;
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.picture.api.BlobHelper;

/**
 *
 * About page sizes, see http://www.prepressure.com/pdf/basics/page-boxes for
 * details. Here, we get the info from the first page only. The dimensions are
 * in points. Divide by 72 to get it in inches
 *
 * Permissions are returned after decryption (if any)
 *
 * @since 5.9.6
 */
public class PDFInfo {

    protected Blob pdfBlob;

    protected PDDocument pdfDoc;

    protected String password;

    protected int numberOfPages = -1;

    protected float mediaBoxWidthInPoints = 0.0f;

    protected float mediaBoxHeightInPoints = 0.0f;

    protected float cropBoxWidthInPoints = 0.0f;

    protected float cropBoxHeightInPoints = 0.0f;

    protected long fileSize = -1;

    protected boolean isEncrypted;

    protected String author = "";

    protected String contentCreator = "";

    protected String fileName = "";

    protected String keywords = "";

    protected String pageLayout = "";

    protected String pdfVersion = "";

    protected String producer = "";

    protected String subject = "";

    protected String title = "";

    protected String xmp;

    protected Calendar creationDate = null;

    protected Calendar modificationDate = null;

    protected boolean alreadyParsed = false;

    public PDFInfo(Blob inBlob) throws ClientException {
        this(inBlob, null, false);
    }

    public PDFInfo(Blob inBlob, boolean inRunNow) throws ClientException {

        this(inBlob, null, inRunNow);
    }

    public PDFInfo(Blob inBlob, String inPassword, boolean inRunNow)
            throws ClientException {

        pdfBlob = inBlob;
        password = inPassword;
        if (inRunNow) {
            run(false);
        }
    }

    protected String checkNotNull(String inValue) {
        return inValue == null ? "" : inValue;
    }

    public void run(boolean inGetXMP) throws ClientException {

        // In case the caller calls several time the run() method
        if (!alreadyParsed) {

            fileName = pdfBlob.getFilename();
            // Getting the file size os ok only if the blob is already backed by
            // a
            // File. If it is pure Stream, we give up
            File pdfFile = BlobHelper.getFileFromBlob(pdfBlob);
            if (pdfFile == null) {
                fileSize = -1;
            } else {
                fileSize = pdfFile.length();
            }

            try {
                pdfDoc = PDDocument.load(pdfBlob.getStream());

                isEncrypted = pdfDoc.isEncrypted();
                if (isEncrypted) {
                    pdfDoc.openProtection(new StandardDecryptionMaterial(
                            password));
                }

                numberOfPages = pdfDoc.getNumberOfPages();
                PDDocumentCatalog docCatalog = pdfDoc.getDocumentCatalog();
                pageLayout = checkNotNull(docCatalog.getPageLayout());
                pdfVersion = "" + pdfDoc.getDocument().getVersion();

                PDDocumentInformation docInfo = pdfDoc.getDocumentInformation();
                author = checkNotNull(docInfo.getAuthor());
                creationDate = docInfo.getCreationDate();
                contentCreator = checkNotNull(docInfo.getCreator());
                keywords = checkNotNull(docInfo.getKeywords());
                modificationDate = docInfo.getModificationDate();
                producer = checkNotNull(docInfo.getProducer());
                subject = checkNotNull(docInfo.getSubject());
                title = checkNotNull(docInfo.getTitle());

                // Getting dimension is a bit tricky
                mediaBoxWidthInPoints = -1;
                mediaBoxHeightInPoints = -1;
                cropBoxWidthInPoints = -1;
                cropBoxHeightInPoints = -1;
                List<PDPage> allPages = docCatalog.getAllPages();
                boolean gotMediaBox = false;
                boolean gotCropBox = false;
                for (PDPage page : allPages) {

                    if (page != null) {
                        PDRectangle r = page.findMediaBox();
                        if (r != null) {
                            mediaBoxWidthInPoints = r.getWidth();
                            mediaBoxHeightInPoints = r.getHeight();
                            gotMediaBox = true;
                        }
                        r = page.findCropBox();
                        if (r != null) {
                            cropBoxWidthInPoints = r.getWidth();
                            cropBoxHeightInPoints = r.getHeight();
                            gotCropBox = true;
                        }
                    }
                    if (gotMediaBox && gotCropBox) {
                        break;
                    }
                }

                if (inGetXMP) {
                    xmp = null;
                    PDMetadata metadata = docCatalog.getMetadata();
                    if (metadata != null) {
                        xmp = "";
                        InputStream xmlInputStream = metadata.createInputStream();

                        InputStreamReader isr = new InputStreamReader(
                                xmlInputStream);
                        BufferedReader reader = new BufferedReader(isr);
                        String line;
                        do {
                            line = reader.readLine();
                            if (line != null) {
                                xmp += line + "\n";
                            }
                        } while (line != null);
                        reader.close();
                    }
                }

            } catch (IOException | BadSecurityHandlerException
                    | CryptographyException e) {
                throw new ClientException(/*
                                           * "Cannot get PDF info: " +
                                           * e.getMessage(),
                                           */e);
            } finally {
                if (pdfDoc != null) {
                    try {
                        pdfDoc.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                    pdfDoc = null;
                }
                alreadyParsed = true;
            }
        }
    }

    public HashMap<String, String> toHashMap() {

        // LinkedHashMap because I like this order :-)
        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");

        result.put("File name", fileName);
        result.put("File size", "" + fileSize);
        result.put("PDF version", pdfVersion);
        result.put("Page count", "" + numberOfPages);
        result.put("Page Size", "" + mediaBoxWidthInPoints + " x "
                + mediaBoxHeightInPoints + " points");
        result.put("Page width", "" + mediaBoxWidthInPoints);
        result.put("Page height", "" + mediaBoxHeightInPoints);
        result.put("Page Layout", pageLayout);
        result.put("Title", title);
        result.put("Author", author);
        result.put("Subject", subject);
        result.put("PDF Producer", producer);
        result.put("Content creator", contentCreator);
        result.put("Creation date", dateFormat.format(creationDate.getTime()));
        result.put("Modification date",
                dateFormat.format(modificationDate.getTime()));

        // "Others"
        result.put("Encrypted", "" + isEncrypted);
        result.put("Keywords", keywords);
        result.put("Media box width", "" + mediaBoxWidthInPoints);
        result.put("Media box height", "" + mediaBoxHeightInPoints);
        result.put("Crop box width", "" + cropBoxWidthInPoints);
        result.put("Crop box height", "" + cropBoxHeightInPoints);

        return result;
    }

    @Override
    public String toString() {

        return toHashMap().toString();
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public float getMediaBoxWidthInPoints() {
        return mediaBoxWidthInPoints;
    }

    public float getMediaBoxHeightInPoints() {
        return mediaBoxHeightInPoints;
    }

    public float getCropBoxWidthInPoints() {
        return cropBoxWidthInPoints;
    }

    public float getCropBoxHeightInPoints() {
        return cropBoxHeightInPoints;
    }

    public long getFileSize() {
        return fileSize;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public String getAuthor() {
        return author;
    }

    public String getContentCreator() {
        return contentCreator;
    }

    public String getFileName() {
        return fileName;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getPageLayout() {
        return pageLayout;
    }

    public String getPdfVersion() {
        return pdfVersion;
    }

    public String getProducer() {
        return producer;
    }

    public String getSubject() {
        return subject;
    }

    public String getTitle() {
        return title;
    }

    public String getXmp() {
        return xmp;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public Calendar getModificationDate() {
        return modificationDate;
    }

}
