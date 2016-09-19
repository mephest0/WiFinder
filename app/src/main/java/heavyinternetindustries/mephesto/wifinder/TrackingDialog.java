package heavyinternetindustries.mephesto.wifinder;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by mephest0 on 23.05.16.
 */
public class TrackingDialog extends DialogFragment {
    TextView bssid, ssid, signalStrength;
    String sSSID, sBSSID;
    ProgressBar bar;

    public TrackingDialog() {
        //don't use me!
    }


    public static TrackingDialog newInstance(String tSSID, String tBSSID) {
        TrackingDialog ret = new TrackingDialog();
        ret.sSSID = tSSID;
        ret.sBSSID = tBSSID;
        return ret;
    }

    /**
     * Updates (or populates) dialog with fresh data
     * @param data
     */
    public void update(ScanResult data) {
        if (data != null) {
            if (data.SSID.equals("")) {
                //hidden ap
                SpannableString hiddenMessage = new SpannableString(getString(R.string.ssid_hidden));
                hiddenMessage.setSpan(new StyleSpan(Typeface.ITALIC), 0, hiddenMessage.length(), 0);
                ssid.setText(hiddenMessage);

            } else {
                ssid.setText(data.SSID);
            }

            bssid.setText(data.BSSID);
            bar.setMax(100);
            int calcSignal = data.level + 100;
            calcSignal = Math.max(calcSignal, 0);
            calcSignal = Math.min(calcSignal, 100);
            bar.setProgress(calcSignal);
            signalStrength.setText(data.level + "");
        } else {
            //no longer in range
            bar.setProgress(0);
            signalStrength.setText("--");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //System.out.println("TrackingDialog.onCreateView");
        return inflater.inflate(R.layout.tracker_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bssid = (TextView) view.findViewById(R.id.tracker_bssid);
        ssid = (TextView) view.findViewById(R.id.tracker_ssid);
        signalStrength = (TextView) view.findViewById(R.id.tracker_signal_strength);
        bar = (ProgressBar) view.findViewById(R.id.tracker_progress_bar);

        if (sSSID.equals("")) {
            //hidden ap
            SpannableString hiddenMessage = new SpannableString(getString(R.string.ssid_hidden));
            hiddenMessage.setSpan(new StyleSpan(Typeface.ITALIC), 0, hiddenMessage.length(), 0);
            ssid.setText(hiddenMessage);

        } else {
            ssid.setText(sSSID);
        }

        bssid.setText(sBSSID);
    }
}
