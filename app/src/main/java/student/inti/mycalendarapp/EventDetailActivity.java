package student.inti.mycalendarapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EventDetailActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailActivity";
    private TextView tvSelectedDate;
    private ListView listViewEvents;
    private Button btnAddEvent, btnEditEvent, btnDeleteEvent;
    private EventDB dbHelper;
    private long selectedDate; // Timestamp for the selected day
    private ArrayList<Event> eventList = new ArrayList<>();
    private Event selectedEvent = null; // currently selected event

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        listViewEvents = findViewById(R.id.listViewEvents);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        btnEditEvent = findViewById(R.id.btnEditEvent);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
        dbHelper = new EventDB(this);

        // Get the selected date from MainActivity
        selectedDate = getIntent().getLongExtra("selectedDate", System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault());
        tvSelectedDate.setText("Selected Date: " + sdf.format(new Date(selectedDate)));

        loadEvents();
        clearSelection();


        btnAddEvent.setOnClickListener(v -> showAddEventDialog());


        listViewEvents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedEvent = eventList.get(position);
                Toast.makeText(EventDetailActivity.this, "Selected event: " + selectedEvent.getTitle(), Toast.LENGTH_SHORT).show();
                btnEditEvent.setEnabled(true);
                btnDeleteEvent.setEnabled(true);
            }
        });


        btnEditEvent.setOnClickListener(v -> {
            if (selectedEvent != null) {
                showEditEventDialog(selectedEvent);
            } else {
                Toast.makeText(EventDetailActivity.this, "No event selected", Toast.LENGTH_SHORT).show();
            }
        });


        btnDeleteEvent.setOnClickListener(v -> {
            if (selectedEvent != null) {
                int rows = dbHelper.deleteEvent(selectedEvent.getId());
                if (rows > 0) {
                    Toast.makeText(EventDetailActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                    loadEvents();
                    clearSelection();
                } else {
                    Toast.makeText(EventDetailActivity.this, "Error deleting event", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(EventDetailActivity.this, "No event selected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
        clearSelection();
    }


    private void clearSelection() {
        selectedEvent = null;
        btnEditEvent.setEnabled(false);
        btnDeleteEvent.setEnabled(false);
    }


    @SuppressWarnings("ResourceType")
    private void loadEvents() {
        long startOfDay = getStartOfDay(selectedDate);
        long endOfDay = startOfDay + (24 * 60 * 60 * 1000);
        Cursor cursor = dbHelper.getEventsForDate(startOfDay, endOfDay);
        eventList.clear();
        if (cursor.moveToFirst()) {
            do {
                int titleIndex = cursor.getColumnIndex(EventDB.COLUMN_TITLE);
                int descIndex = cursor.getColumnIndex(EventDB.COLUMN_DESCRIPTION);
                int startIndex = cursor.getColumnIndex(EventDB.COLUMN_START_TIME);
                int endIndex = cursor.getColumnIndex(EventDB.COLUMN_END_TIME);
                int idIndex = cursor.getColumnIndex(EventDB.COLUMN_ID);
                Log.d(TAG, "Indices - Title: " + titleIndex + ", Desc: " + descIndex +
                        ", Start: " + startIndex + ", End: " + endIndex + ", ID: " + idIndex);
                if (titleIndex == -1 || descIndex == -1 || startIndex == -1 || endIndex == -1 || idIndex == -1) {
                    Log.e(TAG, "Missing column in cursor. Skipping row.");
                    continue;
                }
                String title = cursor.getString(titleIndex);
                String desc = cursor.getString(descIndex);
                long start = cursor.getLong(startIndex);
                long end = cursor.getLong(endIndex);
                Event event = new Event(title, desc, start, end);
                event.setId(cursor.getLong(idIndex));
                eventList.add(event);
            } while (cursor.moveToNext());
        }
        cursor.close();

        ArrayAdapter<Event> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, eventList);
        listViewEvents.setAdapter(adapter);
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


    private void showAddEventDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Event");

        final EditText inputTitle = new EditText(this);
        inputTitle.setHint("Event Title");

        final EditText inputDesc = new EditText(this);
        inputDesc.setHint("Event Description");
        inputDesc.setInputType(InputType.TYPE_CLASS_TEXT);

        final EditText inputStartTime = new EditText(this);
        inputStartTime.setHint("Start Time (e.g. 09:00 AM)");
        inputStartTime.setFocusable(false);

        final EditText inputEndTime = new EditText(this);
        inputEndTime.setHint("End Time (e.g. 10:00 AM)");
        inputEndTime.setFocusable(false);

        final long baseTime = getStartOfDay(selectedDate);
        final long[] chosenStartTime = {baseTime + 9 * 60 * 60 * 1000L};
        final long[] chosenEndTime = {baseTime + 10 * 60 * 60 * 1000L};

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        inputStartTime.setText(timeFormat.format(new Date(chosenStartTime[0])));
        inputEndTime.setText(timeFormat.format(new Date(chosenEndTime[0])));

        inputStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(chosenStartTime[0]);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            new TimePickerDialog(EventDetailActivity.this, (TimePicker view, int hourOfDay, int minute1) -> {
                Calendar newStart = Calendar.getInstance();
                newStart.setTimeInMillis(baseTime);
                newStart.set(Calendar.HOUR_OF_DAY, hourOfDay);
                newStart.set(Calendar.MINUTE, minute1);
                chosenStartTime[0] = newStart.getTimeInMillis();
                inputStartTime.setText(timeFormat.format(new Date(chosenStartTime[0])));
            }, hour, minute, false).show();
        });

        inputEndTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(chosenEndTime[0]);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            new TimePickerDialog(EventDetailActivity.this, (TimePicker view, int hourOfDay, int minute1) -> {
                Calendar newEnd = Calendar.getInstance();
                newEnd.setTimeInMillis(baseTime);
                newEnd.set(Calendar.HOUR_OF_DAY, hourOfDay);
                newEnd.set(Calendar.MINUTE, minute1);
                chosenEndTime[0] = newEnd.getTimeInMillis();
                inputEndTime.setText(timeFormat.format(new Date(chosenEndTime[0])));
            }, hour, minute, false).show();
        });

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(inputTitle);
        layout.addView(inputDesc);
        layout.addView(inputStartTime);
        layout.addView(inputEndTime);
        builder.setView(layout);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = inputTitle.getText().toString().trim();
                String description = inputDesc.getText().toString().trim();
                if (title.isEmpty()) {
                    Toast.makeText(EventDetailActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (chosenEndTime[0] <= chosenStartTime[0]) {
                    Toast.makeText(EventDetailActivity.this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                    return;
                }
                Event newEvent = new Event(title, description, chosenStartTime[0], chosenEndTime[0]);
                long id = dbHelper.addEvent(newEvent);
                if (id != -1) {
                    Toast.makeText(EventDetailActivity.this, "Event added", Toast.LENGTH_SHORT).show();
                    scheduleNotification(newEvent, id);
                    loadEvents();
                    clearSelection();
                } else {
                    Toast.makeText(EventDetailActivity.this, "Error adding event", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    @SuppressLint("ScheduleExactAlarm")
    private void scheduleNotification(Event event, long eventId) {
        if (event.getStartTime() > System.currentTimeMillis()) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(this, EventAlarmReceiver.class);
            alarmIntent.putExtra("eventId", eventId);
            alarmIntent.putExtra("eventTitle", event.getTitle());
            alarmIntent.putExtra("eventDescription", event.getDescription());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, (int) eventId, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Log.d(TAG, "Scheduling notification for event " + event.getTitle() +
                    " at " + event.getStartTime());
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, event.getStartTime(), pendingIntent);
        }
    }


    private void showOptionsDialog(final Event event) {
        String[] options = {"Edit", "Delete", "Notify Now"};
        new AlertDialog.Builder(this)
                .setTitle("Choose Action")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditEventDialog(event);
                    } else if (which == 1) {
                        int rows = dbHelper.deleteEvent(event.getId());
                        if (rows > 0) {
                            Toast.makeText(EventDetailActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                            loadEvents();
                            clearSelection();
                        } else {
                            Toast.makeText(EventDetailActivity.this, "Error deleting event", Toast.LENGTH_SHORT).show();
                        }
                    } else if (which == 2) {
                        triggerNotification(event);
                    }
                })
                .show();
    }


    private void showEditEventDialog(final Event event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Event");

        final EditText inputTitle = new EditText(this);
        inputTitle.setHint("Event Title");
        inputTitle.setText(event.getTitle());

        final EditText inputDesc = new EditText(this);
        inputDesc.setHint("Event Description");
        inputDesc.setInputType(InputType.TYPE_CLASS_TEXT);
        inputDesc.setText(event.getDescription());

        final EditText inputStartTime = new EditText(this);
        inputStartTime.setHint("Start Time");
        inputStartTime.setFocusable(false);

        final EditText inputEndTime = new EditText(this);
        inputEndTime.setHint("End Time");
        inputEndTime.setFocusable(false);

        final long baseTime = getStartOfDay(selectedDate);
        final long[] chosenStartTime = {event.getStartTime()};
        final long[] chosenEndTime = {event.getEndTime()};

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        inputStartTime.setText(timeFormat.format(new Date(chosenStartTime[0])));
        inputEndTime.setText(timeFormat.format(new Date(chosenEndTime[0])));

        inputStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(chosenStartTime[0]);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            new TimePickerDialog(EventDetailActivity.this, (TimePicker view, int hourOfDay, int minute1) -> {
                Calendar newStart = Calendar.getInstance();
                newStart.setTimeInMillis(baseTime);
                newStart.set(Calendar.HOUR_OF_DAY, hourOfDay);
                newStart.set(Calendar.MINUTE, minute1);
                chosenStartTime[0] = newStart.getTimeInMillis();
                inputStartTime.setText(timeFormat.format(new Date(chosenStartTime[0])));
            }, hour, minute, false).show();
        });

        inputEndTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(chosenEndTime[0]);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            new TimePickerDialog(EventDetailActivity.this, (TimePicker view, int hourOfDay, int minute1) -> {
                Calendar newEnd = Calendar.getInstance();
                newEnd.setTimeInMillis(baseTime);
                newEnd.set(Calendar.HOUR_OF_DAY, hourOfDay);
                newEnd.set(Calendar.MINUTE, minute1);
                chosenEndTime[0] = newEnd.getTimeInMillis();
                inputEndTime.setText(timeFormat.format(new Date(chosenEndTime[0])));
            }, hour, minute, false).show();
        });

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(inputTitle);
        layout.addView(inputDesc);
        layout.addView(inputStartTime);
        layout.addView(inputEndTime);
        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newTitle = inputTitle.getText().toString().trim();
            String newDesc = inputDesc.getText().toString().trim();
            if (newTitle.isEmpty()) {
                Toast.makeText(EventDetailActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (chosenEndTime[0] <= chosenStartTime[0]) {
                Toast.makeText(EventDetailActivity.this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                return;
            }
            event.setTitle(newTitle);
            event.setDescription(newDesc);
            event.setStartTime(chosenStartTime[0]);
            event.setEndTime(chosenEndTime[0]);
            int rows = dbHelper.updateEvent(event);
            if (rows > 0) {
                Toast.makeText(EventDetailActivity.this, "Event updated", Toast.LENGTH_SHORT).show();
                loadEvents();
                clearSelection();
            } else {
                Toast.makeText(EventDetailActivity.this, "Error updating event", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    private void triggerNotification(Event event) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "event_channel")
                .setSmallIcon(R.drawable.icon) // Replace with your actual drawable icon
                .setContentTitle(event.getTitle())
                .setContentText(event.getDescription())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions

            return;
        }
        notificationManager.notify((int) event.getId(), builder.build());
    }
}
