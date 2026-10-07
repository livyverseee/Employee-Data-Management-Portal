package com.portal.service;

import com.portal.dto.ColumnDef;
import com.portal.dto.ParsedDataset;
import com.portal.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Step C: Parses XML dataset into ParsedDataset (columns + rows).
 * Features robust XXE protection and supports both internal schema (<columns> + <records>)
 * and generic arbitrary XML files with repeating child elements.
 */
@Service
public class XmlParserService {

    @Autowired
    private XmlConverterService xmlConverterService;

    public ParsedDataset parseXml(byte[] xmlBytes) {
        if (xmlBytes == null || xmlBytes.length == 0) {
            throw new BadRequestException("XML content is empty");
        }

        try (InputStream is = new ByteArrayInputStream(xmlBytes)) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Strict XXE protection
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            try {
                dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            } catch (Exception ignored) {
                // Ignore if feature unsupported by parser
            }
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(is);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            // Check if this document has standard <columns> and <records>
            NodeList columnsNodes = root.getElementsByTagName("columns");
            NodeList recordsNodes = root.getElementsByTagName("records");

            if (columnsNodes.getLength() > 0 && recordsNodes.getLength() > 0) {
                return parseStandardDatasetXml(root);
            } else {
                return parseGenericXml(root);
            }

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Malformed XML: " + e.getMessage());
        }
    }

    public ParsedDataset parseXml(String xmlString) {
        return parseXml(xmlString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses dataset XML that has <columns> and <records> blocks.
     */
    private ParsedDataset parseStandardDatasetXml(Element root) {
        List<ColumnDef> columns = new ArrayList<>();
        Element columnsElem = (Element) root.getElementsByTagName("columns").item(0);
        NodeList colList = columnsElem.getElementsByTagName("column");

        for (int i = 0; i < colList.getLength(); i++) {
            Element col = (Element) colList.item(i);
            String key = col.getAttribute("key");
            String label = col.getAttribute("label");
            if (label == null || label.isEmpty()) {
                label = toTitleCase(key);
            }
            ColumnDef def = new ColumnDef();
            def.setKey(key);
            def.setLabel(label);
            def.setPosition(i);
            columns.add(def);
        }

        if (columns.isEmpty()) {
            throw new BadRequestException("XML contains no column definitions");
        }

        List<Map<String, String>> rows = new ArrayList<>();
        Element recordsElem = (Element) root.getElementsByTagName("records").item(0);
        NodeList recordList = recordsElem.getElementsByTagName("record");

        for (int r = 0; r < recordList.getLength(); r++) {
            Element recordElem = (Element) recordList.item(r);
            Map<String, String> row = new LinkedHashMap<>();
            for (ColumnDef col : columns) {
                NodeList match = recordElem.getElementsByTagName(col.getKey());
                String val = "";
                if (match.getLength() > 0) {
                    val = match.item(0).getTextContent();
                }
                row.put(col.getKey(), val != null ? val : "");
            }
            rows.add(row);
        }

        if (rows.isEmpty()) {
            throw new BadRequestException("XML dataset must contain at least 1 record");
        }

        return new ParsedDataset(columns, rows);
    }

    /**
     * Parses arbitrary XML files without <columns> block.
     * Identifies the repeating record element, derives columns and values.
     */
    private ParsedDataset parseGenericXml(Element root) {
        // Collect repeating element tags
        NodeList children = root.getChildNodes();
        Map<String, List<Element>> tagMap = new LinkedHashMap<>();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                tagMap.computeIfAbsent(el.getTagName(), k -> new ArrayList<>()).add(el);
            }
        }

        if (tagMap.isEmpty()) {
            throw new BadRequestException("XML contains no record elements");
        }

        // Find candidate repeating element tag
        String recordTag = null;
        for (Map.Entry<String, List<Element>> entry : tagMap.entrySet()) {
            List<Element> elements = entry.getValue();
            if (hasComplexChildren(elements.get(0)) || elements.size() > 1 || tagMap.size() == 1) {
                recordTag = entry.getKey();
                break;
            }
        }

        if (recordTag == null) {
            recordTag = tagMap.keySet().iterator().next();
        }

        List<Element> recordElements = tagMap.get(recordTag);
        if (recordElements == null || recordElements.isEmpty()) {
            throw new BadRequestException("No data records found in XML");
        }

        // Discover columns in first-seen order
        Set<String> discoveredTags = new LinkedHashSet<>();
        for (Element rec : recordElements) {
            NodeList recChildren = rec.getChildNodes();
            for (int j = 0; j < recChildren.getLength(); j++) {
                Node n = recChildren.item(j);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    discoveredTags.add(n.getNodeName());
                }
            }
        }

        if (discoveredTags.isEmpty()) {
            throw new BadRequestException("Record elements have no child columns");
        }

        List<ColumnDef> columns = new ArrayList<>();
        int pos = 0;
        for (String tag : discoveredTags) {
            String key = xmlConverterService != null ? xmlConverterService.sanitizeKey(tag) : tag.toLowerCase();
            String label = toTitleCase(tag);
            ColumnDef col = new ColumnDef();
            col.setKey(key);
            col.setLabel(label);
            col.setPosition(pos++);
            columns.add(col);
        }

        List<Map<String, String>> rows = new ArrayList<>();
        for (Element rec : recordElements) {
            Map<String, String> row = new LinkedHashMap<>();
            int colIdx = 0;
            for (String tag : discoveredTags) {
                ColumnDef col = columns.get(colIdx++);
                NodeList list = rec.getElementsByTagName(tag);
                String val = "";
                if (list.getLength() > 0) {
                    val = list.item(0).getTextContent();
                }
                row.put(col.getKey(), val != null ? val : "");
            }
            rows.add(row);
        }

        if (rows.isEmpty()) {
            throw new BadRequestException("XML dataset must contain at least 1 record");
        }

        return new ParsedDataset(columns, rows);
    }

    private boolean hasComplexChildren(Element el) {
        NodeList nl = el.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    public static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return "";
        // Split camelCase or snake_case or hyphen
        String cleaned = input.replaceAll("([a-z])([A-Z])", "$1 $2")
                              .replaceAll("[_-]+", " ")
                              .trim();
        String[] words = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) {
                sb.append(w.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
