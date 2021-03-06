package com.bdt.kiradb;

import com.thoughtworks.xstream.XStream;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import java.io.IOException;
import java.io.InputStream;

public class CacheBackingStore extends BackingStore {

	 private static CacheManager mgr;
     private static Cache cache;

     public CacheBackingStore() throws KiraException {
         InputStream inputStream = getClass().getResourceAsStream("/ehcache-kiradb.xml");

    	 mgr = new CacheManager(inputStream);
         cache = mgr.getCache("KiraDB");
         if (cache == null) {
        	throw new KiraException("Cannot configure cache"); 
         }
     }

	@Override
	void storeObject(XStream xstream, Record r) {
		String key = makeKey(r);
		//System.out.println("r: " + r);
		//System.out.println(" primaryKey: " + r.getPrimaryKeyName() + " key: " + key);
		//System.out.println("cache: " + cache);
        cache.put(new Element(key, r));
	}

	@Override
	public<T extends Record> T retrieveObject(XStream xstream, Record r, String value) {
		String key = makeKey(r, value);

		Element element = cache.get(key);
        if (element != null) {
                return (T) element.getObjectValue();
        }
		return null;
	}

	@Override
	void removeObject(XStream xstream, Record r, String value) {
		String key = makeKey(r, value);
        cache.remove(key);

	}

	@Override
	public<T extends Record> T firstObject(XStream xstream, Record r) throws KiraException,
			IOException, ClassNotFoundException {
		return null;
	}

	@Override
	public<T extends Record> T nextObject(XStream xstream, Record r) throws KiraException,
			IOException, ClassNotFoundException {
		return null;
	}
}
