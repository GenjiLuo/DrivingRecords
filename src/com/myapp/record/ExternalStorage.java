package com.myapp.record;

import java.io.File;
import android.os.StatFs;


public class ExternalStorage {
	//public static int PRIMARY_STORAGE = 1;
	//public static int SECONDARY_STORAGE = 2;
	
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
		
/*		
		String cmd = "cat /proc/mounts";
        Runtime run = Runtime.getRuntime();	//返回与当前 Java 应用程序相关的运行时对象
        
        try {
            Process p = run.exec(cmd); //启动另一个进程来执行命令
            BufferedInputStream in = new BufferedInputStream(p.getInputStream());
            BufferedReader inBr = new BufferedReader(new InputStreamReader(in));

            String lineStr;
            while ((lineStr = inBr.readLine()) != null) {
                // 获得命令执行后在控制台的输出信息
                if (lineStr.contains("sdcard")
                        && lineStr.contains(".android_secure")) {
                    String[] strArray = lineStr.split(" ");
                    if (strArray != null && strArray.length >= 5) {
                        String result = strArray[1].replace("/.android_secure",
                                "");
                        return result;
                    }
                }
            }
            
            inBr.close();
            in.close();
        } catch (Exception e) {
            //return Environment.getExternalStorageDirectory().getPath();
        	return null;
        }

        //return Environment.getExternalStorageDirectory().getPath();
        return null;
*/        
	}
	
	/*
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
	*/
	
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
	
	//public static class StorageDirectory {
	//	File directory;
	//	int type;
	//}
}
