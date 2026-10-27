package ru.uroki.app;
import java.util.TimeZone;
public class ReminderClockTest {
 static void equal(long expected,long actual){if(expected!=actual)throw new AssertionError(expected+" != "+actual);}
 public static void main(String[] args)throws Exception {
  TimeZone.setDefault(TimeZone.getTimeZone("Asia/Yekaterinburg"));
  long start=ReminderClock.start("2026-10-22","15:00");
  equal(java.time.Instant.parse("2026-10-22T10:00:00Z").toEpochMilli(),start);
  equal(start-600000,ReminderClock.trigger(start,start-3600000));
  equal(start-299000,ReminderClock.trigger(start,start-300000));
  equal(-1,ReminderClock.trigger(start,start));equal(-1,ReminderClock.trigger(start,start+1));
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  equal(start+18000000,ReminderClock.start("2026-10-22","15:00"));
  boolean rejected=false;try{ReminderClock.start("2026-02-31","15:00");}catch(Exception e){rejected=true;}if(!rejected)throw new AssertionError("Invalid date accepted");
  System.out.println("PASS: reminder 10 minutes before, imminent lesson, expired lesson, local time and timezone change, invalid date");
 }
}
