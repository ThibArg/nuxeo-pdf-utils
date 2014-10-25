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
 *     Thibaut Arguillere
 */
package org.nuxeo.pdf;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.9.6
 */
public class PDFUtils {

    public static float DEFAULT_FONT_SIZE = 16.0f;

    public enum PAGE_NUMBER_POSITION {
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, TOP_LEFT, TOP_CENTER, TOP_RIGHT
    };

    public static int[] hex255ToRGB(String inHex) {
        int[] result = { 0, 0, 0 };

        if (inHex != null) {
            inHex = inHex.toLowerCase().replace("#", "").replace("0x", "");

            if (inHex.length() >= 6) {
                for (int i = 0; i < 3; i++) {
                    result[i] = Integer.parseInt(
                            inHex.substring(i * 2, i * 2 + 2), 16);
                }
            }
        }

        return result;
    }

    /*
     * This code assumes:
     *
     * - There is no page numbers already (it always draw the numbers)
     *
     * - The pdf is not rotated
     *
     * - Default values apply:
     *
     * * inStartAtPage and inStartAtNumber are set to 1 if they are passed < 1. If inStartAtPage is > number of pages it also is reset to 1
     *
     * * inFontName is set to "Helvetica" if "" or null
     *
     * * inHex255Color is set to black if "", null or if its length < 6
     *
     * * inPosition is set to BOTTOM_RIGHT if null
     */
    public static Blob addPageNumbers(Blob inBlob, int inStartAtPage,
            int inStartAtNumber, String inFontName, float inFontSize,
            String inHex255Color, PAGE_NUMBER_POSITION inPosition)
            throws IOException, COSVisitorException {

        Blob result = null;
        PDDocument doc = null;

        inStartAtPage = inStartAtPage < 1 ? 1 : inStartAtPage;
        int pageNumber = inStartAtNumber < 1 ? 1 : inStartAtNumber;
        inFontSize = inFontSize <= 0 ? DEFAULT_FONT_SIZE : inFontSize;

        int[] rgb = PDFUtils.hex255ToRGB(inHex255Color);

        try {
            doc = PDDocument.load(inBlob.getStream());
            List<?> allPages;
            PDFont font;
            int max;

            if (inFontName == null || inFontName.isEmpty()) {
                font = PDType1Font.HELVETICA;
            } else {
                font = PDType1Font.getStandardFont(inFontName);
                if (font == null) {
                    font = new PDType1Font(inFontName);
                }
            }

            allPages = doc.getDocumentCatalog().getAllPages();
            max = allPages.size();
            inStartAtPage = inStartAtPage > max ? 1 : inStartAtPage;
            for (int i = inStartAtPage; i <= max; i++) {
                String pageNumAsStr = "" + pageNumber;
                pageNumber += 1;

                PDPage page = (PDPage) allPages.get(i - 1);
                PDPageContentStream footercontentStream = new PDPageContentStream(
                        doc, page, true, true);

                float stringWidth = font.getStringWidth(pageNumAsStr)
                        * inFontSize / 1000f;
                float stringHeight = font.getFontDescriptor().getFontBoundingBox().getHeight()
                        * inFontSize / 1000;
                PDRectangle pageRect = page.findMediaBox();

                float xMoveAmount, yMoveAmount;

                if (inPosition == null) {
                    inPosition = PAGE_NUMBER_POSITION.BOTTOM_RIGHT;
                }
                switch (inPosition) {
                case BOTTOM_LEFT:
                    xMoveAmount = 10;
                    yMoveAmount = pageRect.getLowerLeftY() + 10;
                    break;

                case BOTTOM_CENTER:
                    xMoveAmount = (pageRect.getUpperRightX() / 2)
                            - (stringWidth / 2);
                    yMoveAmount = pageRect.getLowerLeftY() + 10;
                    break;

                case TOP_LEFT:
                    xMoveAmount = 10;
                    yMoveAmount = pageRect.getHeight() - stringHeight - 10;
                    break;

                case TOP_CENTER:
                    xMoveAmount = (pageRect.getUpperRightX() / 2)
                            - (stringWidth / 2);
                    yMoveAmount = pageRect.getHeight() - stringHeight - 10;
                    break;

                case TOP_RIGHT:
                    xMoveAmount = pageRect.getUpperRightX() - 10 - stringWidth;
                    yMoveAmount = pageRect.getHeight() - stringHeight - 10;
                    break;

                // Bottom-right is the default
                default:
                    xMoveAmount = pageRect.getUpperRightX() - 10 - stringWidth;
                    yMoveAmount = pageRect.getLowerLeftY() + 10;
                    break;
                }

                footercontentStream.beginText();
                footercontentStream.setFont(font, inFontSize);
                footercontentStream.moveTextPositionByAmount(xMoveAmount,
                        yMoveAmount);
                footercontentStream.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
                footercontentStream.drawString(pageNumAsStr);
                footercontentStream.endText();
                footercontentStream.close();
            }

            File tempFile = File.createTempFile("pdfutils-", ".pdf");
            doc.save(tempFile);
            result = new FileBlob(tempFile);
            Framework.trackFile(tempFile, result);

        } finally {
            if (doc != null) {
                doc.close();
            }
        }

        return result;
    }
}
