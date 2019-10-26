package com.uroflowmetry.library.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.uroflowmetry.R;

public class DialogUtils {

    public interface OnOkayEvent {
        void onOkay();
    }

    public interface OnOkayNoEvent {
        void onOkay();
        void onNo();
    }

    public static void showOkayDialog(Context context, String title, String msg, final OnOkayEvent func) {
        try {
            new MaterialDialog.Builder(context)
                    .title(title)
                    .content(msg)
                    .positiveText("Okay")
                    .theme(Theme.LIGHT)
                    .canceledOnTouchOutside(false)
                    .positiveColorRes(R.color.colorPrimary)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            if (func != null) func.onOkay();
                        }
                    })
                    .show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void showOkayDialog(Context context, String title, String msg) {
        showOkayDialog(context, title, msg, new OnOkayEvent() {
            @Override
            public void onOkay() {

            }
        });
    }

    public static void showOkayNoDialog(Context context, String title, String msg, final OnOkayNoEvent func) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(title)
                .content(msg)
                .cancelable(false)
                .positiveText("Yes")
                .theme(Theme.LIGHT)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (func != null) func.onOkay();
                    }
                })
                .negativeText("No")
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (func != null) func.onNo();
                    }
                });
        builder.show();
    }
}
