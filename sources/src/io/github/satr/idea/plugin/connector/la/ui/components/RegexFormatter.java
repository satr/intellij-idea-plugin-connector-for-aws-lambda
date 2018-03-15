package io.github.satr.idea.plugin.connector.la.ui.components;

import javax.swing.text.DefaultFormatter;
import java.text.ParseException;
import java.util.regex.Pattern;

public class RegexFormatter extends DefaultFormatter {
    private Pattern pattern;

    public RegexFormatter() {
        super();
    }
    public RegexFormatter(String pattern) {
        this();
        this.pattern = Pattern.compile(pattern);
    }

    public Object stringToValue(String text) throws ParseException {
        if (pattern != null) {
            if (pattern.matcher(text).matches()) {
                return super.stringToValue(text);
            }
            //throw new ParseException("Does not match", 0);
        }
        return text;
    }
}
