/*
 * Copyright 2012 Stormpath, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stormpath.sdk.impl.resource;

import com.stormpath.sdk.impl.ds.InternalDataStore;
import com.stormpath.sdk.lang.StringUtils;
import com.stormpath.sdk.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @since 0.1
 */
public abstract class AbstractResource implements Resource {

    private static final Logger log = LoggerFactory.getLogger(AbstractResource.class);

    public static final String HREF_PROP_NAME = "href";

    private final Map<String, Object> properties;       //Protected by read/write lock
    private final Map<String, Object> dirtyProperties;  //Protected by read/write lock
    private final InternalDataStore dataStore;
    protected final Lock readLock;
    protected final Lock writeLock;

    private volatile boolean materialized;
    private volatile boolean dirty;

    protected AbstractResource(InternalDataStore dataStore) {
        this(dataStore, null);
    }

    protected AbstractResource(InternalDataStore dataStore, Map<String, Object> properties) {
        ReadWriteLock rwl = new ReentrantReadWriteLock();
        this.readLock = rwl.readLock();
        this.writeLock = rwl.writeLock();
        this.dataStore = dataStore;
        this.properties = new LinkedHashMap<String, Object>();
        this.dirtyProperties = new LinkedHashMap<String, Object>();
        setProperties(properties);
    }

    public final void setProperties(Map<String, Object> properties) {
        writeLock.lock();
        try {
            this.properties.clear();
            this.dirtyProperties.clear();
            this.dirty = false;
            if (properties != null && !properties.isEmpty()) {
                this.properties.putAll(properties);
                // Don't consider this resource materialized if it is only a reference.  A reference is any object that
                // has only one 'href' property.
                boolean hrefOnly = this.properties.size() == 1 && this.properties.containsKey(HREF_PROP_NAME);
                this.materialized = !hrefOnly;
            } else {
                this.materialized = false;
            }
        } finally {
            writeLock.unlock();
        }
    }

    public String getHref() {
        return getStringProperty(HREF_PROP_NAME);
    }

    protected final InternalDataStore getDataStore() {
        return this.dataStore;
    }

    protected final boolean isMaterialized() {
        return this.materialized;
    }

    protected final boolean isDirty() {
        return this.dirty;
    }

    /**
     * Returns {@code true} if the resource doesn't yet have an assigned 'href' property, {@code false} otherwise.
     *
     * @return {@code true} if the resource doesn't yet have an assigned 'href' property, {@code false} otherwise.
     * @since 0.2
     */
    protected final boolean isNew() {
        //we can't call getHref() in here, otherwise we'll have an infinite loop:
        Object prop = readProperty(HREF_PROP_NAME);
        if (prop == null) {
            return true;
        }
        String href = String.valueOf(prop);
        return !StringUtils.hasText(href);
    }

    protected void materialize() {
        AbstractResource resource = dataStore.getResource(getHref(), getClass());
        writeLock.lock();
        try {
            this.properties.clear();
            this.properties.putAll(resource.properties);

            //retain dirty properties:
            this.properties.putAll(this.dirtyProperties);

            this.materialized = true;
        } finally {
            writeLock.unlock();
        }
    }

    public Set<String> getPropertyNames() {
        readLock.lock();
        try {
            Set<String> keys = this.properties.keySet();
            return new LinkedHashSet<String>(keys);
        } finally {
            readLock.unlock();
        }
    }

    public Object getProperty(String name) {
        if (!HREF_PROP_NAME.equals(name)) {
            //not the href/id, must be a property that requires materialization:
            if (!isNew() && !isMaterialized()) {

                //only materialize if the property hasn't been set previously (no need to execute a server
                // request since we have the most recent value already):
                boolean present = false;
                readLock.lock();
                try {
                    present = this.dirtyProperties.containsKey(name);
                } finally {
                    readLock.unlock();
                }

                if (!present) {
                    //exhausted present properties - we require a server call:
                    materialize();
                }
            }
        }

        return readProperty(name);
    }

    private Object readProperty(String name) {
        readLock.lock();
        try {
            return this.properties.get(name);
        } finally {
            readLock.unlock();
        }
    }

    protected void setProperty(String name, Object value) {
        setProperty(name, value, true);
    }

    /**
     * @since 0.6.0
     */
    protected void setProperty(String name, Object value, final boolean dirty) {
        writeLock.lock();
        try {
            this.properties.put(name, value);
            if (dirty) {
                this.dirtyProperties.put(name, value);
                this.dirty = true;
            }
        } finally {
            writeLock.unlock();
        }

    }

    protected String getStringProperty(String key) {
        Object value = getProperty(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    protected int getIntProperty(String key) {
        Object value = getProperty(key);
        if (value != null) {
            if (value instanceof String) {
                return parseInt((String) value);
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Resource> T getResourceProperty(String key, Class<T> clazz) {
        Object value = getProperty(key);
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return (T)value;
        }
        if (value instanceof Map && !((Map) value).isEmpty()) {
            T resource = dataStore.instantiate(clazz, (Map<String, Object>) value);

            //replace the existing link object (map with an href) with the newly constructed Resource instance.
            //Don't dirty the instance - we're just swapping out a property that already exists for the materialized version.
            setProperty(key, resource, false);
            return resource;
        }

        String msg = "'" + key + "' property value type does not match the specified type.  Specified type: " +
                clazz.getName() + ".  Existing type: " + value.getClass().getName();
        msg += (isPrintableProperty(key) ? ".  Value: " + value : ".");
        throw new IllegalArgumentException(msg);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            if (log.isErrorEnabled()) {
                String msg = "Unabled to parse string '{}' into an integer value.  Defaulting to -1";
                log.error(msg, e);
            }
        }
        return -1;
    }

    public String toString() {
        readLock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : this.properties.entrySet()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                String key = entry.getKey();
                //prevent printing of any sensitive values:
                if (isPrintableProperty(key)) {
                    sb.append(key).append(": ").append(String.valueOf(entry.getValue()));
                }
            }
            return sb.toString();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns {@code true} if the internal property is safe to print in toString(), {@code false} otherwise.
     *
     * @param name The name of the property to check for safe printing
     * @return {@code true} if the internal property is safe to print in toString(), {@code false} otherwise.
     * @since 0.4.1
     */
    protected boolean isPrintableProperty(String name) {
        return true;
    }

    @Override
    public int hashCode() {
        readLock.lock();
        try {
            return this.properties.isEmpty() ? 0 : this.properties.hashCode();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (!o.getClass().equals(getClass())) {
            return false;
        }
        AbstractResource other = (AbstractResource) o;
        readLock.lock();
        try {
            other.readLock.lock();
            try {
                return this.properties.equals(other.properties);
            } finally {
                other.readLock.unlock();
            }
        } finally {
            readLock.unlock();
        }
    }
}
