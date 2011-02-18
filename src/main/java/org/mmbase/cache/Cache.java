/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache;

import java.util.*;

import org.mmbase.util.*;
import org.mmbase.cache.implementation.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.xml.DocumentReader;
import org.w3c.dom.Element;

/**
 * A base class for all Caches. Extend this class for other caches.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */
abstract public class Cache<K, V> implements SizeMeasurable, Map<K, V>, CacheMBean {

    private static final Logger log = Logging.getLoggerInstance(Cache.class);

    private boolean active = true;
    protected int maxEntrySize = -1; // no maximum/ implementation does not support;

    /**
     * @since MMBase-1.8
     */
    private CacheImplementationInterface<K, V> implementation;

    protected volatile Object lock; //

    /**
     * The number of times an element was successfully retrieved from this cache.
     */
    private long hits = 0;

    /**
     * The number of times an element could not be retrieved from this cache.
     */
    private long misses = 0;

    /**
     * The number of times an element was committed to this cache.
     */
    private long puts = 0;

    public Cache(int size) {
        // See: http://www.mmbase.org/jira/browse/MMB-1486
        implementation = new LRUCache<K, V>(size);
        lock           = implementation.getLock();
        //implementation = new LRUHashtable<K, V>(size);

        log.service("Creating cache " + getName() + ": " + getDescription());
    }

    @SuppressWarnings("unchecked")
    void setImplementation(String clazz, Map<String,String> configValues) {
        synchronized(lock) {
            clear();
            try {

                Class<?> clas = Class.forName(clazz);
                if (implementation == null || (! clas.equals(implementation.getClass()))) {
                    log.info("Setting implementation of " + this + " to " + clas);
                    implementation = (CacheImplementationInterface<K,V>) clas.newInstance();
                    implementation.config(configValues);
                    lock = implementation.getLock();
                }
            } catch (ClassNotFoundException cnfe) {
                log.error("For cache " + this + " " + cnfe.getClass().getName() + ": " + cnfe.getMessage());
            } catch (InstantiationException ie) {
                log.error("For cache " + this + " " + ie.getClass().getName() + ": " + ie.getMessage());
            } catch (IllegalAccessException iae) {
                log.error("For cache " + this + " " + iae.getClass().getName() + ": " + iae.getMessage());
            }
        }
    }

    /**
     * If you want to structurally modify this cache, synchronize on this object.
     *
     * @since MMBase-1.8.6
     */
    public final Object getLock() {
        return lock;
    }

    /**
     * Returns a name for this cache type. Default it is the class
     * name, but this normally will be overriden.
     */
    @Override
    public String getName() {
        return getClass().getName();
    }

    /**
     * Gives a description for this cache type. This can be used in
     * cache overviews.
     */
    @Override
    public String getDescription() {
        return "An all purpose Cache";
    }



    /**
     * Return the maximum entry size for the cache in bytes.  If the
     * cache-type supports it (default no), then no values bigger then
     * this will be stored in the cache.
     */
    @Override
    public int getMaxEntrySize() {
        if (getDefaultMaxEntrySize() > 0) {
            return maxEntrySize;
        } else {
            return -1;
        }
    }

