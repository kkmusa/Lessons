package ru.uroki.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.webkit.*;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class MainActivity extends Activity {
 private WebView web;
 private String pendingBackup;
 private static final String ORIGIN="https://appassets.androidplatform.net/";
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  Reminders.channel(this);Reminders.reschedule(this);
  getWindow().setStatusBarColor(0xff183e39);
  getWindow().setNavigationBarColor(0xfff5f6f2);
  // Stay inside system bars on Android versions that enforce edge-to-edge.
  android.widget.FrameLayout container=new android.widget.FrameLayout(this);
  container.setOnApplyWindowInsetsListener((v,insets)->{
   if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}
   else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
   return insets;
  });
  web=new WebView(this);container.addView(web);setContentView(container);
  WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);
  settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  web.addJavascriptInterface(new Bridge(),"Android");
  web.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
    Uri u=request.getUrl();
    if(!"https".equals(u.getScheme())||!"appassets.androidplatform.net".equals(u.getHost())) return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
    String path=u.getPath();if(path.equals("/"))path="/index.html";
    if(path.contains(".."))return null;
    String mime=path.endsWith(".js")?"text/javascript":path.endsWith(".css")?"text/css":path.endsWith(".svg")?"image/svg+xml":path.endsWith(".webmanifest")?"application/manifest+json":"text/html";
    try{return new WebResourceResponse(mime,"UTF-8",getAssets().open("web"+path));}catch(IOException e){return new WebResourceResponse("text/plain","UTF-8",404,"Not found",null,new ByteArrayInputStream(new byte[0]));}
   }
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest r){return !r.getUrl().toString().startsWith(ORIGIN);}
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onJsConfirm(WebView view,String url,String message,JsResult result){new AlertDialog.Builder(MainActivity.this).setMessage(message).setPositiveButton("Да",(d,w)->result.confirm()).setNegativeButton("Отмена",(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;}
  });
  web.loadUrl(ORIGIN);
 }
 public class Bridge {
  @JavascriptInterface public void syncReminders(String json){Reminders.sync(MainActivity.this,json);}
  @JavascriptInterface public String reminderStatus(){return Reminders.status(MainActivity.this);}
  @JavascriptInterface public void enableReminders(){runOnUiThread(()->requestReminders());}

  @JavascriptInterface public void exportBackup(String data){runOnUiThread(()->{pendingBackup=data;Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.setType("application/json");intent.addCategory(Intent.CATEGORY_OPENABLE);intent.putExtra(Intent.EXTRA_TITLE,"uroki-backup.json");startActivityForResult(intent,1);});}
  @JavascriptInterface public void importBackup(){runOnUiThread(()->{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("*/*");intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,2);});}
  @JavascriptInterface public void share(String text){runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,text);startActivity(Intent.createChooser(i,"Отправить расчёт"));});}
 }
 private void requestReminders(){
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},10);return;}
  if(!Reminders.notifications(this)){Intent i=new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(i);return;}
  if(Build.VERSION.SDK_INT>=31&&!Reminders.exact(this)){startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));return;}
  startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName()));
 }
 @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);if(code==10&&grants.length>0&&grants[0]==android.content.pm.PackageManager.PERMISSION_GRANTED){if(Build.VERSION.SDK_INT>=31&&!Reminders.exact(this))startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));Reminders.reschedule(this);}if(web!=null)web.evaluateJavascript("window.refreshReminderStatus?.()",null);}
 @Override protected void onResume(){super.onResume();Reminders.reschedule(this);if(web!=null)web.evaluateJavascript("window.refreshReminderStatus?.()",null);}
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(result!=RESULT_OK||data==null||data.getData()==null){pendingBackup=null;return;}
  try{
   if(request==1&&pendingBackup!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(pendingBackup.getBytes(StandardCharsets.UTF_8));}pendingBackup=null;android.widget.Toast.makeText(this,"Копия сохранена",0).show();}
   if(request==2){try(InputStream in=getContentResolver().openInputStream(data.getData());ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>10000000)throw new IOException("Файл больше 10 МБ");out.write(buf,0,n);}String json=out.toString("UTF-8");web.evaluateJavascript("window.restoreAndroidBackup("+JSONObject.quote(json)+")",null);}}
  }catch(Exception e){android.widget.Toast.makeText(this,"Не удалось прочитать или сохранить файл",1).show();}
 }
 @Override public void onBackPressed(){web.evaluateJavascript("(()=>{const d=document.querySelector('dialog');if(d.open){d.close();return true;}return false;})()",v->{if(!"true".equals(v))new AlertDialog.Builder(this).setMessage("Закрыть приложение?").setPositiveButton("Закрыть",(d,w)->finish()).setNegativeButton("Отмена",null).show();});}
}
