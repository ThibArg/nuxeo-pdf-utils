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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.picture.api.BlobHelper;

/**
 * The class will parse the info embedded in a PDF, and return them either
 * globally (<code>toHashMap()</code> or <code>toString()</code>) or via
 * individual getters.
 * <p>
 * The PDF is parsed only at first call to <code>run()</code>, values are cached
 * during first call.
 * <p>
 * About page sizes, see http://www.prepressure.com/pdf/basics/page-boxes for
 * details. Here, we get the info from the first page only. The dimensions are
 * in points. Divide by 72 to get it in inches.
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

    protected boolean doXMP = false;

    protected String xmp;

    protected Calendar creationDate = null;

    protected Calendar modificationDate = null;

    protected boolean alreadyParsed = false;

    // LinkedHashMap just because wanted to keep the order
    // (nothing requested, really)
    protected LinkedHashMap<String, String> cachedMap;

    /**
     * Constructor with a Blob
     *
     * @param inBlob
     */
    public PDFInfo(Blob inBlob) {
        this(inBlob, null);
    }

    /**
     * Constructor for Blob + encrypted PDF
     *
     * @param inBlob
     * @param inPassword if the pdf is encrypted
     */
    public PDFInfo(Blob inBlob, String inPassword) {
        pdfBlob = inBlob;
        password = inPassword;
    }

    /**
     * Constructor with a DocumentModel. Uses the default
     * <code>file:content</code> xpath to get the blob from the document.
     *
     * @param inDoc
     */
    public PDFInfo(DocumentModel inDoc) {
        this(inDoc, null, null);
    }

    /**
     * Constructor for DocumentModel + encrypted PDF
     * <p>
     * If <inXPath</code> is <code>null</code> or "", it is set to the default
     * <code>file:content</code> value.
     *
     * @param inDoc
     * @param inXPath
     * @param inPassword
     */
    public PDFInfo(DocumentModel inDoc, String inXPath, String inPassword) {

        if (inXPath == null || inXPath.isEmpty()) {
            inXPath = "file:content";
        }

        pdfBlob = (Blob) inDoc.getPropertyValue(inXPath);
        password = inPassword;
    }

    /**
     * If set to true, parsing will extract PDF.
     * <p>
     * The value cannot be modified if <code>run()</code> already has been
     * called.
     *
     * @param true to extract XMP
     *
     * @since 5.9.5
     */
    public void setParseWithXMP(boolean inValue) {
        if (alreadyParsed && doXMP != inValue) {
            throw new ClientException(
                    "Value of 'doXML' cannot be modified after the blob has been already parsed.");
        }
        doXMP = inValue;
    }

    protected String checkNotNull(String inValue) {
        return inValue == null ? "" : inValue;
    }

    /**
     * After building the object with the correct constructor, and after
     * possibly having set some parsing property (<code>setParseWithXMP()</code>
     * for example), this method will extract the information from the PDF.
     * <p>
     * After extraction, caller get the info: Either all of them (
     * <code>toHashMap()</code> or <code>toString()</code>) or individual info
     * (see all getters)
     *
     * @throws ClientException
     *
     * @since 5.9.5
     */
    public void run() throws ClientException {

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
                contentCreator = checkNotNull(docInfo.getCreator());
                keywords = checkNotNull(docInfo.getKeywords());
                creationDate = docInfo.getCreationDate();
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

                if (doXMP) {
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

    /**
     * Return all and every parsed info in a String <code>HashMap</code>.
     * <p>
     * Possible values are:
     * <ul>
     * <li>File name</li>
     * <li>File size</li>
     * <li>PDF version</li>
     * <li>Page count</li>
     * <li>Page size</li>
     * <li>Page width</li>
     * <li>Page height</li>
     * <li>Page layout</li>
     * <li>Title</li>
     * <li>Author</li>
     * <li>Subject</li>
     * <li>PDF producer</li>
     * <li>Content creator</li>
     * <li>Creation date</li>
     * <li>Modification date</li>
     * <li>Encrypted</li>
     * <li>Keywords</li>
     * <li>Media box width</li>
     * <li>Media box height</li>
     * <li>Crop box width</li>
     * <li>Crop box height</li>
     * </ul>
     *
     * @return the HashMap of all the info as Strings
     *
     * @since 5.9.5
     */
    public HashMap<String, String> toHashMap() {

        // Parse if needed
        run();

        if (cachedMap == null) {
            cachedMap = new LinkedHashMap<String, String>();

            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss");

            cachedMap.put("File name", fileName);
            cachedMap.put("File size", "" + fileSize);
            cachedMap.put("PDF version", pdfVersion);
            cachedMap.put("Page count", "" + numberOfPages);
            cachedMap.put("Page size", "" + mediaBoxWidthInPoints + " x "
                    + mediaBoxHeightInPoints + " points");
            cachedMap.put("Page width", "" + mediaBoxWidthInPoints);
            cachedMap.put("Page height", "" + mediaBoxHeightInPoints);
            cachedMap.put("Page layout", pageLayout);
            cachedMap.put("Title", title);
            cachedMap.put("Author", author);
            cachedMap.put("Subject", subject);
            cachedMap.put("PDF producer", producer);
            cachedMap.put("Content creator", contentCreator);
            if (creationDate != null) {
                cachedMap.put("Creation date",
                        dateFormat.format(creationDate.getTime()));
            } else {
                cachedMap.put("Creation date", "");
            }
            if (modificationDate != null) {
                cachedMap.put("Modification date",
                        dateFormat.format(modificationDate.getTime()));
            } else {
                cachedMap.put("Modification date", "");
            }

            // "Others"
            cachedMap.put("Encrypted", "" + isEncrypted);
            cachedMap.put("Keywords", keywords);
            cachedMap.put("Media box width", "" + mediaBoxWidthInPoints);
            cachedMap.put("Media box height", "" + mediaBoxHeightInPoints);
            cachedMap.put("Crop box width", "" + cropBoxWidthInPoints);
            cachedMap.put("Crop box height", "" + cropBoxHeightInPoints);
        }

        return cachedMap;
    }

    /**
     * The <code>inMapping</code> map is a list of key=value pairs (well. it's a
     * HashMap :->) where the key is the xpath of the destination field, and the
     * value is the exact label of a PDF info as returned by
     * <code>toHashMap()</code>. For example:
     * <p>
     * <code><pre>
     * pdfinfo:title=Title
     * pdfinfo:producer=PDF Producer
     * pdfinfo:mediabox_width=Media box width
     * . . .
     * </pre></code>
     * <p>
     * If <code>inSave</code> is false, inSession can be null.
     *
     * @param inDoc
     * @param inMapping
     * @param inSave
     * @param inSession
     * @return
     *
     * @since 5.9.5
     */
    public DocumentModel toFields(DocumentModel inDoc,
            HashMap<String, String> inMapping, boolean inSave,
            CoreSession inSession) {

        // Parse if needed
        run();

        HashMap<String, String> values = toHashMap();
        for (String inXPath : inMapping.keySet()) {
            String value = values.get(inMapping.get(inXPath));
            inDoc.setPropertyValue(inXPath, value);
        }

        if (inSave) {
            inDoc = inSession.saveDocument(inDoc);
        }

        return inDoc;
    }

    /**
     * Wrapper for <code>toHashMap().toString()</code>
     */
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
