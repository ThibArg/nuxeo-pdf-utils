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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.9.6
 */
public class PDFMerge {

    protected BlobList blobs = new BlobList();

    public PDFMerge(Blob inBlob) {
        addBlob(inBlob);
    }

    public void addBlob(Blob inBlob) {
        blobs.add(inBlob);
        /*
        if (inBlob != null) {
            if ("application/pdf".equals(inBlob.getMimeType())) {
                blobs.add(inBlob);
            } else {
                throw new ClientException("Blob " + inBlob.getFilename()
                        + " is not a pdf (mimeType is <" + inBlob.getMimeType()
                        + ">)");
            }
        }
        */
    }

    public void addBlob(DocumentModel inDoc) {
        addBlob(inDoc, "file:content");
    }

    public void addBlob(DocumentModel inDoc, String inXPath) {

        if (inXPath == null || inXPath.isEmpty()) {
            inXPath = "file:content";
        }
        addBlob((Blob) inDoc.getPropertyValue(inXPath));
    }

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
