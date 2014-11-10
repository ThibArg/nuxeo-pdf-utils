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

package org.nuxeo.pdf.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.pdf.PDFWatermarking;

/**
 * Returns a <i>new</i> blob combining the input pdf and the image (stored in a
 * blob) referenced either by <code>imageContextVarName</code> (name of a
 * Context variable holding the blob) of by <code>imageDoc</code> (path or ID of
 * a document whose <code>file:content</code> is the image to use). If the value
 * of <code>scale</code> is <= 0 it is reset to 1.0.
 *
 */
@Operation(id = WatermarkWithImageOp.ID, category = Constants.CAT_CONVERSION, label = "PDF: Watermark with Image", description = "Returns a <i>new</i> blob combining the input pdf and the image (stored in a blob) referenced either by <code>imageContextVarName</code> (name of a Context variable holding the blob) of by <code>imageDoc</code> (path or ID of a document whose <code>file:content</code> is the image to use). If <code>scale</code> is <= 0 it is reste to 1.0")
public class WatermarkWithImageOp {

    public static final String ID = "PDF.WatermarkWithImage";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext context;

    @Param(name = "imageContextVarName", required = true)
    String imageContextVarName;

    @Param(name = "imageDocRef", required = false)
    String imageDocRef;

    @Param(name = "x", required = false, values = { "0" })
    protected long x;

    @Param(name = "y", required = false, values = { "0" })
    protected long y;

    @Param(name = "scale", required = false, values = { "1.0" })
    protected Double scale;

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob inBlob) {

        Blob result = null;
        Blob blobImage = null;

        if (imageContextVarName != null && !imageContextVarName.isEmpty()) {
            blobImage = (Blob) context.get(imageContextVarName);
        } else if (imageDocRef != null && !imageDocRef.isEmpty()) {
            DocumentModel doc = null;
            if (imageDocRef.startsWith("/")) {
                doc = session.getDocument(new PathRef(imageDocRef));
            } else {
                doc = session.getDocument(new IdRef(imageDocRef));
            }
            blobImage = (Blob) doc.getPropertyValue("file:content");
        }

        PDFWatermarking pdfw = new PDFWatermarking(inBlob);
        result = pdfw.watermarkWithImage(blobImage, (int) x, (int) y,
                scale.floatValue());

        return result;
    }

}
