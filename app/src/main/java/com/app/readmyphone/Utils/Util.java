package com.app.readmyphone.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.app.readmyphone.R;

public class Util {

    public static SharedPreferences getPref(Context context)
    {
        return context.getSharedPreferences(
                context.getString(R.string.my_pref), Context.MODE_PRIVATE);
    }
}
