/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     thibaud
 */

package org.nuxeo.pdf;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;

/**
 * The blob(s) in input will always be the first blob(s), to which we append:
 * (1) blobToAppendVarName, if used, then (2) toAppendListVarName if used
 *
 */
@Operation(id = MergePDFsOp.ID, category = Constants.CAT_CONVERSION, label = "Merge PDFs", description = "The input blob(s) always is(are) the first PDFs. The operation appends the blob referenced in the <code>toAppendVarName</code> Context variable, then, appends all the blobs stored in the <code>toAppendListVarName</code> Context variable. Returns the final pdf.")
public class MergePDFsOp {

    public static final String ID = "Blob.MergePDFs";

    @Context
    protected OperationContext ctx;

    @Param(name = "toAppendVarName", required = false)
    protected String toAppendVarName;

    @Param(name = "toAppendListVarName", required = false)
    protected String toAppendListVarName;

    @Param(name = "fileName", required = false)
    protected String fileName;

    @OperationMethod
    public Blob run(Blob inBlob) throws COSVisitorException, IOException {

        PDFMerge pdfm = new PDFMerge(inBlob);

        return doMerge(pdfm);
    }

    @OperationMethod
    public Blob run(BlobList inBlobs) throws COSVisitorException, IOException {

        PDFMerge pdfm = new PDFMerge(inBlobs.get(0));
        int max = inBlobs.size();
        for (int i = 1; i < max; i++) {
            pdfm.addBlob(inBlobs.get(i));
        }

        return doMerge(pdfm);
    }

    protected Blob doMerge(PDFMerge inMergeTool) throws COSVisitorException, IOException {

        // The first blob(s) has(have) already been added

        // Add the simple blob
        if (toAppendVarName != null && !toAppendVarName.isEmpty()) {
            inMergeTool.addBlob((Blob) ctx.get(toAppendVarName));
        }

        // Add the blob list
        if (toAppendListVarName != null && !toAppendListVarName.isEmpty()) {

            Collection<?> list = null;
            if (ctx.get(toAppendListVarName) instanceof Object[]) {
                list = Arrays.asList((Object[]) ctx.get(toAppendListVarName));
            } else if (ctx.get(toAppendListVarName) instanceof Collection<?>) {
                list = (Collection<?>) ctx.get(toAppendListVarName);
            } else {
                throw new UnsupportedOperationException(
                        ctx.get(toAppendListVarName).getClass()
                                + " is not a Collection");
            }

            for (Object value : list) {
                inMergeTool.addBlob((Blob) value);
            }
        }

        // Merge
        return inMergeTool.merge(fileName);
    }
}