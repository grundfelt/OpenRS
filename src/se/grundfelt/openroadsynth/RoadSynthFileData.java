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
import java.io.Serializable;

public class RoadSynthFileData implements Serializable {

    public int carsInBuffer0;        //Number of Cars in the buffer
    public int carsInBuffer1;
    public int carsInBuffer2;
    public int carsInBuffer3;
    public int carsInBuffer4;
    public int carsInBuffer5;

    public int contentLength0;       //Length of Content
    public int contentLength1;
    public int contentLength2;
    public int contentLength3;
    public int contentLength4;
    public int contentLength5;

    public int bufferPosition0;      //Sample position of each buffer
    public int bufferPosition1;
    public int bufferPosition2;
    public int bufferPosition3;
    public int bufferPosition4;
    public int bufferPosition5;

    public int bufferOffset0;      //Sample position of each buffer
    public int bufferOffset1;
    public int bufferOffset2;
    public int bufferOffset3;
    public int bufferOffset4;
    public int bufferOffset5;

    public double bufferLAFmax0;      //Fast max for the buffer
    public double bufferLAFmax1;
    public double bufferLAFmax2;
    public double bufferLAFmax3;
    public double bufferLAFmax4;
    public double bufferLAFmax5;

    public double bufferLAeq0;      //Buffer LAeq
    public double bufferLAeq1;
    public double bufferLAeq2;
    public double bufferLAeq3;
    public double bufferLAeq4;
    public double bufferLAeq5;

    public float[] signalArray0_L;
    public float[] signalArray0_R;

    public float[] signalArray1_L;
    public float[] signalArray1_R;

    public float[] signalArray2_L;
    public float[] signalArray2_R;

    public float[] signalArray3_L;
    public float[] signalArray3_R;

    public float[] signalArray4_L;
    public float[] signalArray4_R;

    public float[] signalArray5_L;
    public float[] signalArray5_R;

    //Constructor
    RoadSynthFileData(int len1, int len2, int len3, int len4, int len5, int len6) {
        signalArray0_L = new float[len1];
        signalArray0_R = new float[len1];
        signalArray1_L = new float[len2];
        signalArray1_R = new float[len2];
        signalArray2_L = new float[len3];
        signalArray2_R = new float[len3];
        signalArray3_L = new float[len4];
        signalArray3_R = new float[len4];
        signalArray4_L = new float[len5];
        signalArray4_R = new float[len5];
        signalArray5_L = new float[len6];
        signalArray5_R = new float[len6];

        //fills the arrays with zeros
        for (int i = 0; i < len1; i++) {
            signalArray0_L[i] = 0;
            signalArray0_R[i] = 0;
        }
        for (int i = 0; i < len2; i++) {
            signalArray1_L[i] = 0;
            signalArray1_R[i] = 0;
        }
        for (int i = 0; i < len3; i++) {
            signalArray2_L[i] = 0;
            signalArray2_R[i] = 0;
        }
        for (int i = 0; i < len4; i++) {
            signalArray3_L[i] = 0;
            signalArray3_R[i] = 0;
        }
        for (int i = 0; i < len5; i++) {
            signalArray4_L[i] = 0;
            signalArray4_R[i] = 0;
        }
        for (int i = 0; i < len6; i++) {
            signalArray5_L[i] = 0;
            signalArray5_R[i] = 0;
        }

    }
}
