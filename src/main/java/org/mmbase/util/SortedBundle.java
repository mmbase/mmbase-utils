/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util;

import java.util.*;
import java.lang.reflect.*;
import java.text.Collator;
//import org.mmbase.cache.Cache;
import org.mmbase.util.logging.*;

/**
 * A bit like {@link java.util.ResourceBundle} (on which it is based), but it creates
 * SortedMap's. The order of the entries of the Map can be influenced in tree ways. You can
 * associate the keys with JAVA constants (and their natural ordering can be used), you can wrap the
 * keys in a 'wrapper' (which can be of any type, the sole restriction being that there is a
 * constructor with String argument or of the type of the assiocated JAVA constant if that happened
 * too, and the natural order of the wrapper can be used (a wrapper of some Number type would be
 * logical). Finally you can also explicitely specify a {@link java.util.Comparator} if no natural
 * order is good.
 *
 * @author Michiel Meeuwissen
 * @since  MMBase-1.8
 * @version $Id$
 */
public class SortedBundle {

    private static final Logger log = Logging.getLoggerInstance(SortedBundle.class);

    /**
     * Constant which can be used as an argument for {@link #getResource}
     */
    public static final Class<?> NO_WRAPPER    = null;
    /**
     * Constant which can be used as an argument for {@link #getResource}
     */
    public static final Comparator<? super Object> NO_COMPARATOR = null;
    /**
     * Constant which can be used as an argument for {@link #getResource}
     */
    public static final HashMap<String, Object> NO_CONSTANTSPROVIDER = null;

    // cache of maps.
    /* TODO
    private static final Cache<String, SortedMap<?, String>> knownResources = new Cache<String, SortedMap<?, String>>(100) {
        @Override
        public String getName() {
            return "ConstantBundles";
        }
        @Override
        public String getDescription() {
            return "A cache for constant bundles, to avoid a lot of reflection.";
        }
    };
    */

    static {
        /*
        try {
            org.mmbase.cache.CacheManager.putCache(knownResources);
        } catch (Throwable t) {
        }
        */
    }

    /**
     * You can specify ValueWrapper.class as a value for the wrapper argument. The keys will be objects with natural order of the values.
     */

