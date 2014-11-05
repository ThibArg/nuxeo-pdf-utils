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
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.pdf.PDFPageNumbering;
import org.nuxeo.pdf.PDFPageNumbering.PAGE_NUMBER_POSITION;
import org.nuxeo.pdf.PDFUtils;

/**
 * Add page numbers to the PDF.
 *
 * Accepts either a Blob(s) or Document(s) as input
 */
@Operation(id = AddPageNumbersOp.ID, category = Constants.CAT_CONVERSION, label = "PDF: Add Page Numbers", description = "Add the page numbers to the pdf, using the misc. parameters. If the input is a document, the <code>xpath</code> parameter is used (default value: <code>file:content</code>. If the input parameter is a blob, <code>xpath</code> parameter is just ignored.")
public class AddPageNumbersOp {

    public static final String ID = "PDF.AddPageNumbers";

    @Context
    protected CoreSession session;

    @Param(name = "startAtPage", required = false, values = { "1" })
    protected long startAtPage = 1;

    @Param(name = "startAtNumber", required = false, values = { "1" })
    protected long startAtNumber = 1;

    @Param(name = "position", required = false, widget = Constants.W_OPTION, values = {
            "Bottom right", "Bottom center", "Bottom left", "Top right",
            "Top center", "Top left" })
    String position = "Bottom right";

    @Param(name = "fontName", required = false, values = { "Helvetica" })
    protected String fontName = "Helvetica";

    @Param(name = "fontSize", required = false, values = { "16" })
    protected long fontSize = 16;

    @Param(name = "hex255Color", required = false, values = { "0xffffff" })
    protected String hex255Color = "0xffffff";

    @Param(name = "xpath", required = false, values = {"file:content" })
    protected String xpath = "file:content";

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob inBlob) throws IOException, COSVisitorException {

        PAGE_NUMBER_POSITION pos;
        switch (position.toLowerCase()) {
        case "bottom center":
            pos = PAGE_NUMBER_POSITION.BOTTOM_CENTER;
            break;

        case "bottom left":
            pos = PAGE_NUMBER_POSITION.BOTTOM_LEFT;
            break;

        case "top right":
            pos = PAGE_NUMBER_POSITION.TOP_RIGHT;
            break;

        case "top center":
            pos = PAGE_NUMBER_POSITION.TOP_CENTER;
            break;

        case "top left":
            pos = PAGE_NUMBER_POSITION.TOP_LEFT;
            break;

        default:
            pos = PAGE_NUMBER_POSITION.BOTTOM_RIGHT;
            break;
        }

        PDFPageNumbering pn = new PDFPageNumbering(inBlob);
        Blob result = pn.addPageNumbers((int) startAtPage,
                (int) startAtNumber, fontName, fontSize, hex255Color, pos);
        result.setFilename(inBlob.getFilename());

        return result;
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public Blob run(DocumentModel inDoc) throws COSVisitorException, IOException {

        Blob theBlob = (Blob) inDoc.getPropertyValue(PDFUtils.checkXPath(xpath));
        return run(theBlob);
    }
}
