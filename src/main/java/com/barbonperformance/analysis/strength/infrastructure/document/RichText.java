package com.barbonperformance.analysis.strength.infrastructure.document;

import java.util.regex.Pattern;

// The coach marks bold with **word**, as in Markdown. Everything else is escaped first, so the text
// cannot inject markup into the template; the result goes out through th:utext.
final class RichText {

    private static final Pattern BOLD = Pattern.compile("\\*\\*(\\S(?:.*?\\S)?)\\*\\*");

    public String html(String text) {
        if (text == null) {
            return null;
        }
        return BOLD.matcher(escape(text)).replaceAll("<strong>$1</strong>");
    }

    // XML mode, so only the five XML entities — HTML names like &eacute; would break the parser.
    private static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
