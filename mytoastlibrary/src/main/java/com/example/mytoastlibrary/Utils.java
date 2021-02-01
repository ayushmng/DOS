package com.example.mytoastlibrary;

import android.content.Context;
import android.widget.Toast;

public class Utils {

    public static void shortToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
