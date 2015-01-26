package uj.edu.bluetooth_chat;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeviceListActivity extends Activity {
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "BluetoothChat";
    public static final String TOAST = "toast";

    private static final int REQUEST_ENABLE_BT = 198;
    private static final String TAG = "BluetoothChat";

    private BluetoothAdapter mBluetoothAdapter;
    private List<DeviceDescriptor> devices;
    private BroadcastReceiver mReceiver;
    private DeviceListAdapter mDeviceListAdapter;

    private ChatService mChatService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        enableBluetooth();

        setContentView(R.layout.device_list);

        ((Button) findViewById(R.id.refreshDeviceList)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeviceListActivity.this.searchForDevices();
            }
        });

        mDeviceListAdapter = new DeviceListAdapter(this, R.layout.device_list_item);
        ListView deviceList = (ListView) findViewById(R.id.devices);
        deviceList.setAdapter(mDeviceListAdapter);
        deviceList.setOnItemClickListener(mDeviceClickListener);

        mChatService = new ChatService(this, mChatHandler);
        mChatService.start();

        showDeviceName();
    }

    private final Handler mChatHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);

                    switch (msg.arg1) {
                        case ChatService.STATE_CONNECTED:
                            Toast.makeText(DeviceListActivity.this, "connected!", Toast.LENGTH_SHORT).show();
                            break;
                        case ChatService.STATE_CONNECTING:
                            Toast.makeText(DeviceListActivity.this, "connecting...", Toast.LENGTH_SHORT).show();
                            break;
                        case ChatService.STATE_LISTEN:
                        case ChatService.STATE_NONE:
                            Toast.makeText(DeviceListActivity.this, "not connected", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //if (resultCode == REQUEST_ENABLE_BT) {
            showDeviceName();
        // }
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
        mDeviceListAdapter.clear();
        mDeviceListAdapter.notifyDataSetChanged();

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

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            mChatService.stop();

            String address = devices.get(i).getAddress();
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

            mChatService.connect(device);

            Intent intent = new Intent(DeviceListActivity.this, DialogActivity.class);
            intent.putExtra("target_address", address);
            startActivity(intent);
        }
    };

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
