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

package org.nuxeo.pdf.operations;

import java.io.IOException;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.pdf.PDFMerge;

/**
 * The input document(s) always is(are) the first PDF(s), and each pdf is read
 * in the <code>xpath</code> field. If
 * <code>xpath</xpath>, it is set to the default value <code>file:content</code>
 * <p>
 * Also, it is of for the input document to have no blob (it is just ignored and the blobs in the misc. variables will be merged)
 * <p>
 * The operation appends:
 * <ul>
 * <li>First, The blob referenced in the <code>toAppendVarName</code> Context
 * variable.</li>
 * <li>Then, it appends all the blobs stored in the
 * <code>toAppendListVarName</code> Context variable</li>
 * <li>And last, it appends the blobs stored in the docs whose IDs are passed in
 * <code>toAppendDocIDsVarName</code>, using the <code>xpath</code> parameter</li>
 * </ul>
 * <p>
 * All variable names are optional: You can pass only
 * <code>toAppendVarName</code>, or <code>toAppendVarName</code> and
 * <code>toAppendDocIDsVarName</code>, or ...
 * <p>
 * Returns the final pdf.
 */
@Operation(id = MergePDFsWithDocsInputOp.ID, category = Constants.CAT_CONVERSION, label = "Document(s): Merge PDFs", description = "The input document(s) always is(are) the first PDFs, and their pdf is read in the <code>xpath</code> field (but it is ok for the input doc to have no blob). The operation appends the blob referenced in the <code>toAppendVarName</code> Context variable. It then appends all the blobs stored in the <code>toAppendListVarName</code> Context variable. It then append the blobs stored in the docs whose IDs are passed in <code>toAppendDocIDsVarName</code> (the same <code>xpath</code> is used). Returns the final pdf.")
public class MergePDFsWithDocsInputOp {

    public static final String ID = "Document.MergePDFs";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService autService;

    @Param(name = "xpath", required = false, values = { "file:content" })
    String xpath = "file:content";

    @Param(name = "toAppendVarName", required = false)
    protected String toAppendVarName;

    @Param(name = "toAppendListVarName", required = false)
    protected String toAppendListVarName;

    @Param(name = "toAppendDocIDsVarName", required = false)
    protected String toAppendDocIDsVarName;

    @Param(name = "fileName", required = false)
    protected String fileName;

    @Param(name = "pdfTitle", required = false)
    protected String pdfTitle;

    @Param(name = "pdfSubject", required = false)
    protected String pdfSubject;

    @Param(name = "pdfAuthor", required = false)
    protected String pdfAuthor;

    @OperationMethod
    public Blob run(DocumentModel inDoc) throws ClientException {

        PDFMerge pdfm = new PDFMerge(inDoc, xpath);

        return doMerge(pdfm);
    }

    @OperationMethod
    public Blob run(DocumentModelList inDocs) throws ClientException {

        PDFMerge pdfm = new PDFMerge(inDocs, xpath);

        return doMerge(pdfm);
    }

    protected Blob doMerge(PDFMerge inMergeTool) throws ClientException {

        // Append the single blob
        if (toAppendVarName != null && !toAppendVarName.isEmpty()) {
            inMergeTool.addBlob((Blob) ctx.get(toAppendVarName));
        }

        // Append the blob list
        if (toAppendListVarName != null && !toAppendListVarName.isEmpty()) {

            if (ctx.get(toAppendListVarName) instanceof BlobList) {
                inMergeTool.addBlobs((BlobList) ctx.get(toAppendListVarName));
            } else {
                throw new ClientException(
                        ctx.get(toAppendListVarName).getClass()
                                + " is not a Collection");
            }
        }

        // Append a list of Documents via their IDs
        if (toAppendDocIDsVarName != null && !toAppendDocIDsVarName.isEmpty()) {
            if (ctx.get(toAppendDocIDsVarName) instanceof String[]) {
                inMergeTool.addBlobs((String[]) ctx.get(toAppendDocIDsVarName),
                        xpath, session);
            } else {
                throw new ClientException(
                        ctx.get(toAppendDocIDsVarName).getClass()
                                + " is not a String[]");
            }
        }

        // Merge
        try {
            return inMergeTool.merge(fileName, pdfTitle, pdfSubject, pdfAuthor);
        } catch (COSVisitorException | IOException e) {
            throw new ClientException(e);
        }
    }
}
