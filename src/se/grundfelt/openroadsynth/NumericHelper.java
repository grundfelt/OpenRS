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

/**
 *
 * @author Gustav Grundfelt
 */
public class NumericHelper {

    /**
     * Transforms a string to an integer. If no numerical chars exits output
     * will be "0".
     *
     * @param str
     * @return
     */
    static String makeToInteger(String str) {
        String s = str;
        double d;
        d = Double.parseDouble(makeToDouble(s));
        int i = (int) (d + 0.5D);
        String retStr = String.valueOf(i);
        System.out.printf(retStr + "   ");
        return retStr;
    }

    /**
     * Transforms a string to an double. If no numerical chars exits output will
     * be "0".
     *
     * @param str
     * @return
     */
    static String makeToDouble(String str) {

        Boolean dotWasFound = false;
        String orgStr = str;
        String retStr;
        int firstDotPos = 0;
        Boolean negative = false;
        
        //check if str is null
        if(str.length()==0){
            str="0";
        }

        //check if first sign is "-"
        if (str.charAt(0) == '-') {
            negative = true;
        }

        //check if str containg any number or else set the string to '0'
        if (!str.matches(".*\\d+.*")) {
            str = "0";
        }

        //Replace ',' with '.'  (for some european users who use the ',' as decimal separator)
        str = str.replaceAll(",", ".");
        str = str.replaceAll("[^\\d.]", "");

        //Removes the any second dots
        for (int i_char = 0; i_char < str.length(); i_char++) {
            if (str.charAt(i_char) == '.') {
                dotWasFound = true;
                firstDotPos = i_char;
                break;
            }
        }
        if (dotWasFound) {
            String befDot = str.substring(0, firstDotPos + 1);
            String aftDot = str.substring(firstDotPos + 1, str.length());

            aftDot = aftDot.replaceAll("\\.", "");
            //System.out.printf("<"+befDot+":"+aftDot+">");

            str = befDot + aftDot;
        }

        //Removes zeros from the begining
        double uglyMethod = Double.parseDouble(str);
        str = String.valueOf(uglyMethod);

        //Removes the .0
        str = str.replaceAll("([0-9])\\.0+([^0-9]|$)", "$1$2");

        retStr = str;

        if (!orgStr.equals(retStr)) {
            //System.out.printf("[" + orgStr + "->" + retStr + "]");
        }

        if (negative) {
            retStr = "-"+retStr;
        }

        return retStr;

    }

    static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

}
