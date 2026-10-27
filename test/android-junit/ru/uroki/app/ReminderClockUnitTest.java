package ru.uroki.app;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReminderClockUnitTest {
    @Test public void tenMinutesBeforeLesson() {
        assertEquals(3_000_000L, ReminderClock.trigger(3_600_000L, 0L));
    }
    @Test public void imminentLessonRemindsImmediately() {
        assertEquals(301_000L, ReminderClock.trigger(600_000L, 300_000L));
    }
    @Test public void expiredLessonDoesNotRemind() {
        assertEquals(-1L, ReminderClock.trigger(600_000L, 600_000L));
        assertEquals(-1L, ReminderClock.trigger(600_000L, 700_000L));
    }
    @Test(expected = java.text.ParseException.class)
    public void invalidCalendarDateIsRejected() throws Exception {
        ReminderClock.start("2026-02-31", "15:00");
    }
}
