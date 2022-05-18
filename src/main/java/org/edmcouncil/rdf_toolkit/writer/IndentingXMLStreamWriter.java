/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Enterprise Data Management Council
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.edmcouncil.rdf_toolkit.writer;

import org.apache.commons.lang3.StringEscapeUtils;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Adds indenting support to Java's built-in XMLStreamWriter
 */
public class IndentingXMLStreamWriter implements XMLStreamWriter {

    private class NamespaceContextImpl implements NamespaceContext {
        private HashMap<String, String> prefixToUriMap = new HashMap<>();
        private HashMap<String, List<String>> uriToPrefixMap = new HashMap<>();
        private String defaultNamespaceUri = null;
        private NamespaceContext suppliedContext = null;

        public void setPrefix(String prefix, String uri) {
            prefixToUriMap.put(prefix, uri);
            if (!uriToPrefixMap.containsKey(uri)) {
                uriToPrefixMap.put(uri, new ArrayList<>());
            }
            uriToPrefixMap.get(uri).add(prefix);
        }

        public void setDefaultNamespace(String uri) {
            defaultNamespaceUri = uri;
        }

        public void setNamespaceContext(NamespaceContext context) {
            suppliedContext = context;
        }

        @Override
        public String getNamespaceURI(String prefix) {
            if (suppliedContext != null) {
                String uri = suppliedContext.getNamespaceURI(prefix);
                if (uri != null) { return uri; }
            }
            return prefixToUriMap.get(prefix);
        }

        @Override
        public String getPrefix(String uri) {
            if (suppliedContext != null) {
                String prefix = suppliedContext.getPrefix(uri);
                if (prefix != null) { return prefix; }
            }
            List<String> prefixes = uriToPrefixMap.get(uri);
            if (prefixes.size() < 1) {
                return null;
            } else {
                return prefixes.get(0);
            }
        }

        @Override
        public Iterator getPrefixes(String uri) {
            if (suppliedContext != null) {
                Iterator prefixes = suppliedContext.getPrefixes(uri);
                if (prefixes != null) { return prefixes; }
            }
            List<String> prefixes = uriToPrefixMap.get(uri);
            if (prefixes.size() < 1) {
                return null;
            } else {
                return prefixes.iterator();
            }
        }
    }

    private OutputStream out = null;
    private Writer writer = null;
    private String encoding = "UTF-8";
    private String indent = "\t";
    private boolean useCompactAttributes = false;
    private IndentingWriter output = null;

    private boolean inStartElement = false;
    private boolean inEmptyStartElement = false;
    private boolean isAfterText = false;
    private Stack<String> elementNameStack = new Stack<>();
    private NamespaceContextImpl namespaceContext = new NamespaceContextImpl();

    public IndentingXMLStreamWriter(OutputStream out, String lineEnd) throws Exception {
        this(out, "UTF-8", null, false, lineEnd);
    }

    public IndentingXMLStreamWriter(OutputStream out, String encoding, String indent, boolean useCompactAttributes,
                                    String lineEnd) {
        this.out = out;
        if (encoding != null) { this.encoding = encoding; }
        if (indent != null) { this.indent = indent; }
        this.useCompactAttributes = useCompactAttributes;

        // Set up output writers
        writer = new OutputStreamWriter(this.out, Charset.forName(this.encoding));
        output = new IndentingWriter(writer);
        output.setIndentationString(this.indent);
        output.setLineEnd(lineEnd);
    }

    public IndentingXMLStreamWriter(Writer writer, String lineEnd) throws Exception {
        this(writer, null, true, lineEnd);
    }

    public IndentingXMLStreamWriter(Writer writer, String indent, boolean useCompactAttributes, String lineEnd) {
        this.out = null;
        this.writer = writer;
        if (writer instanceof OutputStreamWriter) {
            this.encoding = ((OutputStreamWriter)writer).getEncoding();
        } else {
            this.encoding = null; // encoding unknown
        }
        if (indent != null) { this.indent = indent; }
        this.useCompactAttributes = useCompactAttributes;

        // Set up output writers
        output = new IndentingWriter(this.writer);
        output.setIndentationString(this.indent);
        output.setLineEnd(lineEnd);
    }