    /**
     * @since MMBase-1.9.2
     */
    @Override
    public void setMaxEntrySize(int i) {
        if (getDefaultMaxEntrySize() > 0) {
            maxEntrySize = i;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns the average 'length' of the values in the cache. Whatever that may mean. A natural meaning may be the length of the cached lists.
     *
     * May return <code>NaN</code> if unknown or undetermined.
     * @since MMBase-1.9.2
     */
    @Override
    public double getAverageValueLength() {
        return Double.NaN;
    }

    /**
     * This has to be overridden by Caches which support max entry size.
     */

    protected int getDefaultMaxEntrySize() {
        return -1;
    }

    @Override
    public Set<Map.Entry<K,V>> entrySet() {
        if (! active) {
            return new HashSet<Map.Entry<K,V>>();
        }
        return implementation.entrySet();
    }

    /**
     * @since MMBase-1.8.6
     */
    public Class<?> getImplementation() {
        return implementation.getClass();
    }

    /**
     * Checks whether the key object should be cached.
     * This method returns <code>false</code> if either the current cache is inactive, or the object to cache
     * has a cache policy associated that prohibits caching of the object.
     * @param key the object to be cached
     * @return <code>true</code> if the object can be cached
     * @since MMBase-1.8
     */
    protected boolean checkCachePolicy(Object key) {
        CachePolicy policy = null;
        if (active) {
            if (key instanceof Cacheable) {
                policy = ((Cacheable)key).getCachePolicy();
                if (policy != null) {
                    return policy.checkPolicy(key);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Like 'get' of Maps but considers if the cache is active or not,  and the cache policy of the key.
     */
    @Override
    public  V get(Object key) {
        if (!checkCachePolicy(key)) {
            return null;
        }
        @SuppressWarnings("element-type-mismatch")
        V res = implementation.get(key);
        if (res != null) {
            hits++;
        } else {
            misses++;
        }
        return res;
    }

    /**
     * Like 'put' of LRUHashtable but considers if the cache is active or not.
     *
     */
    @Override
    public V put(K key, V value) {
        if (!checkCachePolicy(key)) {
            return null;
        }
        puts++;
        return implementation.put(key, value);
    }

    /**
     * Returns the number of times an element was succesfully retrieved
     * from the table.
     */
    @Override
    public long getHits() {
        return hits;
    }

    /**
     * Returns the number of times an element cpould not be retrieved
     * from the table.
     */
    @Override
    public long getMisses() {
        return misses;
    }

    /**
     * Returns the number of times an element was committed to the table.
     */
    @Override
    public long getPuts() {
        return puts;
    }

    /**
     * Reset 'puts', 'misses' and 'puts' to 0.
     * @since MMBase-1.9.2
     */
    @Override
    public void reset() {
        hits = 0; misses = 0; puts = 0;
    }

    @Override
    public  void setMaxSize(int size) {
        implementation.setMaxSize(size);
    }
    public  int maxSize() {
        return implementation.maxSize();
    }
    @Override
    public int getMaxSize() {
        return maxSize();
    }

    /**
     * @see java.util.Map#size()
     */
    @Override
    public  int size() {
        return implementation.size();
    }

    @Override
    public int getSize() {
        return size();
    }
    @SuppressWarnings("element-type-mismatch")
    public  boolean contains(Object key) {
        return implementation.containsKey(key);
    }

    public int getCount(K key) {
        return implementation.getCount(key);
    }

    /**
     * Returns the ratio of hits and misses.
     * The higher the ratio, the more succesfull the table retrieval is.
     * A value of 1 means every attempt to retrieve data is succesfull,
     * while a value nearing 0 means most times the object requested it is
     * not available.
     * Generally a high ratio means the table can be shrunk, while a low ratio
     * means its size needs to be increased.
     *
     * @return A double between 0 and 1 or NaN.
     */
    @Override
    public double getRatio() {
        return ((double) hits) / (  hits + misses );
    }


    /**
     * Returns statistics on this table.
     * The information shown includes number of accesses, ratio of misses and hits,
     * current size, and number of puts.
     */
    public String getStats() {
        return "Access "+ (hits + misses) + " Ratio " + getRatio() + " Size " + size() + " Puts " + puts;
    }


    /**
     * Sets this cache to active or passive.
     * TODO: Writing back to caches.xml if necessary (if this call was nog caused by change of caches.xml itself)
     */
    @Override
    public void setActive(boolean a) {
        active = a;
        if (! active) {
            implementation.clear();
        }
        // inactive caches cannot contain anything
        // another option would be to override also the 'contains' methods (which you problable should not use any way)
    }

    @Override
    public String toString() {
        return "Cache " + getName() + ", Ratio: " + getRatio() + " " + implementation;
    }

    /**
     * Wether this cache is active or not.
     */
    @Override
    public final boolean isActive() {
        return active;
    }

    @Override
    public int getByteSize() {
        return getByteSize(new SizeOf());
    }

    @Override
    public int getByteSize(SizeOf sizeof) {
        int size = 26;
        if (implementation instanceof SizeMeasurable) {
            size += ((SizeMeasurable) implementation).getByteSize(sizeof);
        } else {
            // sizeof.sizeof(implementation) does not work because this.equals(implementation)
            synchronized(lock) {
                for (Map.Entry<K, V> entry : implementation.entrySet()) {
                    size += sizeof.sizeof(entry.getKey());
                    size += sizeof.sizeof(entry.getValue());
                }
            }
        }
        return size;
    }

    /**
     * Returns the sum of bytesizes of every key and value. This may count too much, because objects
     * (like Nodes) may occur in more then one value, but this is considerably cheaper then {@link
     * #getByteSize()}, which has to keep a Collection of every counted object.
     * @since MMBase-1.8
     */
    public int getCheapByteSize() {
        int size = 0;
        SizeOf sizeof = new SizeOf();
        synchronized(lock) {
            for (Map.Entry<K, V> entry : implementation.entrySet()) {
                size += sizeof.sizeof(entry.getKey());
                size += sizeof.sizeof(entry.getValue());
                sizeof.clear();
            }
        }
        return size;
    }


    /**
     * @see java.util.Map#clear()
     */
    @Override
    public void clear() {
        implementation.clear();
    }


    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean containsKey(Object key) {
        return implementation.containsKey(key);
    }


    /**
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean containsValue(Object value) {
        return implementation.containsValue(value);
    }


    /**
     * @see java.util.Map#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        // odd, but this is accordinding to javadoc of Map.
        if (o == this)
            return true;

        if (!(o instanceof Cache))
            return false;
        Cache<?,?> c = (Cache<?,?>) o;
        if (!c.getName().equals(getName())) {
            return false;
        }
        return implementation.equals(o);
    }


    /**
     * @see java.util.Map#hashCode()
     */
    @Override
    public int hashCode() {
        int hash = getName().hashCode();
        hash = HashCodeUtil.hashCode(hash, implementation.hashCode());
        return hash;
    }


    /**
     * @see java.util.Map#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return implementation.isEmpty();
    }


    /**
     * @see java.util.Map#keySet()
     */
    @Override
    public Set<K> keySet() {
        return implementation.keySet();
    }


    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends K,? extends V> t) {
        implementation.putAll(t);
    }


    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    @Override
    @SuppressWarnings("element-type-mismatch")
    public V remove(Object key) {
        return implementation.remove(key);
    }


    /**
     * @see java.util.Map#values()
     */
    @Override
    public Collection<V> values() {
        return implementation.values();
    }


    /**
     * Puts this cache in the caches repository.
     * @see CacheManager#putCache(Cache)
     */

    public Cache<K,V> putCache() {
        return CacheManager.putCache(this);
    }

    /**
     * @since MMBase-2.0
     */
    public void configure(Element cacheElement) {

        String clazz = DocumentReader.getElementValue(DocumentReader.getElementByPath(cacheElement, "cache.implementation.class"));
        if(!"".equals(clazz)) {
            Element cacheImpl = DocumentReader.getElementByPath(cacheElement, "cache.implementation");
            Map<String,String> configValues = new HashMap<String,String>();
            for (Element attrNode: DocumentReader.getChildElements(cacheImpl, "param")) {
                String paramName = attrNode.getAttribute("name");
                String paramValue = DocumentReader.getElementValue(attrNode);
                configValues.put(paramName, paramValue);
            }
            setImplementation(clazz, configValues);
        }
        String status = DocumentReader.getElementValue(DocumentReader.getElementByPath(cacheElement, "cache.status"));
        setActive(status.equalsIgnoreCase("active"));
        try {
            Integer size = Integer.valueOf(DocumentReader.getElementValue(DocumentReader.getElementByPath(cacheElement, "cache.size")));
            setMaxSize(size.intValue());
            log.service("Setting " + getName() + " " + status + " with size " + size);
        } catch (NumberFormatException nfe) {
            log.error("Could not configure cache " + getName() + " because the size was wrong: " + nfe.toString());
        } catch (Throwable t) {
            log.error(" " + getName() + " maxsize " + t.getMessage());
        }
        String maxSize = DocumentReader.getElementValue(DocumentReader.getElementByPath(cacheElement, "cache.maxEntrySize"));
        if (!"".equals(maxSize)) {
            try {
                maxEntrySize = Integer.parseInt(maxSize);
                log.service("Setting maximum entry size on " + getName() + ": " + maxEntrySize + " bytes ");
            } catch (NumberFormatException nfe2) {
                log.error("Could not set max entry size cache  of " + getName() + " because " + nfe2.toString());
            } catch (Throwable t) {
                log.error(" " + getName() + " maxentrysize " + t.getMessage());
            }
        } else {
            if (getDefaultMaxEntrySize() > 0) {
                log.service("No max entry size specified for this cache taking default " + getDefaultMaxEntrySize() + " bytes");
            }
            maxEntrySize = getDefaultMaxEntrySize();
        }

    }
}
