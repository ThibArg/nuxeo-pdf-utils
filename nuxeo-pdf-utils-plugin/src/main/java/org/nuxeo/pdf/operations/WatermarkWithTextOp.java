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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.pdf.PDFWatermarking;

/**
 * Return a <i>new</i> blob combining the input pdf and the
 * <code>watermark</code> text, using the different <code>properties</code> (default values
 * apply). Notice <code>xPosition</code> and <code>yPosition</code> start at the
 * bottom-left corner. If <code>watermark</code> is empty, a simple copy o the
 * input blob is returned
 *
 * Properties must be one or more of the following (in parenthesis, the default
 * value if the property is not used): <code>fontFamily</code> (Helvetica),
 * <code>fontSize</code> (36), <code>textRotation</code> (0),
 * <code>hex255Color</code> (#000000), <code>alphaColor</code> (0.5),
 * <code>xPosition</code> (0), <code>yPosition</code> (0), <code>invertY</code>
 * (false)
 *
 */
@Operation(id = WatermarkWithTextOp.ID, category = Constants.CAT_CONVERSION, label = "PDF: Watermark with Text", description = "Return a <i>new</i> blob combining the input pdf and the <code>watermark</code> text, using the different properties. Properties must be one or more of the following (in parenthesis, the default value if the property is not used): <code>fontFamily</code> (Helvetica), <code>fontSize</code> (36), <code>textRotation</code> (0), <code>hex255Color</code> (#000000), <code>alphaColor</code> (0.5), <code>xPosition</code> (0), <code>yPosition</code> (0), <code>invertY</code> (false). <code>xPosition</code> and <code>yPosition</code> start at the <i>bottom-left</i> corner of the page. If <code>watermark</code> is empty, a simple copy o the input blob is returned")
public class WatermarkWithTextOp {

    public static final String ID = "PDF.WatermarkWithText";

    @Param(name = "watermark", required = true)
    protected String watermark;

    @Param(name = "properties", required = false)
    protected Properties properties;

    @OperationMethod(collector=BlobCollector.class)
    public Blob run(Blob inBlob) {

        Blob result = null;

        PDFWatermarking pdfw = new PDFWatermarking(inBlob);
        pdfw.setText(watermark).setProperties(properties);
        result = pdfw.watermark();

        return result;
    }

}
