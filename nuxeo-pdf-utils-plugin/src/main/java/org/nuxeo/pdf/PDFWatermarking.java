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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDExtendedGraphicsState;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.pdf.PDFUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * This class adds a watermark to a Blob holding a PDF. It never changes the
 * original Blob: It returns a watermarked copy of it.
 *
 * Setters return the PDFWatermark object so they can be chained
 *
 * @since 6.0
 */
public class PDFWatermarking {

    public static final String DEFAULT_FONT_FAMILY = "Helvetica";

    public static final float DEFAULT_FONT_SIZE = 36f;

    public static final int DEFAULT_TEXT_ROTATION = 0;

    public static final String DEFAULT_TEXT_COLOR = "#000000";

    public static final float DEFAULT_ALPHA = 0.5f;

    public static final float DEFAULT_X_POSITION = 0f;

    public static final float DEFAULT_Y_POSITION = 0f;

    public static final boolean DEFAULT_INVERT_Y = false;

    protected Blob blob = null;

    protected String text = null;

    protected String fontFamily = DEFAULT_FONT_FAMILY;

    protected float fontSize = DEFAULT_FONT_SIZE;

    protected int textRotation = DEFAULT_TEXT_ROTATION;

    protected String hex255Color = DEFAULT_TEXT_COLOR;

    protected float alphaColor = DEFAULT_ALPHA;

    protected float xPosition = DEFAULT_X_POSITION;

    protected float yPosition = DEFAULT_Y_POSITION;

    protected boolean invertY = DEFAULT_INVERT_Y;

    /**
     * Constructor
     *
     * @param inBlob
     */
    public PDFWatermarking(Blob inBlob) {

        blob = inBlob;
    }

    /**
     * Constructor
     *
     * @param inDoc
     * @param inXPath
     */
    public PDFWatermarking(DocumentModel inDoc, String inXPath) {

        blob = (Blob) inDoc.getPropertyValue(PDFUtils.checkXPath(inXPath));
    }

