/* OpenRoadSynth - The free road noise synthisizer
 Copyright (C) 2016  Gustav Grundfelt

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package se.grundfelt.openroadsynth;

import java.awt.Color;

/**
 * A class for all the constants
 *  @author Gustav Grundfelt
 */
public class Globals {
    
    public final static int SAMPLING_RATE=44100;
    public final static String VERSION = "0.9.5";
    public static Color TABLE_TEXT_COLOR=Color.BLACK;//new Color(255,255,255);
    public static Color TEXT_COLOR=Color.BLACK;//new Color(255,255,255);
    public static Color PANEL_COLOR=new Color(214,217,223);//new Color(153,153,153);
    public final static int SIGNAL_DUR = 60;                              // duration of each loop in sec
    public final static int PCN_DUR = SIGNAL_DUR;
    public static double DYN_RANGE_LIMIT_LOW=10;
    public static float DYN_RANGE_GAIN=(float)Math.pow(10d, -10d / 20d);
    public static int EXP_YEAR=0;
    public static int EXP_MONTH=0;
    public static String LIC_CODE="";
    
}
