package com.healthmonitor.khann.healthmonitor;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;

import android.widget.Button;
import android.view.View.OnClickListener;

// TODO Assignment 2
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;
import android.os.AsyncTask;


import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URL;
import android.util.Log;
import android.os.StrictMode;

// Wherever not marked as reference, is our code.
public class MainActivity extends AppCompatActivity implements OnClickListener, SensorEventListener {
    // Declare all the private variables for this class
    Button runButton, stopButton;
    GraphView graphView;
    boolean firstRun = true;
    SQLiteDatabase db;
    EditText patientID;
    EditText Age;
    EditText patientName;
    String patientSex="Female";
    String patientIDText = "";
    String ageText = "";
    String patientNameText = "";
    Sensor accelerometer;
    boolean started = false;
    static int ACCE_FILTER_DATA_MIN_TIME = 1000; // 1000ms
    long lastSaved = System.currentTimeMillis();
    String table_name;
    int x_value=0;
    int offSet = 1;
    int dloadRun = 0;
    LineGraphSeries mSeries1 = new LineGraphSeries<>();
    LineGraphSeries mSeries2 = new LineGraphSeries<>();
    LineGraphSeries mSeries3 = new LineGraphSeries<>();
    LineGraphSeries downSeries1 = new LineGraphSeries<>();
    LineGraphSeries downSeries2 = new LineGraphSeries<>();
    LineGraphSeries downSeries3 = new LineGraphSeries<>();


    RunnableDemo R1 = new RunnableDemo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // All initializations should go here

        // Created directory where database will be stored
        super.onCreate(savedInstanceState);

        final File file = new File(Environment.getExternalStorageDirectory()+ File.separator+"Android/Data/CSE535_ASSIGNMENT2");
        if (!file.exists()) {
            file.mkdirs();
        }

        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        runButton = (Button) findViewById(R.id.run);
        runButton.setOnClickListener(this);
        stopButton = (Button) findViewById(R.id.stop);
        stopButton.setOnClickListener(this);
        graphView = (GraphView) findViewById(R.id.graph);
        Viewport viewport = graphView.getViewport();

        // Initializing graph parameters
        viewport.setXAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxX(10);
        viewport.setScalable(true);
        mSeries1.setDrawDataPoints(true);
        mSeries1.setThickness(10);
        mSeries1.setColor(Color.RED);
        mSeries2.setDrawDataPoints(true);
        mSeries2.setThickness(10);
        mSeries2.setColor(Color.BLUE);
        mSeries3.setDrawDataPoints(true);
        mSeries3.setThickness(10);
        mSeries3.setColor(Color.GREEN);
        downSeries1.setDrawDataPoints(true);
        downSeries1.setThickness(10);
        downSeries1.setColor(Color.RED);
        downSeries2.setDrawDataPoints(true);
        downSeries2.setThickness(10);
        downSeries2.setColor(Color.BLUE);
        downSeries3.setDrawDataPoints(true);
        downSeries3.setThickness(10);
        downSeries3.setColor(Color.GREEN);


        File existing_file = new File(Environment.getExternalStorageDirectory()+ File.separator+"Android/Data/CSE535_ASSIGNMENT2_Extra/group2");
        boolean deleted = existing_file.delete();
        // Data about the patient taken from the fields
        patientID = (EditText) findViewById(R.id.editText2);
        Age = (EditText) findViewById(R.id.editText5);
        patientName = (EditText) findViewById(R.id.editText);