    public static class ValueWrapper implements Comparable<ValueWrapper> {
        private final Object key;
        private final Object value;
        private final Comparator<Object> com;
        public ValueWrapper(Object k, Comparable<Object> v) {
            key   = k;
            value = v;
            com   = null;
        }
        public ValueWrapper(Object k, Object v, Comparator <Object> c) {
            key   = k;
            value = v;
            com   = c;
        }
        @SuppressWarnings({"unchecked"})
        public  int compareTo(ValueWrapper other) {
            int result =
                com != null ? com.compare(value, other.value) :
                ((Comparable) value).compareTo(other.value);
            if (result != 0) return result;
            if (key instanceof Comparable) {
                return ((Comparable<Object>) key).compareTo(other.key);
            } else {
                return 0;
            }
        }
        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o == null) return false;
            if (getClass() == o.getClass()) {
                ValueWrapper other = (ValueWrapper) o;
                return key.equals(other.key) && (value == null ? other.value == null : value.equals(other.value));
            }
            return false;
        }
        @Override
        public String toString() {
            return Casting.toString(key);
        }
        public Object getKey() {
            return key;
        }
        /**
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            int result = 0;
            result = HashCodeUtil.hashCode(result, key);
            result = HashCodeUtil.hashCode(result, value);
            result = HashCodeUtil.hashCode(result, com);
            return result;
        }
    }


    /**
     * @param baseName A string identifying the resource. See {@link java.util.ResourceBundle#getBundle(java.lang.String, java.util.Locale, java.lang.ClassLoader)} for an explanation of this string.
     *
     * @param locale   the locale for which a resource bundle is desired
     * @param loader   the class loader from which to load the resource bundle
     * @param constantsProvider A map representing constants for the value. Can be based on a class using {@link #getConstantsProvider(Class)}, then the class's constants ar used to associate with the elements of this resource.
     * @param wrapper           the keys will be wrapped in objects of this type (which must have a
     *                          constructor with the right type (String, or otherwise the type of the variable given by the constantsProvider), and must be Comparable.
     *                          You could specify e.g. Integer.class if the keys of the
     *                          map are meant to be integers. This can be <code>null</code>, in which case the keys will remain unwrapped (and therefore String).
     * @param comparator        the elements will be sorted (by key) using this comparator or by natural key order if this is <code>null</code>.
     *
     * @throws NullPointerException      if baseName or locale is <code>null</code>  (not if loader is <code>null</code>)
     * @throws MissingResourceException  if no resource bundle for the specified base name can be found
     * @throws IllegalArgumentExcpetion  if wrapper is not Comparable.
     */
    public static <C> SortedMap<C, String> getResource(final String baseName,  Locale locale, final ClassLoader loader, final Map<String, Object> constantsProvider, final Class<?> wrapper, Comparator<? super Object> comparator) {
        //String resourceKey = baseName + '/' + locale + (constantsProvider == null ? "" : "" + constantsProvider.hashCode()) + "/" + (comparator == null ? "" : "" + comparator.hashCode()) + "/" + (wrapper == null ? "" : wrapper.getName());
        @SuppressWarnings("unchecked")
        SortedMap<C, String> m = null; //(SortedMap<C, String>) knownResources.get(resourceKey);
        if (locale == null) locale = LocalizedString.getDefault();

        if (m == null) { // find and make the resource
            ResourceBundle bundle;
            if (loader == null) {
                bundle = ResourceBundle.getBundle(baseName, locale);
            } else {
                bundle = ResourceBundle.getBundle(baseName, locale, loader);
            }
            if (comparator == null && wrapper != null && ! Comparable.class.isAssignableFrom(wrapper)) {
                throw new IllegalArgumentException("Key wrapper " + wrapper + " is not Comparable");
            }

            m = new TreeMap<C, String>(comparator);

            Enumeration<String> keys = bundle.getKeys();
            while (keys.hasMoreElements()) {
                String bundleKey = keys.nextElement();
                String value = bundle.getString(bundleKey);
                C key = (C) castKey(bundleKey, value, constantsProvider, wrapper, locale);
                if (key == null) continue;
                m.put(key, value);
            }
            m = Collections.unmodifiableSortedMap(m);
            //knownResources.put(resourceKey, m);
        }
        return m;
    }


    public static Object castKey(final String bundleKey, final Object value, final Map<String,Object> constantsProvider, final Class<?> wrapper) {
        return castKey(bundleKey, value, constantsProvider, wrapper, null);
    }
    /**
     * Casts a key of the bundle to the specified key-type. This type is defined by
     * the combination of the arguments. See {@link #getResource}.
     */
    protected static Object castKey(final String bundleKey, final Object value, final Map<String,Object> constantsProvider, final Class<?> wrapper, final Locale locale) {

        if (bundleKey == null) return null;
        Object key;
        // if the key is numeric then it will be sorted by number
        //key Double

        Map<String, Object> provider = constantsProvider; // default class (may be null)
        int lastDot = bundleKey.lastIndexOf('.');
        if (lastDot > 0) {
            Class<?> providerClass;
            String className = bundleKey.substring(0, lastDot);
            try {
                providerClass = Class.forName(className);
                provider = getConstantsProvider(providerClass);
            } catch (ClassNotFoundException cnfe) {
                if (log.isDebugEnabled()) {
                    log.debug("No class found with name " + className + " found from " + bundleKey);
                }
            }
        }

        if (provider != null) {
            key = provider.get(bundleKey.toUpperCase());
            if (key == null) {
                log.debug("Could not find " + bundleKey.toUpperCase() + " in " + constantsProvider);
                key = bundleKey;

            }
        } else {
            key = bundleKey;
        }

        if (wrapper != null && ! wrapper.isAssignableFrom(key.getClass())) {
            try {
                if (ValueWrapper.class.isAssignableFrom(wrapper)) {
                    if (locale == null) {
                        Constructor<?> c = wrapper.getConstructor(Object.class, Comparable.class );
                        key = c.newInstance(key, value);
                    } else {
                        Constructor<?> c = wrapper.getConstructor(Object.class, Object.class, Comparator.class );
                        Collator comp = Collator.getInstance(locale);
                        comp.setStrength(Collator.PRIMARY);
                        key = c.newInstance(key, value, comp);
                    }
                } else if (Number.class.isAssignableFrom(wrapper)) {
                    if (key instanceof String) {
                        if (Casting.DOUBLE_PATTERN.matcher((String) key).matches()) {
                            key = Casting.toType(wrapper, key);
                        }
                    } else {
                        key = Casting.toType(wrapper, key);
                        log.debug("wrapper is a Number, that can simply be cast " + value + " --> " + key + "(" + wrapper + ")");
                    }
                } else if (Boolean.class.isAssignableFrom(wrapper)) {
                    if (key instanceof String) {
                        if (Casting.BOOLEAN_PATTERN.matcher((String) key).matches()) {
                            key = Casting.toType(wrapper, key);
                        }
                    } else {
                        key = Casting.toType(wrapper, key);
                        log.debug("wrapper is a Boolean, that can simply be cast " + value + " --> " + key + "(" + wrapper + ")");
                    }

                } else {
                    log.debug("wrapper is unrecognized, suppose constructor " + key.getClass());
                    Constructor<?> c = wrapper.getConstructor(key.getClass());
                    key = c.newInstance(key);
                }
            } catch (NoSuchMethodException nsme) {
                log.warn(nsme.getClass().getName() + ". Could not convert " + key.getClass().getName() + " " + key + " to " + wrapper.getName() + " : " + nsme.getMessage() + " locale " + locale, nsme);
            } catch (SecurityException se) {
                log.error(se.getClass().getName() + ". Could not convert " + key.getClass().getName() + " " + key + " to " + wrapper.getName() + " : " + se.getMessage());
             } catch (InstantiationException ie) {
                log.error(ie.getClass().getName() + ". Could not convert " + key.getClass().getName() + " " + key + " to " + wrapper.getName() + " : " + ie.getMessage());
             } catch (InvocationTargetException ite) {
                log.debug(ite.getClass().getName() + ". Could not convert " + key.getClass().getName() + " " + key + " to " + wrapper.getName() + " : " + ite.getMessage());
             } catch (IllegalAccessException iae) {
                log.error(iae.getClass().getName() + ". Could not convert " + key.getClass().getName() + " " + key + " to " + wrapper.getName() + " : " + iae.getMessage());
             }
        }
        return key;
    }

    /**
     * Returns a (serializable) Map representing all accessible static public members of given class (so, all constants).
     * @since MMBase-1.8
     */
    public static HashMap<String, Object> getConstantsProvider(Class<?> clazz) {
        if (clazz == null) return null;
        HashMap<String, Object> map  = new HashMap<String, Object>();
        fillConstantsProvider(clazz, map);
        return map;
    }
    private static void fillConstantsProvider(Class<?> clazz, Map<String, Object> map) {
        while(clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field constant : fields) {
                if (Modifier.isStatic(constant.getModifiers())) {
                    String key = constant.getName().toUpperCase();
                    if (! map.containsKey(key)) { // super should not override this.
                        try {
                            Object value = constant.get(null);
                            try {
                                // support for enums where ordinal is no good.
                                Method keyMethod = value.getClass().getMethod("getValue");
                                value = "" + keyMethod.invoke(value);
                            } catch (NoSuchMethodException nsme) {
                                log.debug("" + nsme);
                                try {
                                    // support for enums
                                    Method keyMethod = value.getClass().getMethod("ordinal");
                                    value = "" + keyMethod.invoke(value);
                                } catch (Exception e1) {
                                    log.debug("" + e1);
                                }
                            } catch (Exception e2) {
                                log.debug("" + e2);
                            }
                            map.put(key, value);
                        } catch (IllegalAccessException ieae) {
                            log.debug("The java constant with name " + key + " is not accessible");
                        }
                    }
                }
            }
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> element : interfaces) {
                fillConstantsProvider(element, map);
            }
            clazz = clazz.getSuperclass();
        }
    }

    private SortedBundle() {
    }
}
