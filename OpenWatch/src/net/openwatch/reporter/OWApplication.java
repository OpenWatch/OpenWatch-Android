package net.openwatch.reporter;

import net.openwatch.reporter.remoteimage.OWRemoteImageLoader;

import com.github.ignition.core.widgets.RemoteImageView;

import android.app.Application;

public class OWApplication extends Application {
	// Keep track of whether per-launch actions have been performed
	public static boolean per_launch_sync = false;
	
	public void onCreate (){
		RemoteImageView.setSharedImageLoader(new OWRemoteImageLoader(this.getApplicationContext()));
	}

}
