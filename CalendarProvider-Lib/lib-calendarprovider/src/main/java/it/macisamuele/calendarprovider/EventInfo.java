package it.macisamuele.calendarprovider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Simple interface to communicate with the Google Calendar Provider (into Android).
 * The interface is valid for Android API 14+
 *
 * This interface allows to retrieve information about the events, to modify them,
 * to delete them or to insert a new one.
 *
 * REMARK: in this version is not able to perform any operation with recurrent events
 *
 * The interface is tested for almost any cases, in case of bugs please report into the issue section
 * of the GitHub project.
 *
 * I thank you for the usage of this interface, if you want to give me some suggestions (like useful
 * operations not included) please add it into the pull request section of the GitHub project.
 *
 * GitHub project link: https://github.com/macisamuele/CalendarProvider-Lib
 */
//TODO: manage recurrent events
public class EventInfo implements Comparable<EventInfo> {

    /**
     * Utility factor scale from minute to millisecond
     */
    private static final int MINUTE_IN_MILLISECOND = 60000;

    /**
     * Columns to extract for the correct filling of the EventInfo data structure
     */
    private static final String[] COLUMN = new String[]{
            Instances.EVENT_ID, Instances.CALENDAR_ID, Instances.TITLE, Instances.DESCRIPTION, Instances.START_DAY,
            Instances.START_MINUTE, Instances.END_DAY, Instances.END_MINUTE, Instances.ALL_DAY, Instances.CALENDAR_DISPLAY_NAME,
            Instances.EVENT_LOCATION, Instances.RRULE, Instances.RDATE, Instances.EXRULE, Instances.EXDATE
    };
    /**
     * Default order of the results
     */
    private static final String SELECT_ORDER = Instances.START_DAY + " ASC, " + Instances.START_MINUTE + " ASC, " +
            Instances.END_DAY + " DESC, " + Instances.END_MINUTE + " DESC, " + Instances.TITLE + " ASC";

    protected Long id;
    protected Integer calendarId;
    protected String calendarDisplayName;
    protected String title;
    protected String description;
    protected Date startDate;
    protected Date endDate;
    protected Boolean allDay;
    protected String location;
    protected RecurrenceRule recurrenceRule;

    /**
     * Instantiate an empty EventInfo object
     */
    public EventInfo() {
    }

    /**
     * Extract an EventInfo object from its representation as ContentValues (value extracted querying the Android Content Provider)
     *
     * @param contentValues information extracted from the content provider
     * @return the EventInfo representation of the {@code contentValues}
     * @throws IllegalArgumentException if the {@code contentValues} cannot be parsed to a EventInfo object
     */
    public static EventInfo fromContentValues(ContentValues contentValues) {
        try {
            EventInfo eventInfo = new EventInfo();
            eventInfo.id = contentValues.getAsLong(Instances.EVENT_ID);
            eventInfo.calendarId = contentValues.getAsInteger(Instances.CALENDAR_ID);
            eventInfo.calendarDisplayName = contentValues.getAsString(Instances.CALENDAR_DISPLAY_NAME);
            eventInfo.title = contentValues.getAsString(Instances.TITLE);
            eventInfo.description = contentValues.getAsString(Instances.DESCRIPTION);
            eventInfo.startDate = new Date(JulianDate.toDate(contentValues.getAsLong(Instances.START_DAY)).getTime() + contentValues.getAsInteger(Instances.START_MINUTE) * MINUTE_IN_MILLISECOND);
            eventInfo.endDate = new Date(JulianDate.toDate(contentValues.getAsLong(Instances.END_DAY)).getTime() + contentValues.getAsInteger(Instances.END_MINUTE) * MINUTE_IN_MILLISECOND);
            eventInfo.allDay = contentValues.getAsInteger(Instances.ALL_DAY) == 1;
            eventInfo.location = contentValues.getAsString(Instances.EVENT_LOCATION);
            if (contentValues.getAsString(Instances.RRULE) == null && contentValues.getAsString(Instances.RDATE) == null) {
                eventInfo.recurrenceRule = null;
            } else {
                eventInfo.recurrenceRule = new RecurrenceRule()
                        .setRrule(contentValues.getAsString(Instances.RRULE))
                        .setRdate(contentValues.getAsString(Instances.RDATE))
                        .setExrule(contentValues.getAsString(Instances.EXRULE))
                        .setExdate(contentValues.getAsString(Instances.EXDATE));
            }
            return eventInfo;
        } catch (NullPointerException e) {
            StringBuilder errorString = new StringBuilder();
            StringBuilder missingColumns = new StringBuilder();
            errorString.append("There is NOT all the required parameters in the contentValues\nThe required keys are: ");
            for (String col : COLUMN) {
                errorString.append(col).append(", ");
                if (!contentValues.containsKey(col)) {
                    missingColumns.append(col).append(", ");
                }
            }
            errorString.setLength(errorString.length() - 2);
            if (missingColumns.length() > 0) {
                missingColumns.setLength(missingColumns.length() - 2);
            }
            errorString.append("\n the following columns are missing: ").append(missingColumns);
            throw new IllegalArgumentException(errorString.toString());
        }
    }

