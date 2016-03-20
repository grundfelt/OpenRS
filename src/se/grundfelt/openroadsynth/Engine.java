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

import java.util.Calendar;
import java.util.Random;

/**
 *
 * @author Gustav Grundfelt
 */
public class Engine {

    private double pitch;         //engine pitch
    private double gain;
    private double dieselFreqShift;
    private double gasolineFreqShift;
    private boolean isActive;
    private int type;

    public double[] gfex = new double[11];
    public double[] fex = new double[11];
    public double[] gfen = new double[19];
    public double[] fen = new double[19];

    //constructor
    Engine(int sel_type) {
        Random randdbl = new Random();
        double tmp_rand1 = randdbl.nextDouble();
        double tmp_rand2 = randdbl.nextDouble();
        double tmp_rand3 = randdbl.nextDouble();

        this.isActive = true;

        this.pitch = 1d + 0.8 * tmp_rand1;// randomizes a pitch rpm between 1-1.8
        this.gain = 16 + 10d * tmp_rand2; // randomizes a gain between 3 and 10 dB

        switch (sel_type) {
            case 0: {
                this.dieselFreqShift = 0d;
                this.gasolineFreqShift = 20d;
                break;
            }
            case 1: {
                this.isActive = false;
            }
            
            
        }

        gfex[0] = 53d;
        gfex[1] = 43d;
        gfex[2] = 44d;
        gfex[3] = 43d;
        gfex[4] = 39d;
        gfex[5] = 27d;
        gfex[6] = 24d;
        gfex[7] = 33d;
        gfex[8] = 35d;
        gfex[9] = 36d;
        gfex[10] = 27d;

        fex[0] = 73d;
        fex[1] = 146d;
        fex[2] = 219d;
        fex[3] = 292d;
        fex[4] = 365d;
        fex[5] = 438d;
        fex[6] = 511d;
        fex[7] = 584d;
        fex[8] = 657d;
        fex[9] = 730d;
        fex[10] = 803d;

        gfen[0] = 45d;
        gfen[1] = 40d;
        gfen[2] = 29d;
        gfen[3] = 29d;
        gfen[4] = 21d;
        gfen[5] = 21d;
        gfen[6] = 28d;
        gfen[7] = 28d;
        gfen[8] = 29d;
        gfen[9] = 29d;
        gfen[10] = 32d;
        gfen[11] = 32d;
        gfen[12] = 25d;
        gfen[13] = 25d;
        gfen[14] = 31d;
        gfen[15] = 31d;
        gfen[16] = 27d;
        gfen[17] = 27d;
        gfen[18] = 21d;

        fen[0] = 36.5d;
        fen[1] = 73d;
        fen[2] = 109.5;
        fen[3] = 146d;
        fen[4] = 182.5;
        fen[5] = 219d;
        fen[6] = 255.5;
        fen[7] = 292d;
        fen[8] = 328.5;
        fen[9] = 365d;
        fen[10] = 401.5;
        fen[11] = 438d;
        fen[12] = 474.5;
        fen[13] = 511d;
        fen[14] = 547.5;
        fen[15] = 584d;
        fen[16] = 620.5;
        fen[17] = 657d;
        fen[18] = 693.5;

        //Calculate the engine gains from dB tp Pascal
        int j;
        for (j = 0; j < gfex.length; j++) {
            this.gfex[j] = Math.pow(10, (this.gain + this.gfex[j]) / 20);

        }
        for (j = 0; j < gfen.length; j++) {
            this.gfen[j] = Math.pow(10, (this.gain + this.gfen[j]) / 20);

        }//*/

    }
    
    /** gets the active status
     * @return  **/
    public boolean isActive(){
        return this.isActive;
    }
    
    /** gets the engine pitch
     * this.pitch = 1 gives an rpm 2220 (36,5 Hz)
     * @return  **/
    public double getPitch(){
        return this.pitch;
    }

    

}
