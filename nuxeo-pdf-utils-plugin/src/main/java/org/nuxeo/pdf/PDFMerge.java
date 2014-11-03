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

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.9.6
 */
public class PDFMerge {

    protected BlobList blobs = new BlobList();

    /*
     * Constructors accept a blob, a list of blobs, a DocumentModel, or a list of DocumentModels
     */
    public PDFMerge(Blob inBlob) {
        addBlob(inBlob);
    }

    public PDFMerge(BlobList inBlobs) {
        addBlobs(inBlobs);
    }

    public PDFMerge(DocumentModel inDoc, String inXPath) {
        addBlob(inDoc, inXPath);
    }

    public PDFMerge(DocumentModelList inDocs, String inXPath) {
        addBlobs(inDocs, inXPath);
    }

    // The original usecase actually :-)
    public PDFMerge(String[] inDocIDs, String inXPath, CoreSession inSession) {
        addBlobs(inDocIDs, inXPath, inSession);
    }



    /*
     * Appending accepts a single blob, a list of blobs, a single DocumentModel, or a list of DocumentModels
     */
    public void addBlob(Blob inBlob) {
        blobs.add(inBlob);
    }

    public void addBlobs(BlobList inBlobs) {
        blobs.addAll(inBlobs);
    }

    public void addBlob(DocumentModel inDoc, String inXPath) {

        if (inXPath == null || inXPath.isEmpty()) {
            inXPath = "file:content";
        }
        addBlob((Blob) inDoc.getPropertyValue(inXPath));
    }

    public void addBlobs(DocumentModelList inDocs, String inXPath) {

        for(DocumentModel doc : inDocs) {
            addBlob(doc, inXPath);
        }
    }

    public void addBlobs(String [] inDocIDs, String inXPath, CoreSession inSession) {
        for(String id : inDocIDs) {
            DocumentModel doc = inSession.getDocument( new IdRef(id) );
            addBlob(doc, inXPath);
        }
    }

    /*
     * Now we can merge ;->
     */
    public Blob merge(String inFileName) throws IOException, COSVisitorException {

        Blob finalBlob = null;

        switch(blobs.size()) {
        case 0:
            finalBlob = null;
            break;

        case 1:
            finalBlob = blobs.get(0);
            break;

        default:
            PDFMergerUtility ut = new PDFMergerUtility();
            for(Blob b : blobs) {
                ut.addSource(b.getStream());
            }

            File tempFile = File.createTempFile("mergepdf", ".pdf");
            ut.setDestinationFileName(tempFile.getAbsolutePath());

            ut.mergeDocuments();

            finalBlob = new FileBlob(tempFile);
            Framework.trackFile(tempFile, finalBlob);

            if(inFileName != null && !inFileName.isEmpty()) {
                finalBlob.setFilename(inFileName);
            } else {
                finalBlob.setFilename(blobs.get(0).getFilename());
            }
            finalBlob.setMimeType("application/pdf");
            break;

        }

        return finalBlob;
    }
}
