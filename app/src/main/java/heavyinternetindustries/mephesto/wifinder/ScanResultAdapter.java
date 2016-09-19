package heavyinternetindustries.mephesto.wifinder;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by mephest0 on 22.05.16.
 */
public class ScanResultAdapter extends ArrayAdapter<ScanResult> {
    List<ScanResult> list;
    Context context;
    public ScanResultAdapter(Context context, int resource, List<ScanResult> objects) {
        super(context, resource, objects);

        this.list = objects;
        this.context = context;

        Collections.sort(list, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                return Integer.compare(lhs.level, rhs.level) * -1;
            }
        });
        for (ScanResult ap : list) MainActivity.resolveManufacturer(ap.BSSID);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view;
        if (convertView == null)
            view = inflater.inflate(R.layout.scan_list_item, parent, false);
        else
            view = convertView;

        TextView ssid = (TextView) view.findViewById(R.id.ssid);
        TextView security = (TextView) view.findViewById(R.id.security);
        TextView strength = (TextView) view.findViewById(R.id.strength);
        TextView mac = (TextView) view.findViewById(R.id.bssid);
        TextView other = (TextView) view.findViewById(R.id.etc);
        TextView manufacturer = (TextView) view.findViewById(R.id.manufacturer);

        ScanResult ap = list.get(position);

        if (ap.SSID.equals("")) {
            //hidden ap
            SpannableString hiddenMessage = new SpannableString(getContext().getString(R.string.ssid_hidden));
            hiddenMessage.setSpan(new StyleSpan(Typeface.ITALIC), 0, hiddenMessage.length(), 0);
            ssid.setText(hiddenMessage);

        } else {
            ssid.setText(ap.SSID);
        }

        security.setText(MainActivity.getSecurity(ap));

        String strengthString = ap.level + " | ";
        int calcStrength = WifiManager.calculateSignalLevel(ap.level, 5);
        if (calcStrength == 4) strengthString += getContext().getString(R.string.signal_good);
        else if (calcStrength == 3 || calcStrength == 2) strengthString += getContext().getString(R.string.signal_ok);
        else if (calcStrength == 1 || calcStrength == 0) strengthString += getContext().getString(R.string.signal_bad);
        else strengthString += "??";

        strength.setText(strengthString);
        mac.setText(ap.BSSID);

        other.setText(ap.capabilities);

        String manufacturerName = MainActivity.resolveManufacturer(ap.BSSID);
        if (manufacturerName.equals("")) manufacturerName = getContext().getString(R.string.no_manufacturer_name);
        manufacturer.setText(manufacturerName);

        return view;
    }
}
