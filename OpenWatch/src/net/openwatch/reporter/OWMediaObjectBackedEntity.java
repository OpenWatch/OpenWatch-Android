package net.openwatch.reporter;

import android.content.Context;
import net.openwatch.reporter.model.OWServerObject;
import net.openwatch.reporter.model.OWVideoRecording;

public interface OWMediaObjectBackedEntity {

	public void populateViews(OWServerObject media_object, Context app_context);
}
