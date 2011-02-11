package org.apache.log4j.net;

import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import com.facebook.scribe.thrift.LogEntry;

public class LogEntryMatcher extends BaseMatcher<List<LogEntry>> {

    private final String category;

    private final Matcher<String> messageMatcher;

    LogEntryMatcher(final String category, final Matcher<String> messageMatcher) {

        this.category = category;
        this.messageMatcher = messageMatcher;
    }

    @Override
    public void describeTo(final Description description) {

        // not sure what this is for
        description.appendText("LogEntryMatcher");
    }

    @Override
    public boolean matches(final Object other) {

        if (!(other instanceof List<?>)) {
            doesntMatch("object is not a List");
        }

        List<?> list = (List<?>) other;

        if (list.size() != 1) {
            doesntMatch("list is not of size 1, size=" + list.size());
        }

        Object item = list.get(0);

        if (!(item instanceof LogEntry)) {
            doesntMatch("inner object is not a LogEntry");
        }

        LogEntry logEntry = (LogEntry) item;

        boolean matches = logEntry.getCategory().equals(category) && messageMatcher.matches(logEntry.getMessage());

        if (!matches) {
            doesntMatch("category and message don't match; logEntry=" + logEntry.toString());
        }

        return true;
    }

    private void doesntMatch(final String reason) {
        throw new IllegalArgumentException("Matcher failed. Local state: category=" + category + ", reason=[" + reason
                + "]");
    }
}
