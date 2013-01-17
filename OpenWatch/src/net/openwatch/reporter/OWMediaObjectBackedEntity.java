package net.openwatch.reporter;

import android.content.Context;
import net.openwatch.reporter.model.OWMediaObject;
import net.openwatch.reporter.model.OWVideoRecording;

public interface OWMediaObjectBackedEntity {

	public void populateViews(OWMediaObject media_object, Context app_context);
}
