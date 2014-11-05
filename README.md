# nuxeo-pdf-utils
===

_(Work in progress)_

A set of quite usefull utilities for who needs to deal with PDFs. The following operations are added:

* **`PDF: Add Page Numbers`** (id `PDF.AddPageNumbers`)
  * Accepts a Blob, returns a Blob
  * The input blob must be a pdf
  * The returned blob icontains the page numbers
    * Notice the input blob is modified (no duplication is done before adding the numbers)
  * The following parameters let you tune the operation:
    * `startAtPage` (default: 1)
    * `startAtNumber` (default: 1)
    * `position`
      * Can be Bottom right, Bottom center, Bottom left, Top right, Top center, or Top left
      * Default: Bottom Right
    * `fontName` (default: Helvetica)
    * `fontSize` (default: 16)
    * `hex255Color`
      * Expressed as either 0xrrggbb or #rrggbb (case insensitive)
      * Default value: 0xffffff


* **`PDF: Extract Pages`** (id `PDF.AddPageNumbers`)
  * Accept either blob or a document as input
  * Returns a blob built with the extracted pages
  * If the input is a document, the `xpath` parameter must be used (default: `file:content`)
  * The following parameters let you tune the operation:
    * `startPage`
    * `endPage`
    * `fileName`
      * If not used, the filename will be the original file name plus the page range. For example, if the original name was "mydoc.pdf" and you extract pages 10 to 25, the resulting pdf will have a file name of "mydoc-10-25.pdf".
    * `pdfTitle`
      * If not used, title is not set
    * `pdfSubject`
      * If not used, subject is not set
    * `pdfAuthor`
      * If not used, author is not set

* **`PDF: Merge with Blob(s)`** (id `PDF.MergeWithBlobs`)


* **`PDF: PDF: Merge with Document(s)`** (id `PDF.MergeWithDocs`)


* **`PDF: Info to Fields`** (id `PDF.InfoToFields`)


## License
(C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.

All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Lesser General Public License
(LGPL) version 2.1 which accompanies this distribution, and is available at
http://www.gnu.org/licenses/lgpl-2.1.html

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

Contributors:
Thibaud Arguillere (https://github.com/ThibArg)

## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com) and packaged applications for Document Management, Digital Asset Management and Case Management. Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.
