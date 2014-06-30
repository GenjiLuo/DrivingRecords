package com.myapp.record;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class RecordFileList {
	
	public static ArrayList<FileInfo> getFilelist(String dirPath, String ext) {
		if (dirPath == null)
			return null;
		
		File storageDir = new File(dirPath);
		File[] files =storageDir.listFiles(getFileExtensionFilter(ext));
		if (files == null)
			return null;
		
		ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();
		
		for (int i = 0; i < files.length; i++) {  
            File file = files[i];  
            FileInfo fileInfo = new FileInfo();  
            fileInfo.name = file.getName();  
            fileInfo.path = file.getPath();  
            fileInfo.lastModified= file.lastModified();  
            fileInfo.title = fileInfo.name;
            
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
            	return 1;  
            else 
            	return -1;  
        }  
    }  
	
	public static class FileInfo {
		String name;
		String path;
		String title;
		long lastModified;
	}
}
