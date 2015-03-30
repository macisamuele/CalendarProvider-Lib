package it.macisamuele.calendarprovider;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Provides conversion functions between Julian Date and "normal" date (Gregorian Date)
 * The Julian Date is a long value that represent a date with the granularity of the day
 * <p/>
 * The following code is published in: http://www.rgagnon.com/javadetails/java-0506.html
 */
class JulianDate {
    /**
     * Returns the Julian day number that begins at midnight of this day, Positive year signifies A.D., negative year B.C.
     * Remember that the year after 1 B.C. was 1 A.D.
     * reference: Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
     */
    // Gregorian Calendar adopted Oct. 15, 1582 (2299161)
    private static int JGREG = 15 + 31 * (10 + 12 * 1582);
    private static double HALFSECOND = 0.5;

    private JulianDate() {
        throw new IllegalStateException(getClass().getCanonicalName() + " cannot be instantiated");
    }

    /**
     * Convert the date passed as parameter in Julian Date value
     *
     * @param date date to convert
     * @return Julian Day value
     */
    public static long toJulian(Date date) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        return toJulian(calendar);
    }

    /**
     * Convert the date passed as parameter in Julian Date value
     *
     * @param calendar calendar that contains the date to convert
     * @return Julian Day value
     */
    public static long toJulian(Calendar calendar) {
        return toJulian(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Convert the date passed with the parameters in Julian Date value
     *
     * @param year  year of the date to convert
     * @param month month of the date to convert (January=1, February=2, ...) => Calendar.get(Calendar.MONTH)+1
     * @param day   day of the date to convert
     * @return Julian Day value
     */
    public static long toJulian(int year, int month, int day) {
        int julianYear = year;
        if (year < 0) julianYear++;
        int julianMonth = month;
        if (month > 2) {
            julianMonth++;
        } else {
            julianYear--;
            julianMonth += 13;
        }
        double julian = (Math.floor(365.25 * julianYear) + Math.floor(30.6001 * julianMonth) + day + 1720995.0);
        if (day + 31 * (month + 12 * year) >= JGREG) {
            // change over to Gregorian calendar
            int ja = (int) (0.01 * julianYear);
            julian += 2 - ja + (0.25 * ja);
        }
        return (long) julian;
    }

    /**
     * Convert the Julian Date passed in Standard Date
     *
     * @param julianDate julian date to convert
     * @return date object corresponding to the day (represented by julianDate) at midnight
     */
    public static Date toDate(long julianDate) {
        int jalpha, ja, jb, jc, jd, je, year, month, day;
        double julian = julianDate + HALFSECOND / 86400.0;
        ja = (int) julian;
        if (ja >= JGREG) {
            jalpha = (int) (((ja - 1867216) - 0.25) / 36524.25);
            ja = ja + 1 + jalpha - jalpha / 4;
        }
        jb = ja + 1524;
        jc = (int) (6680.0 + ((jb - 2439870) - 122.1) / 365.25);
        jd = 365 * jc + jc / 4;
        je = (int) ((jb - jd) / 30.6001);
        day = jb - jd - (int) (30.6001 * je);
        month = je - 1;
        if (month > 12) {
            month = month - 12;
        }
        year = jc - 4715;
        if (month > 2) {
            year--;
        }
        if (year <= 0) {
            year--;
        }
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.set(year, month - 1, day, 0, 0, 0);
        return calendar.getTime();
    }
}

