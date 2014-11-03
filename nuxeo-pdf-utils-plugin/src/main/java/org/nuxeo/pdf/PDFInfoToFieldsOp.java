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
import java.util.HashMap;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * If there is no blob of if the blob is not a pdf document, we empty the
 * values.
 *
 * IMPORTANT: We don't check if the blob is a PDF or not. If it is not, this
 * will likely lead to PDFBox errors.
 *
 * For values to use in the properties parameter, see PDFInfo#toHashMap
 */
@Operation(id = PDFInfoToFieldsOp.ID, category = Constants.CAT_DOCUMENT, label = "PDF info to Fields", description = "")
public class PDFInfoToFieldsOp {

    public static final String ID = "Document.PDFInfoToFields";

    @Context
    protected CoreSession session;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    // The map has the xpath as key and the metadata property as value. For
    // exampe, say we have a custom pdfinfo schema:
    // pdfinfo:title=Title
    // pdfinfo:producer=PDF Producer
    // pdfinfo:mediabox_width=Media box width
    // . . .
    @Param(name = "properties", required = false)
    protected Properties properties;

    @Param(name = "save", required = false, values = { "true" })
    protected boolean save = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel inDoc) throws IOException {

        // Get the blob
        // If there is no blob, we empty all the values
        Blob theBlob = null;
        theBlob = (Blob) inDoc.getPropertyValue(xpath);
        if (theBlob == null) {
            for (String inXPath : properties.keySet()) {
                inDoc.setPropertyValue(inXPath, "");
            }
        } else {
            PDFInfo info = new PDFInfo(theBlob, true);
            HashMap<String, String> values = info.toHashMap();
            for (String inXPath : properties.keySet()) {
                String value = values.get(properties.get(inXPath));
                inDoc.setPropertyValue(inXPath, value);
            }
        }

        // Save the document
        if (save) {
            session.saveDocument(inDoc);
        }

        return inDoc;
    }
}
