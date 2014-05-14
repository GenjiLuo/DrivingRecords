package com.myapp.record;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;


public class BitmapCache {
	private static final long DISK_CACHE_SIZE = 1024*1024*50;  //50MB
    private static final int DISK_CACHE_INDEX = 0;
	private static final String DISK_CACHE_SUBDIR = "thumbnails";
	
	private static LruCache<String, Bitmap> mMemoryCache = null; 
	private static DiskLruCache mDiskCache = null;
	private static final Object mDiskCacheLock = new Object();
	private static boolean mDiskCacheStarting = true;
	
	public static void initBitmapCache(Context context) {
		if (mMemoryCache == null) {
			// Get max available VM memory, exceeding this amount will throw an
			// OutOfMemory exception. Stored in kilobytes as LruCache takes an
			// int in its constructor.
			final int maxMemory = (int)(Runtime.getRuntime().maxMemory()/1024);

			// Use 1/16th of the available memory for this memory cache.
			final int cacheSize = maxMemory/16;

			mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
				@Override
				protected int sizeOf(String key, Bitmap bitmap) {
					// The cache size will be measured in kilobytes rather than
					// number of items.
					return bitmap.getByteCount()/1024;
				}
			};
		}	
		
		if (mDiskCache == null) {
			File cacheDir = getDiskCacheDir(context, DISK_CACHE_SUBDIR);
		    new InitDiskCacheTask().execute(cacheDir);
		}
	}
	
	private static File getDiskCacheDir(Context context, String uniqueName) {
	    final String cachePath = context.getCacheDir().getPath();
	    return new File(cachePath + File.separator + uniqueName);
	}
	
	private static class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
	    @Override
	    protected Void doInBackground(File... params) {
	        synchronized (mDiskCacheLock) {
	            File cacheDir = params[0];
	            
                try {
                	mDiskCache = DiskLruCache.open(cacheDir, 1, 1, DISK_CACHE_SIZE);
                } catch (final IOException e) {
                	mDiskCache = null;
                }
                
                mDiskCacheStarting = false;
                mDiskCacheLock.notifyAll(); // Wake any waiting threads
	        }
	        
	        return null;
	    }
	}
	
	public static void addBitmapToCache(String key, Bitmap bitmap) {
		if (mMemoryCache.get(key) == null) 
			mMemoryCache.put(key, bitmap);
		
		synchronized (mDiskCacheLock) {
			// Add to disk cache
			if (mDiskCache != null) {
				OutputStream out = null;
                
                try {
                    DiskLruCache.Snapshot snapshot = mDiskCache.get(key);
                    if (snapshot == null) {
                        final DiskLruCache.Editor editor = mDiskCache.edit(key);
                        if (editor != null) {
                            out = editor.newOutputStream(DISK_CACHE_INDEX);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            editor.commit();
                            out.close();
                        }
                    } else {
                        snapshot.getInputStream(DISK_CACHE_INDEX).close();
                    }
                } catch (final IOException e) {
                    //Log.e(TAG, "addBitmapToCache - " + e);
                } catch (Exception e) {
                    //Log.e(TAG, "addBitmapToCache - " + e);
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {}
                }
            }
        }
	}

	public static Bitmap getBitmapFromCache(String key) {
		Bitmap bitmap = mMemoryCache.get(key);
		if (bitmap != null)
			return bitmap;
		
		synchronized (mDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {}
            }
            
            if (mDiskCache != null) {
                InputStream inputStream = null;
                
                try {
                    final DiskLruCache.Snapshot snapshot = mDiskCache.get(key);
                    if (snapshot != null) {
                        //if (BuildConfig.DEBUG) {
                            //Log.d(TAG, "Disk cache hit");
                        //}
                        inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
                        if (inputStream != null) {
                            FileDescriptor fd = ((FileInputStream) inputStream).getFD();
 
                            // Decode bitmap, but we don't want to sample so give
                            // MAX_VALUE as the target dimensions
                            bitmap = BitmapFactory.decodeFileDescriptor(fd);
                            mMemoryCache.put(key, bitmap);
                        }
                    }
                } catch (final IOException e) {
                    //Log.e(TAG, "getBitmapFromDiskCache - " + e);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {}
                }
            }
            return bitmap;
        }
	}
}
