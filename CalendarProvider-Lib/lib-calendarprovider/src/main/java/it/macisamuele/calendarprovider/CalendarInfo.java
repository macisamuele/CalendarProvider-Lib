package it.macisamuele.calendarprovider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simple interface to communicate with the Google Calendar Provider (into Android).
 * The interface is valid for Android API 14+
 *
 * This interface allows to retrieve information about the calendars, to modify them,
 * to delete them or to insert a local calendar.
 *
 * The interface is tested for almost any cases, in case of bugs please report into the issue section
 * of the GitHub project.
 *
 * I thank you for the usage of this interface, if you want to give me some suggestions (like useful
 * operations not included) please add it into the pull request section of the GitHub project.
 *
 * GitHub project link: https://github.com/macisamuele/CalendarProvider-Lib
 */
public class CalendarInfo implements Comparable<CalendarInfo> {

    /**
     * Columns to extract for the correct filling of the CalendarInfo data structure
     */
    final static String[] COLUMNS = new String[]{
            Calendars._ID, Calendars.ACCOUNT_NAME, Calendars.NAME, Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.OWNER_ACCOUNT, Calendars.CALENDAR_COLOR, Calendars.VISIBLE, Calendars.ACCOUNT_TYPE};
    /**
     * Default order of the results
     */
    static final String ORDER = Calendars.ACCOUNT_NAME + " ASC, " + Calendars.NAME + " ASC";

    protected Long id;
    protected String accountName;
    protected String name;
    protected String displayName;
    protected String ownerAccount;
    protected Integer color;
    protected Boolean visible;
    protected String accountType;

    /**
     * Initialize an empty CalendarInfo object (doesn't contain any default information)
     */
    public CalendarInfo() {
    }

