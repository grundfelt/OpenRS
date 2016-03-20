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
public class FilterTimeWeighting {

    private int nBands = 29; //number of bands
    private float initValue;

    FilterTimeWeighting() {
        initValue=0;
    }

    /**
     * Apply the LAmax RC-filter on the signal. Valid for fsamp=44100 Hz.
     * Returns the signal in Volts after applying filter.
     * 
     * //y(n)=a*y(n-1)+(1-a)*x(n)
     *
     * Do not forget to log the output of this function in order to get dB
     * 
     * Source: http://support.01db-metravib.com/uploads/files/gb_Acoustic%20time%20weightings.pdf
     *
     * @param signalIn
     * @return
     */
    public float[] applyLmaxFilter(float[] signal) {

        int n_samples = signal.length;

        float[] x_signal = new float[n_samples];
        float[] y_signal = new float[n_samples];
        float[] ret_signal = new float[n_samples];


        //Square the signal
        for (int i_sample = 0; i_sample < n_samples; i_sample++) {
            x_signal[i_sample] = signal[i_sample] * signal[i_sample];
        }

        //Exponential moving average parameters
        float tau=0.125f;
        float fs=44100f;
        float alpha=tau/(1f/fs+tau);
   
        //for the first sample 
        y_signal[0] = initValue*alpha+(1-alpha)*x_signal[0];
        
        //for the rest of the samples
        for (int i_sample = 1; i_sample < n_samples; i_sample++) {
            y_signal[i_sample] = y_signal[i_sample-1]*alpha+(1-alpha)*x_signal[i_sample];
        }
        
        //remember last sample for next time step;
        initValue=y_signal[n_samples-1];

        //Square root
        for (int i_sample = 0; i_sample < n_samples; i_sample++) {
            ret_signal[i_sample] = (float) Math.sqrt(y_signal[i_sample]);
        } 

        return ret_signal;
    }

}