    /**
     * Convert a list of {@code ContentValues} to a list of {@code EventInfo}
     *
     * @param contentValuesList collection of {@code ContentValues}
     * @return list of {@code EventInfo} or null if there is at least one {@code ContentValues} which not represent a {@code EventInfo} object
     */
    public static List<EventInfo> contentValuesToList(Collection<ContentValues> contentValuesList) {
        try {
            List<EventInfo> eventInfoList = new ArrayList<>(contentValuesList.size());
            for (ContentValues contentValues : contentValuesList) {
                eventInfoList.add(fromContentValues(contentValues));
            }
            return eventInfoList;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Commodity method to generate the correct filter string and arguments list according to the owners and visibility defined
     *
     * @param idCalendars          calendar IDs to filter
     * @param calendarDisplayNames calendar display name to filter
     * @return a {@code Pair} containing as first field the selection string and as second field the selection arguments
     */
    private static Pair<String, String[]> filterOnCalendarIDAndCalendarDisplayName(Collection<Integer> idCalendars, Collection<String> calendarDisplayNames) {
        String selection = null;
        String selectionArgs[] = null;

        if (idCalendars != null && idCalendars.size() == 0) {
            idCalendars = null;
        }
        if (calendarDisplayNames != null && calendarDisplayNames.size() == 0) {
            calendarDisplayNames = null;
        }

        if (idCalendars != null || calendarDisplayNames != null) {
            // selection will look like
            // calendar_id=? OR calendar_id=? OR calendar_displayName=? OR calendar_displayName=?
            StringBuilder builder = new StringBuilder();
            List<String> arguments = new ArrayList<>();
            if (idCalendars != null) {
                for (Integer idCalendar : idCalendars) {
                    builder.append(Instances.CALENDAR_ID).append("=? OR ");
                    arguments.add(idCalendar + "");
                }
                builder.setLength(builder.length() - 4);
            }
            if (calendarDisplayNames != null) {
                for (String calendarDisplayName : calendarDisplayNames) {
                    builder.append(Instances.CALENDAR_DISPLAY_NAME).append("=? OR ");
                    arguments.add(calendarDisplayName);
                }
                builder.setLength(builder.length() - 4);
            }
            selection = builder.toString();
            selectionArgs = arguments.toArray(new String[arguments.size()]);
        }
        return Pair.create(selection, selectionArgs);
    }

    /**
     * Commodity method for the building of the ContentValues object starting from the EventInfo representation
     *
     * @return ContentValues representation of the EventInfo object
     */
    public ContentValues toContentValues() {
        final ContentValues contentValues = new ContentValues(COLUMN.length);
        if (id != null) {
            contentValues.put(Events._ID, id);
        }
        if (calendarId != null) {
            contentValues.put(Events.CALENDAR_ID, calendarId);
        }
        if (calendarDisplayName != null) {
            contentValues.put(Events.CALENDAR_DISPLAY_NAME, calendarDisplayName);
        }
        if (title != null) {
            contentValues.put(Events.TITLE, title);
        }
        if (description != null) {
            contentValues.put(Events.DESCRIPTION, description);
        }
        if (allDay != null && allDay) {
            contentValues.put(Events.ALL_DAY, 1);
            final Calendar calendar = Calendar.getInstance();
            if (startDate != null) {
                calendar.setTime(startDate);
                calendar.set(Calendar.MILLISECOND, 0);
                if (calendar.get(Calendar.HOUR_OF_DAY) != 0 || calendar.get(Calendar.MINUTE) != 0 || calendar.get(Calendar.SECOND) != 0) {
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                }
                startDate = calendar.getTime();
            }
            if (endDate != null) {
                calendar.setTime(endDate);
                calendar.set(Calendar.MILLISECOND, 0);
                if (calendar.get(Calendar.HOUR_OF_DAY) != 0 || calendar.get(Calendar.MINUTE) != 0 || calendar.get(Calendar.SECOND) != 0) {
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.add(Calendar.DATE, 1);
                }
                endDate = calendar.getTime();
            }
        }
        if (startDate != null) {
            contentValues.put(Events.DTSTART, startDate.getTime());
        }
        if (endDate != null) {
            contentValues.put(Events.DTEND, endDate.getTime());
        }
        if (location != null) {
            contentValues.put(Events.EVENT_LOCATION, location);
        }
        if (recurrenceRule != null) {
            contentValues.putAll(recurrenceRule.toContentValues());
        }
        return contentValues;
    }

    /**
     * Commodity method for the building of the ContentValues object starting from the EventInfo representation.
     * The method will return a ContentValues that will be utilised for the insert of an event
     *
     * @param context application's context
     * @return ContentValues representation of the CalendarInfo object
     */
    private ContentValues toInsertContentValues(Context context) {
        final ContentValues contentValues = toContentValues();
        if (contentValues.containsKey(Events._ID) || contentValues.containsKey(Events.CALENDAR_DISPLAY_NAME)) {
            Log.w(EventInfo.class.getSimpleName(), "TThe EventInfo to insert shouldn't have id and calendarDisplayName defined, they are automatically removed");
            contentValues.remove(Events._ID);
            id = null;
            contentValues.remove(Events.CALENDAR_DISPLAY_NAME);
            calendarDisplayName = null;
        }
        if (isRecurrentEvent()) {
            contentValues.remove(Events.DTEND);
            if (endDate != null) {
                contentValues.put(Events.DURATION, "P" + ((endDate.getTime() - startDate.getTime()) / 1000) + "S");
            }
        }
        if (!contentValues.containsKey(Events.EVENT_TIMEZONE)) {
            String timezone = CalendarInfo.getCompleteInformation(context, calendarId).getAsString(Calendars.CALENDAR_TIME_ZONE);
            if (timezone == null) {
                timezone = TimeZone.getDefault().getDisplayName();
            }
            contentValues.put(Events.EVENT_TIMEZONE, timezone);
        }
        return contentValues;
    }

    /**
     * Select all the filtered events on the provider.
     * The events are filtered according to the time interval defined, to the calendar identifiers and the calendar's display name.
     * The filters will be avoided with a null value for it
     *
     * @param context              application context (needed for the ContentResolver)
     * @param fromDate             lower bound of the time interval
     * @param toDate               upper bound of the time interval
     * @param idCalendars          list of calendar IDs from which extracts events
     * @param calendarDisplayNames list of calendar display name from which extract events
     * @return the list of the events that respects the constraints
     */
    public static List<EventInfo> getEvents(Context context, Date fromDate, Date toDate, Collection<Integer> idCalendars, Collection<String> calendarDisplayNames) {
        final Pair<String, String[]> filter = filterOnCalendarIDAndCalendarDisplayName(idCalendars, calendarDisplayNames);
        Cursor cursor = context.getContentResolver().query(UriBuilder.getUri(fromDate, toDate), COLUMN, filter.first, filter.second, EventInfo.SELECT_ORDER);
        return contentValuesToList(CursorUtils.cursorToContentValuesList(cursor));
    }

    /**
     * Extract the number of the filtered events on the provider.
     * The events are filtered according to the time interval defined, to the calendar identifiers and the calendar's display name.
     * The filters will be avoided with a null value for it
     *
     * @param context              application context (needed for the ContentResolver)
     * @param fromDate             lower bound of the time interval, null to not apply the filter
     * @param toDate               upper bound of the time interval, null to not apply the filter
     * @param idCalendars          list of calendar IDs from which extracts events, null to not apply the filter
     * @param calendarDisplayNames list of calendar display name from which extract events, null to not apply the filter
     * @return the list of the events that respects the constraints
     */
    public static int getCountEvents(Context context, Date fromDate, Date toDate, Collection<Integer> idCalendars, Collection<String> calendarDisplayNames) {
        final Pair<String, String[]> filter = filterOnCalendarIDAndCalendarDisplayName(idCalendars, calendarDisplayNames);
        final Integer count = CursorUtils.getCount(context, UriBuilder.getUri(fromDate, toDate), filter.first, filter.second);
        return count == null ? 0 : count;
    }

    /**
     * Extract all the available events on the provider
     *
     * @param context application context (needed for the ContentResolver)
     * @return list of {@code EventInfo} reachable from the provider
     */
    public static List<EventInfo> getAllEvents(Context context) {
        return getEvents(context, null, null, null, null);
    }

    /**
     * Extract the number of available events on the provider
     *
     * @param context application's context
     * @return the number of events reachable from the context
     */
    public static int getCountAllEvents(Context context) {
        return getCountEvents(context, null, null, null, null);
    }

    /**
     * Extract the complete ContentValues related to the event with identifier set to {@code eventId} stored into the Android Calendar Provider
     *
     * @param context application's context
     * @param eventId identifier of the event
     * @return the complete information of the event, null if there is any event with the defined {@code eventId}
     */
    public static ContentValues getCompleteInformation(Context context, long eventId) {
        return getInformation(context, eventId, (String[]) null);
    }

    /**
     * Extract the columns stored into the Android Calendar Provider for the event identified from {@code eventId}
     *
     * @param context application's context
     * @param eventId identifier of the event
     * @param columns columns to extract, null to extract all columns
     * @return the columns of the event, null if there is any event with the defined {@code eventId}
     */
    public static ContentValues getInformation(Context context, long eventId, Collection<String> columns) {
        return getInformation(context, eventId, columns.toArray(new String[columns.size()]));
    }

    /**
     * Extract the columns stored into the Android Calendar Provider for the event identified from {@code eventId}
     *
     * @param context application's context
     * @param eventId identifier of the event
     * @param columns columns to extract, null to extract all columns
     * @return the columns of the event, null if there is any event with the defined {@code eventId}
     */
    public static ContentValues getInformation(Context context, long eventId, String[] columns) {
        Cursor cursor = context.getContentResolver().query(UriBuilder.getUri(eventId), columns, null, null, null);
        try {
            List<ContentValues> cv = CursorUtils.cursorToContentValuesList(cursor);
            return cv.get(0);
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Commodity method to check if the {@code contentValues} has all the required fields to complete successfully the insert
     *
     * @param contentValues ContentValues to insert
     * @return true if the {@code contentValues} has all the needed fields
     */
    private static boolean hasMandatoryInsertField(ContentValues contentValues) {
        return contentValues.containsKey(Events.DTSTART) &&
                contentValues.containsKey(Events.EVENT_TIMEZONE) &&
                contentValues.containsKey(Events.CALENDAR_ID) &&
                (contentValues.containsKey(Events.DTEND) || (contentValues.containsKey(Events.DURATION) &&
                        (contentValues.containsKey(Events.RRULE) || contentValues.containsKey(Events.RDATE))));
    }

    /**
     * Create a new event starting from the information set into the {@code eventInfo}.
     * If the event is correctly added to the Android Calendar Provider then the {@code eventInfo}
     * will be updated with the assigned identifier and displayed name of the event.
     *
     * @param context   application's context
     * @param eventInfo EventInfo representation of the event to create
     * @return true if the event is successfully insert into the Android Calendar Provider
     */
    public static boolean insert(Context context, EventInfo eventInfo) {
        if (eventInfo.isRecurrentEvent()) {  //TODO: manage insertion of recurrent event
            return false;
        }
        final ContentValues contentValues = eventInfo.toInsertContentValues(context);
        if (hasMandatoryInsertField(contentValues)) {
            final Uri uri = context.getContentResolver().insert(UriBuilder.EVENTS_URI, contentValues);
            eventInfo.id = Long.valueOf(uri.getLastPathSegment());
            eventInfo.calendarDisplayName = getInformation(context, eventInfo.id,
                    new String[]{Calendars.CALENDAR_DISPLAY_NAME}).getAsString(Calendars.CALENDAR_DISPLAY_NAME);
            return true;
        }
        return false;
    }

    /**
     * Update the event passed as parameter into the Android Calendar Provider
     *
     * @param context   application's context
     * @param eventInfo EventInfo representation of the information to update into the provider
     * @return true if the event is successfully updated into the Android Calendar Provider
     */
    public static boolean update(Context context, EventInfo eventInfo) {
        return eventInfo.id != null && update(context, eventInfo.id, eventInfo.toContentValues());
    }

    /**
     * Update the field defined in {@code fieldToUpdate} of the event
     *
     * @param context       application's context
     * @param eventInfo     EventInfo representation of the information to update into the provider
     * @param fieldToUpdate field of the event to be updated
     * @return true if the event is successfully updated into the Android Calendar Provider
     */
    public static boolean update(Context context, EventInfo eventInfo, Collection<String> fieldToUpdate) {
        return eventInfo.id != null && update(context, eventInfo.id, eventInfo.toContentValues(), fieldToUpdate);
    }

    /**
     * Update the event identified by the {@code eventId}
     * It is suggested to avoid to use this method only if strictly needed, prefer to use {@link #update(android.content.Context, EventInfo)}
     *
     * @param context       application's context
     * @param eventId       identifier of the event to update
     * @param contentValues contentValues representation of the event
     * @return true if the event is successfully updated into the Android Calendar Provider
     */
    public static boolean update(Context context, long eventId, ContentValues contentValues) {
        if (contentValues.containsKey(Events.RRULE) || contentValues.containsKey(Events.RDATE) || contentValues.containsKey(Events.EXRULE) || contentValues.containsKey(Events.EXDATE)) {
            return false;
        }
        return context.getContentResolver().update(UriBuilder.getUri(eventId), contentValues, null, null) != 0;
    }

    /**
     * Update the field defined in {@code fieldToUpdate} of the event identified by the {@code eventId}
     * It is suggested to avoid to use this method only if strictly needed, prefer to use {@link #update(android.content.Context, EventInfo)}
     *
     * @param context       application's context
     * @param eventId       identifier of the event to update
     * @param contentValues contentValues representation of the event
     * @param fieldToUpdate field of the event to be updated
     * @return true if the event is successfully updated into the Android Calendar Provider
     */
    public static boolean update(Context context, long eventId, ContentValues contentValues, Collection<String> fieldToUpdate) {
        for (String key : contentValues.keySet()) {
            if (!fieldToUpdate.contains(key)) {
                contentValues.remove(key);
            }
        }
        return update(context, eventId, contentValues);
    }

    /**
     * Delete the event identified from {@code eventId}
     *
     * @param context application's context
     * @param eventId identifier of the event
     * @return true if the event is successfully deleted from the Android Calendar Provider
     */
    public static boolean delete(Context context, long eventId) {
        return context.getContentResolver().delete(UriBuilder.getUri(eventId), null, null) != 0;
    }

    /**
     * Extract the identifier of the event
     *
     * @return identifier of the event
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the identifier of the event
     *
     * @param id event identifier
     * @return this EventInfo
     */
    public EventInfo setId(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Extract the identifier of the associated calendar with the event
     *
     * @return identifier of the associated calendar with the event
     */
    public Integer getCalendarId() {
        return calendarId;
    }

    /**
     * Set the identifier of the associated calendar with the event
     *
     * @param calendarId calendar identifier
     * @return this EventInfo
     */
    public EventInfo setCalendarId(Integer calendarId) {
        this.calendarId = calendarId;
        return this;
    }

    /**
     * Extract the displayed name of the calendar associated with the event
     *
     * @return displayed name of the calendar associated with the event
     */
    public String getCalendarDisplayName() {
        return calendarDisplayName;
    }

    /**
     * Set the displayed name of the calendar associated with the event
     *
     * @param calendarDisplayName calendar displayed name
     * @return this EventInfo
     */
    public EventInfo setCalendarDisplayName(String calendarDisplayName) {
        this.calendarDisplayName = calendarDisplayName;
        return this;
    }

    /**
     * Extract the title of the event
     *
     * @return title of the event
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title of the event
     *
     * @param title event title
     * @return this EventInfo
     */
    public EventInfo setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Extract the description of the event
     *
     * @return description of the event
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of the event
     *
     * @param description event description
     * @return this EventInfo
     */
    public EventInfo setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Extract the starting date of the event
     *
     * @return starting date of the event
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Set the starting date of the event
     *
     * @param startDate start instant
     * @return this EventInfo
     */
    public EventInfo setStartDate(Date startDate) {
        this.startDate = startDate;
        return this;
    }

    /**
     * Extract the ending date of the event
     *
     * @return ending date of the event
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Set the ending date of the event
     *
     * @param endDate end instant
     * @return this EventInfo
     */
    public EventInfo setEndDate(Date endDate) {
        this.endDate = endDate;
        return this;
    }

    /**
     * Extract the all day flag of the event
     *
     * @return all day flag of the event
     */
    public Boolean getAllDay() {
        return allDay;
    }

    /**
     * Do the event spans over all the day?
     *
     * @return true if the event is an all day event
     */
    public boolean isAllDayEvent() {
        return allDay != null && allDay;
    }

    /**
     * Set the all day flag to the event
     *
     * @param allDay all day flag
     * @return this EventInfo
     */
    public EventInfo setAllDay(Boolean allDay) {
        this.allDay = allDay;
        return this;
    }

    /**
     * Extract the location of the event
     *
     * @return location of the event
     */
    public String getLocation() {
        return location;
    }

    /**
     * Set the location of the event
     *
     * @param location event location
     * @return this EventInfo
     */
    public EventInfo setLocation(String location) {
        this.location = location;
        return this;
    }

    /**
     * Extract the recurrence rule of the event
     *
     * @return recurrence rule of the event
     */
    public RecurrenceRule getRecurrenceRule() {
        return recurrenceRule;
    }

    /**
     * Is the event a recurrent event?
     *
     * @return true if the event is a recurrent event
     */
    public boolean isRecurrentEvent() {
        return recurrenceRule != null && recurrenceRule.isWellDefined();
    }

    /**
     * Set the recurrence rule of the event
     *
     * @param recurrenceRule event recurrence rule
     * @return this EventInfo
     */
    public EventInfo setRecurrenceRule(RecurrenceRule recurrenceRule) {
        this.recurrenceRule = recurrenceRule;
        return this;
    }

    @Override
    public int compareTo(EventInfo another) {
        return id.compareTo(another.id);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EventInfo && compareTo((EventInfo) o) == 0;
    }

    @Override
    public String toString() {
        return toContentValues().toString();
    }

    /**
     * Definition of the recurrence rules of an event
     */
    //TODO: this class is just an interface, actually it performs nothing => will be improved adding all the parsing required to manage really the recurrence of the events
    public static class RecurrenceRule {
        protected String rrule;
        protected String rdate;
        protected String exrule;
        protected String exdate;

        /**
         * @return rrule field
         */
        public String getRrule() {
            return rrule;
        }

        /**
         * Set the rrule field
         *
         * @param rrule rrule field
         * @return this RecurrenceRule
         */
        public RecurrenceRule setRrule(String rrule) {
            this.rrule = rrule;
            return this;
        }

        /**
         * @return rdate field
         */
        public String getRdate() {
            return rdate;
        }

        /**
         * Set the rdate field
         *
         * @param rdate rdate field
         * @return this RecurrenceRule
         */
        public RecurrenceRule setRdate(String rdate) {
            this.rdate = rdate;
            return this;
        }

        /**
         * @return exrule field
         */
        public String getExrule() {
            return exrule;
        }

        /**
         * Set the exrule field
         *
         * @param exrule exrule field
         * @return this RecurrenceRule
         */
        public RecurrenceRule setExrule(String exrule) {
            this.exrule = exrule;
            return this;
        }

        /**
         * @return exdate field
         */
        public String getExdate() {
            return exdate;
        }

        /**
         * Set the exdate field
         *
         * @param exdate exdate field
         * @return this RecurrenceRule
         */
        public RecurrenceRule setExdate(String exdate) {
            this.exdate = exdate;
            return this;
        }

        /**
         * Commodity method for the building of the ContentValues object starting from the RecurrenceRule representation
         *
         * @return ContentValues representation of the RecurrenceRule object
         */
        public ContentValues toContentValues() {
            ContentValues contentValues = new ContentValues(4);
            if (rrule != null) {
                contentValues.put(Events.RRULE, rrule);
            }
            if (rdate != null) {
                contentValues.put(Events.RDATE, rdate);
            }
            if (exrule != null) {
                contentValues.put(Events.EXRULE, exrule);
            }
            if (exdate != null) {
                contentValues.put(Events.EXDATE, exdate);
            }
            return contentValues;
        }

        @Override
        public String toString() {
            return toContentValues().toString();
        }

        /**
         * Is the recurrence rule well defined?
         *
         * @return true if the recurrence rule is well defined (the required fields are set)
         */
        public boolean isWellDefined() {
            return rrule != null || rdate != null;
        }
    }

    /**
     * Utility class for URI definition
     */
    private static class UriBuilder {
        static final Uri EVENTS_URI = Events.CONTENT_URI;
        static final Uri INSTANCES_URI = Instances.CONTENT_URI;

        static Uri getUri(long eventId) {
            return ContentUris.withAppendedId(EVENTS_URI, eventId);
        }

        /**
         * @param from_date - first date from which want results, will be considered that date with day granularity, NULL if you don't want any lower bound
         * @param to_date   - last date from which want results, will be considered that date with day granularity, NULL if you don't want any upper bound
         * @return the URI to execute the select to the standard android content provider (Instances table)
         */
        private static Uri getUri(Date from_date, Date to_date) {
            return getUri(from_date != null ? from_date.getTime() : 0, to_date != null ? to_date.getTime() : Long.MAX_VALUE);
        }

        /**
         * @param from_date_time - first date (timestamp) from which want results, will be considered that date with day granularity, NULL if you don't want any lower bound
         * @param to_date_time   - last date (timestamp) from which want results, will be considered that date with day granularity, NULL if you don't want any upper bound
         * @return the URI to execute the select to the standard android content provider (Instances table)
         */
        private static Uri getUri(long from_date_time, long to_date_time) {
            Uri.Builder builder = INSTANCES_URI.buildUpon();
            ContentUris.appendId(builder, from_date_time);
            ContentUris.appendId(builder, to_date_time);
            return builder.build();
        }
    }
}
