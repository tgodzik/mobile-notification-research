package com.tokudu.demo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class PushActivity implements TestClient {

    private String mDeviceID;
    private Activity parent;

    public PushActivity(Activity _parent) {
        parent= _parent;
        mDeviceID = Secure.getString(parent.getContentResolver(), Secure.ANDROID_ID);
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        PushService.messageListener = handler;
    }

    @Override
    public boolean create() {
        Editor editor = parent.getSharedPreferences(PushService.tag, Context.MODE_PRIVATE).edit();
        editor.putString(PushService.PREF_DEVICE_ID, mDeviceID);
        editor.commit();
        PushService.actionStart(parent.getApplicationContext());

        return true;
    }

    @Override
    public void dispose() {
        PushService.actionStop(parent.getApplicationContext());
    }
}