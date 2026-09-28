package vn.qahub.api.document;

import java.util.regex.Pattern;

/** Glob đơn giản cho đường dẫn file trong repo (vd {@code docs/**&#47;*.md}) — chỉ hỗ trợ {@code *}/{@code **}, đủ cho MVP. */
public final class GlobMatcher {

    private GlobMatcher() {
    }

    public static boolean matches(String glob, String path) {
        return toRegex(glob).matcher(path).matches();
    }

    private static Pattern toRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        int i = 0;
        while (i < glob.length()) {
            char c = glob.charAt(i);
            if (c == '*') {
                if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                    regex.append(".*");
                    i += 2;
                } else {
                    regex.append("[^/]*");
                    i++;
                }
            } else if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
                regex.append('\\').append(c);
                i++;
            } else {
                regex.append(c);
                i++;
            }
        }
        return Pattern.compile(regex.toString());
    }
}
