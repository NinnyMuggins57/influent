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
}
