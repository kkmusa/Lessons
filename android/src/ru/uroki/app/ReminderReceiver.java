package ru.uroki.app;
import android.content.*;
import org.json.JSONObject;
public class ReminderReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){try{Reminders.deliver(c,new JSONObject(i.getStringExtra("lesson")));}catch(Exception e){android.util.Log.e("Uroki","Invalid reminder",e);}}
}
