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
public class FilterFrequencyWeighting {

    //SOS MATRIX..SEE http://www.mathworks.se/help/signal/ref/sosfilt.html
    private double[][] SOS;

    private double[] G;

    //Constructor
    FilterFrequencyWeighting(char type) {
        initSOS(type);
    }

    float[] apply(float[] x_signal) {

        int n_samples = x_signal.length;

        double[] y_signal = new double[n_samples];
        float[] ret_signal = new float[n_samples];

        //double bandwith=1;
        //if(compensate_bandwith) bandwith=this.BANDWITH; 
        float short_scaler = (float) Math.pow(2d, 15d);

        //convering to double
        for (int i_sample = 0; i_sample < n_samples; i_sample++) {
            y_signal[i_sample] = ((float) x_signal[i_sample]) / short_scaler;
        }

        //System.arraycopy(x_signal,0,y_signal,0,n_samples);
        for (int i_filter = 0; i_filter < 3; i_filter++) {
            double x0 = 0, x1 = 0, x2 = 0;
            double y0 = 0, y1 = 0, y2 = 0;
            //Taking the filter parameters from the SOS matrix.
            double b0 = SOS[i_filter][0];
            double b1 = SOS[i_filter][1];
            double b2 = SOS[i_filter][2];
            double a0 = SOS[i_filter][3];
            double a1 = SOS[i_filter][4];
            double a2 = SOS[i_filter][5];//*/

            for (int i_sample = 0; i_sample < n_samples; i_sample++) {
                x2 = x1;
                x1 = x0;

                x0 = G[i_filter] * y_signal[i_sample];

                //x0 = 0.02*y_signal[i_sample];
                y2 = y1;
                y1 = y0;
                y0 = (b0 * x0 + b1 * x1 + b2 * x2
                        - a1 * y1
                        - a2 * y2);

                y_signal[i_sample] = y0;
                //System.out.printf("y_signal[i_sample]=%d -- x_signal[i_sample]=%d\n",(int)y_signal[i_sample],(int)x_signal[i_sample]);
            }

        }
        //convering to short

        for (int i_sample = 0; i_sample < n_samples; i_sample++) {
            ret_signal[i_sample] = (float) (y_signal[i_sample] * short_scaler);
        }

        return ret_signal;
    }

    private void initSOS(char type) {
        switch (type) {
            case 'a':
            case 'A': { //Class 1 A-filter valid for 44100
                double[][] SOStmp
                        = {{1d, 2d, 1d, 1d, -0.140536082420711, 0.0049375976155402},
                        {1d, -2d, 1d, 1d, -1.88490121742879, 0.886421471816167},
                        {1d, -2d, 1d, 1d, -1.99413888126633, 0.994147469444531}};
                SOS = SOStmp;
                double[] Gtmp = {0.255741125204258, 1d, 1d, 1d};
                G = Gtmp;
                break;
            }
            case 'c':
            case 'C': { //Class 2 C-filter valid for 44100
                double[][] SOStmp
                        = {{1d, 2d, 1d, 1d, 0.310944870989022, 0.024171678198595},
                        {1d, -2d, 1d, 1d, -1.9907765091159, 0.990797777311926}};
                SOS = SOStmp;
                double[] Gtmp = {0.334617784562102, 1d, 1d};
                G = Gtmp;
                break;
            }
        }
    }
}
