package it.casaricci.provinciawifi;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

public class WifiAuthenticator extends Service {

    private static final int NOTIFICATION_NO_CREDENTIALS = 1;

    private static final String FORM_URI = "https://captiveportal.provinciawifi.it:8001/";
    private static final String PARAM_VALUE_ACCEPT = "Accedi+al+servizio";
    private static final String PARAM_VALUE_REDIRURL = "http://www.google.com/";

    // TEST
    //private static final String FORM_URI = "http://10.0.2.2/provinciawifi/";
    //private static final String PARAM_VALUE_REDIRURL = "https://wasp.provinciawifi.it/captiveportal/?q=it/spot/91&redirect=http://www.google.com/";
    //private static final String USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686; it; rv:1.9.2.13) Gecko/20101206 Ubuntu/10.10 (maverick) Firefox/3.6.13";

    final Handler mHandler = new Handler();

    @Override
    public void onStart(Intent intent, int startId) {
        handleIntent(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return Service.START_NOT_STICKY;
    }

    private void handleIntent(Intent intent) {
        // the service got started! It means that we are connected to an interesting WiFi network
        (new AuthThread()).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO
        return null;
    }

    public class AuthThread extends Thread {
        @SuppressWarnings("deprecation")
        public void run() {
            // request http login
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(FORM_URI);

            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WifiAuthenticator.this);
                String username = prefs.getString("username", null);
                String password = prefs.getString("password", null);
                Context context = WifiAuthenticator.this;

                // login data not set or invalid -- launch preferences dialog
                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {

                    NotificationManager nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                    CharSequence txt = context.getText(R.string.msg_not_insert_credentials);
                    Notification n = new Notification(R.drawable.icon, txt, 0);

                    Intent notificationIntent = new Intent(context, EditPreferences.class);
                    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
                    n.flags += Notification.FLAG_AUTO_CANCEL;
                    n.setLatestEventInfo(context,
                            context.getText(R.string.app_name),
                            context.getText(R.string.msg_insert_credentials),
                            contentIntent);

                    nm.notify(NOTIFICATION_NO_CREDENTIALS, n);
                    return;
                }

                // POST data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
                nameValuePairs.add(new BasicNameValuePair("auth_user", username));
                nameValuePairs.add(new BasicNameValuePair("auth_pass", password));
                nameValuePairs.add(new BasicNameValuePair("accept", PARAM_VALUE_ACCEPT));
                nameValuePairs.add(new BasicNameValuePair("redirurl", PARAM_VALUE_REDIRURL));

                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // headers
                // seems useless - httppost.addHeader("Referer", "https://wasp.provinciawifi.it/captiveportal/?q=it&mac=MACADDRESS&redirurl=http://www.google.com/");
                // seems useless - httppost.addHeader("Cookie", "gid=91; style=null");
                // seems useless - will be removed after some other tests
                //httppost.addHeader("User-Agent", USER_AGENT);

                // connection parameters
                httpclient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
                httpclient.getParams().setBooleanParameter("http.protocol.expect-continue", false);

                // execute request
                HttpResponse response = httpclient.execute(httppost);
                // TODO we will need to check for a proper redirect page (Meta redirect, sigh!)
                // TODO in the meanwhile just check the HTTP response code
                if (response.getStatusLine().getStatusCode() != 200)
                    throw new Exception();

                Scanner sc = new Scanner(response.getEntity().getContent());
                if (sc.findWithinHorizon("\\Q" + PARAM_VALUE_REDIRURL + "\\E", 0) == null)
                    throw new Exception();

                mHandler.post(new Toaster(R.string.msg_logged_in, Toast.LENGTH_LONG));
            }

            catch (Exception e) {
                mHandler.post(new Toaster(R.string.msg_login_error, Toast.LENGTH_LONG));
            }

        }
    }

    public class Toaster implements Runnable {
        private int resId;
        private int length;

        public Toaster(int resId, int length) {
            this.resId = resId;
            this.length = length;
        }

        public void run() {
            Toast.makeText(WifiAuthenticator.this, resId, length).show();
            WifiAuthenticator.this.stopSelf();
        }
    }

}
