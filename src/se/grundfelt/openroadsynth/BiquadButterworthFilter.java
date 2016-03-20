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
 * Implements a 2nd order ButterWorthFilter
 * 
 */
public class BiquadButterworthFilter {

    private double n0, n1, n2, d0, d1, d2;

    BiquadButterworthFilter(int samplerate, int cutoff, String type) {

        double PI = Math.PI;
        double sqrt2 = Math.sqrt(2);

        double omegaDc = (2d * PI * (double)cutoff);
        double Ts = 1d / (double)samplerate; // Find cutoff frequency in [0..PI]
        double c = 1d / Math.tan(0.5 * omegaDc * Ts); // Warp cutoff frequency

        switch (type) {
            case "lp":
                this.d0 = Math.pow(c, 2) + sqrt2 * c + 1;
                this.d1 = -2D * (Math.pow(c, 2) - 1D);
                this.d2 = Math.pow(c, 2) - sqrt2 * c + 1;
                this.n0 = 1D; //n0
                this.n1 = 2D;
                this.n2 = 1D;
                break;
            case "hp":
                this.d0 = Math.pow(c, 2) + sqrt2 * c + 1;
                this.d1 = -2D * (Math.pow(c, 2) - 1D);
                this.d2 = Math.pow(c, 2) - sqrt2 * c + 1;
                this.n0 = Math.pow(c, 2); //n0
                this.n1 = -2D *Math.pow(c, 2);
                this.n2 = Math.pow(c, 2);
                break;
        }

    }

    /**
     * Filters a float[] array signal.
     * 
     * @param x_signal signal to be filtered
     * @return 
     */
    public float[] apply(float[] x_signal) {
        int n_samples = x_signal.length;
        float[] y_signal = new float[n_samples];
        double[] xv = new double[3];
        double[] yv = new double[3];

        for (int i_sample = 0; i_sample < n_samples; i_sample++) {
            xv[2] = xv[1];
            xv[1] = xv[0];
            xv[0] = (double) x_signal[i_sample];
            yv[2] = yv[1];
            yv[1] = yv[0];

            yv[0] = 1D / this.d0 * (this.n0 * xv[0] + this.n1 * xv[1] + this.n2 * xv[2] - this.d1 * yv[1] - this.d2 * yv[2]);

            y_signal[i_sample] = (float) yv[0];
        }
        return y_signal;
    }
}
