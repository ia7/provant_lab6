package wallOfTweets;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Parameters {
	private static final String BUNDLE_NAME = "wallOfTweets.parameters"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private Parameters() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
