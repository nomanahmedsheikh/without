package org.utd.cs.gm.utility;

import java.text.DecimalFormat;

/**
 * Created by Happy on 3/6/17.
 */
public class Timer {
    public static String time(double sec)
    {
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(3);
        String s = new String();
        if(sec < 60)
            s += sec + " secs";
        else if(sec < 3600)
        {
            int min = (int) sec/60;
            sec = sec - (int)(sec/60)*60;
            s += min + " mins, " + df.format(sec) + " secs";
        }
        else if(sec < 86400)
        {
            int hrs = (int)sec/3600;
            double mins = (sec - (int)(sec/3600)*3600)/60.0;
            s += hrs + " hrs, " + df.format(mins) + " mins";
        }
        else
        {
            int days = (int)sec/86400;
            double hrs = (sec - (int)(sec/86400)*86400)/3600.0;
            s += days + " days, " + df.format(hrs) + " hrs";
        }
        return s;
    }
}
