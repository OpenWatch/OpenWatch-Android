package org.ale.occupygezi;

import org.ale.occupygezi.model.OWServerObject;

import android.content.Context;

public interface OWMediaObjectBackedEntity {

	public void populateViews(OWServerObject media_object, Context app_context);
}
