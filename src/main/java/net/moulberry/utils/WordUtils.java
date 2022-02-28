package net.moulberry.utils;

public class WordUtils {

    /**
     * Sets the first character after a space to title case, and everything else to lower case
     */
    public String capitalizeFully(String input) {
        final char[] buffer = input.toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (ch == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch);
                capitalizeNext = false;
            } else {
                buffer[i] = Character.toLowerCase(ch);
            }
        }
        return new String(buffer);
    }

}
