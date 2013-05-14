package net.openwatch.reporter;

import net.openwatch.reporter.model.OWServerObject;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.webkit.WebView;

public class OWInvestigationViewActivity extends Activity implements OWMediaObjectBackedEntity {
	
	private WebView web_view;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_owinvestigation_view);
		
		web_view = (WebView)findViewById(R.id.web_view);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.owinvestigation_view, menu);
		return true;
	}

	@Override
	public void populateViews(OWServerObject media_object, Context app_context) {
		
	}

}
