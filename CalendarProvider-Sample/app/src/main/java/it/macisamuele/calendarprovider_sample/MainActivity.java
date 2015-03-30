package it.macisamuele.calendarprovider_sample;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        for(CalendarInfo calendarInfo: CalendarInfo.getAllCalendars(this)) {
            Log.d("MainActivity - Calendars", calendarInfo.toString());
        }
        for(EventInfo eventInfo: EventInfo.getAllEvents(this)) {
            Log.d("MainActivity - Events", eventInfo.toString());
        }
    }
}
