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

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
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

/**
 *
 */
@Operation(id=MergePDFsWithDocsInputOp.ID, category=Constants.CAT_CONVERSION, label="Document(s): Merge PDFs", description="The input document(s) always is(are) the first PDFs, and their pdf is read in the <code>xpath</code> field. The operation appends the blob referenced in the <code>toAppendVarName</code> Context variable. It then appends all the blobs stored in the <code>toAppendListVarName</code> Context variable. Returns the final pdf.")
public class MergePDFsWithDocsInputOp {

    public static final String ID = "Document.MergePDFs";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService autService;

    @Param(name = "xpath", required = false, values = {"file:content"})
    String xpath = "file:content";

    @Param(name = "toAppendVarName", required = false)
    protected String toAppendVarName;

    @Param(name = "toAppendListVarName", required = false)
    protected String toAppendListVarName;

    @Param(name = "fileName", required = false)
    protected String fileName;

    @OperationMethod
    public Blob run(DocumentModel inDoc) throws ClientException {

        Blob blob = (Blob) inDoc.getPropertyValue(xpath);

        return doMerge(blob);
    }

    @OperationMethod
    public Blob run(DocumentModelList inDocs) throws ClientException {

        BlobList blobs = new BlobList();

        for(DocumentModel doc : inDocs) {
            blobs.add( (Blob) doc.getPropertyValue(xpath) );
        }

        return doMerge(blobs);
    }

    protected Blob doMerge(Object input) throws ClientException {

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(input);

        OperationChain chain = new OperationChain("MergePDFsFromDocs");
        chain.add(MergePDFsWithBlobsInputOp.ID)
                .set("toAppendVarName", ctx.get("toAppendVarName"))
                .set("toAppendListVarName", ctx.get("toAppendListVarName"))
                .set("fileName", ctx.get("fileName"));

        try {
            return (Blob) autService.run(ctx, chain);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }
}
