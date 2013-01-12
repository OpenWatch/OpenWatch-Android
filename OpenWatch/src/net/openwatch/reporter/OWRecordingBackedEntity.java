package net.openwatch.reporter;

import android.content.Context;
import net.openwatch.reporter.model.OWRecording;

public interface OWRecordingBackedEntity {

	public void populateViews(OWRecording recording, Context app_context);
}
