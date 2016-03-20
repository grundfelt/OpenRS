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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Gustav Grundfelt
 */
public class Hardware {

    static final String key = "nyckelordet"; // The key for 'encrypting' and 'decrypting'.

    public static String encryptString(String str) {
        StringBuffer sb = new StringBuffer(str);

        int lenStr = str.length();
        int lenKey = key.length();

      //
        // For each character in our string, encrypt it...
        for (int i = 0, j = 0; i < lenStr; i++, j++) {
            if (j >= lenKey) {
                j = 0;  // Wrap 'round to beginning of key string.
            }
         //
            // XOR the chars together. Must cast back to char to avoid compile error. 
            //
            sb.setCharAt(i, (char) (str.charAt(i) ^ key.charAt(j)));
        }

        return sb.toString();
    }

    public static String decryptString(String str) {
      //
        // To 'decrypt' the string, simply apply the same technique.
        return encryptString(str);
    }

    //From: http://www.rgagnon.com/javadetails/java-0580.html
    public static String getMotherboardSN() {
        String result = "";
        try {
            File file = File.createTempFile("realhowto", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);

            String vbs
                    = "Set objWMIService = GetObject(\"winmgmts:\\\\.\\root\\cimv2\")\n"
                    + "Set colItems = objWMIService.ExecQuery _ \n"
                    + "   (\"Select * from Win32_BaseBoard\") \n"
                    + "For Each objItem in colItems \n"
                    + "    Wscript.Echo objItem.SerialNumber \n"
                    + "    exit for  ' do the first cpu only! \n"
                    + "Next \n";

            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader input
                    = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                result += line;
            }
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Check number of chars
        result=result.trim();
        int chars = result.length();

        if(chars > 14)
        {
            result = result.substring(0, 13);
        }
        
        while(chars < 14)
        {   
            result = result + "0";
            chars=result.length();
        }
        
        
        return result;
    }

    private static class key {

        public key() {
        }
    }

}
