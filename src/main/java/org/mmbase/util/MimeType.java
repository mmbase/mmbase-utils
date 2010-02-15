/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

/**
 *
 * @author Michiel Meeuwissen
 * @since MMBase-2.0
 */

public class MimeType implements java.io.Serializable {
    private static final long serialVersionUID = 0L;

    public static final String STAR = "*";
    public static final MimeType ANY = new MimeType(STAR, STAR);
    public static final MimeType OCTETSTREAM = new MimeType("application", "octet-stream");

    private final String type;
    private final String subType;


    public MimeType(String s) {
        if (s != null && s.length() > 0) {
            String[] m = s.split("/", 2);
            type = m[0];
            if (m.length > 1) {
                subType = m[1];
            } else {
                subType = STAR;
            }
        } else {
            type = STAR;
            subType = STAR;
        }
    }
    public MimeType(String t, String s) {
        type = t;
        subType = s;
    }

    public String getType() {
        return type;
    }
    public String getSubType() {
        return subType;
    }

    public String toString() {
        return type + "/" + subType;
    }

    public boolean matches(MimeType mt) {
        return
            (type.equals(STAR) || mt.type.equals(STAR) || type.equals(mt.type)) &&
            (subType.equals(STAR) || mt.subType.equals(STAR) || subType.equals(mt.subType));
    }
    @Override
    public int hashCode() {
        return type.hashCode()  + 13 * subType.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MimeType) {
            MimeType m = (MimeType) o;
            return m.type.equals(type) && m.subType.equals(subType);
        } else {
            return false;
        }
    }
}
