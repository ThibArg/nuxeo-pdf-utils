# nuxeo-pdf-utils
===

A set of utilities for who needs to deal with PDFs from a [nuxeo](http://nuxeo.com) application. The following operations are added:


## Operations
These operations can be used in Studio after importing their JSON definitions to the Automation registry.

_A quick reminder: To get the JSON definition of an operation, you can install the plug-in, start nuxeo server then go to {server:port}/nuxeo/site/automation/doc. All available operations are listed, find the one you are looking for and follow the links to get its JSON definition._

* **`PDF: Add Page Numbers`** (id `PDF.AddPageNumbers`)
  * Accepts a Blob or a document, returns a Blob
  * The input blob must be a pdf
  * The returned blob contains the page numbers, displayed using the parameters (position, font, ...)
    * Notice the input blob is _not_ modified, a copy (+ page numbers) is returned
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
    * `xpath`
      * If the input is a document, `xpath` parameter is used (default value: `file:content`). If the input parameter is a blob, `xpath` is just ignored.


* **`PDF: Extract Pages`** (id `PDF.AddPageNumbers`)
  * Accept either a blob or a document as input
  * Returns a blob built with the extracted pages
  * If the input is a document, the `xpath` parameter must be used (default: `file:content`)
  * The following parameters let you tune the operation:
    * `startPage`
      * If < 1 => realigned to 1
      * If > `endPage` or > number of pages, a blank PDF is returned
    * `endPage`
      * If > number of pages, it is realigned to the number of pages
    * `fileName`
      * If not used, the filename will be the original file name plus the page range. For example, if the original name was "mydoc.pdf" and you extract pages 10 to 25, the resulting pdf will have a file name of "mydoc-10-25.pdf".
    * `pdfTitle`
      * If not used, title is not set
      * Warning: This is not the `dc:title`. It is the title as stored in the metadata of the PDF.
    * `pdfSubject`
      * If not used, subject is not set
    * `pdfAuthor`
      * If not used, author is not set

* **`PDF: Merge with Blob(s)`** (id `PDF.MergeWithBlobs`)
  * This operation merges all the blobs in a specific order (see below) and returns the final, merged PDF. Some properties (subject, ...) can also be set at the same time (optional)
  * The order of the PDF is the following:
    * First, the input blob
    * Then, the blob referenced by the Context variable whose name is `toAppendVarName`
    * Then the blobs referenced as a `BlobList` by the Context variable whose name is `toAppendListVarName`
    * And then the blobs stored in the documents whose IDs are referenced as a `String List` by the Context variable whose name is `toAppendDocIDsVarName`
      * The `xpath` parameter is used to get the blob in each document
      * Optional. Default value is `file:content`
    * **Important**: The operation expects the _Context variable names_, _not the values_ of the variables. For example in Studio, say you have a multivalued String field named `myschema:the_ids`. It stores IDs of documents (typically, filled by the user using a "Multiple Documents Suggestion Widget"). In an Automation Chain, to merge the PDF embedded in a these documents with an input blob you would write (see we use `listArticles`, not `@{listArticles}`):
    ```
    . . . previous operations . . .
    Set Context Variable
      name: listArticles
      value: @{Document["myschema:the_ids"]}
    . . . 
    PDF: Merge with Blob(s)
      ..other parameters
      toAppendDocIDsVarName: listArticles
    ```
    * These parameters are optional. Still, you probably want to use at least one of them :-)


* **`PDF: PDF: Merge with Document(s)`** (id `PDF.MergeWithDocs`)
  * See the documentation of `PDF: Merge with Blob(s)`
  * The difference is that the input is a document. The operation extracts the blob from the `xpath` field. Notice that it is ok for this blob to be null, the operation will still merge all the other blobs referenced in the parameters


* **`PDF: Info to Fields`** (id `PDF.InfoToFields`)
  * Extract the info of the PDF and put them in the fields referenced by the `properties` parameter, return the modified document. If there is no blob or if the blob is not a PDF, all the values referenced in `properties` are cleared (set to empty string, 0, ...)
  * Parameters:
    * `xpath`: The xpath of the blob to handle in the document. Default value is `file:content`
    * `save`: If true, the document is saved after its fields have been populated
    * `properties`
      * A `key=value` list (one key-value pair/line), where `key` is the xpath of the destination field and `value` is one of the following (case sensitive):
    ```
    File name
    File size
    PDF version
    Page count
    Page size
    Page width
    Page height
    Page layout
    Title
    Author
    Subject
    PDF producer
    Content creator
    Creation date
    Modification date
    Encrypted
    Keywords
    Media box width
    Media box height
    Crop box width
    Crop box height
    ```
      * For example, say you have an `InfoOfPDF` schema, prefix `iop`, with misc. fields. You could write:
    ```
    iop:pdf_version=PDF version
    iop:page_count=Page count
    iop:page_size=Page size
    ...etc...
    ```


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