    public String getXmlEncoding() { return encoding; }

    public String getJavaEncoding() {
        if (writer instanceof OutputStreamWriter) {
            return ((OutputStreamWriter)writer).getEncoding();
        } else {
            return null; // unknown
        }
    }

    public String getIndentationString() { return output.getIndentationString(); }

    public void setIndentationString(String indent) { output.setIndentationString(indent); }

    public int getIndentationLevel() { return output.getIndentationLevel(); }

    public void setIndentationLevel(int level) { output.setIndentationLevel(level); }

    public void increaseIndentation() { output.increaseIndentation(); }

    public void decreaseIndentation() { output.decreaseIndentation(); }

    public void writeEOL() throws XMLStreamException {
        try {
            output.writeEOL();
            isAfterText = false;
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        try {
            finishStartElement();
            inStartElement = true;
            inEmptyStartElement = false;
            elementNameStack.push(localName);
            writeEOL();
            output.write("<");
            output.write(localName);
            increaseIndentation();
            isAfterText = false;
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        try {
            finishStartElement();
            inStartElement = true;
            inEmptyStartElement = false;
            elementNameStack.push(localName);
            writeEOL();
            output.write("<");
            output.write(localName);
            increaseIndentation();
            isAfterText = false;
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        try {
            finishStartElement();
            inStartElement = true;
            inEmptyStartElement = false;
            String elementName = (((prefix != null) && (prefix.length() >= 1)) ? (prefix + ":") : "") + localName;
            elementNameStack.push(elementName);
            writeEOL();
            output.write("<");
            output.write(elementName);
            increaseIndentation();
            isAfterText = false;
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    private void finishStartElement() throws XMLStreamException {
        try {
            if (inStartElement) {
                if (inEmptyStartElement) {
                    output.write("/>");
                    inEmptyStartElement = false;
                } else {
                    output.write(">");
                }
                inStartElement = false;
            }
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        writeStartElement(localName);
        inEmptyStartElement = true;
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        writeStartElement(namespaceURI, localName);
        inEmptyStartElement = true;
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        writeStartElement(prefix, localName, namespaceURI);
        inEmptyStartElement = true;
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        try {
            boolean isEmpty = inEmptyStartElement; // check if the current element is an empty element
            finishStartElement();
            decreaseIndentation();
            String elementName = elementNameStack.pop();
            if (!isEmpty) {
                if (!isAfterText) { writeEOL(); }
                output.write("</");
                output.write(elementName);
                output.write(">");
            }
            isAfterText = false;
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        try {
            output.flush();
            inEmptyStartElement = false;
            inStartElement = false;
            isAfterText = false;
            assert(elementNameStack.empty());
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void close() throws XMLStreamException {
        try {
            output.flush();
            output.close();
            if (out != null) { writer.close(); }
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void flush() throws XMLStreamException {
        try {
            output.flush();
            writer.flush();
            if (out != null) { out.flush(); }
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    public void writeStartAttribute(String localName) throws XMLStreamException {
        try {
            if (useCompactAttributes) { output.write(' '); } else { writeEOL(); }
            output.write(localName);
            output.write("=\"");
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    public void writeStartAttribute(String namespaceURI, String localName) throws XMLStreamException {
        try {
            if (useCompactAttributes) { output.write(' ');; } else { writeEOL(); }
            output.write(localName);
            output.write("=\"");
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    public void writeStartAttribute(String prefix, String namespaceURI, String localName) throws XMLStreamException {
        try {
            if (useCompactAttributes) { output.write(' '); } else { writeEOL(); }
            if ((prefix != null) && (prefix.length() >= 1)) {
                output.write(prefix);
                output.write(":");
            }
            output.write(localName);
            output.write("=\"");
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    public void writeAttributeCharacters(String text) throws XMLStreamException {
        try {
            String escapedText = StringEscapeUtils.escapeXml10(text);
            output.write(escapedText.replaceAll("\\s+"," ").trim()); // do attribute whitespace normalisation
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    public void writeAttributeEntityRef(String entityName) throws XMLStreamException {
        try {
            if ((entityName != null) && (entityName.length() >= 1)) {
                output.write("&");
                output.write(entityName);
                output.write(";");
            }
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    public void endAttribute() throws XMLStreamException {
        try {
            output.write("\"");
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        writeStartAttribute(localName);
        writeAttributeCharacters(value);
        endAttribute();
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        writeStartAttribute(namespaceURI, localName);
        writeAttributeCharacters(value);
        endAttribute();
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        writeStartAttribute(prefix, namespaceURI, localName);
        writeAttributeCharacters(value);
        endAttribute();
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        try {
            String escapedNamespaceURI = StringEscapeUtils.escapeXml10(namespaceURI);
            writeEOL();
            output.write("xmlns:");
            output.write(prefix);
            output.write("=\"");
            output.write(escapedNamespaceURI);
            output.write("\"");
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        try {
            String escapedNamespaceURI = StringEscapeUtils.escapeXml10(namespaceURI);
            writeEOL();
            output.write("xmlns=\"");
            output.write(escapedNamespaceURI);
            output.write("\"");
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        try {
            finishStartElement();
            output.write("<!--");
            output.write(data);
            output.write("-->");
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        try {
            finishStartElement();
            output.write("<?");
            output.write(target);
            output.write("?>");
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        try {
            output.write("<?");
            output.write(target);
            output.write(" ");
            output.write(data);
            output.write("?>");
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        try {
            finishStartElement();
            output.write("<[CDATA[");
            output.write(data);
            output.write("]]>");
            isAfterText = true;
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        try {
            output.write(dtd);
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    public void startDTD(String rootElementName) throws XMLStreamException {
        try {
            output.write("<!DOCTYPE ");
            output.write(rootElementName);
            output.write(" [");
            increaseIndentation();
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    public void endDTD() throws XMLStreamException {
        try {
            decreaseIndentation();
            writeEOL();
            output.write("]>");
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    public void writeDtdEntity(String name, String value) throws XMLStreamException {
        try {
            writeEOL();
            output.write("<!ENTITY ");
            output.write(name);
            output.write(" \"");
            output.write(StringEscapeUtils.escapeXml10(value));
            output.write("\">");
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
        try {
            finishStartElement();
            if ((name != null) && (name.length() >= 1)) {
                output.write("&");
                output.write(name);
                output.write(";");
            }
            isAfterText = true;
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        setIndentationLevel(0);
        writeProcessingInstruction("xml");
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        setIndentationLevel(0);
        writeProcessingInstruction("xml", "version=\"" + version + "\"");
    }

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        // Note: more encoding translations may be appropriate here.
        setIndentationLevel(0);
        String xmlEncoding = encoding;
        if ("UTF8".equals(encoding)) { xmlEncoding = "UTF-8"; }
        if ("UTF16".equals(encoding)) { xmlEncoding = "UTF-16"; }
        writeProcessingInstruction("xml", "version=\"" + version + (xmlEncoding == null ? "" : ("\" encoding=\"" + xmlEncoding)) + "\"");
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        try {
            finishStartElement();
            output.write(StringEscapeUtils.escapeXml10(text));
            isAfterText = true;
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        try {
            finishStartElement();
            output.write(StringEscapeUtils.escapeXml10(String.copyValueOf(text, start, len)));
            isAfterText = true;
        } catch (Throwable t) {
            throw new XMLStreamException(t);
        }
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return namespaceContext.getPrefix(uri);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        namespaceContext.setPrefix(prefix, uri);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        namespaceContext.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        namespaceContext.setNamespaceContext(context);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return null; // no properties at present
    }
}
