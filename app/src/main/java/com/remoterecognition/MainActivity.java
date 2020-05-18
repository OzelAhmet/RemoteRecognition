package com.remoterecognition;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MAIN", "Act started.");

        ThreadPooledServer server = new ThreadPooledServer(8080);
        new Thread(server).start();

    }

}
