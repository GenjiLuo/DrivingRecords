package com.myapp.record;

import java.io.File;

import android.os.StatFs;

public class ExternalStorage {
	public static int PRIMARY_STORAGE = 1;
	public static int SECONDARY_STORAGE = 2;
	
	public static StorageDirectory getStorageDirectory() {
		StorageDirectory dir = new StorageDirectory();
		
		String path = System.getenv("SECONDARY_STORAGE");
		if (path != null) {
			File file = new File(path);
			if (file.isDirectory() && file.canRead() && file.canWrite()) {
				dir.directory = file;
				dir.type = SECONDARY_STORAGE;	
				return dir;
			}
		}
		
		path = System.getenv("EXTERNAL_STORAGE");
		File file = new File(path);
		dir.directory = file;
		dir.type = PRIMARY_STORAGE;	
		return dir;
	}
	
	public static long getAvailableStorageSize(String path) {
		StatFs stat = new StatFs(path);
           
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return availableBlocks*blockSize;
	}

	public static long getTotalStorageSize(String path) {
		StatFs stat = new StatFs(path);
	   
		long blockSize = stat.getBlockSize();
		long totalBlocks = stat.getBlockCount();
		return totalBlocks*blockSize;
	}
	
	public static boolean checkAvailable(String path) {
		File f = new File(path);
		if (f.isDirectory() && f.canRead() && f.canWrite()) 
			return true;
		else
			return false;
	}
	
	public static class StorageDirectory {
		File directory;
		int type;
	}
}
