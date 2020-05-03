package org.sample.bluetooth_in_out;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;

import android.os.Handler;
import android.view.MotionEvent;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> devices;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;

    private String uid = "00001101-0000-1000-8000-00805f9b34fb";

    private OutputStream outputStream = null;
    private InputStream inputStream = null;

    final static private int REQUEST_ENABLE_BT = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothSocket = null;

        Button btn1 = findViewById(R.id.button1);
        Button btn2 = findViewById(R.id.button2);
        Button btn3 = findViewById(R.id.button3);
        Button btn4 = findViewById(R.id.button4);
        Button btn5 = findViewById(R.id.button5);
        Button btn6 = findViewById(R.id.button6);

        if(bluetoothAdapter==null){
// 블루투스 어뎁터의 값이 비어있는 경우 사용불가능 메시지를 출력한다.
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        } else {
            if(bluetoothAdapter.isEnabled()){
// 블루투스가 활성화 상태인 경우
                selectBluetoothDevice();
            } else {
// 블루투스가 비활성화 상태인 경우
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
        }
        btn1.setOnClickListener(v -> sendData("1"));
        btn2.setOnClickListener(v -> sendData("2"));
        btn3.setOnTouchListener((v, event) -> {
            sendData("3");
            if (event.getAction() == MotionEvent.ACTION_UP) {
                sendData("a\na");
                return false;
            }
            return false;
        });
        btn4.setOnClickListener(v -> sendData("4"));
        btn5.setOnClickListener(v -> sendData("5"));
        btn6.setOnClickListener(v -> sendData("6"));
    }
    public void selectBluetoothDevice() {
        // 페어링 되어있는 기기
        devices = bluetoothAdapter.getBondedDevices();
        int pairedDeviceCount = devices.size();
        if(pairedDeviceCount ==0){ } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Pairing Devices");
            ArrayList<String> list= new ArrayList<>();
            for(BluetoothDevice bluetoothDevice : devices) {
                list.add(bluetoothDevice.getName());
            }
            list.add("장치검색");
            list.add("취소");
            final CharSequence[] charSequences = list.toArray(new CharSequence[list.size()]);

            builder.setItems(charSequences, (dialog, which) -> {
                if(charSequences[which].equals("장치검색")) {
                    ensureDiscoverable();
                } else if(charSequences[which].equals("취소")){
                    dialog.dismiss();
                } else {
                    connectDevice(charSequences[which].toString());
                }
            });

            builder.setCancelable(false);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }
    public void receiveData() {
        final Handler handler = new Handler();
        byte readBuff[] = new byte[32];

        TextView textView = findViewById(R.id.textView);
        TextView textView2 = findViewById(R.id.textView2);

        Thread workThread = new Thread(( ) -> {
            int readBuffPos = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    int byteAvailable = inputStream.available();
                    if (byteAvailable > 0) {
                        byte[] bytes = new byte[byteAvailable];
                        inputStream.read(bytes);
                        for (int i = 0; i < byteAvailable; i++) {
                            byte temp = bytes[i];
                            if (temp == '\n') {
                                byte[] encodedBytes = new byte[readBuffPos];
                                System.arraycopy(readBuff, 0, encodedBytes, 0, encodedBytes.length);
                                final String text = new String(encodedBytes, "US-ASCII");
                                readBuffPos = 0;
                                handler.post(( ) -> {
                                    try {
                                        String[] array = text.split(",");

                                        textView.setText(array[0].concat("C"));
                                        textView2.setText(array[1].concat("%"));
                                    } catch (Exception e) { }
                                });
                            } else {
                                readBuff[readBuffPos++] = temp;
                            }
                        }
                    }
                } catch (IOException e) { }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        workThread. start();
    }
    public void connectDevice(String deviceName) {
        for(BluetoothDevice device : devices) {
            if(deviceName.equals(device.getName())){
                bluetoothDevice = device;
                break;
            }
        }
        UUID uuid = UUID.fromString(uid);
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
            receiveData();
        } catch (IOException e){ }
    }
    void sendData(String text) {
        text += "\n";
        try {
            outputStream.write(text.getBytes());
        } catch (IOException e) { }
    }
    void ensureDiscoverable(){
        if(bluetoothAdapter.getScanMode()!=bluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent intent = new Intent(bluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
            startActivity(intent);
        } else { }
    }
}