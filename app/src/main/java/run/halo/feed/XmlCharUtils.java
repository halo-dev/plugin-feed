package run.halo.feed;


public class XmlCharUtils {
    /**
     * Remove invalid xml characters.
     * <a href="https://stackoverflow.com/a/11672807">Reference</a>
     *
     * @param text need to be removed
     * @return removed text
     */
    public static String removeInvalidXmlChar(String text) {
        if (null == text || text.isEmpty()) {
            return text;
        }
        final int len = text.length();
        char current = 0;
        int codePoint = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            current = text.charAt(i);
            boolean surrogate = false;
            if (Character.isHighSurrogate(current)
                    && i + 1 < len && Character.isLowSurrogate(text.charAt(i + 1))) {
                surrogate = true;
                codePoint = text.codePointAt(i++);
            } else {
                codePoint = current;
            }
            if ((codePoint == 0x9) || (codePoint == 0xA) || (codePoint == 0xD)
                    || ((codePoint >= 0x20) && (codePoint <= 0xD7FF))
                    || ((codePoint >= 0xE000) && (codePoint <= 0xFFFD))
                    || ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF))) {
                sb.append(current);
                if (surrogate) {
                    sb.append(text.charAt(i));
                }
            }
        }
        return sb.toString();
    }
}
