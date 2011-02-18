/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.text.*;
import java.util.*;
import java.io.*;

/**
 * This class wraps a {@link java.text.Collator} and associates it with a {@link
 * java.util.Locale}. Also, it is {@link java.io.Serializable} (mostly to help RMMCI).
 *
 * An instance can be obtained with {@link #getInstance(String)}.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.9.2
 */
public class LocaleCollator  extends Collator implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * An enum wrapping the 'strength' related constants of {@link Collator}. Mainly because {@link Strength#valueOf(String)} is used in the implementation of {@link LocaleCollator#getInstance(String)}.
     */
    public static enum Strength {
        /**
         * Wraps {@link Collator#IDENTICAL}
         */
        IDENTICAL(Collator.IDENTICAL),
        /**
         * Wraps {@link Collator#PRIMARY}
         */
            PRIMARY(Collator.PRIMARY),
            /**
             * Wraps {@link Collator#SECONDARY}
             */
            SECONDARY(Collator.SECONDARY),
            /**
             * Wraps {@link Collator#TERTIARY}
             */
            TERTIARY(Collator.TERTIARY);

        private final int value;
        private Strength(int v) {
            value = v;
        }
        public int get() {
            return value;
        }
        /**
         * @param i {@link Collator#IDENTICAL}, {@link Collator#PRIMARY}, {@link Collator#SECONDARY} or {@link Collator#TERTIARY}.
         */
        public static Strength valueOf(int i) {
            for (Strength s : Strength.values()) {
                if (s.get() == i) return s;
            }
            throw new IllegalArgumentException();
        }

    }
    /**
     * An enum wrapping the 'decomposition' related constants of {@link Collator}. Mainly because {@link LocaleCollator.Decomposition#valueOf(String)} is used in the implementation of {@link LocaleCollator#getInstance(String)}.
     */
    public static enum Decomposition {
        CANONICAL(Collator.CANONICAL_DECOMPOSITION),
        FULL(Collator.FULL_DECOMPOSITION),
        NO(Collator.NO_DECOMPOSITION);
        private final int value;
        private Decomposition(int v) {
            value = v;
        }
        public int get() {
            return value;
        }
        /**
         * @param i {@link Collator#CANONICAL_DECOMPOSITION}, {@link Collator#FULL_DECOMPOSITION} or {@link Collator#NO_DECOMPOSITION}.
         */
        public static LocaleCollator.Decomposition valueOf(int i) {
            for (LocaleCollator.Decomposition s : LocaleCollator.Decomposition.values()) {
                if (s.get() == i) {
                    return s;
                }
            }
            throw new IllegalArgumentException();
        }

    }

    /**
     * Gets the collator associated with {@link LocalizedString#getDefault}.
     */

    public static Collator getInstance() {
        return new LocaleCollator(LocalizedString.getDefault());
    }


    /**
     * Get a (fresh) Collator defined by
     * <code>&lt;locale&gt;:&lt;strength&gt;:&lt;decomposition&gt;</code>
     * Elements of these string can be left away from the right, or left away by leaving empty.
     * For example:
     * <ul>
     *  <li>"da_DK" The Collator associated with danish</li>
     *  <li>":IDENTIY The case sensitive Collator associated with the default locale</li>
     * </ul>
     * @see Strength
     * @see LocaleCollator.Decomposition
     */

    public static Collator getInstance(String s) {
        final String[] elements = s.split(":", -1);
        final Locale locale = elements[0].equals("") ? LocalizedString.getDefault() : LocalizedString.getLocale(elements[0]);
        final Collator collator = new LocaleCollator(locale);
        if (elements.length > 1) {
            collator.setStrength(Strength.valueOf(elements[1]).get());
        }
        if (elements.length > 2) {
            collator.setDecomposition(LocaleCollator.Decomposition.valueOf(elements[2]).get());
        }
        return collator;
    }

    private transient Collator wrapped;
    private final Locale locale;
    private LocaleCollator(Locale loc) {
        locale = loc;
        wrapped = (Collator) Collator.getInstance(loc).clone();
    }

    public LocaleCollator(Locale loc, Collator col) {
        locale = loc;
        wrapped = col;
    }

    @Override
    public  int compare(Object o1, Object o2) {
        return wrapped.compare(o1, o2);
    }

    public   int compare(String source, String target) {
        return wrapped.compare(source, target);
    }
    @Override
    public boolean  equals(Object that) {
        return wrapped.equals(that);
    }
    @Override
    public boolean equals(String source, String target) {
        return wrapped.equals(source, target);
    }
    public  CollationKey  getCollationKey(String source) {
        return wrapped.getCollationKey(source);
    }
    @Override
    public int getDecomposition() {
        return wrapped.getDecomposition();
    }
    @Override
    public int getStrength() {
        return wrapped.getStrength();
    }
    public int hashCode() {
        return wrapped.hashCode();

    }
    @Override
    public void setDecomposition(int decompositionMode) {
        wrapped.setDecomposition(decompositionMode);
    }
    @Override
    public void  setStrength(int newStrength) {
        wrapped.setStrength(newStrength);
    }
    @Override
    public String toString() {
        return locale + ":" + Strength.valueOf(wrapped.getStrength()) + ":" + LocaleCollator.Decomposition.valueOf(wrapped.getDecomposition());
    }

    /**
     * Wether a string matches this Collator. It's the same string as in
     * {@link #getInstance(String)}. Unspecified values never make the result false.
     */
    public boolean matches(final String s) {
        final String[] elements = s.split(":", -1);
        if (elements[0].length() > 0) {
            Locale otherLocale = LocalizedString.getLocale(elements[0]);
            if (! otherLocale.getLanguage().equals(locale.getLanguage())) {
                return false;
            }
            if (otherLocale.getCountry().length() > 0 &&
                locale.getCountry().length() > 0 &&
                ! otherLocale.getCountry().equals(locale.getCountry())) {
                return false;
            }
            if (otherLocale.getVariant().length() > 0 &&
                locale.getVariant().length() > 0 &&
                ! otherLocale.getVariant().equals(locale.getVariant())) {
                return false;
            }
        }
        if (elements.length > 1 &&
            Strength.valueOf(elements[1]).get() != getStrength()) {
                return false;
        }
        return !(elements.length > 2 &&
                Decomposition.valueOf(elements[2]).get() != getDecomposition());
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeUTF(wrapped.getClass().getName());
        if (wrapped instanceof RuleBasedCollator) {
            out.writeUTF(((RuleBasedCollator) wrapped).getRules());
        } else {
            throw new IOException("Don't know how to serialize " + wrapped);
        }

    }
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        String clazz = in.readUTF(); // ignore for now
        try {
            wrapped = new RuleBasedCollator(in.readUTF());
        } catch (ParseException pe) {
            IOException e = new IOException(pe.getMessage());
            e.initCause(pe);
            throw e;
        }


    }



    public static void main(String[] argv) throws Exception {
        Collator col = getInstance(argv[0]);

        System.out.println("" + col);
        String[][] s = new String[][] {
            { "a", "A" },
            { "a", "�" },
            { "a", "�" },
            { "a", "x" },
            { "�", "z" },
            { "�", "z" }
        };
        for (String [] pair : s) {
            System.out.println("COMPARE " + pair[0] + "/" + pair[1] + ": " + col.compare(pair[0], pair[1]) + " " + col.equals(pair[0], pair[1]));
        }

    }

}
