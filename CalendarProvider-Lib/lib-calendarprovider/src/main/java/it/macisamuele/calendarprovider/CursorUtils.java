package it.macisamuele.calendarprovider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for dealing with cursors
 */
class CursorUtils {

    /**
     * Convert a cursor in a list of given object and automatically close the cursor
     *
     * @param cursor cursor to convert into a ContentValues list
     * @return the {@code android.content.ContentValues} list corresponding to the given cursor
     */
    public static List<ContentValues> cursorToContentValuesList(Cursor cursor) {
        return cursorToContentValuesList(cursor, true);
    }

    /**
     * Convert a cursor in a list of given object
     *
     * @param cursor             cursor to convert into a ContentValues list
     * @param automaticallyClose decide if the cursor must be closed from the method
     *                           (useful in order to get rid of Cursor-related memory leakage)
     * @return the {@code android.content.ContentValues} list corresponding to the given cursor
     */
    public static List<ContentValues> cursorToContentValuesList(Cursor cursor, boolean automaticallyClose) {
        try {
            List<ContentValues> resultArray = new ArrayList<>();
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    ContentValues contentValues = new ContentValues();
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        switch (cursor.getType(i)) {
                            case Cursor.FIELD_TYPE_NULL:
                                contentValues.putNull(cursor.getColumnName(i));
                                break;
                            case Cursor.FIELD_TYPE_BLOB:
                                contentValues.put(cursor.getColumnName(i), cursor.getBlob(i));
                                break;
                            case Cursor.FIELD_TYPE_FLOAT:
                                contentValues.put(cursor.getColumnName(i), cursor.getDouble(i));
                                break;
                            case Cursor.FIELD_TYPE_INTEGER:
                                contentValues.put(cursor.getColumnName(i), cursor.getInt(i));
                                break;
                            case Cursor.FIELD_TYPE_STRING:
                                contentValues.put(cursor.getColumnName(i), cursor.getString(i));
                                break;
                        }
                    }
                    resultArray.add(contentValues);
                    cursor.moveToNext();
                }
            }
            return resultArray;
        } finally {
            if (automaticallyClose && cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    /**
     * Get the number of entries in the provider
     *
     * @param context application's context
     * @param uri     uri of the provider
     * @return the count of the entries inside the provider selected (null if some error happens)
     */
    public static Integer getCount(Context context, Uri uri) {
        return getCount(context, uri, null, null);
    }

    /**
     * Get the number of entries in the provider, filtered from the parameters
     *
     * @param context       application's context
     * @param uri           uri of the provider
     * @param selection     A filter declaring which rows to return, formatted as an
     *                      SQL WHERE clause (excluding the WHERE itself). Passing null will
     *                      return all rows for the given URI.
     * @param selectionArgs You may include ?s in selection, which will be
     *                      replaced by the values from selectionArgs, in the order that they
     *                      appear in the selection. The values will be bound as Strings.
     * @return the count of the entries inside the provider selected (null if some error happens)
     */
    public static Integer getCount(Context context, Uri uri, String selection, String selectionArgs[]) {
        Cursor cursor = context.getContentResolver().query(uri, new String[]{"count(*) as cnt"}, selection, selectionArgs, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return null;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

}
