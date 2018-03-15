package utilities;

public abstract class StringManipulations {
	/**
	 * A method to turn the first letter of a string into a lower case letter.
	 * 
	 * @param name
	 *            - the string that should have a lower case character at the
	 *            beginning
	 * @return the input string with its first letter now in lower case.
	 */
	public static String firstCharToLowerCase(String name) {
		String lowerCaseName = name;
		if (!Character.isLowerCase(lowerCaseName.charAt(0))) {
			lowerCaseName = Character.toLowerCase(lowerCaseName.charAt(0)) + lowerCaseName.substring(1);
		}
		return lowerCaseName;
	}

	/**
	 * A method to turn the first letter of a string into a upper case letter.
	 * 
	 * @param name
	 *            - the string that should have an upper case character at the
	 *            beginning
	 * @return the input string with its first letter now in upper case.
	 */
	public static String firstCharToUpperCase(String name) {
		String upperCaseName = name;
		if (!Character.isUpperCase(upperCaseName.charAt(0))) {
			upperCaseName = Character.toUpperCase(upperCaseName.charAt(0)) + upperCaseName.substring(1);
		}
		return upperCaseName;
	}

}