    /**
     * Adds the watermark to the Blob passed to the constructor.
     * <p>
     * If no setter has been used, the DEFAULT_nnn values apply
     * <p>
     * If
     * <code>text</text> is empty or null, a simple copy of the original Blob is returned
     * <p>
     * With thanks to the sample code found at https://issues.apache.org/jira/browse/PDFBOX-1176
     * and to Jack (https://jackson-brain.com/a-better-simple-pdf-stamper-in-java/)
     *
     * @return a new Blob with the watermark on each page
     *
     * @throws ClientException
     *
     * @since 6.0
     */
    public Blob watermark() throws ClientException {

        Blob result = null;
        PDDocument pdfDoc = null;
        PDPageContentStream contentStream = null;

        if (text == null || text.isEmpty()) {
            try {

                File tempFile = File.createTempFile("nuxeo-pdfwatermarking-",
                        ".pdf");
                blob.transferTo(tempFile);
                result = new FileBlob(tempFile);
                // Just duplicate the info
                result.setFilename(blob.getFilename());
                result.setMimeType(blob.getMimeType());
                result.setEncoding(blob.getEncoding());
                Framework.trackFile(tempFile, result);

                return result;

            } catch (IOException e) {
                throw new ClientException(e);
            }
        }

        // Set up the graphic state to handle transparency
        // Define a new extended graphic state
        PDExtendedGraphicsState extendedGraphicsState = new PDExtendedGraphicsState();
        // Set the transparency/opacity
        extendedGraphicsState.setNonStrokingAlphaConstant(alphaColor);

        try {

            pdfDoc = PDDocument.load(blob.getStream());
            PDFont font = PDType1Font.getStandardFont(fontFamily);
            int[] rgb = PDFUtils.hex255ToRGB(hex255Color);

            List<?> allPages = pdfDoc.getDocumentCatalog().getAllPages();
            for (int i = 0; i < allPages.size(); i++) {
                contentStream = null;

                PDPage page = (PDPage) allPages.get(i);
                PDRectangle pageSize = page.findMediaBox();

                PDResources resources = page.findResources();
                // Get the defined graphic states.
                HashMap<String, PDExtendedGraphicsState> graphicsStateDictionary = (HashMap<String, PDExtendedGraphicsState>) resources.getGraphicsStates();
                if (graphicsStateDictionary != null) {
                    graphicsStateDictionary.put("TransparentState",
                            extendedGraphicsState);
                    resources.setGraphicsStates(graphicsStateDictionary);
                } else {
                    Map<String, PDExtendedGraphicsState> m = new HashMap<>();
                    m.put("TransparentState", extendedGraphicsState);
                    resources.setGraphicsStates(m);
                }

                if (invertY) {
                    yPosition = pageSize.getHeight() - yPosition;
                }

                float stringWidth = font.getStringWidth(text) * fontSize
                        / 1000f;

                int pageRot = page.findRotation();
                boolean pageRotated = pageRot == 90 || pageRot == 270;
                boolean textRotated = textRotation != 0 && textRotation != 360;

                int totalRot = pageRot - textRotation;

                float pageWidth = pageRotated ? pageSize.getHeight()
                        : pageSize.getWidth();
                float pageHeight = pageRotated ? pageSize.getWidth()
                        : pageSize.getHeight();

                double centeredXPosition = pageRotated ? pageHeight / 2f
                        : (pageWidth - stringWidth) / 2f;
                double centeredYPosition = pageRotated ? (pageWidth - stringWidth) / 2f
                        : pageHeight / 2f;

                contentStream = new PDPageContentStream(pdfDoc, page, true,
                        true, true);
                contentStream.beginText();

                contentStream.setFont(font, fontSize);
                contentStream.appendRawCommands("/TransparentState gs\n");
                contentStream.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);

                if (pageRotated) {
                    contentStream.setTextRotation(Math.toRadians(totalRot),
                            centeredXPosition, centeredYPosition);
                } else if (textRotated) {
                    contentStream.setTextRotation(Math.toRadians(textRotation),
                            xPosition, yPosition);
                } else {
                    contentStream.setTextTranslation(xPosition, yPosition);
                }

                contentStream.drawString(text);

                contentStream.endText();
                contentStream.close();
            }

            result = PDFUtils.saveInTempFile(pdfDoc);
            pdfDoc.close();
            pdfDoc = null;

        } catch (IOException | COSVisitorException e) {
            throw new ClientException(e);
        } finally {
            if (contentStream != null) {
                try {
                    contentStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (pdfDoc != null) {
                try {
                    pdfDoc.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return result;
    }

    /*
     * Utilities to handle null in setProperties()
     */
    protected float stringToFloat(String inValue) {
        if(inValue == null) {
            return 0f;
        }

        return Float.valueOf(inValue);
    }
    protected int stringToInt(String inValue) {
        if(inValue == null) {
            return 0;
        }

        return Integer.valueOf(inValue);
    }
    protected boolean stringToBoolean(String inValue) {
        if(inValue == null) {
            return false;
        }

        return Boolean.valueOf(inValue);
    }

    public PDFWatermarking setProperties(HashMap<String, String> inProps) {

        setFontFamily(inProps.get("fontFamily"));
        setFontSize(stringToFloat(inProps.get("fontSize")));
        setTextRotation(stringToInt(inProps.get("textRotation")));
        setTextColor(inProps.get("hex255Color"));
        setAlphaColor(stringToFloat(inProps.get("alphaColor")));
        setXPosition(stringToInt(inProps.get("xPosition")));
        setYPosition(stringToInt(inProps.get("yPosition")));
        setInvertY(stringToBoolean(inProps.get("invertY")));

        return this;
    }

    public String getText() {
        return text;
    }

    public PDFWatermarking setText(String inValue) {
        text = inValue;
        return this;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public PDFWatermarking setFontFamily(String inValue) {
        fontFamily = inValue == null || inValue.isEmpty() ? DEFAULT_FONT_FAMILY
                : inValue;
        return this;
    }

    public float getFontSize() {
        return fontSize;
    }

    public PDFWatermarking setFontSize(float inValue) {
        fontSize = inValue < 1f ? DEFAULT_FONT_SIZE : inValue;
        return this;
    }

    public int getTextRotation() {
        return textRotation;
    }

    public PDFWatermarking setTextRotation(int inValue) {
        textRotation = inValue < 1 ? DEFAULT_TEXT_ROTATION : inValue;
        return this;
    }

    public String getTextColor() {
        return hex255Color;
    }

    public PDFWatermarking setTextColor(String inValue) {
        hex255Color = inValue == null || inValue.isEmpty() ? DEFAULT_TEXT_COLOR
                : inValue;
        return this;
    }

    public float getAlphaColor() {
        return alphaColor;
    }

    public PDFWatermarking setAlphaColor(float inValue) {
        alphaColor = inValue < 0f || inValue > 1.0f ? DEFAULT_ALPHA : inValue;
        return this;
    }

    public float getXPosition() {
        return xPosition;
    }

    public PDFWatermarking setXPosition(float inValue) {
        xPosition = inValue < 0 ? DEFAULT_X_POSITION : inValue;
        return this;
    }

    public float getYPosition() {
        return yPosition;
    }

    public PDFWatermarking setYPosition(float inValue) {
        yPosition = inValue < 0 ? DEFAULT_Y_POSITION : inValue;
        return this;
    }

    public boolean isInvertY() {
        return invertY;
    }

    public PDFWatermarking setInvertY(boolean inValue) {
        invertY = inValue;
        return this;
    }
}
