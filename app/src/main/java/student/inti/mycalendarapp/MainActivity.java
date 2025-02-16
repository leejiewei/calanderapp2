package student.inti.mycalendarapp;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements CalendarAdapter.OnDayClickListener {

    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private CalendarAdapter adapter;
    private List<Calendar> daysList;
    private Calendar currentMonthCalendar;
    private TextView tvMonth;
    private ImageButton btnPrev, btnNext;
    private ListView listViewMonthlyEvents;
    private EventDB dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = "event_channel";
            CharSequence channelName = "Event Notifications";
            String channelDescription = "Notifications for scheduled events";
            int importance = android.app.NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDescription);
            android.app.NotificationManager notificationManager = getSystemService(android.app.NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        tvMonth = findViewById(R.id.tvMonth);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        recyclerView = findViewById(R.id.recyclerViewCalendar);
        // New ListView below the calendar to show events for the month
        listViewMonthlyEvents = findViewById(R.id.listViewMonthlyEvents);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 7));

        currentMonthCalendar = Calendar.getInstance();
        currentMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);

        dbHelper = new EventDB(this);

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentMonthCalendar.add(Calendar.MONTH, -1);
                updateCalendar();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentMonthCalendar.add(Calendar.MONTH, 1);
                updateCalendar();
            }
        });

        updateCalendar();

    }

    private void updateCalendar() {

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonth.setText(sdf.format(currentMonthCalendar.getTime()));


        daysList = generateDaysForMonth(currentMonthCalendar);
        adapter = new CalendarAdapter(daysList, this);
        recyclerView.setAdapter(adapter);


        loadMonthlyEvents();
    }


    private List<Calendar> generateDaysForMonth(Calendar monthCalendar) {
        List<Calendar> days = new ArrayList<>();
        Calendar calendar = (Calendar) monthCalendar.clone();
        int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        for (int i = 1; i < dayOfWeek; i++) {
            days.add(null);
        }
        for (int i = 1; i <= maxDays; i++) {
            calendar.set(Calendar.DAY_OF_MONTH, i);
            days.add((Calendar) calendar.clone());
        }
        return days;
    }

    @Override
    public void onDayClick(Calendar date) {
        if (date == null) return;
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra("selectedDate", date.getTimeInMillis());
        startActivity(intent);
    }


    private void loadMonthlyEvents() {

        Calendar startCal = (Calendar) currentMonthCalendar.clone();
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        long startOfMonth = getStartOfDay(startCal.getTimeInMillis());


        Calendar endCal = (Calendar) currentMonthCalendar.clone();
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        long endOfMonth = getStartOfDay(endCal.getTimeInMillis()) + (24 * 60 * 60 * 1000);

        Cursor cursor = dbHelper.getEventsForDate(startOfMonth, endOfMonth);
        ArrayList<String> monthlyEvents = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex(EventDB.COLUMN_TITLE));
                @SuppressLint("Range") String desc = cursor.getString(cursor.getColumnIndex(EventDB.COLUMN_DESCRIPTION));
                @SuppressLint("Range") long start = cursor.getLong(cursor.getColumnIndex(EventDB.COLUMN_START_TIME));
                SimpleDateFormat timeFormat = new SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault());
                String timeStr = timeFormat.format(new Date(start));
                monthlyEvents.add(timeStr + " - " + title + "\n" + desc);
            } while (cursor.moveToNext());
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, monthlyEvents);
        listViewMonthlyEvents.setAdapter(adapter);
    }


    private long getStartOfDay(long timeMillis) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = new Date(timeMillis);
            Date start = sdf.parse(sdf.format(date));
            return start.getTime();
        } catch (Exception e) {
            return timeMillis;
        }
    }
}
