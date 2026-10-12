package com.project.ads.helpers;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.project.ads.R;
import com.project.ads.models.ServiceRequest;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Campos de agenda (layout view_schedule_fields): fecha y hora de llegada y
 * duración estimada (D9). Valida lo mismo que el backend: la llegada es futura
 * y dentro de los próximos 30 días, y la duración está entre 15 y 720 minutos.
 */
public class ScheduleFields {

    public static final int MAX_DAYS_AHEAD = 30;
    private static final int[] DURATIONS_MIN = {15, 30, 45, 60, 90, 120, 180, 240, 300, 360, 480, 600, 720};
    private static final int DEFAULT_DURATION_INDEX = 3;   // 1 h

    private final Context mContext;
    private final Button mDateButton;
    private final Button mTimeButton;
    private final Spinner mDurationSpinner;
    private final TextView mErrorText;

    private Calendar mDate;     // solo año, mes y día
    private int mHour = -1;
    private int mMinute;

    public ScheduleFields(Context context, View root) {
        mContext = context;
        mDateButton = root.findViewById(R.id.btn_schedule_date);
        mTimeButton = root.findViewById(R.id.btn_schedule_time);
        mDurationSpinner = root.findViewById(R.id.sp_duration);
        mErrorText = root.findViewById(R.id.tv_schedule_error);

        String[] labels = new String[DURATIONS_MIN.length];
        for (int i = 0; i < DURATIONS_MIN.length; i++) labels[i] = ServiceRequest.formatDuration(DURATIONS_MIN[i]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDurationSpinner.setAdapter(adapter);
        mDurationSpinner.setSelection(DEFAULT_DURATION_INDEX);

        mDateButton.setOnClickListener(v -> pickDate());
        mTimeButton.setOnClickListener(v -> pickTime());
    }

    /** Precarga una agenda existente (al reprogramar). */
    public void setInitial(long scheduledAt, int durationMin) {
        if (scheduledAt > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(scheduledAt);
            mDate = Calendar.getInstance();
            mDate.clear();
            mDate.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            mHour = calendar.get(Calendar.HOUR_OF_DAY);
            mMinute = calendar.get(Calendar.MINUTE);
            refreshButtons();
        }
        for (int i = 0; i < DURATIONS_MIN.length; i++) {
            if (DURATIONS_MIN[i] == durationMin) mDurationSpinner.setSelection(i);
        }
    }

    private void pickDate() {
        Calendar now = Calendar.getInstance();
        Calendar initial = mDate != null ? mDate : now;
        DatePickerDialog dialog = new DatePickerDialog(mContext, (view, year, month, day) -> {
            mDate = Calendar.getInstance();
            mDate.clear();
            mDate.set(year, month, day);
            refreshButtons();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(now.getTimeInMillis() - 1000);
        dialog.getDatePicker().setMaxDate(now.getTimeInMillis() + MAX_DAYS_AHEAD * 24L * 60 * 60 * 1000);
        dialog.show();
    }

    private void pickTime() {
        int hour = mHour >= 0 ? mHour : 8;
        new TimePickerDialog(mContext, (view, h, m) -> {
            mHour = h;
            mMinute = m;
            refreshButtons();
        }, hour, mHour >= 0 ? mMinute : 0, true).show();
    }

    private void refreshButtons() {
        mErrorText.setVisibility(View.GONE);
        if (mDate != null) {
            mDateButton.setText(new SimpleDateFormat("EEE dd/MM", new Locale("es", "CO")).format(mDate.getTime()));
        }
        if (mHour >= 0) {
            mTimeButton.setText(String.format(Locale.ROOT, "%02d:%02d", mHour, mMinute));
        }
    }

    /** Llegada en ms, o null si falta o no es válida (muestra el error). */
    public Long getScheduledAt() {
        if (mDate == null || mHour < 0) {
            showError(R.string.schedule_error_missing);
            return null;
        }
        Calendar scheduled = (Calendar) mDate.clone();
        scheduled.set(Calendar.HOUR_OF_DAY, mHour);
        scheduled.set(Calendar.MINUTE, mMinute);
        long value = scheduled.getTimeInMillis();
        long now = System.currentTimeMillis();
        if (value <= now) {
            showError(R.string.schedule_error_past);
            return null;
        }
        if (value > now + MAX_DAYS_AHEAD * 24L * 60 * 60 * 1000) {
            showError(R.string.schedule_error_too_far);
            return null;
        }
        return value;
    }

    public int getDurationMin() {
        return DURATIONS_MIN[mDurationSpinner.getSelectedItemPosition()];
    }

    public void showError(String message) {
        mErrorText.setText(message);
        mErrorText.setVisibility(View.VISIBLE);
    }

    private void showError(int messageRes) {
        showError(mContext.getString(messageRes, MAX_DAYS_AHEAD));
    }
}
