package no.met.metadataeditor.dataTypes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.helpers.DefaultHandler;

class TemplateHandler extends DefaultHandler {

    final String EDT = "http://www.met.no/schema/metadataeditor/editorDataTypes";
    Deque<EditorVariable> edtElements;
    Deque<String> fullPathElements;
    Deque<String> finalPathElements; // without EDT-elements
    // xpath to find an EditorVariable-attribute
    Map<String, Deque<EditorVariable>> attributeXPath;
    private Map<String, EditorVariable> resultConfig;
    StringBuffer elementContent;
    private Map<String, String> namespacePrefixes;

    @Override
    public void startDocument() throws SAXException {
        edtElements  = new ArrayDeque<EditorVariable>();

        fullPathElements = new ArrayDeque<String>();
        finalPathElements = new ArrayDeque<String>();
        resultConfig = null;
        attributeXPath = new HashMap<String, Deque<EditorVariable>>();
        elementContent = new StringBuffer();
        namespacePrefixes = new HashMap<String, String>();
    }

    private static String variableAddAttributes(EditorVariable ev, Attributes atts) throws SAXException {
        String maxOccurs = atts.getValue("maxOccurs");
        if (maxOccurs == null) {
            ev.setMaxOccurs(1);
        }else if ("unbounded".equals(maxOccurs)) {
            ev.setMaxOccurs(Integer.MAX_VALUE);
        } else {
            ev.setMaxOccurs(Integer.parseInt(maxOccurs));
        }
        String minOccurs = atts.getValue("minOccurs");
        if (minOccurs == null) {
            ev.setMinOccurs(1);
        } else {
            ev.setMinOccurs(Integer.parseInt(minOccurs));
        }
        String res = atts.getValue("resource");
        if (res != null) {
            URI uri;
            try {
                uri = new URI(res);
            } catch (URISyntaxException e) {
                throw new SAXNotRecognizedException(e.toString());
            }
            ev.addResource("default", uri);
        }

        return atts.getValue("varName");
    }

    private void addStandardEDT(DataAttributes da, String nsUri, String lName, Attributes atts) throws SAXException {
        EditorVariable ev = new EditorVariable(da);
        String varName = variableAddAttributes(ev, atts);
        fullPathElements.addLast(getTemplateQName(nsUri, String.format("%s[@varName='%s']",lName, varName)));

        ev.setTemplateXPath(StringUtils.join(fullPathElements.iterator(), "/"));
        edtElements.getLast().addChild(varName, ev);
        edtElements.addLast(ev);

        for (String key : ev.getAttrsType().keySet()) {
            if (! attributeXPath.containsKey(key)) {
                attributeXPath.put(key, new ArrayDeque<EditorVariable>());
            }
            attributeXPath.get(key).addLast(ev);
        }
    }

    /**
     * check if one of the EditorVariable Attributes are placed in an attribute
     * @param atts
     */
    private void searchEditorAttributes(Attributes atts) {
        Set<String> keys = new HashSet<String>(attributeXPath.keySet());
        for (String key : keys) {
            Pattern keyPattern = Pattern.compile("\\s*\\Q$"+key+"\\E\\s*");
            for (int i = 0; i < atts.getLength(); ++i) {
                if (keyPattern.matcher(atts.getValue(i)).matches()) {
                    String xPath = org.apache.commons.lang3.StringUtils.join(finalPathElements.iterator(), "/");
                    xPath += "@"+atts.getQName(i);
                    Deque<EditorVariable> evs = attributeXPath.get(key);
                    EditorVariable ev = evs.removeLast();
                    ev.setAttrsXPath(key, xPath);
                    if (evs.size() == 0) {
                        attributeXPath.remove(key);
                    }
                }
            }
        }
    }

    private void searchEditorAttributesInContent() {
        Set<String> keys = new HashSet<String>(attributeXPath.keySet());
        for (String key : keys) {
            Pattern keyPattern = Pattern.compile("\\s*\\Q$"+key+"\\E\\s*");
            if (keyPattern.matcher(elementContent).matches()) {
                String xPath = org.apache.commons.lang3.StringUtils.join(finalPathElements.iterator(), "/");
                xPath += "/text()";
                Deque<EditorVariable> evs = attributeXPath.get(key);
                EditorVariable ev = evs.removeLast();
                ev.setAttrsXPath(key, xPath);
                if (evs.size() == 0) {
                    attributeXPath.remove(key);
                }
            }
        }
    }

