package net.openwatch.reporter;

import java.util.UUID;

public class OWUtils {
	
	public static String generateRecordingIdentifier()
	{
		return UUID.randomUUID().toString();
	}

}
