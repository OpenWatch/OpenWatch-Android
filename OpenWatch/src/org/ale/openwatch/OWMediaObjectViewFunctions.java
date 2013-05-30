package org.ale.openwatch;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

/**
 * An excercise in determinging what's in common between
 * the OWMediaObject viewing activities for a later refactor
 * @author davidbrodsky
 *
 */
public class OWMediaObjectViewFunctions {
	
	// Stub
	public static void showDeleteDialog(Context c, int model_id){
		AlertDialog.Builder builder = new AlertDialog.Builder(c);
		builder.setTitle("Delete Local Files?")
		.setMessage("Eventually, this function will allow you to remove any local files associated with this recording on your device.")
		.setPositiveButton("Ok!", new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
			
		}).show();
	}

}