        // Sensor initialize data
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer , SensorManager.SENSOR_DELAY_NORMAL);

        // Add functionality for Add to Database button
        Button addtoDB = (Button) findViewById(R.id.addtodb);
        addtoDB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                patientIDText = (patientID.getText().toString());
                patientNameText = (patientName.getText().toString());
                ageText = (Age.getText().toString());
                dloadRun=0;
                try {
                    if( (patientIDText.length() != 0) && (patientNameText.length() != 0) && (ageText.length() != 0) ) {
                        db = SQLiteDatabase.openDatabase(file.toString()+"/group2", null, SQLiteDatabase.CREATE_IF_NECESSARY);
                        db.beginTransaction();
                        table_name = patientNameText + "_" + patientIDText + "_" + ageText + "_" + patientSex;
                        try {
                            started = true;
                            //perform your database operations here ...
                            db.execSQL("create table " + table_name + " ("
                                    + " recID integer PRIMARY KEY autoincrement, "
                                    + "timestamp TIMESTAMP, "
                                    + " x_values double, "
                                    + " y_values double, "
                                    + "z_values double); ");

                            db.setTransactionSuccessful();
                        } catch (SQLiteException e) {
                            //report problem
                        } finally {
                            db.endTransaction();
                            //db.close();
                        }
                        Toast.makeText(MainActivity.this,"Successfully added data to database.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Error. Enter complete information.", Toast.LENGTH_LONG).show();
                    }
                } catch (SQLException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        // Add functionality for Download Database button
        Button downloaddbButton = (Button) findViewById(R.id.downloaddb);
        downloaddbButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                String path_to_server = "http://10.218.110.136/CSE535Fall17Folder/group2";

                final DownloadFileFromURL downloadTask = new DownloadFileFromURL();
                downloadTask.execute(path_to_server);

                File downloaded_file = new File(Environment.getExternalStorageDirectory()+ File.separator+"Android/Data/CSE535_ASSIGNMENT2_Extra/group2");
                if (downloaded_file.exists()) {

                    Log.d("Debug", "DOWNLOADED DB---: " + String.valueOf(dloadRun));

                    String sourceFileUri = Environment.getExternalStorageDirectory() + File.separator + "Android/Data/CSE535_ASSIGNMENT2_Extra/group2";
                    SQLiteDatabase openDB = SQLiteDatabase.openDatabase(sourceFileUri, null, SQLiteDatabase.OPEN_READONLY);
                    table_name = patientNameText + "_" + patientIDText + "_" + ageText + "_" + patientSex;
                    String sqlSelectQuery = "SELECT * FROM " + table_name + " ORDER BY recID LIMIT 10";
                    Log.d("Debug", "QUERY---: " + sqlSelectQuery);
                    Cursor cursor = openDB.rawQuery(sqlSelectQuery, null);
                    dloadRun = 0;
                    downSeries1.resetData(new DataPoint[] {});
                    downSeries2.resetData(new DataPoint[] {});
                    downSeries3.resetData(new DataPoint[] {});


                    if (cursor.moveToFirst()) {

                        do {
                            String x=cursor.getString(cursor.getColumnIndex("x_values"));
                            String y=cursor.getString(cursor.getColumnIndex("y_values"));
                            String z=cursor.getString(cursor.getColumnIndex("z_values"));
                            // Put  the  data into array,or class variable

                            float fx = Float.parseFloat(x);
                            float fy = Float.parseFloat(y);
                            float fz = Float.parseFloat(z);
                            downSeries1.appendData(new DataPoint(dloadRun, fx), true, 500);
                            downSeries2.appendData(new DataPoint(dloadRun, fy), true, 500);
                            downSeries3.appendData(new DataPoint(dloadRun, fz), true, 500);
                            dloadRun += 1;
                            Log.d("Debug", "x-"+x+" y-"+y+" z-"+z+" XVALUES="+dloadRun);


                        } while (cursor.moveToNext());

                        openDB.close();

                    }
                    Log.d("Debug", "PLOTTING FROM DB---: " + sqlSelectQuery);
                    graphView.removeAllSeries();
                    graphView.getViewport().setXAxisBoundsManual(true);
                    graphView.getViewport().setMinX(0);
                    graphView.getViewport().setMaxX(10);
                    graphView.addSeries(downSeries1);
                    graphView.addSeries(downSeries2);
                    graphView.addSeries(downSeries3);

                    Toast.makeText(MainActivity.this,"Database downloaded successfully.", Toast.LENGTH_LONG).show();

                }
                else{
                    //Download unsuccessful
                    //Toast.makeText(MainActivity.this,"Unsuccessful. File does not exist.", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Add functionality for Upload Database button
        Button uploadDb = (Button) findViewById(R.id.upload_database);
        uploadDb.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // https://stackoverflow.com/questions/25398200/uploading-file-in-php-server-from-android-device
                Log.d("debug", "code flow 0 onclickevent of upload database");
                String uploadUrl = "http://10.218.110.136/CSE535Fall17Folder/UploadToServer.php";
                String sourceFileUri = Environment.getExternalStorageDirectory()+ File.separator+ "Android/Data/CSE535_ASSIGNMENT2/group2";
                Log.d("debug", "code flow 1 after getting sourceFileUri");
                File uploadFile = new File(sourceFileUri);
                HttpURLConnection conn = null;
                DataOutputStream dos = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1024 * 1024;
                Log.d("debug", "code flow 2 before opening fileInputStream");

                Toast.makeText(MainActivity.this,"Uploading...", Toast.LENGTH_LONG).show();

                if (uploadFile.isFile()) {
                    try {
                        // open a URL connection to the Servlet
                        FileInputStream fileInputStream = new FileInputStream(uploadFile);
                        URL url = new URL(uploadUrl);

                        // Open a HTTP connection to the URL
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true); // Allow Inputs
                        conn.setDoOutput(true); // Allow Outputs
                        conn.setUseCaches(false); // Don't use a Cached Copy
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                        conn.setRequestProperty("Content-Type",
                                "multipart/form-data;boundary=" + boundary);
                        conn.setRequestProperty("uploaded_file", sourceFileUri);

                        dos = new DataOutputStream(conn.getOutputStream());

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                                + sourceFileUri + "\"" + lineEnd);

                        dos.writeBytes(lineEnd);
                        Log.d("debug", "code flow 3 before bytesAvailable");

                        // create a buffer of maximum size
                        bytesAvailable = fileInputStream.available();

                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];

                        // read file and write it into form...
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        while (bytesRead > 0) {

                            dos.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math.min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        }

                        Log.d("debug", "code flow 4 before sending");
                        // send multipart form data necesssary after file
                        // data...
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                        Log.d("debug", "code flow 5 after sending");

                        // Responses from the server (code and message)
                        int serverResponseCode = conn.getResponseCode();
                        String serverResponseMessage = conn.getResponseMessage();

                        if (serverResponseCode == 200) {


                            Log.d("debug", "uploaded successfully");

                        }
                        Log.d("debug", "code flow 6 check server response code: " + serverResponseCode);

                        // close the streams //
                        fileInputStream.close();
                        dos.flush();
                        dos.close();
                        //Uploaded successfully
                        Toast.makeText(MainActivity.this,"Database uploaded successfully.", Toast.LENGTH_LONG).show();

                    } catch (Exception e) {


                        e.printStackTrace();
                        /*
                        //upload unsuccessful
                        Toast.makeText(MainActivity.this,"Unsuccessful in uploading database.", Toast.LENGTH_LONG).show();
                        */

                    }
                    // dialog.dismiss();

                } // End else block
            }
        });

    }

    // Add option for Gender button
    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.radioButton:
                if (checked) {
                    patientSex="Female";
                }
                break;
            case R.id.radioButton2:
                if (checked) {
                    patientSex="Male";
                }
                break;
        }
    }

    // onClick function for Run button
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.run: {

                Log.d("MR.bool", "RUN WAS CLICKED CLEARING GRAPH");


                GraphView graph = (GraphView) findViewById(R.id.graph);
                graph.removeAllSeries();
                // Triggered when run button is clicked

                Log.d("MR.bool", "Run was clicked ");
                graph.addSeries(mSeries1);
                graph.addSeries(mSeries2);
                graph.addSeries(mSeries3);

                if (firstRun) {
                    // Start graph for the first time if run clicked in the start
                    // RESET FOR NEW USER,
                    offSet=0;
                    x_value=0;
                    R1.start();

                } else {
                    // If run is clicked again, resume drawing graph from when it was stopped
                    R1.resume();
                }
                break;

            }
            case R.id.stop: {
                //Triggered when stop button is clicked
                Log.d("MR.bool", "Stop was clicked ");
                GraphView graph = (GraphView) findViewById(R.id.graph);
                graph.removeAllSeries();
                firstRun = false;
                R1.suspend();
                break;
            }
        }
    }

    // How to set sampling rate of accelerometer to 1 Hz
    // https://stackoverflow.com/questions/19859571/is-that-possible-accelerometer-1hz-update-frequency
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (started) {
            if ((System.currentTimeMillis() - lastSaved) >= ACCE_FILTER_DATA_MIN_TIME) {
                lastSaved = System.currentTimeMillis();
                double x = event.values[0];
                double y = event.values[1];
                double z = event.values[2];

                db.execSQL("INSERT INTO " + table_name + " (timestamp, x_values, "
                        + "y_values, z_values) values (CURRENT_TIMESTAMP," + x + ", "+ y + ", "
                        + z + ")");
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Class to support download database button, referred from code in Blackboard
    private class DownloadFileFromURL extends AsyncTask<String, Integer, String> {

        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... sURL) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            try {
                //System.out.println("URL is " + sURL[0]);
                URL url = new URL(sURL[0]);
                connection = (HttpURLConnection) url.openConnection();

                connection.connect();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    final File file = new File(Environment.getExternalStorageDirectory()+ File.separator+"Android/Data/CSE535_ASSIGNMENT2_Extra");
                    if (!file.exists()) {
                        file.mkdirs();
                    }

                    input = new BufferedInputStream(url.openStream(), 8192);
                    output = new FileOutputStream(file + File.separator + "group2");

                    byte data[] = new byte[4096];
                    int count = 0;
                    while ((count = input.read(data)) != -1) {
                        // writing data to file
                        output.write(data, 0, count);
                    }
                    // flushing output
                    output.flush();

                    // closing streams
                    output.close();
                    input.close();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

    }

    // For the Run button
    class RunnableDemo implements Runnable {
        public Thread thread;
        public boolean suspended = false;

        public void run() {
            // we add 5000 new entries
            for (int i = 0; i < 5000; i++) {
                runOnUiThread(new RunnableDemo() {
                    @Override
                    public void run() {
                        addEntry();

                    }
                });

                // sleep to slow down the add of entries
                try {
                    Thread.sleep(1000);
                    synchronized (this) {
                        while (suspended) {
                            wait();
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("Exception caught: " + e);
                }
            }
        }

        void start () {
            if (thread == null) {
                thread = new Thread (this);
                thread.start ();
            }
        }

        void suspend() {
            suspended = true;
        }

        synchronized void resume() {
            suspended = false;
            notify();
        }
    }


    // Function for plotting the data into graph
    public void addEntry() {
        Log.d("Debug", "tablename: " + table_name);
        String sourceFileUri = Environment.getExternalStorageDirectory() + File.separator + "Android/Data/CSE535_ASSIGNMENT2/group2";
        SQLiteDatabase openDB = SQLiteDatabase.openDatabase(sourceFileUri, null, SQLiteDatabase.OPEN_READONLY);
        String sqlSelectQuery = "SELECT * FROM " + table_name + " ORDER BY recID LIMIT 1 OFFSET " + Integer.toString(offSet);
        Log.d("Debug", "QUERY---: " + sqlSelectQuery);
        offSet += 1;
        Cursor cursor = openDB.rawQuery(sqlSelectQuery, null);

        if (cursor.moveToFirst()) {

            do {
                String x=cursor.getString(cursor.getColumnIndex("x_values"));
                String y=cursor.getString(cursor.getColumnIndex("y_values"));
                String z=cursor.getString(cursor.getColumnIndex("z_values"));
                // get  the  data into array,or class variable

                float fx = Float.parseFloat(x);
                float fy = Float.parseFloat(y);
                float fz = Float.parseFloat(z);
                mSeries1.appendData(new DataPoint(x_value, fx), true, 500);
                mSeries2.appendData(new DataPoint(x_value, fy), true, 500);
                mSeries3.appendData(new DataPoint(x_value, fz), true, 500);
                x_value += 1;
                Log.d("Debug", "x-"+x+" y-"+y+" z-"+z+" XVALUES="+x_value);
                Log.d("Debug", "PLOTTING_----------------"+mSeries1);

            } while (cursor.moveToNext());

            openDB.close();

        }
    }

}