    private String getTemplateQName(String nsUri, String lName) {
        String prefix = namespacePrefixes.get(nsUri);
        if (prefix == null) {
            prefix = "ns" + namespacePrefixes.size();
            namespacePrefixes.put(nsUri, prefix);
        }
        return String.format("%s:%s", prefix, lName);
    }

    // generate a xpath identifier with name and attributes
    private String generateXPathIdentifier(String nsUri, String lName, String qName, Attributes atts) {
        String xpathPart = getTemplateQName(nsUri, lName);
        List<String> attrs = new ArrayList<String>();
        for (int i = 0; i < atts.getLength(); ++i) {
            // only use local attributes
            if ("".equals(atts.getURI(i)) ||
                    atts.getURI(i).equals(nsUri)) {
                // don't use local attributes containing EditorAttributes wildcards
                if (!atts.getValue(i).matches("\\s*\\$.*")) {
                    attrs.add(String.format("@%s='%s'", atts.getQName(i), atts.getValue(i)));
                }
            }
        }
        if (attrs.size() > 0) {
            xpathPart = String.format("%s[%s]", xpathPart, StringUtils.join(attrs.iterator(), " and "));
        }
        return xpathPart;
    }

    @Override
    public void startElement(String nsUri, String lName, String qName, Attributes atts) throws SAXException {
        if (EDT.equals(nsUri)) {
            if ("editorDataTypes".equals(lName)) {
                //  start element, empty container
                assert(edtElements.size() == 0);
                EditorVariable ev = new EditorVariable(new NullAttributes());
                edtElements.addLast(ev);
                fullPathElements.addLast(getTemplateQName(nsUri, lName));
            } else if ("container".equals(lName)) {
                addStandardEDT(new NullAttributes(), nsUri, lName, atts);
            } else if ("lonLatBoundingBox".equals(lName)) {
                addStandardEDT(new LatLonBBAttributes(), nsUri, lName, atts);
            } else if ("string".equals(lName)) {
                addStandardEDT(new StringAttributes(), nsUri, lName, atts);
            } else if ("uri".equals(lName)) {
                addStandardEDT(new UriAttributes(), nsUri, lName, atts);
            } else if ("list".equals(lName)) {
                addStandardEDT(new ListElementAttributes(), nsUri, lName, atts);
            } else if ("stringAndList".equals(lName)) {
                addStandardEDT(new StringAndListElementAttributes(), nsUri, lName, atts);
            } else if ("startAndStopTime".equals(lName)) {
                addStandardEDT(new StartAndStopTimeAttributes(), nsUri, lName, atts);
            } else {
                throw new UndefinedEditorVariableException(lName);
            }
        } else {
            fullPathElements.addLast(getTemplateQName(nsUri, lName));
            finalPathElements.addLast(generateXPathIdentifier(nsUri, lName, qName, atts));
            searchEditorAttributes(atts);
            String documentXPath = StringUtils.join(finalPathElements.iterator(), "/");
            for (EditorVariable ev : edtElements) {
                if (ev.getDocumentXPath() == null) {
                    ev.setDocumentXPath(documentXPath);
                }
            }
        }

    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        elementContent.append(ch, start, length);
    }

    @Override
    public void endElement(String nsUri, String lName, String qName) {
        searchEditorAttributesInContent();
        elementContent.setLength(0); // clear
        fullPathElements.removeLast();
        if (EDT.equals(nsUri)) {
            if ("editorDataTypes".equals(lName)) {
                assert(edtElements.size() == 1);
                resultConfig = edtElements.getFirst().getChildren();
            }
            EditorVariable ev  = edtElements.removeLast();
        } else {
            finalPathElements.removeLast();
        }
    }

    public Map<String, EditorVariable> getResultConfig() {
        return resultConfig;
    }

    public Map<String, String> getNamespacePrefixes() {
        return namespacePrefixes;
    }

}
