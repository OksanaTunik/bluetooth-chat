package uj.edu.bluetooth_chat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 198;
    private BluetoothAdapter mBluetoothAdapter;
    private List<DeviceDescriptor> devices;
    private BroadcastReceiver mReceiver;
    private DeviceListAdapter mDeviceListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        enableBluetooth();

        setContentView(R.layout.main);

        ((Button) findViewById(R.id.refreshDeviceList)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.searchForDevices();
            }
        });

        mDeviceListAdapter = new DeviceListAdapter(this, R.layout.device_list_item);
        ListView deviceList = (ListView) findViewById(R.id.devices);
        deviceList.setAdapter(mDeviceListAdapter);

        showDeviceName();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        showDeviceName();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
    }

    private void showDeviceName() {
        String status;

        if (mBluetoothAdapter.isEnabled()) {
            String mydeviceaddress = mBluetoothAdapter.getAddress();
            String mydevicename = mBluetoothAdapter.getName();
            int state = mBluetoothAdapter.getState();

            status = mydevicename + " : " + mydeviceaddress + " : " + parseBluetoothState(state);
        }
        else {
            status = "Bluetooth is not enabled.";
        }

        ((TextView) findViewById(R.id.deviceName)).setText(status);
        // Toast.makeText(this, status, Toast.LENGTH_LONG).show();
    }

    private void searchForDevices() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        mBluetoothAdapter.startDiscovery();

        devices = new ArrayList<DeviceDescriptor>();

        // Create a BroadcastReceiver for ACTION_FOUND
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                Set<String> knownDevices = new HashSet<String>();

                for (DeviceDescriptor d : devices) {
                    knownDevices.add(d.getAddress());
                }

                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    showDeviceName();
                }

                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    if (knownDevices.contains(device.getAddress())) {
                        return;
                    }

                    Boolean isPaired = (pairedDevices.contains(device));
                    // Add the name and address to an array adapter to show in a ListView
                    DeviceDescriptor dd = new DeviceDescriptor(device.getName(), device.getAddress(), isPaired);
                    devices.add(dd);
                    mDeviceListAdapter.add(dd);
                    mDeviceListAdapter.notifyDataSetChanged();
                }
            }
        };

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        devices.clear();
        mDeviceListAdapter.clear();
        mDeviceListAdapter.addAll(devices);
        mDeviceListAdapter.notifyDataSetChanged();
    }

    private String parseBluetoothState(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_ON:
                return "Bluetooth ON";

            case BluetoothAdapter.STATE_OFF:
                return "Bluetooth OFF";

            case BluetoothAdapter.STATE_TURNING_ON:
                return "Bluetooth is turning on...";

            case BluetoothAdapter.STATE_TURNING_OFF:
                return "Bluetooth is turning off...";
        }

        return null;
    }

    private void enableBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.e("MOO", "This device does not support Bluetooth");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            Intent discoverableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private class DeviceListAdapter extends ArrayAdapter<DeviceDescriptor> {
        public DeviceListAdapter(Context context, int resource) {
            super(context, resource);
        }

        public DeviceListAdapter(Context context, int resource, List<DeviceDescriptor> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View listItem = convertView;

            if (listItem == null) {
                listItem = getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
            }

            DeviceDescriptor device = getItem(position);

            if (device == null) {
                return listItem;
            }

            // ImageView image = (ImageView) listItem.findViewById(R.id.image);
            // image.setImageResource(...);

            TextView name = (TextView) listItem.findViewById(R.id.name);
            TextView address = (TextView) listItem.findViewById(R.id.address);
            TextView isPaired = (TextView) listItem.findViewById(R.id.paired);

            name.setText(device.getName());
            address.setText(device.getAddress());

            if (device.getPaired()) {
                isPaired.setText("(was paired earlier)");
            } else {
                isPaired.setVisibility(View.INVISIBLE);
            }

            // listItem.setTag(String.format("%d", alert.getId()));

            return listItem;
        }
    }
}
