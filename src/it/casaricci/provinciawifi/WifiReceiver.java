package it.casaricci.provinciawifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

public class WifiReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo ni = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            WifiManager mWifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wInfo = mWifi.getConnectionInfo();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String ssid = prefs.getString("ssid", ProvinciaWiFi.DEFAULT_WIFI_SSID);

            if (ni.isConnected() && ssid.equalsIgnoreCase(wInfo.getSSID())) {
                // launch login service
                context.startService(new Intent(context, WifiAuthenticator.class));
            }
        }
    }

}
