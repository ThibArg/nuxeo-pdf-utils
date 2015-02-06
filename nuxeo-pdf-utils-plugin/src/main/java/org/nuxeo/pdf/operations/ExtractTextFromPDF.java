/**
 *
 */

package org.nuxeo.pdf.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author fvadon
 */
@Operation(id=ExtractTextFromPDF.ID, category=Constants.CAT_DOCUMENT, label="ExtractTextFromPDF", description="")
public class ExtractTextFromPDF {

    public static final String ID = "ExtractTextFromPDF";

    @Param(name = "fileName", required = false)
    protected String fileName = "file:content";

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentModel input) {

        return null;
    }

}
