package ru.uroki.app;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){Reminders.reschedule(c);}
}
