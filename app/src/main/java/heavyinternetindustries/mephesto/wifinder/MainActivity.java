package heavyinternetindustries.mephesto.wifinder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    static HashMap<String, String> manufacturerList = null; // cache
    static MainActivity mainActivity;
    WifiManager manager;
    ListView listView;
    boolean hasShownHint;
    WifiScanReceiver wifiReciever;
    private boolean isWaitingForScanResult;
    String trackingBSSID;
    boolean isTracking;
    TrackingDialog trackingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        listView = (ListView) findViewById(R.id.listView);
        wifiReciever = new WifiScanReceiver();

        isWaitingForScanResult = false;

        mainActivity = this;
        hasShownHint = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }

        isTracking = false;
        trackingBSSID = "";


//        startScan();
    }

    protected void onPause() {
        unregisterReceiver(wifiReciever);
        super.onPause();
    }

    protected void onResume() {
        registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    private void printConnectionInfo() {
        WifiInfo connectionInfo = manager.getConnectionInfo();
        System.out.println("frequency: " + connectionInfo.getFrequency());
        System.out.println("IP: " + Formatter.formatIpAddress(connectionInfo.getIpAddress()));
        System.out.println("Link speed: " + connectionInfo.getLinkSpeed());
        System.out.println("Service set identifier (SSID): " + connectionInfo.getSSID());
        System.out.println("Basic service set identifier (BSSID): " + connectionInfo.getBSSID());
        System.out.println("toString():\n" + connectionInfo);
    }

    private void startScan() {
        isWaitingForScanResult = true;
        manager.startScan();
    }

    private void stopScan() {
        isWaitingForScanResult = false;
    }

    private void showHintSnackbar() {
        View layout = findViewById(R.id.mainLayout);
        if (layout != null && !hasShownHint) {
            Snackbar bar = Snackbar.make(layout, "Tap network to start tracking", Snackbar.LENGTH_LONG);
            bar.show();
            hasShownHint = true;
        }
    }

    private void selectedNetworkForTracking(ScanResult network) {
        FragmentManager fm = getSupportFragmentManager();
        trackingDialog = TrackingDialog.newInstance(network.SSID, network.BSSID);
        trackingDialog.show(fm, "TAG_bs");
        trackingBSSID = network.BSSID;/////////////
        isTracking = true;
        startScan();
    }

    private void updateDialog(ScanResult data) {
        trackingDialog.update(data);
    }

    private void updateScanResults() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) { // permission granted
            final List<ScanResult> list = manager.getScanResults();

            if (isTracking && trackingDialog.isVisible()) {
                ScanResult data = null;

                for (ScanResult result : list)
                        if (result.BSSID.equals(trackingBSSID)) data = result;

                updateDialog(data);
                startScan();
            } else {
                if (isWaitingForScanResult) {
                    isTracking = false;

                    listView.setAdapter(new ScanResultAdapter(this, R.id.listView, list));
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            ScanResult click = list.get(position);
                            selectedNetworkForTracking(click);
                        }
                    });

                    isWaitingForScanResult = false;
                    showHintSnackbar();
                }
            }
        } else {
            System.out.println("Permission not granted");
        }
    }

    public void onClickStartScan(View view) {
        startScan();
    }

    /**
     * Returns (readable) name of enctyption
     * @param result
     * @return
     */
    public static String getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return "WEP";
        } else if (result.capabilities.contains("PSK")) {
            return "PSK";
        } else if (result.capabilities.contains("EAP")) {
            return "EAP";
        }
        return "Not secured";
    }

    /**
     * Callback
     */
    private class WifiScanReceiver extends BroadcastReceiver {
        /**
         * Called by OS when results of Wifi-scan is refreshed
         * @param c
         * @param intent
         */
        public void onReceive(Context c, Intent intent) {
            updateScanResults();
        }
    }

    public static MainActivity getActivity() {
        return mainActivity;
    }

    /**
     * Returns name of chip manufacturer by MAC
     * @param mac duh
     * @return name
     */
    public static String resolveManufacturer(String mac) {
        if (manufacturerList == null)
            // dynamic programming ftw
            manufacturerList = new HashMap<>();

        mac = mac.replace(":", "");
        mac = mac.substring(0, 3).toUpperCase();

        if (manufacturerList.containsKey(mac)) return manufacturerList.get(mac);

        BufferedReader reader = null;
        String ret = "";
        try {
            reader = new BufferedReader(new InputStreamReader(
                    getActivity().getAssets().open("maclist.txt")));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(mac))
                    ret = line.substring(line.indexOf(" ") + 1);
            }
        } catch (Exception e) {
            System.out.println("Error reading mac list");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        manufacturerList.put(mac, ret);

        return ret;
    }
}
