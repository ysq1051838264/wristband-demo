/**************************************************************************************************
 * Filename:       FwUpdateActivity.java
 * Revised:        $Date: 2013-09-05 05:55:20 +0200 (to, 05 sep 2013) $
 * Revision:       $Revision: 27614 $
 * <p>
 * Copyright 2013 Texas Instruments Incorporated. All rights reserved.
 * <p>
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user
 * who downloaded the software, his/her employer (which must be your employer)
 * and Texas Instruments Incorporated (the "License").  You may not use this
 * Software unless you agree to abide by the terms of the License.
 * The License limits your use, and you acknowledge, that the Software may not be
 * modified, copied or distributed unless used solely and exclusively in conjunction
 * with a Texas Instruments Bluetooth device. Other than for the foregoing purpose,
 * you may not use, reproduce, copy, prepare derivative works of, modify, distribute,
 * perform, display or sell this Software and/or its documentation for any purpose.
 * <p>
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED �AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * TEXAS INSTRUMENTS OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT,
 * NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER
 * LEGAL EQUITABLE THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES
 * INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE
 * OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT
 * OF SUBSTITUTE GOODS, TECHNOLOGY, SERVICES, OR ANY CLAIMS BY THIRD PARTIES
 * (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 * <p>
 * Should you have any questions regarding your right to use this Software,
 * contact Texas Instruments Incorporated at www.TI.com
 **************************************************************************************************/
package com.hdr.wristband;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.*;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.hdr.wristband.ble.OTAService;
import com.hdr.wristband.ble.WristBleService;
import no.nordicsemi.android.support.v18.scanner.*;

import java.util.ArrayList;
import java.util.List;

public class NordicFwUpdateActivity extends Activity {

    // GUI

