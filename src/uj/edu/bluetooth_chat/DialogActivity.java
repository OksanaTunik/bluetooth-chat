package uj.edu.bluetooth_chat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by shybovycha on 12.01.15.
 */
public class DialogActivity extends Activity {
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    private static final String TAG = "BluetoothChat";

    private TextView mDialog;
    private ChatService mChatService;
    private DeviceDescriptor mConnectedDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private EditText mMessageBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog);

        if (mChatService == null) {
            mChatService = new ChatService(this, mChatHandler);
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String address = getIntent().getExtras().getString("target_address");
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mConnectedDevice = new DeviceDescriptor(device.getName(), address, true);
        // Attempt to connect to the device
        mChatService.connect(device);

        mMessageBox = (EditText) findViewById(R.id.messageText);

        ((Button) findViewById(R.id.sendButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = mMessageBox.getText().toString();
                sendMessage(message);
            }
        });

        mDialog = (TextView) findViewById(R.id.dialog);
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != ChatService.STATE_CONNECTED) {
            Toast.makeText(this, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            // mOutStringBuffer.setLength(0);
            mMessageBox.setText("");
        }
    }

    // The Handler that gets information back from the ChatService
    private final Handler mChatHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);

                    switch (msg.arg1) {
                        case ChatService.STATE_CONNECTED:
//                            mTitle.setText(R.string.title_connected_to);
//                            mTitle.append(mConnectedDeviceName);
//                            mConversationArrayAdapter.clear();
                                mDialog.append(mConnectedDevice.getName() + " joined\n");
                            break;
                        case ChatService.STATE_CONNECTING:
                            mDialog.append("Connecting...\n");
                            break;
                        case ChatService.STATE_LISTEN:
                        case ChatService.STATE_NONE:
                            mDialog.append("Not connected\n");
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mDialog.append("Me: " + writeMessage + "\n"); //add("Me:  " + writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                    mDialog.append(mConnectedDevice.getName() + ": " + readMessage + "\n");
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    // mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDevice.getName(), Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
//                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}