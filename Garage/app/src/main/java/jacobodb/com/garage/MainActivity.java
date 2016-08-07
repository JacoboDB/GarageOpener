package jacobodb.com.garage;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    protected static final int BT_DESCONNECTED = 3;
    boolean BTon=false, BTPaired = false, BTConnected=false;
    ConnectThread connect;
    ConnectedThread connectedThread;
    Handler connectionHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            switch(msg.what){
                case SUCCESS_CONNECT:
                    // DO something
                    Log.e("LOG------","handler case 0");
                    BTConnected=true;
                    connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
                    cThread=connectedThread;
                    String message = "1";
                    cThread.write(message.getBytes());

                    //CUARTA LUZ VERDE
                    firstStep.setImageResource(R.mipmap.success);
                    textInfo.setText("La puerta se ha accionado");
                    btnAction.setEnabled(true);
                    btnAction.setBackgroundResource(R.color.colorPrimary);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[])msg.obj;
                    String string = new String(readBuf);
                    Toast.makeText(getApplicationContext(), string, Toast.LENGTH_LONG).show();
                    break;
                case BT_DESCONNECTED:
                    BTConnected=false;
                    Log.e("LOG------","handler case 3");
                    firstStep.setImageResource(R.mipmap.fail);
                    textInfo.setText("No se ha podido conectar con el dispositivo: "+BTNamePaired);
                    btnAction.setEnabled(true);
                    btnAction.setBackgroundResource(R.color.colorPrimary);
                    break;
                default:
                    BTConnected=false;
                    Toast.makeText(getApplicationContext(), "Default", Toast.LENGTH_LONG).show();
            }
        }
    };
    ConnectedThread cThread;
    Button btnName;
    Button btnAction;
    ImageView firstStep;
    TextView textInfo;
    SharedPreferences spref;
    String BTName, BTNamePaired;
    BluetoothAdapter btAdapter;
    BluetoothDevice BTdevice;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.mipmap.icon);
        toolbar.setTitle("  El control de la puerta");

        setSupportActionBar(toolbar);
        init();
        setUp();
        action();

    }

    private void init(){
        btnName = (Button)findViewById(R.id.buttonName);
        btnAction = (Button) findViewById(R.id.btnAction);
        firstStep = (ImageView) findViewById(R.id.imageView);
        textInfo = (TextView) findViewById(R.id.textView2);
        spref = getSharedPreferences("", Context.MODE_PRIVATE);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    private void setUp(){
        String blueName=spref.getString("BTName", "");
        if(blueName==null || blueName=="") {
            showInputDialog();
        }else{
            BTName=blueName;
            btnName.setText(BTName);
        }
        btnName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action();

            }
        });

    }
    private void restartSteps(){
        firstStep.setImageResource(R.mipmap.neutro);
    }
    private void action(){
        btnAction.setEnabled(false);
        btnAction.setBackgroundResource(R.color.disabled);
        updateStates();
        restartSteps();
        textInfo.setText("Accionando...");
        if(btAdapter==null){
            textInfo.setText("Su dispositivo no cuenta con bluetooth");
            Toast.makeText(getApplicationContext(), "Su dispositivo no cuenta con bluetooth", Toast.LENGTH_LONG).show();
            btnAction.setEnabled(true);
            btnAction.setBackgroundResource(R.color.colorPrimary);
            finish();
        }else{
            if(!BTon) {
                turnOnBT();
            }
            if(!BTPaired){
                getPairedBT();
            }
            if(!BTPaired){
                textInfo.setText("El dispositivo no está enlazado: " + BTName);
                btnAction.setEnabled(true);
                btnAction.setBackgroundResource(R.color.colorPrimary);
                //ROJO SEGUNDA LUZ
                firstStep.setImageResource(R.mipmap.fail);
            }else{
                if(!BTConnected){
                    Log.e("LOG------","NOT CONNECTED");
                    createConnection();
                }else{
                    Log.e("LOG------","CONNECTED");
                    sendMessage("1");

                }
            }

        }

    }
    private void turnOnBT(){
        btAdapter.enable();
        BTon=true;
        try { Thread.sleep(1500); } catch (InterruptedException e) {e.printStackTrace();}
    }
    private void getPairedBT(){
        if(btAdapter.getBondedDevices().size()>0){
            for(BluetoothDevice device:btAdapter.getBondedDevices()){
                if(device.getName().toString().equals(BTName)){
                    BTdevice=device;
                    BTPaired=true;
                    BTNamePaired = device.getName().toString();
                    break;
                }
            }
        }
    }
    private void createConnection(){
        connect = new ConnectThread(BTdevice);
        connect.start();
    }
    private void sendMessage(String message){
        cThread.write(message.getBytes());
        btnAction.setEnabled(true);
        btnAction.setBackgroundResource(R.color.colorPrimary);
        firstStep.setImageResource(R.mipmap.success);
        textInfo.setText("La puerta ha sido accionada");
    }
    private void updateStates(){
        BTon=btAdapter.isEnabled();
    }
    private void showInputDialog() {

        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.input_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);

        final EditText editText = (EditText) promptView.findViewById(R.id.edittext);
        editText.setText(spref.getString("BTName", "").toString());
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String userText = editText.getText().toString();
                        //Toast.makeText(getApplicationContext(), "Texto"+userText+"del usuario", Toast.LENGTH_LONG).show();
                        if (userText == null || userText.equals("")) {
                            Toast.makeText(getApplicationContext(), "El nombre no puede estar vacio", Toast.LENGTH_LONG).show();
                            showInputDialog();

                        } else {
                            SharedPreferences.Editor editor = spref.edit();
                            editor.putString("BTName", editText.getText().toString());
                            editor.commit();
                            btnName.setText(editText.getText().toString());
                            BTName = editText.getText().toString();
                        }

                    }
                });


        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        btAdapter.disable();
        BTPaired=false;
        finish();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        updateStates();
        restartSteps();
        action();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }



    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();

            }
            mmSocket = tmp;
        }

        public void disconnect(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public boolean isConnected(){
            return mmSocket.isConnected();
        }
        public void run() {
            // Cancel discovery because it will slow down the connection
            btAdapter.cancelDiscovery();
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Log.e("LOG------","try connect");
                mmSocket.connect();

            } catch (IOException connectException) {
                Log.e("LOG------","catch connect");
                connectionHandler.obtainMessage(3, mmSocket).sendToTarget();
                connectException.printStackTrace();
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {}
                return;
            }

            // Do work to manage the connection (in a separate thread)

            connectionHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
        }



        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer;  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    buffer = new byte[1024];
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    connectionHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();

                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


}
