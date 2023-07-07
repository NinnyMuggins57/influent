package influent.server.utilities;

import java.util.UUID;


public class GuidValidator {

	public static boolean validateGuidString(String guid) {
		try {
			UUID.fromString(guid);
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}
	
	
	
	
	public static boolean validateContextString(String context) {

		String[] parts = context.split("_");
		if (parts.length != 2) {
			return false;
		}
		
		if (!parts[0].equalsIgnoreCase("file") && !parts[0].equalsIgnoreCase("column")) {
			return false;
		}
		
		return validateGuidString(parts[1]);
	}
}
