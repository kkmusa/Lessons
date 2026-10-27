package ru.uroki.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import org.json.*;



final class Reminders {
 static final String CHANNEL="lessons-reminders";
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("reminders",Context.MODE_PRIVATE);}
 static void channel(Context c){NotificationManager n=c.getSystemService(NotificationManager.class);n.createNotificationChannel(new NotificationChannel(CHANNEL,"Напоминания о занятиях",NotificationManager.IMPORTANCE_HIGH));}
 static boolean notifications(Context c){channel(c);NotificationManager n=c.getSystemService(NotificationManager.class);return (Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)&&n.areNotificationsEnabled()&&n.getNotificationChannel(CHANNEL).getImportance()!=NotificationManager.IMPORTANCE_NONE;}
 static boolean exact(Context c){return Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
 static String status(Context c){return "{\"notifications\":"+notifications(c)+",\"exact\":"+exact(c)+"}";}
 static PendingIntent pending(Context c,JSONObject lesson){Intent i=new Intent(c,ReminderReceiver.class).setAction("ru.uroki.REMIND").setData(Uri.parse("uroki://lesson/"+Uri.encode(lesson.optString("id"))));i.putExtra("lesson",lesson.toString());return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static long start(JSONObject l)throws Exception {return ReminderClock.start(l.getString("date"),l.getString("time"));}
 static String deliveredKey(JSONObject l){return "sent:"+l.optString("id")+":"+l.optString("date")+":"+l.optString("time");}
 static synchronized void sync(Context c,String json){
  try{JSONArray next=new JSONArray(json);JSONArray old=new JSONArray(prefs(c).getString("lessons","[]"));AlarmManager alarm=c.getSystemService(AlarmManager.class);
   for(int i=0;i<old.length();i++){JSONObject l=old.getJSONObject(i);alarm.cancel(pending(c,l));c.getSystemService(NotificationManager.class).cancel(l.optString("id"),1);}
   prefs(c).edit().putString("lessons",next.toString()).apply();schedule(c,next);
  }catch(Exception e){android.util.Log.e("Uroki","Reminder sync failed",e);}
 }
 static synchronized void reschedule(Context c){try{schedule(c,new JSONArray(prefs(c).getString("lessons","[]")));}catch(Exception e){android.util.Log.e("Uroki","Reminder restore failed",e);}}
 static void schedule(Context c,JSONArray lessons)throws Exception {
  channel(c);AlarmManager alarm=c.getSystemService(AlarmManager.class);long now=System.currentTimeMillis();
  for(int i=0;i<lessons.length();i++){JSONObject l=lessons.getJSONObject(i);PendingIntent p=pending(c,l);alarm.cancel(p);long start=start(l);
   if(start<=now||prefs(c).getBoolean(deliveredKey(l),false))continue;
   long when=ReminderClock.trigger(start,now);
   try{if(exact(c))alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);}catch(SecurityException e){alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);}
  }
 }
 static void deliver(Context c,JSONObject l){
  try{
   if(!notifications(c)||start(l)<=System.currentTimeMillis()||prefs(c).getBoolean(deliveredKey(l),false))return;
   // Reject obsolete alarms after a lesson was edited/deleted or marked complete.
   JSONArray current=new JSONArray(prefs(c).getString("lessons","[]"));boolean found=false;
   for(int i=0;i<current.length();i++){JSONObject x=current.getJSONObject(i);if(x.optString("id").equals(l.optString("id"))&&x.optString("date").equals(l.optString("date"))&&x.optString("time").equals(l.optString("time")))found=true;}
   if(!found)return;
   PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(ru.uroki.app.R.drawable.ic_notification).setContentTitle("Скоро занятие: "+l.optString("name")).setContentText("Начало в "+l.optString("time")+" · "+l.optString("date")).setContentIntent(open).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER).build();
   c.getSystemService(NotificationManager.class).notify(l.optString("id"),1,n);prefs(c).edit().putBoolean(deliveredKey(l),true).apply();
  }catch(Exception e){android.util.Log.e("Uroki","Notification failed",e);}
 }
}
