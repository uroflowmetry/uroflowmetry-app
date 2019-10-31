package com.uroflowmetry.library.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateTimeUtils {

    public static String FMT_FULL = "yyyy-MM-dd HH:mm:ss";
    public static String FMT_TIMESTAMP = "yyyy-MM-dd HH:mm:ss.SSSSSS";

    public static String convertFormat(String nowFmt, String newFmt, String datetime) {
        SimpleDateFormat srcDf = new SimpleDateFormat(nowFmt);
        try {
            Date date = srcDf.parse(datetime);
            SimpleDateFormat destDf = new SimpleDateFormat(newFmt);
            String mydate = destDf.format(date);
            return mydate;
        } catch (ParseException e) {
            e.printStackTrace();
            return datetime;
        }
    }

    public static String getTimestamp(){
        return getDateString(FMT_TIMESTAMP);
    }

    public static String getDateString(String df) {
        SimpleDateFormat sdf = new SimpleDateFormat(df);
        return sdf.format(new Date());
    }

    public static Calendar stringToCalendar(String str, String strFormt){
        Calendar cal = Calendar.getInstance();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(strFormt);
            cal.setTime(sdf.parse(str));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return cal;
    }

    public static long getDifferentMilliSec(String startTime, String endTime){
        Calendar start = stringToCalendar(startTime, FMT_TIMESTAMP);
        Calendar end = stringToCalendar(endTime, FMT_TIMESTAMP);
        Date startDate = start.getTime();
        Date endDate = end.getTime();
        long startTimeInMilliSec = startDate.getTime();
        long endTimeInMilliSec = endDate.getTime();
        long diffTime = endTimeInMilliSec - startTimeInMilliSec;
        return diffTime;
    }
}
