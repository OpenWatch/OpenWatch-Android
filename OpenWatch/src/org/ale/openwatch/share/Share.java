package org.ale.openwatch.share;

import android.content.Context;
import android.content.Intent;
import org.ale.openwatch.R;

public class Share {

	public static void showShareDialog(Context c, String dialog_title, String url){
		
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_TEXT, url);
		c.startActivity(Intent.createChooser(i, dialog_title));
	}

    public static void showShareDialogWithInfo(Context c, String dialog_title, String item_title,  String url){
        String toShare = url;
        if(item_title != null)
            toShare += "\n" + item_title;
        toShare += "\n" + c.getString(R.string.via_openwatch);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, toShare);
        c.startActivity(Intent.createChooser(i, dialog_title));
    }

}
