package org.ale.openwatch;

import org.ale.openwatch.model.OWServerObject;

import android.content.Context;

public interface OWMediaObjectBackedEntity {

	public void populateViews(OWServerObject media_object, Context app_context);
}
