package ru.uroki.app;
import java.text.SimpleDateFormat;
import java.util.Locale;
final class ReminderClock {
 static long start(String date,String time)throws Exception {
  SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US);format.setLenient(false);
  return format.parse(date+" "+time).getTime();
 }
 static long trigger(long start,long now){return start<=now?-1:Math.max(now+1000,start-600000);}
}
