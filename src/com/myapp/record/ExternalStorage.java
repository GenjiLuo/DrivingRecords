package com.myapp.record;

import java.io.File;
import android.os.StatFs;


public class ExternalStorage {
	public static String getStorageDirectory() {
		long primarySize = 0, secondarySize = 0;
		String primaryPath, secondaryPath;
		
		secondaryPath = System.getenv("SECONDARY_STORAGE");
		if (secondaryPath != null) {
			File file = new File(secondaryPath);
			if (file.isDirectory() && file.canRead() && file.canWrite())
				secondarySize = getTotalStorageSize(secondaryPath);
		}
		
		primaryPath = System.getenv("EXTERNAL_STORAGE");
		primarySize = getTotalStorageSize(primaryPath);
		
		return primarySize >= secondarySize ? primaryPath : secondaryPath;
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
}
