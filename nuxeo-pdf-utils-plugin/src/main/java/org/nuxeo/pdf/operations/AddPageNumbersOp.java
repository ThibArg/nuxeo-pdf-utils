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
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.pdf.PDFUtils;
import org.nuxeo.pdf.PDFUtils.PAGE_NUMBER_POSITION;

/**
 * Add page numbers to the pDF
 */
@Operation(id = AddPageNumbersOp.ID, category = Constants.CAT_CONVERSION, label = "Add Page Numbers to PDF", description = "")
public class AddPageNumbersOp {

    public static final String ID = "PDF.AddPageNumbers";

    @Context
    protected CoreSession session;

    @Param(name = "startAtPage", required = false, values = { "1" })
    protected long startAtPage = 1;

    @Param(name = "inStartAtNumber", required = false, values = { "1" })
    protected long inStartAtNumber = 1;

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

        Blob result = PDFUtils.addPageNumbers(inBlob, (int) startAtPage,
                (int) inStartAtNumber, fontName, fontSize, hex255Color, pos);
        result.setFilename(inBlob.getFilename());

        return result;
    }

}
