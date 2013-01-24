package net.openwatch.reporter.share;

import android.content.Context;
import android.content.Intent;

public class Share {
	
	public static void showShareDialog(Context c, String dialog_title, String url){
		
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_TEXT, url);
		c.startActivity(Intent.createChooser(i, dialog_title));
	}

}
