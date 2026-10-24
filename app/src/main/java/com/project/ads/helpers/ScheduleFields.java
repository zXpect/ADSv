package com.project.ads.helpers;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.project.ads.R;
import com.project.ads.models.ServiceRequest;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Campos de agenda (layout view_schedule_fields): fecha y hora de llegada y
 * duración estimada (D9). Valida lo mismo que el backend: la llegada es futura
 * y dentro de los próximos 30 días, y la duración está entre 15 y 720 minutos.
 * Fecha y hora con los selectores de Material, que toman el tema ADS (DEF-29).
 */
public class ScheduleFields {

    public static final int MAX_DAYS_AHEAD = 30;
    private static final int[] DURATIONS_MIN = {15, 30, 45, 60, 90, 120, 180, 240, 300, 360, 480, 600, 720};
    private static final int DEFAULT_DURATION_INDEX = 3;   // 1 h

    private final FragmentActivity mActivity;
    private final Button mDateButton;
    private final Button mTimeButton;
    private final Spinner mDurationSpinner;
    private final TextView mErrorText;

    private Calendar mDate;     // solo año, mes y día
    private int mHour = -1;
    private int mMinute;

    public ScheduleFields(FragmentActivity activity, View root) {
        mActivity = activity;
        mDateButton = root.findViewById(R.id.btn_schedule_date);
        mTimeButton = root.findViewById(R.id.btn_schedule_time);
        mDurationSpinner = root.findViewById(R.id.sp_duration);
        mErrorText = root.findViewById(R.id.tv_schedule_error);

        String[] labels = new String[DURATIONS_MIN.length];
        for (int i = 0; i < DURATIONS_MIN.length; i++) labels[i] = ServiceRequest.formatDuration(DURATIONS_MIN[i]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, labels);
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
        // MaterialDatePicker trabaja con la medianoche UTC del día elegido
        Calendar today = Calendar.getInstance();
        long first = utcMidnight(today);
        today.add(Calendar.DAY_OF_YEAR, MAX_DAYS_AHEAD);
        long last = utcMidnight(today);
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(first)
                .setEnd(last)
                .setOpenAt(mDate != null ? utcMidnight(mDate) : first)
                .setValidator(CompositeDateValidator.allOf(Arrays.asList(
                        DateValidatorPointForward.from(first), DateValidatorPointBackward.before(last + 1))))
                .build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.schedule_picker_date_title)
                .setSelection(mDate != null ? utcMidnight(mDate) : first)
                .setCalendarConstraints(constraints)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);
            mDate = Calendar.getInstance();
            mDate.clear();
            mDate.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
            refreshButtons();
        });
        picker.show(mActivity.getSupportFragmentManager(), "schedule_date");
    }

    /** Medianoche UTC del mismo día (año, mes y día locales). */
    public static long utcMidnight(Calendar local) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return utc.getTimeInMillis();
    }

    private void pickTime() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setHour(mHour >= 0 ? mHour : 8)
                .setMinute(mHour >= 0 ? mMinute : 0)
                .setTitleText(R.string.schedule_picker_time_title)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            mHour = picker.getHour();
            mMinute = picker.getMinute();
            refreshButtons();
        });
        picker.show(mActivity.getSupportFragmentManager(), "schedule_time");
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
        showError(mActivity.getString(messageRes, MAX_DAYS_AHEAD));
    }
}
