package org.ale.openwatch.share;

import android.content.Context;
import android.content.Intent;
import org.ale.openwatch.OWUtils;
import org.ale.openwatch.R;
import org.ale.openwatch.model.OWServerObject;

public class Share {

	public static void showShareDialog(Context c, String dialog_title, String url){
		
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_TEXT, url);
		c.startActivity(Intent.createChooser(i, dialog_title));
	}

    public static void showShareDialogWithInfo(Context c, String dialog_title, String item_title,  String url){
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, generateShareText(c, item_title, url));
        c.startActivity(Intent.createChooser(i, dialog_title));
    }

    public static String generateShareText(Context c, OWServerObject serverObject){
        return generateShareText(c, serverObject.getTitle(c), OWUtils.urlForOWServerObject(serverObject, c));
    }

    public static String generateShareText(Context c, String title, String url){
        String toShare = url;
        if(title != null)
            toShare += "\n" + title;
        toShare += "\n" + c.getString(R.string.via_openwatch);
        return toShare;
    }

}
