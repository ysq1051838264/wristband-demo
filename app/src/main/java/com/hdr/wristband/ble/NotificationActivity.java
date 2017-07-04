package com.hdr.wristband.ble;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class NotificationActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If this activity is the root activity of the task, the app is not running

        // Now finish, which will drop the user in to the activity that was at the top
        //  of the task stack
        finish();
    }
}