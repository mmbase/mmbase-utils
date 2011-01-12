/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.transformers;
import java.io.*;
import org.mmbase.util.logging.*;

/**
 * Escapes and Unescapes undesirable characters using % (URLEncoding)
 *
 * Contrary to java.net.URLEncoder, it does <em>not</em> encode '+'.
 *
 * @author vpro (as org.mmbase.util.URLEscape, still present in SCAN application)
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class UrlEscaper extends ReaderTransformer{

    private static final Logger log = Logging.getLoggerInstance(UrlEscaper.class);

    private static final int BUF_SIZE = 100;
    /**
     * List for all ASCII characters whether it can be part of an
     * URL line.
     * http://www.ietf.org/rfc/rfc1808.txt
     * unreserved  = alpha | digit | safe | extra
     * alpha       = lowalpha | hialpha
     * digit       = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" |
     *                "8" | "9"

     * safe        = "$" | "-" | "_" | "." | "+"
     * extra       = "!" | "*" | "'" | "(" | ")" | ","

     * correspondes with 'unreserved', first entry is 32, space.
     */
    private static final boolean isacceptable[] = {
        false, true, false, false, false, false, false, false,     //  !"#$%&'
        true, true, true, true, true, true, true, false,           // ()*+,-./
        true, true, true, true, true, true, true, true,            // 01234567
        true, true, false, false, false, false, false, false,       // 89:;<=>?
        false, true, true, true, true, true, true, true,            // @ABCDEFG
        true, true, true, true, true, true, true, true,            // HIJKLMNO
        true, true, true, true, true, true, true, true,            // PQRSTUVW
        true, true, true, false, false, false, false, true,        // XYZ[\]^_
        true, true, true, true, true, true, true, true,           // `abcdefg
        true, true, true, true, true, true, true, true,            // hijklmno
        true, true, true, true, true, true, true, true,            // pqrstuvw
        true, true, true, false, false, false, false, false        // xyz{|}~
    };

    /**
     * Hex characters
     */
    private static final char hex[] = {
        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };

    /**
     * Character to use for escaping invalid characters
     */
    private static final int HEX_ESCAPE = (int) '%';

    /**
     * @since MMBase-1.9.2
     */
    public static final UrlEscaper INSTANCE = new UrlEscaper();


    /**
     * Escape a url.
     * Replaces 'invalid characters' with their Escaped code, i.e.
     * the questionmark (?) is escaped with %3F.
     */
    @Override
    public Writer transform(Reader r, Writer w) {
        escape(new BufferedInputStream(new org.mmbase.util.ReaderInputStream(r, "UTF-8")), w);
        return w;
    }

    public static void escape(BufferedInputStream r, Writer w) {
        byte[] buf = new byte[BUF_SIZE];
        try {
            int n = r.read(buf, 0, BUF_SIZE);
            while (n > 0) {
                for (int i = 0; i < n;i++) {
                    int a = (int)buf[i] & 0xff;
                    if (a >= 32 && a < 128 && isacceptable[a - 32]) {
                        w.write((char)a);
                    } else {
                        w.write(HEX_ESCAPE);
                        w.write(hex[a >> 4]);
                        w.write(hex[a & 15]);
                    }
                }
                n = r.read(buf, 0, BUF_SIZE);
            }
        } catch (IOException ioe) {
            log.warn(ioe.getMessage(), ioe);
        }
    }


    /**
     * Unescape a url.
     * Replaces escape sequences with the actual character.
     * i.e %3F is replaced with the the question mark (?).
     * @return the unescaped url.
     */
    @Override
    public Writer transformBack(Reader reader, Writer writer) {
        BufferedReader br = new BufferedReader(reader, BUF_SIZE);
        try {
            String line;
            while ((line = br.readLine()) != null) {
                writer.write(java.net.URLDecoder.decode(line, "UTF-8"));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return writer;

    }

    /**
     * Method for testing this class from the command line
     */
    public static void main(String args[]) {
        UrlEscaper e = new UrlEscaper();
        for (int i = 0; i < args.length; i++) {
            log.info("Original : '" + args[i] + "'");
            String escaped = e.transform(args[i]);
            log.info("Escaped : '" + escaped + "'");
            log.info("Unescaped again : '" + e.transformBack(escaped) + "'");
        }

    }
}
