package net.openwatch.reporter;

import android.content.Context;
import net.openwatch.reporter.model.OWServerObject;

public interface OWMediaObjectBackedEntity {

	public void populateViews(OWServerObject media_object, Context app_context);
}
