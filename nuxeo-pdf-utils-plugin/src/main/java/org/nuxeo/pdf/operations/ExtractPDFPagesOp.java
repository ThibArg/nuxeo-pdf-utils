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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.pdf.PDFPageExtractor;

/**
 * Extract pages <code>startPage</code> to <code>endPage</code> (inclusive) from
 * the input object. If a Blob is used as input, the
 * <code>xpath</xpath> parameter is not used. If <code>title</code>,
 * <code>subject</code> and <code>author</code> are optional")
 */
@Operation(id = ExtractPDFPagesOp.ID, category = Constants.CAT_CONVERSION, label = "PDF: Extract Pages", description = "Extract pages <code>startPage</code> to <code>endPage</code> (inclusive) from the input object. If a Blob is used as input, the <code>xpath</xpath> parameter is not used. If <code>title</code>, <code>subject</code> and <code>author</code> are optional")
public class ExtractPDFPagesOp {

    public static final String ID = "PDF.ExtractPages";

    @Context
    protected CoreSession session;

    @Param(name = "startPage", required = true)
    protected long startPage;

    @Param(name = "endPage", required = true)
    protected long endPage;

    @Param(name = "fileName", required = false)
    protected String fileName;

    @Param(name = "pdfTitle", required = false)
    protected String pdfTitle;

    @Param(name = "pdfSubject", required = false)
    protected String pdfSubject;

    @Param(name = "pdfAuthor", required = false)
    protected String pdfAuthor;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath;

    @OperationMethod
    public Blob run(Blob inBlob) {

        PDFPageExtractor pe = new PDFPageExtractor(inBlob);

        Blob result = pe.extract((int) startPage, (int) endPage, fileName,
                pdfTitle, pdfSubject, pdfAuthor);

        return result;
    }

    @OperationMethod
    public Blob run(DocumentModel inDoc) {

        PDFPageExtractor pe = new PDFPageExtractor(inDoc, xpath);

        Blob result = pe.extract((int) startPage, (int) endPage, fileName,
                pdfTitle, pdfSubject, pdfAuthor);

        return result;
    }
}
