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
public class CalibratorSignalBuffer {
    
        boolean isPlaying=false;               //Indication if buffer is playing;
        int n_samples;
        int position;
        int selectedAmplitude=0;
        float[][] signalArray=new float[10][20*Globals.SAMPLING_RATE];
        
        //Constructor
        CalibratorSignalBuffer() {
            isPlaying=false;        //Indication if buffer is playing;
            n_samples=20*Globals.SAMPLING_RATE;            //Length of Content
            position=0;                       //Sample position of each buffer
        }
    
}