    private static IntentFilter makeDfuUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(OTAService.BROADCAST_PROGRESS);
        intentFilter.addAction(OTAService.BROADCAST_ERROR);
        intentFilter.addAction(OTAService.BROADCAST_LOG);
        return intentFilter;
    }

    private Button mBtnStart;

    // dialog begin
    TextView max_progress, progress_percent, progress_time;
    ProgressBar mProgress;
    // dialog end
    ImageView back;
    ProgressDialog progressDialog;

    BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();

    Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 1) {
                new Builder(NordicFwUpdateActivity.this)
                        .setTitle(getResources().getString(R.string.bind_warm))
                        .setMessage(
                                getResources().getString(
                                        R.string.FirmwareUpgrade_msg))
                        .setPositiveButton(
                                getResources().getString(R.string.button_ok),
                                new OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        finish();
                                    }
                                }).show();
            } else if (msg.what == 2) {
                new Builder(NordicFwUpdateActivity.this)
                        .setMessage(getResources().getString(R.string.upsucc))
                        .setPositiveButton(
                                getResources().getString(R.string.button_ok),
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                        .setCancelable(false)
                        .show();

            } else if (msg.what == 3) {
                new Builder(NordicFwUpdateActivity.this)
                        .setMessage(getResources().getString(R.string.uperror))
                        .setPositiveButton(
                                getResources().getString(R.string.button_ok),
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                        .setCancelable(false)
                        .show();
            }
        }

    };


    boolean flag = false;
    boolean otaHasBegin = false;

    ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("wrist", result.toString());
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanner.stopScan(scanCallback);
                    if (!otaHasBegin)
                        startOTA();
                    otaHasBegin = true;
                }
            },2000);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };


    private final BroadcastReceiver mDfuUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            // DFU is in progress or an error occurred
            final String action = intent.getAction();
            if (OTAService.BROADCAST_PROGRESS.equals(action)) {
                final int progress = intent.getIntExtra(OTAService.EXTRA_DATA, 0);
                final int currentPart = intent.getIntExtra(OTAService.EXTRA_PART_CURRENT, 1);
                final int totalParts = intent.getIntExtra(OTAService.EXTRA_PARTS_TOTAL, 1);
                Log.e("progress=", progress + "--");
                if (progress == 99) {
                    flag = true;
                }
                updateProgressBar(progress, currentPart, totalParts, false, false);
            } else if (OTAService.BROADCAST_ERROR.equals(action)) {
                final int error = intent.getIntExtra(OTAService.EXTRA_DATA, 0);
                final boolean connectionStateError = intent.getIntExtra(OTAService.EXTRA_ERROR_TYPE, OTAService.ERROR_TYPE_OTHER) == OTAService.ERROR_TYPE_COMMUNICATION_STATE;
                updateProgressBar(error, 0, 0, true, connectionStateError);
            } else if ((OTAService.BROADCAST_LOG.equals(action))) {
                final String log = intent.getStringExtra(OTAService.EXTRA_LOG_MESSAGE);
                Log.i("更新固件", log);
            }
        }
    };

    Dialog downloadDialog;


    public static String getOTAMac(String DeviceAddress) {
        String lastStr = DeviceAddress.substring(DeviceAddress.length() - 2,
                DeviceAddress.length());
        int a = Integer.parseInt(lastStr, 16) + 1;
        String ad = Integer.toHexString(a).toUpperCase();
        if (ad.length() == 1) {
            String temp1 = ad + "0";
            String temp2 = "0" + ad;
            if (Integer.parseInt(temp1, 16) == a) {
                ad = temp1;
            } else if (Integer.parseInt(temp2, 16) == a) {
                ad = temp2;
            }
        } else if (ad.equals("100")) {
            ad = "00";
        }
        return DeviceAddress.substring(0, DeviceAddress.length() - 2) + ad;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fwupdate);


        List<ScanFilter> sfList = new ArrayList<>();
        ScanFilter sf = new ScanFilter.Builder().setDeviceAddress(WristBleService.getTargetAddress()).build();
        sfList.add(sf);
        sf = new ScanFilter.Builder().setDeviceAddress(getOTAMac(WristBleService.getTargetAddress())).build();
        sfList.add(sf);
        scanner.startScan(sfList, new ScanSettings.Builder().setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE).setNumOfMatches(1).build(), scanCallback);

    }


    void startOTA() {
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/btlinker_w/hdr.zip";
        Intent d = new Intent(this, OTAService.class);
        d.putExtra(OTAService.EXTRA_DEVICE_ADDRESS, getOTAMac(WristBleService.getTargetAddress()));
        d.putExtra(OTAService.EXTRA_FILE_MIME_TYPE, "application/zip");
        d.putExtra(OTAService.EXTRA_FILE_TYPE, 0);
        d.putExtra(OTAService.EXTRA_FILE_PATH, filePath);
        d.putExtra(OTAService.EXTRA_KEEP_BOND, false);
        startService(d);


        // Initialize widgets
        back = (ImageView) findViewById(R.id.menu);
        back.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mBtnStart = (Button) findViewById(R.id.btn_start);
        mBtnStart.setEnabled(false);
        mBtnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });
        showDialog();
        progress_time.setText(getString(R.string.firmware_msg));
        progressDialog = new ProgressDialog(this);

        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(mDfuUpdateReceiver, makeDfuUpdateIntentFilter());

        progressDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.unregisterReceiver(mDfuUpdateReceiver);

        FunctionUtilsKt.wristConnect(this);
    }

    private void showDialog() {
        Builder builder = new Builder(this);
        builder.setTitle(getResources().getString(R.string.FirmwareUpgrade));

        final LayoutInflater inflater = LayoutInflater
                .from(getApplicationContext());
        View v = inflater.inflate(R.layout.updateprogressview, null);
        mProgress = (ProgressBar) v.findViewById(R.id.upgrade_progress);
        mProgress.setMax(100);
        progress_percent = (TextView) v.findViewById(R.id.progress_percent);
        progress_time = (TextView) v.findViewById(R.id.progress_time);

        builder.setView(v);

        builder.setCancelable(false);
        downloadDialog = builder.create();
    }

    private void updateProgressBar(final int progress, final int part, final int total, final boolean error, final boolean connectionError) {
        if (downloadDialog.isShowing()) mProgress.setProgress(progress);
        switch (progress) {
            case OTAService.PROGRESS_STARTING:
                if (progressDialog.isShowing()) progressDialog.dismiss();
                downloadDialog.show();
                break;

            case OTAService.PROGRESS_VALIDATING:
                if (progressDialog.isShowing()) progressDialog.dismiss();
                downloadDialog.dismiss();
                stopService(new Intent(this, OTAService.class));
                handler.sendEmptyMessage(1);
                break;

            case OTAService.PROGRESS_COMPLETED:
                downloadDialog.dismiss();
                stopService(new Intent(this, OTAService.class));
                handler.sendEmptyMessage(2);
                break;
            case -5:
                if (progressDialog.isShowing()) progressDialog.dismiss();
                downloadDialog.dismiss();
                stopService(new Intent(this, OTAService.class));
                if (flag) {
                    handler.sendEmptyMessage(2);
                } else {
                    handler.sendEmptyMessage(3);
                }

                break;
        }
    }


}
