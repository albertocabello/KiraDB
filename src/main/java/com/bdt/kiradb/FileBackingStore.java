package com.bdt.kiradb;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileLock;
import java.util.logging.Logger;

/**
 * @author David Beckemeyer and Mark Petrovic
 */
public class FileBackingStore extends BackingStore {

    private Logger logger = Logger.getLogger(FileBackingStore.class.getName());
	private File rootpath;
	private File[] filesList;
	private int filesIndex;
	
	private static final long WAIT_TIME = 100L;
    private static final long LOCK_TIMEOUT = 5000L;

    private static final String LOCKPATH = "/locks/";
    
    public FileBackingStore(File path) {
    	this.rootpath = path;
    }
    
	private File lock(String group, String key) {
		String odir = new File(rootpath, LOCKPATH + group).getAbsolutePath();
	    boolean exists = (new File(odir)).exists();
	    if (!exists) {
		    boolean status;
	    	status = new File(odir).mkdirs();
	    	//logger.info("lock: " + group + "/" + key + " dir " + odir + " " + (status ? "success" : "failure"));
	    }
	    //logger.info("lock: " + odir + "/" + key);
	    File lockfile = new File(odir, key);
	    try {
			long waited = 0;
			boolean fl = false;
			while(waited <= LOCK_TIMEOUT && !(fl = lockfile.mkdir())) {
				logger.info("lock in use " + group + "/" + key + " retrying in " + WAIT_TIME + "ms");
				try {Thread.sleep(WAIT_TIME);} catch(InterruptedException ie) { logger.warning("Lock InterruptedException on " + group + "/" + key); }
				waited += WAIT_TIME;
			}
			if (!fl) {
			     // TIMEOUT!
				logger.severe("Lock Timeout on " + group + "/" + key);
				return null;
			}
			//logger.info("got lock on " + group + "/" + key);
			return lockfile;

		} catch (Exception e) {
			e.printStackTrace();
			logger.severe("lock Exception group: " + group + " key: " + key + " " + e.getMessage());
			return null;
		}

	}

	private void unlock(File lockfile) {
		lockfile.delete();
	}

	@Override
	public void storeObject(XStream xstream, Record r) throws IOException,
			KiraException {
		if (r.getRecordName().contains("/")) {
			logger.warning("storeObject: Invalid record name: " + r.getRecordName());
			throw new KiraException("storeObject: Invalid record name: " + r.getRecordName());
		}
		if (r.getPrimaryKeyName().contains("/")) {
			logger.warning("storeObject: Invalid primary key name: " + r.getPrimaryKeyName());
			throw new KiraException("storeObject: Invalid primary key name: " + r.getPrimaryKeyName());

		}
		String key = makeKey(r);

		File lck = lock(r.getRecordName(), (String)r.descriptor().getPrimaryKey().getValue());
		if (lck == null) {
			logger.severe("storeObject: cannot lock key: " + key);
			throw new KiraException("storeObject: cannot lock key: " + key);
		}
		try {
			FileOutputStream fos;
			String odir = new File(rootpath, "/" + r.getRecordName()).getAbsolutePath();
			boolean exists = (new File(odir)).exists();
			if (!exists) {
				boolean status;
				status = new File(odir).mkdir();
				//logger.info(odir + " " + (status ? "success" : "failure"));
			}
			try {
				fos = new FileOutputStream(new File(rootpath,  "/" + key));
				ObjectOutputStream oos = xstream.createObjectOutputStream(fos);

				FileLock lock = fos.getChannel().lock();

				try {
					oos.writeObject(r);
					// Flush and close the ObjectOutputStream.
					//
					oos.flush();

				} finally {
					lock.release();
				}
				oos.close();
			} catch (Exception e) {
				logger.severe("failed to store Object " + key);
				throw new KiraException("storeObject: failed to store Object " + key + " error: " + e.getMessage());

			}
		} finally {
			unlock(lck);
		}

	}

	@Override
	public<T extends Record> T retrieveObject(XStream xstream, Record r, String value)
			throws KiraException, IOException, ClassNotFoundException {
		String key = makeKey(r, value);
		
		File lck = lock(r.getRecordName(), value);
		if (lck == null)
			throw new KiraException("retrieveObject cannot obtain lock on " + value);
		
		Object result = null;
		try {
			FileInputStream fis;

			try {
				fis = new FileInputStream(new File(rootpath, "/" + key));
			} catch (FileNotFoundException e) {
				return null;
			}

			ObjectInputStream ois;

			ois = xstream.createObjectInputStream(fis);

			result = ois.readObject();
			ois.close();
		} finally {
			unlock(lck);
		}
		return (T)result;
	}

	public void removeObject(XStream xstream, Record r, String value) throws KiraException {
		String key = makeKey(r, value);
		File lck = lock(r.getRecordName(), value);
		if (lck == null)
			throw new KiraException("removeObject cannot obtain lock on " + value);

		try {
                 File f;
                 f = new File(rootpath + "/" + key);
                 f.delete();

         } finally {
                 unlock(lck);
         }

	}

	@Override
	public<T extends Record> T firstObject(XStream xstream, Record r) throws KiraException, IOException, ClassNotFoundException {
        File directory = new File(rootpath + "/" + r.getRecordName());
        filesList = directory.listFiles();
        filesIndex = 0;
		return (T)nextObject(xstream, r);
	}

	@Override
    public<T extends Record> T nextObject(XStream xstream, Record r) throws KiraException, IOException, ClassNotFoundException {
		while (filesIndex < filesList.length) {
			File fileEntry = filesList[filesIndex++];
			if (fileEntry.isFile()) {
				return retrieveObject(xstream, r, fileEntry.getName());
			}
		}
		return null;
	}
}
