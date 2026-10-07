package com.portal.service;

import com.portal.dto.ColumnDef;
import com.portal.dto.TableData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Step B: Converts TableData into the standardized internal XML representation.
 * 
 * Target Format:
 * <dataset>
 *   <columns>
 *     <column key="first_name" label="First Name"/>
 *   </columns>
 *   <records>
 *     <record>
 *       <first_name>John</first_name>
 *     </record>
 *   </records>
 * </dataset>
 */
@Service
public class XmlConverterService {

    /**
     * Generates a sanitized column key from a column label.
     * Rule: lowercase, non-alphanumeric runs -> "_", trim "_", prefix "c_" if starts with digit.
     */
    public String sanitizeKey(String label) {
        if (label == null || label.trim().isEmpty()) {
            return "col";
        }
        String key = label.toLowerCase();
        key = key.replaceAll("[^a-z0-9]+", "_");
        key = key.replaceAll("^_+|_+$", "");
        if (key.isEmpty()) {
            key = "col";
        }
        if (Character.isDigit(key.charAt(0))) {
            key = "c_" + key;
        }
        return key;
    }

    /**
     * Builds unique column definitions for the table headers.
     */
    public List<ColumnDef> buildColumnDefinitions(List<String> headers) {
        List<ColumnDef> cols = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (int i = 0; i < headers.size(); i++) {
            String label = headers.get(i);
            String baseKey = sanitizeKey(label);
            String uniqueKey = baseKey;
            int counter = 2;
            while (seenKeys.contains(uniqueKey)) {
                uniqueKey = baseKey + "_" + counter++;
            }
            seenKeys.add(uniqueKey);

            ColumnDef col = new ColumnDef();
            col.setKey(uniqueKey);
            col.setLabel(label);
            col.setPosition(i);
            cols.add(col);
        }
        return cols;
    }

    /**
     * Converts TableData into standard internal XML string.
     */
    public String convertToXml(TableData tableData) {
        List<ColumnDef> columns = buildColumnDefinitions(tableData.getHeaders());
        StringBuilder xml = new StringBuilder(1024 * 16);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<dataset>\n");
        xml.append("  <columns>\n");
        for (ColumnDef col : columns) {
            xml.append("    <column key=\"")
               .append(escapeXmlAttribute(col.getKey()))
               .append("\" label=\"")
               .append(escapeXmlAttribute(col.getLabel()))
               .append("\"/>\n");
        }
        xml.append("  </columns>\n");
        xml.append("  <records>\n");

        for (List<String> row : tableData.getRows()) {
            xml.append("    <record>\n");
            for (int i = 0; i < columns.size(); i++) {
                ColumnDef col = columns.get(i);
                String val = i < row.size() ? row.get(i) : "";
                xml.append("      <").append(col.getKey()).append(">");
                if (val != null && !val.isEmpty()) {
                    xml.append(escapeXmlContent(val));
                }
                xml.append("</").append(col.getKey()).append(">\n");
            }
            xml.append("    </record>\n");
        }

        xml.append("  </records>\n");
        xml.append("</dataset>");
        return xml.toString();
    }

    public static String escapeXmlContent(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder(str.length() + 16);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '&':  sb.append("&amp;"); break;
                case '<':  sb.append("&lt;"); break;
                case '>':  sb.append("&gt;"); break;
                default:   sb.append(c); break;
            }
        }
        return sb.toString();
    }

    public static String escapeXmlAttribute(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder(str.length() + 16);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '&':  sb.append("&amp;"); break;
                case '<':  sb.append("&lt;"); break;
                case '>':  sb.append("&gt;"); break;
                case '"':  sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                default:   sb.append(c); break;
            }
        }
        return sb.toString();
    }
}
