/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util.magicfile;
import org.mmbase.util.*;
import java.io.*;
import org.w3c.dom.Element;
import org.mmbase.util.xml.DocumentReader;
import org.mmbase.util.logging.*;

/**
 * A Detector stores one entry from the magic.xml file, and contains
 * the functionality to determines if a certain byte[] satisfies it.
 *
 * Implementation made on the basis of actual magic file and its manual.<br />
 *
 * TODO:<br />
 * - link the info with mimetypes<br />
 * - add test modifiers<br />
 * - add commandline switches for warning, error and debugging messages<br />
 *<br />
 * Ignored features of magic:<br />
 * - date types<br />
 * - indirect offsets (prefix of '&' in sublevel match or (address+bytes) where offset = value of address plus bytes<br />
 * - AND'ing of type<br />
 *<br />
 * BUGS:<br />
 * - test string isn't read when end of line is reached in absence of a message string<br />
 * <br />
 *
 * Tested:<br />
 * - .doc<br />
 * - .rtf<br />
 * - .pdf<br />
 * - .sh<br />
 * - .gz<br />
 * - .bz2<br />
 * - .html<br />
 * - .rpm<br />
 * - .wav<br />
 *<br />
 * Not supported by magic file:<br />
 * - StarOffice<br />
 * @version $Id$
 */

public class BasicDetector extends AbstractDetector {
    private static final Logger log = Logging.getLoggerInstance(BasicDetector.class);


    // No configuration below
    private static final int BIG_ENDIAN = 0;
    private static final int LITTLE_ENDIAN = 1;
    private static final String[] label = new String[] { "big endian", "little endian" };

    private String rawinput; // Original input line
    private int offset = -1;
    private String type;
    // types: byte, short, long, string, date, beshort, belong, bedate, leshort, lelong, ledate
    private String typeAND;
    // Some types are defined as e.g. "belong&0x0000ff70", then typeAND=0x0000ff70 (NOT IMPLEMENTED!)
    private String test; // Test value
    private char testComparator; // What the test is like,

    // What are these?
    private String xString;
    private int xInt;
    private char xChar;


    private boolean hasX; // Is set when an 'x' value is matched