    /**
     * Extract a CalendarInfo object from its representation as ContentValues (value extracted querying the Android Content Provider)
     *
     * @param contentValues information extracted from the content provider
     * @return the CalendarInfo representation of the {@code contentValues}
     * @throws IllegalArgumentException if the {@code contentValues} cannot be parsed to a CalendarInfo object
     */
    public static CalendarInfo fromContentValues(ContentValues contentValues) {
        CalendarInfo calendarInfo = new CalendarInfo();
        try {
            calendarInfo.id = contentValues.getAsLong(Calendars._ID);
            calendarInfo.accountName = contentValues.getAsString(Calendars.ACCOUNT_NAME);
            calendarInfo.name = contentValues.getAsString(Calendars.NAME);
            if (calendarInfo.name == null) {
                calendarInfo.name = calendarInfo.accountName;
            }
            calendarInfo.displayName = contentValues.getAsString(Calendars.CALENDAR_DISPLAY_NAME);
            if (calendarInfo.displayName == null) {
                calendarInfo.displayName = calendarInfo.name;
            }
            calendarInfo.ownerAccount = contentValues.getAsString(Calendars.OWNER_ACCOUNT);
            calendarInfo.color = contentValues.getAsInteger(Calendars.CALENDAR_COLOR);
            calendarInfo.visible = contentValues.getAsInteger(Calendars.VISIBLE) == 1;
            calendarInfo.accountType = contentValues.getAsString(Calendars.ACCOUNT_TYPE);
            return calendarInfo;
        } catch (NullPointerException e) {
            StringBuilder errorString = new StringBuilder();
            StringBuilder missingColumns = new StringBuilder();
            errorString.append("There is NOT all the required parameters in the contentValues\nThe required keys are: ");
            for (String col : COLUMNS) {
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
     * Convert a list of {@code ContentValues} to a list of {@code CalendarInfo}
     *
     * @param contentValuesList collection of {@code ContentValues}
     * @return list of {@code CalendarInfo} or null if there is at least one {@code ContentValues} which not represent a {@code CalendarInfo} object
     */
    public static List<CalendarInfo> contentValuesToList(Collection<ContentValues> contentValuesList) {
        try {
            List<CalendarInfo> calendarInfoList = new ArrayList<>(contentValuesList.size());
            for (ContentValues contentValues : contentValuesList) {
                calendarInfoList.add(fromContentValues(contentValues));
            }
            return calendarInfoList;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Commodity method to generate the correct filter string and arguments list according to the owners and visibility defined
     *
     * @param owners  filter on the owners, null to not apply the filter
     * @param visible filter on the visibility, null to not apply the filter
     * @return a {@code Pair} containing as first field the selection string and as second field the selection arguments
     */
    private static Pair<String, String[]> filterOnOwnersAndVisible(Collection<String> owners, Boolean visible) {
        String selection = null;
        String selectionArgs[] = null;
        if (owners != null && owners.size() == 0) {
            owners = null;
        }
        if (owners != null || visible != null) {
            // selection will look like
            // visible=? AND (accountName=? OR accountName=? OR accountName=? OR accountName=?)
            StringBuilder builder = new StringBuilder();
            List<String> arguments = new ArrayList<>();
            if (visible != null) {
                builder.append(Calendars.VISIBLE).append("=?");
                arguments.add(visible ? "1" : "0");
            }
            if (owners != null) {
                if (visible != null) {
                    builder.append(" AND (");
                }
                for (String owner : owners) {
                    builder.append(Calendars.ACCOUNT_NAME).append("=? OR ");
                    arguments.add(owner);
                }
                builder.setLength(builder.length() - 4);  //4 is the length of " OR "
                if (visible != null) {
                    builder.append(")");
                }
            }
            selection = builder.toString();
            selectionArgs = arguments.toArray(new String[arguments.size()]);
        }
        return Pair.create(selection, selectionArgs);
    }

    /**
     * Commodity method for the building of the ContentValues object starting from the CalendarInfo representation
     *
     * @return ContentValues representation of the CalendarInfo object
     */
    public ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Calendars._ID, id);
        if (accountName != null) {
            contentValues.put(Calendars.ACCOUNT_NAME, accountName);
        }
        contentValues.put(Calendars.NAME, name == null ? accountName : name);
        contentValues.put(Calendars.CALENDAR_DISPLAY_NAME, displayName == null ? name : displayName);
        if (ownerAccount != null) {
            contentValues.put(Calendars.OWNER_ACCOUNT, ownerAccount);
        }
        if (color != null) {
            contentValues.put(Calendars.CALENDAR_COLOR, color);
        }
        if (visible != null) {
            contentValues.put(Calendars.VISIBLE, visible ? 1 : 0);
        }
        if (accountType != null) {
            contentValues.put(Calendars.ACCOUNT_TYPE, accountType);
        }
        return contentValues;
    }

    /**
     * Commodity method for the building of the ContentValues object starting from the CalendarInfo representation.
     * The method will return a ContentValues that will be utilised for a new local calendar
     *
     * @return ContentValues representation of the CalendarInfo object
     */
    private ContentValues toCreateLocalContentValues() {
        final ContentValues contentValues = toContentValues();
        if (contentValues.containsKey(Calendars._ID)) {
            Log.w(getClass().getSimpleName(), "The CalendarInfo to insert shouldn't have id defined, they are automatically removed");
            contentValues.remove(Calendars._ID);
            id = null;
        }
        contentValues.put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        contentValues.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
        return contentValues;
    }

    /**
     * Commodity method for the building of the ContentValues object starting from the CalendarInfo representation.
     * The method will return a ContentValues that will be utilised for the update of the calendar
     *
     * @return ContentValues representation of the CalendarInfo object
     */
    public ContentValues toUpdateContentValues() {
        final ContentValues contentValues = toContentValues();
        if (contentValues.containsKey(Calendars.ACCOUNT_NAME)) {
            contentValues.remove(Calendars.ACCOUNT_NAME);
            contentValues.remove(Calendars.ACCOUNT_TYPE);
            contentValues.remove(Calendars.OWNER_ACCOUNT);
        }
        return contentValues;
    }

    /**
     * Extract the filtered calendars on the provider.
     * The calendars are filtered according to the owners and the visibility.
     * If are defined the owners (not null) the resulting calendars will be owned by one of
     * defined owners, while if is defined visible (not null) the resulting calendars will be
     * the visible or not visible ones.
     *
     * @param context application's context
     * @param owners  filter on the owners, null to not apply the filter
     * @param visible filter on the visibility, null to not apply the filter
     * @return the filtered calendars reachable from the context
     */
    public static List<CalendarInfo> getCalendars(Context context, Collection<String> owners, Boolean visible) {
        final Pair<String, String[]> filter = filterOnOwnersAndVisible(owners, visible);
        final Cursor cursor = context.getContentResolver().query(UriBuilder.getUri(), COLUMNS, filter.first, filter.second, ORDER);
        return contentValuesToList(CursorUtils.cursorToContentValuesList(cursor));
    }

    /**
     * Extract the number of the filtered calendars on the provider.
     * The calendars are filtered according to the owners and the visibility.
     * If are defined the owners (not null) the resulting calendars will be owned by one of
     * defined owners, while if is defined visible (not null) the resulting calendars will be
     * the visible or not visible ones.
     *
     * @param context application's context
     * @param owners  filter on the owners, null to not apply the filter
     * @param visible filter on the visibility, null to not apply the filter
     * @return the number of the filtered calendars reachable from the context
     */
    public static int getCountCalendars(Context context, Collection<String> owners, Boolean visible) {
        final Pair<String, String[]> filter = filterOnOwnersAndVisible(owners, visible);
        final Integer count = CursorUtils.getCount(context, UriBuilder.getUri(), filter.first, filter.second);
        return count == null ? 0 : count;
    }

    /**
     * Extract all the available calendars on the provider
     *
     * @param context application's context
     * @return list of {@code CalendarInfo} reachable from the provider
     */
    public static List<CalendarInfo> getAllCalendars(Context context) {
        return getCalendars(context, null, null);
    }

    /**
     * Extract the number of available calendars on the provider
     *
     * @param context application's context
     * @return the number of calendar reachable from the context
     */
    public static int getCountAllCalendar(Context context) {
        return getCountCalendars(context, null, null);
    }

    /**
     * Extract the complete ContentValues related to the calendar with identifier set to {@code calendarId} stored into the Android Calendar Provider
     *
     * @param context    application's context
     * @param calendarId identifier of the calendar
     * @return the complete information of the calendar, null if there is any calendar with the defined {@code calendarId}
     */
    public static ContentValues getCompleteInformation(Context context, long calendarId) {
        return getInformation(context, calendarId, (String[]) null);
    }

    /**
     * Extract the columns stored into the Android Calendar Provider for the calendar identified from {@code calendarId}
     *
     * @param context    application's context
     * @param calendarId identifier of the calendar
     * @param columns    columns to extract, null to extract all columns
     * @return the columns of the calendar, null if there is any calendar with the defined {@code calendarId}
     */
    public static ContentValues getInformation(Context context, long calendarId, Collection<String> columns) {
        return getInformation(context, calendarId, columns.toArray(new String[columns.size()]));
    }

    /**
     * Extract the columns stored into the Android Calendar Provider for the calendar identified from {@code calendarId}
     *
     * @param context    application's context
     * @param calendarId identifier of the calendar
     * @param columns    columns to extract, null to extract all columns
     * @return the columns of the calendar, null if there is any calendar with the defined {@code calendarId}
     */
    public static ContentValues getInformation(Context context, long calendarId, String[] columns) {
        Cursor cursor = context.getContentResolver().query(UriBuilder.getUri(calendarId), columns, null, null, null);
        try {
            List<ContentValues> cv = CursorUtils.cursorToContentValuesList(cursor);
            return cv.get(0);
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Commodity method to check if the {@code contentValues} has all the required fields to complete successfully the creation
     *
     * @param contentValues ContentValues to insert
     * @return true if the {@code contentValues} has all the needed fields
     */
    private static boolean hasMandatoryCreateField(ContentValues contentValues) {
        return contentValues.containsKey(Calendars.ACCOUNT_NAME) &&
                contentValues.containsKey(Calendars.ACCOUNT_TYPE) &&
                contentValues.containsKey(Calendars.NAME) &&
                contentValues.containsKey(Calendars.CALENDAR_DISPLAY_NAME) &&
                contentValues.containsKey(Calendars.CALENDAR_ACCESS_LEVEL);
    }

    /**
     * Commodity method to check if the {@code contentValues} has all the required fields to complete successfully the update
     *
     * @param contentValues ContentValues to insert
     * @return true if the {@code contentValues} has all the needed fields
     */
    private static boolean hasMandatoryUpdateField(ContentValues contentValues) {
        return !contentValues.containsKey(Calendars.ACCOUNT_NAME) &&
                !contentValues.containsKey(Calendars.ACCOUNT_TYPE) &&
                !contentValues.containsKey(Calendars.OWNER_ACCOUNT);
    }

    /**
     * Create a local calendar starting from the information set into the {@code calendarInfo}.
     * If the calendar is correctly added to the Android Calendar Provider then the {@code calendarInfo}
     * will be updated with the assigned identifier.
     *
     * @param context      application's context
     * @param calendarInfo CalendarInfo representation of the calendar to create
     * @return true if the calendar is successfully insert into the Android Calendar Provider
     */
    public static boolean createLocal(Context context, CalendarInfo calendarInfo) {
        final ContentValues contentValues = calendarInfo.toCreateLocalContentValues();
        if (hasMandatoryCreateField(contentValues)) {
            final Uri.Builder uriBuilder = UriBuilder.getUri().buildUpon();
            uriBuilder.appendQueryParameter(Calendars.ACCOUNT_NAME, calendarInfo.accountName);
            uriBuilder.appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
            uriBuilder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true");
            final Uri uri = context.getContentResolver().insert(uriBuilder.build(), contentValues);
            calendarInfo.id = Long.valueOf(uri.getLastPathSegment());
            return true;
        }
        return false;
    }

    /**
     * Update the calendar passed as parameter into the Android Calendar Provider
     *
     * @param context      application's context
     * @param calendarInfo CalendarInfo representation of the information to update into the provider
     * @return true if the calendar is successfully updated into the Android Calendar Provider
     */
    public static boolean update(Context context, CalendarInfo calendarInfo) {
        return calendarInfo.id != null && update(context, calendarInfo.id, calendarInfo.toUpdateContentValues());
    }

    /**
     * Update the field defined in {@code fieldToUpdate} of the calendar
     *
     * @param context       application's context
     * @param calendarInfo  CalendarInfo representation of the information to update into the provider
     * @param fieldToUpdate field of the calendar to be updated
     * @return true if the calendar is successfully updated into the Android Calendar Provider
     */
    public static boolean update(Context context, CalendarInfo calendarInfo, Collection<String> fieldToUpdate) {
        return calendarInfo.id != null && update(context, calendarInfo.id, calendarInfo.toUpdateContentValues(), fieldToUpdate);
    }

    /**
     * Update the calendar identified by the {@code calendarId}
     * It is suggested to avoid to use this method only if strictly needed, prefer to use {@link #update(android.content.Context, CalendarInfo)}
     *
     * @param context       application's context
     * @param calendarId    identifier of the calendar to update
     * @param contentValues contentValues representation of the calendar
     * @return true if the calendar is successfully updated into the Android Calendar Provider
     */
    public static boolean update(Context context, long calendarId, ContentValues contentValues) {
        return hasMandatoryUpdateField(contentValues) && context.getContentResolver().update(UriBuilder.getUri(calendarId), contentValues, null, null) != 0;
    }

    /**
     * Update the field defined in {@code fieldToUpdate} of the calendar identified by the {@code calendarId}
     * It is suggested to avoid to use this method only if strictly needed, prefer to use {@link #update(android.content.Context, CalendarInfo)}
     *
     * @param context       application's context
     * @param calendarId    identifier of the calendar to update
     * @param contentValues contentValues representation of the calendar
     * @param fieldToUpdate field of the calendar to be updated
     * @return true if the calendar is successfully updated into the Android Calendar Provider
     */
    public static boolean update(Context context, long calendarId, ContentValues contentValues, Collection<String> fieldToUpdate) {
        for (String key : contentValues.keySet()) {
            if (!fieldToUpdate.contains(key)) {
                contentValues.remove(key);
            }
        }
        return hasMandatoryUpdateField(contentValues) && update(context, calendarId, contentValues);
    }

    /**
     * Delete the calendar identified from {@code calendarId}
     *
     * @param context    application's context
     * @param calendarId identifier of the calendar
     * @return true if the calendar is successfully deleted from the Android Calendar Provider
     */
    public static boolean delete(Context context, long calendarId) {
        return context.getContentResolver().delete(UriBuilder.getUri(calendarId), null, null) != 0;
    }

    /**
     * Extract the identifier of the calendar
     *
     * @return identifier of the calendar
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the identifier of the calendar
     *
     * @param id calendar identifier
     * @return this CalendarInfo
     */
    public CalendarInfo setId(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Extract the account name associated with the calendar
     *
     * @return account name associated with the calendar
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Set the account name associated with the calendar
     *
     * @param accountName account name of the calendar
     * @return this CalendarInfo
     */
    public CalendarInfo setAccountName(String accountName) {
        this.accountName = accountName;
        return this;
    }

    /**
     * Extract the name associated of the calendar
     *
     * @return name associated of the calendar
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name associated of the calendar
     *
     * @param name calendar name
     * @return this CalendarInfo
     */
    public CalendarInfo setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Extract the displayed name of the calendar
     *
     * @return displayed name of the calendar
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Set the displayed name of the calendar
     *
     * @param displayName calendar display name
     * @return this CalendarInfo
     */
    public CalendarInfo setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Extract the owner of the calendar
     *
     * @return owner of the calendar
     */
    public String getOwnerAccount() {
        return ownerAccount;
    }

    /**
     * Set the owner of the calendar
     *
     * @param ownerAccount calendar owner
     * @return this CalendarInfo
     */
    public CalendarInfo setOwnerAccount(String ownerAccount) {
        this.ownerAccount = ownerAccount;
        return this;
    }

    /**
     * Extract the calendar color
     *
     * @return calendar color
     */
    public Integer getColor() {
        return color;
    }

    /**
     * Set the calendar color
     *
     * @param color calendar color
     * @return this CalendarInfo
     */
    public CalendarInfo setColor(Integer color) {
        this.color = color;
        return this;
    }

    /**
     * Extract the calendar visibility state
     *
     * @return calendar visibility state
     */
    public Boolean getVisible() {
        return visible;
    }

    /**
     * Set the calendar visibility state
     *
     * @param visible calendar visibility state
     * @return this CalendarInfo
     */
    public CalendarInfo setVisible(Boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Extract the account type of the calendar
     *
     * @return account type of the calendar
     */
    public String getAccountType() {
        return accountType;
    }

    /**
     * Set the account type of the calendar
     *
     * @param accountType calendar account type
     * @return this CalendarInfo
     */
    public CalendarInfo setAccountType(String accountType) {
        this.accountType = accountType;
        return this;
    }

    @Override
    public int compareTo(CalendarInfo another) {
        return id.compareTo(another.id);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CalendarInfo && compareTo((CalendarInfo) o) == 0;
    }

    @Override
    public String toString() {
        return toContentValues().toString();
    }

    /**
     * Utility class for URI definition
     */
    static class UriBuilder {
        static final Uri CALENDAR_URI = Calendars.CONTENT_URI;

        /**
         * @return the URI related to the calendar provider
         */
        static Uri getUri() {
            return CALENDAR_URI;
        }

        /**
         * @param calendarId identifier of the event
         * @return the URI related to the calendar, can be used for extracting information, updating and deletion
         */
        static Uri getUri(long calendarId) {
            return ContentUris.withAppendedId(CALENDAR_URI, calendarId);
        }
    }

}