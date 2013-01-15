package net.openwatch.reporter;

import android.content.Context;
import net.openwatch.reporter.model.OWVideoRecording;

public interface OWRecordingBackedEntity {

	public void populateViews(OWVideoRecording recording, Context app_context);
}