    public void setOffset(String offset) {
        this.offset = Integer.parseInt(offset);
    }
    public int getOffset() {
        return offset;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getType() {
        return type;
    }
    public void setTest(String test) {
        this.test = test;
    }
    public String getTest() {
        return test;
    }
    public void setComparator(char comparator) {
        this.testComparator = comparator;
    }
    public char getComparator() {
        return testComparator;
    }




    /**
     * @return Whether detector matches the prefix/lithmus of the file
     */
    @Override
    public boolean test(byte[] lithmus, InputStream sin) {
        if (lithmus == null || lithmus.length == 0 || offset == -1) {
            return false;
        }
        boolean hit;
        //log.debug("TESTING "+rawinput);
        if (type.equals("string")) {
            hit = testString(lithmus);
        } else if (type.equals("beshort")) {
            hit = testShort(lithmus, BIG_ENDIAN);
        } else if (type.equals("belong")) {
            hit = testLong(lithmus, BIG_ENDIAN);
        } else if (type.equals("leshort")) {
            hit = testShort(lithmus, LITTLE_ENDIAN);
        } else if (type.equals("lelong")) {
            hit = testLong(lithmus, LITTLE_ENDIAN);
        } else if (type.equals("byte")) {
            hit = testByte(lithmus);
        } else {
            // Date types are not supported
            hit = false;
        }
        if (hit) {
            log.debug("Detector " + this + " hit");
            for (Detector child : childList) {
                if (child.test(lithmus, sin)) {
                    String s = child.getDesignation();
                    if (s.startsWith("\\b")) {
                        s = s.substring(2);
                    }
                    this.message = this.message + " " + s;
                }
            }
        }
        return hit;
    }

    /**
     * todo: I noticed there is also a %5.5s variation in magic...
     */
    @Override
    public String getDesignation() {
        if (hasX) {
            int n = message.indexOf("%d");
            if (n >= 0) {
                return message.substring(0, n) + xInt + message.substring(n + 2);
            }

            n = message.indexOf("%s");
            if (n >= 0) {
                return message.substring(0, n) + xString + message.substring(n + 2);
            }

            n = message.indexOf("%c");
            if (n >= 0) {
                return message.substring(0, n) + xChar + message.substring(n + 2);
            }
        }
        return message;
    }

    /**
     * @return Conversion of 2 byte array to integer
     */
    private int byteArrayToInt(byte[] ar) {
        StringBuilder buf = new StringBuilder();
        for (byte element : ar) {
            buf.append(Integer.toHexString(element & 0x000000ff));
        }
        return Integer.decode("0x" + buf.toString()).intValue();
    }

    /**
     * @return Conversion of 4 byte array to long
     */
    private long byteArrayToLong(byte[] ar) {
        StringBuilder buf = new StringBuilder();
        for (byte element : ar) {
            buf.append(Integer.toHexString(element & 0x000000ff));
        }
        return Long.decode("0x" + buf.toString()).longValue();
    }

    /**
     * Test whether a string matches
     */
    protected boolean testString(byte[] lithmus) {

        if (test.length() == 0) {
            log.warn("TEST STRING LENGTH ZERO FOR [" + rawinput + "]");
            return false;
        }

        int maxNeeded = offset + test.length();

        if (maxNeeded > lithmus.length) {
            return false;
        }

        try {
            xString = new String(lithmus, offset, test.length(), "US-ASCII");
            // US-ASCII: fixate the charset, do not depend on platform default:
            //           US-ASCCII: one byte = one char, so length can be predicted
        } catch (java.io.UnsupportedEncodingException usee) { // could not happen: US-ASCII is supported
        }

        log.debug("test string = '" + test + "' (" + message + ") comparing with '" + xString + "'");
        int n = xString.compareTo(test);
        switch (testComparator) {
        case '=' :
            return n == 0;
        case '>' :
            hasX = true;
            return n > 0;
        case '<' :
            hasX = true;
            return n < 0;
        default:
            return false;
        }
    }

    /**
     * Test whether a short matches
     */
    protected boolean testShort(byte[] lithmus, int endian) {
        if (lithmus.length < offset + 1) return false;
        log.debug("testing " + label[endian] + " short for " + rawinput);
        int found = 0;
        if (endian == BIG_ENDIAN) {
            found = byteArrayToInt(new byte[] { lithmus[offset], lithmus[offset + 1] });
        } else if (endian == LITTLE_ENDIAN) {
            found = byteArrayToInt(new byte[] { lithmus[offset + 1], lithmus[offset] });
        }
        xInt = found;

        if (test.equals("x")) {
            hasX = true;
            return true;
        } else if (test.equals("")) {
            return false;
        } else {
            int v = Integer.decode(test).intValue();
            // Hm. How did that binary arithmatic go?
            log.debug(
                      "dumb string conversion: 0x"
                      + Integer.toHexString(lithmus[offset] & 0x000000ff)
                      + Integer.toHexString(lithmus[offset + 1] & 0x000000ff));

            switch (testComparator) {
            case '=' :
                log.debug(
                          Integer.toHexString(v)
                          + " = "
                          + Integer.toHexString(found));
                return v == found;
            case '>' :
                hasX = true;
                return found > v;
            case '<' :
                hasX = true;
                return found < v;
            default:
                return false;
            }
        }
    }

    /**
     * Test whether a long matches
     */
    protected boolean testLong(byte[] lithmus, int endian) {
        if (lithmus.length < 4) return false;
        log.debug("testing " + label[endian] + " long for " + rawinput);
        long found = 0;
        try {
            if (endian == BIG_ENDIAN) {
                found = byteArrayToLong(
                                        new byte[] {
                                            lithmus[offset],
                                            lithmus[offset + 1],
                                            lithmus[offset + 2],
                                            lithmus[offset + 3] });
            } else if (endian == LITTLE_ENDIAN) {
                found =
                    byteArrayToLong(
                                    new byte[] {
                                        lithmus[offset + 3],
                                        lithmus[offset + 2],
                                        lithmus[offset + 1],
                                        lithmus[offset] });
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            if (!message.equals("")) {
                log.error("Failed to test " + label[endian] + " long for " + message);
            } else {
                log.error("Failed to test " + label[endian] + " long:");
            }
            log.error("Offset out of bounds: " + offset + " while max is " /*+BUFSIZE*/ );
            return false;
        }
        xInt = (int) found;
        // If it really is a long, we wouldn't want to know about it

        if (test.equals("x")) {
            hasX = true;
            return true;
        } else if (test.equals("")) {
            return false;
        } else {
            long v = Long.decode(test).longValue();

            // Hm. How did that binary arithmatic go?

            switch (testComparator) {
            case '=' :
                log.debug("checking " + label[endian] + " long: " + Long.toHexString(v)
                          + " = " + Long.toHexString(found));
                return v == found;
            case '>' :
                hasX = true;
                return found > v;
            case '<' :
                hasX = true;
                return found < v;
            default:
                return false;
            }
        }
    }

    /**
     * Test whether a byte matches
     */
    protected boolean testByte(byte[] lithmus) {
        log.debug("testing byte for " + rawinput);
        if (test.equals("x")) {
            hasX = true;
            xInt = lithmus[offset];
            xChar = (char) lithmus[offset];
            xString = "" + xChar;
            return true;
        } else if (test.equals("")) {
            return false;
        } else {
            byte b = (byte) Integer.decode(test).intValue();
            switch (testComparator) {
                // DOES THIS MAKE ANY SENSE AT ALL!!
            case '=' :
                return b == lithmus[offset];
            case '&' :
                // All bits in the test byte should be set in the found byte
                //log.debug("byte test as string = '"+test+"'");
                byte filter = (byte) (lithmus[offset] & b);
                //log.debug("lithmus = "+lithmus[offset]+"; test = "+b+"; filter = "+filter);
                return filter == b;
            default :
                return false;
            }
        }
    }

    /**
     * @return Original unprocessed input line
     * @since MMBase-1.7
     */
    public String getRawInput() {
        return rawinput;
    }

    protected String xmlEntities(String s) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '>' :
                res.append("&gt;");
                break;
            case '<' :
                res.append("&lt;");
                break;
            case '&' :
                res.append("&amp;");
                break;
            default :
                // Convert all characters not in the allowed XML character set
                int n = c;
                /* -- below is actual xml standard definition of allowed characters
                   if (n == 0x9 || n == 0xA || n == 0xD || (n >= 0x20 && n <= 0xD7FF) || (n >= 0xE000 && n <= 0xFFFD) ||
                   (n >= 0x10000 && n <= 0x10FFFF)) {
                */
                if (n == 0x9
                    || n == 0xA
                    || n == 0xD
                    || (n >= 0x20 && n < 128)) {
                    res.append(c);
                } else {
                    // octal representation of number; pad with zeros
                    String oct = Integer.toOctalString(n);
                    res.append("\\");
                    for (int j = 3; j > oct.length(); j--) {
                        res.append("0");
                    }
                    res.append(oct);
                }
            }
        }
        return res.toString();
    }

    /**
     * XML notatie:
     * <detector>
     *   <mimetype>foo/bar</mimetype>
     *   <extension>bar</extension>
     *   <designation>blablabla</designation>
     *   <test offset="bla" type="bla" comparator="=">test string</test>
     *   <childlist>
     *     <detector>etc</detector>
     *   </childlist>
     * </detector>
     *
     */
    public void toXML(FileWriter f) throws IOException {
        toXML(f, 0);
    }

    /**
     * @param level Indicates depth of (child) element
     */
    public void toXML(FileWriter f, int level) throws IOException {
        StringBuilder s = new StringBuilder();
        String comparatorEntity;

        char[] pad;
        if (level > 0) {
            pad = new char[level * 4];
            for (int i = 0; i < level * 4; i++) {
                pad[i] = ' ';
            }
        } else {
            pad = new char[] { };
        }
        String padStr = new String(pad);

        if (testComparator == '>') {
            comparatorEntity = "&gt;";
        } else
            if (testComparator == '<') {
                comparatorEntity = "&lt;";
            } else if (testComparator == '&') {
                comparatorEntity = "&amp;";
            } else {
                comparatorEntity = "" + testComparator;
            }
        s.append(
                 padStr
                 + "<detector>\n"
                 + padStr
                 + "  <mimetype>" + getMimeType() + "</mimetype>\n"
                 + padStr
                 + "  <extension>" + getExtension() + "</extension>\n"
                 + padStr
                 + "  <designation>"
                 + xmlEntities(message)
                 + "</designation>\n"
                 + padStr
                 + "  <test offset=\""
                 + offset
                 + "\" type=\""
                 + type
                 + "\" comparator=\""
                 + comparatorEntity
                 + "\">"
                 + xmlEntities(test)
                 + "</test>\n");
        f.write(s.toString());
        if (childList.size() > 0) {
            f.write(padStr + "  <childlist>\n");
            for (Detector detector : childList) {
                if (detector instanceof BasicDetector) {
                    ((BasicDetector) detector).toXML(f, level + 1);
                } else {
                    log.warn("" + detector);
                }
            }
            f.write(padStr + "  </childlist>\n");
        }
        f.write(padStr + "</detector>\n");

    }

    /**
     * Replaces octal representations of bytes, written as \ddd to actual byte values.
     */
    private String convertOctals(String s) {
        int p = 0;
        int stoppedAt = 0;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        char c;
        try {
            while (p < s.length()) {
                c = s.charAt(p);
                if (c == '\\') {
                    if (p > s.length() - 4) {
                        // Can't be a full octal representation here, let's cut it off
                        break;
                    } else {
                        char c0;
                        boolean failed = false;
                        for (int p0 = p + 1; p0 < p + 4; p0++) {
                            c0 = s.charAt(p0);
                            if (!(c0 >= '0' && c0 <= '7')) {
                                failed = true;
                            }
                        }
                        if (!failed) {
                            byte[]  bytes = s.substring(stoppedAt, p).getBytes("US-ASCII");
                            buf.write(bytes, 0, bytes.length);
                            buf.write(Integer.parseInt(s.substring(p + 1, p + 4), 8));
                            stoppedAt = p + 4;
                            p = p + 4;
                        } else {
                            p++;
                        }
                    }
                } else {
                    p++;
                }
            }
            byte[]  bytes = s.substring(stoppedAt, p).getBytes("US-ASCII");
            buf.write(bytes, 0, bytes.length);
            return buf.toString("US-ASCII");
        } catch (java.io.UnsupportedEncodingException use) { // could not happen US-ASCII is supported
            return "";
        }
    }


    @Override
    public void configure(Element e) {
        super.configure(e);
        Element e1 = DocumentReader.getElementByPath(e, "detector.test");
        if (e1 != null) {
            setTest(convertOctals(DocumentReader.getElementValue(e1)));
            setOffset(e1.getAttribute("offset"));
            setType(e1.getAttribute("type"));
            String comparator = e1.getAttribute("comparator");
            if (comparator.equals("&gt;")) {
                setComparator('>');
            } else if (comparator.equals("&lt;")) {
                setComparator('<');
            } else if (comparator.equals("&amp;")) {
                setComparator('&');
            } else if (comparator.length() == 1) {
                setComparator(comparator.charAt(0));
            } else {
                setComparator('=');
            }
        }
    }

    /**
     * @return String representation of Detector object.
     */
    @Override
    public String toString() {
        if (!valid) {
            return "parse error";
        } else {
            StringBuilder res = new StringBuilder("[" + offset + "] {" + type);
            if (typeAND != null) {
                res.append("[" + typeAND + "]");
            }
            res.append("} " + testComparator + "(" + test + ") " + message);
            if (childList.size() > 0) {
                res.append("\n");
                for (int i = 0; i < childList.size(); i++) {
                    res.append("> ").append(childList.get(i).toString());
                }
            }
            return res.toString();
        }
    }
}
