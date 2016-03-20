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
 *  @author Gustav Grundfelt
 */
public class SoundBuffer {
        public boolean isPlaying;        //Indication if buffer is playing;
        public boolean isCalculating;    //
        public int n_cars;                    //Number of Cars in the buffer
        public int n_samples;            //Length of Content
        public int position;                       //Sample position of each buffer
        public int offset;                    //Total delay in seconds beetween first and last Car
        float[] signalArrayL=new float[Globals.SIGNAL_DUR*44100];
        float[] signalArrayR=new float[Globals.SIGNAL_DUR*44100];
        public double LAFmax;      //Fast max for the buffer
        public double LAeq;      //Buffer LAeq
  
        
        //Constructor
        SoundBuffer() {
            isPlaying=false;        //Indication if buffer is playing;
            isCalculating=false;    //
            n_cars=0;                    //Number of Cars in the buffer
            n_samples=44100;            //Length of Content
            position=0;                       //Sample position of each buffer
            offset=0;
        }
    
}
