package com.myapp.record;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.os.Environment;

public class RecordFileList {
	
	public static ArrayList<FileInfo> getFilelist(String dir, String ext) {
		File storageDir = new File(Environment.getExternalStorageDirectory(), dir);
		File[] files =storageDir.listFiles(getFileExtensionFilter(ext));
		
		ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();
		
		for (int i = 0; i < files.length; i++) {  
            File file = files[i];  
            FileInfo fileInfo = new FileInfo();  
            fileInfo.name = file.getName();  
            fileInfo.path = file.getPath();  
            fileInfo.lastModified= file.lastModified();   
            
            String s[] = fileInfo.name.split("-");
            fileInfo.title = s[1].substring(0, 4) + "/"
            				+ s[1].substring(4, 6) +"/"
            				+ s[1].substring(6, 8) + " "
            				+ s[2].substring(0, 2) + ":"
            				+ s[2].substring(2, 4) + ":"
            				+ s[2].substring(4, 6);
            
            fileList.add(fileInfo);  
        }  
		
        Collections.sort(fileList, new FileComparator());
        
        return fileList;
	}
	
	private static FilenameFilter getFileExtensionFilter(String extension) {   
		final String _extension = extension;   
		return new FilenameFilter() {   
			public boolean accept(File file, String name) {   
				boolean ret = name.endsWith(_extension);    
				return ret;   
			}   
		};   
	} 
	
	private static class FileComparator implements Comparator<FileInfo> {  
		@Override
        public int compare(FileInfo file1, FileInfo file2) {  
        	if(file1.lastModified < file2.lastModified)   
            	return -1;  
            else 
            	return 1;  
        }  
    }  
	
	public static class FileInfo {
		String name;
		String path;
		String title;
		long lastModified;
	}
}
