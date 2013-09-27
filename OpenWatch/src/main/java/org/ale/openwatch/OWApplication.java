package org.ale.openwatch;

import android.app.Application;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.Map;

public class OWApplication extends Application {
	
	// Keep track of whether per-launch actions have been performed
	public static boolean per_launch_sync = false;
	
	//Hold user info in memory
	public static Map user_data = null;
	
	public void onCreate (){
		super.onCreate();
		ImageLoader.getInstance().init(getImageLoaderConfiguration());
	}
	
	private ImageLoaderConfiguration getImageLoaderConfiguration(){
		DisplayImageOptions displayOptions = new DisplayImageOptions.Builder()
		.cacheInMemory()
		.cacheOnDisc()
        .showImageOnFail(R.drawable.thumbnail_placeholder)
        .showStubImage(R.drawable.thumbnail_placeholder)
		.build();
			
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
		.defaultDisplayImageOptions(displayOptions)
        .memoryCache(new LruMemoryCache(2 * 1024 * 1024))
        .memoryCacheSize(2 * 1024 * 1024)
        .discCacheSize(25 * 1024 * 1024)
        .discCacheFileCount(100)
		.build();
		
		return config;
	}

}
