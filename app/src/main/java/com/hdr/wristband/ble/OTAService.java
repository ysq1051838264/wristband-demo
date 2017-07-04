package com.hdr.wristband.ble;

import android.app.Activity;
import com.hdr.wristband.NordicFwUpdateActivity;
import no.nordicsemi.android.dfu.DfuBaseService;

/**
 * Created by hdr on 16/8/25.
 */
public class OTAService extends DfuBaseService {
    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return NotificationActivity.class;
    }
}
