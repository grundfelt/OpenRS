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

import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * One third octave band filter valid for signals with fsamp=44100 Hz
 * 
 *
 * @author Gustav Grundfelt
 */
public class FilterOneThirdOctaveBand {

    //sosMatrix: http://www.mathworks.se/help/signal/ref/sosfilt.html
    //filtering algoritm: http://www.mathworks.se/help/matlab/ref/filter.html#f83-1015962
    private double[] G;

    double[] FREQ_CENTER = {25d, 32d, 40d, 50d, 63d, 80d, 100d, 125d, 160d, 200d, 250d, 315d, 400d, 500d, 630d, 800d, 1000d, 1250d, 1600d, 2000d, 2500d, 3150d, 4000d, 5000d, 6300d, 8000d, 10000d, 12500d, 16000d, 20000d};
    private int nBands = 29;

    private double[][] yArray = new double[nBands][3];
    private double[][] xArray = new double[nBands][3];

    //CONSTRUCTOR
    FilterOneThirdOctaveBand() {

    }

    public void resetFilter() {
        for (int i = 0; i < nBands; i++) {
            for (int j = 0; j < 3; j++) {
                yArray[i][j] = 0;
                xArray[i][j] = 0;
            }
        }
    }

    /**
     * Apply the second order filters to a signal float array
     *
     * @param x_signal
     * @param bandNr
     * @param compensateBandwith
     * @return
     */
    public float[] applyBandPassFilter(float[] x_signal, int bandNr, boolean compensateBandwith) {
        int n_samples = x_signal.length;

        double[] y_signal = new double[n_samples];
        float[] ret_signal = new float[n_samples];

        //double bandwith=1;
        //if(compensateBandwith) bandwith=this.BANDWITH; 
        float short_scaler = (float) Math.pow(2d, 15d);

        //convering to double
        for (int i_sample = 0; i_sample < n_samples; i_sample++) {
            y_signal[i_sample] = ((float) x_signal[i_sample]) / short_scaler;
        }

        //Get the SOS-matrix and Gain for the band
        double[][] sosMatrix = getSOSmatrix(bandNr);
        double[] sosGain = getSOSgain(bandNr);

        //System.arraycopy(x_signal,0,y_signal,0,n_samples);
        for (int i_filter = 0; i_filter < 4; i_filter++) {
            double x0 = 0, x1 = 0, x2 = 0;
            double y0 = 0, y1 = 0, y2 = 0;

            //Taking the filter parameters from the SOS matrix.
            double b0 = sosMatrix[i_filter][0];
            double b1 = sosMatrix[i_filter][1];
            double b2 = sosMatrix[i_filter][2];
            double a0 = sosMatrix[i_filter][3];
            double a1 = sosMatrix[i_filter][4];
            double a2 = sosMatrix[i_filter][5];//*/

            for (int i_sample = 0; i_sample < n_samples; i_sample++) {
                x2 = x1;
                x1 = x0;
                x0 = sosGain[i_filter] * y_signal[i_sample];
                y2 = y1;
                y1 = y0;
                y0 = (b0 * x0 + b1 * x1 + b2 * x2
                        - a1 * y1
                        - a2 * y2);

                y_signal[i_sample] = y0;
                //System.out.printf("y_signal[i_sample]=%d -- x_signal[i_sample]=%d\n",(int)y_signal[i_sample],(int)x_signal[i_sample]);
            }

        }

        double bandwidthGain = 0.5 * Math.pow(10d, (nBands - bandNr - 5) / 20d);

        //convering to short
        if (compensateBandwith) {
            for (int i_sample = 0; i_sample < n_samples; i_sample++) {
                ret_signal[i_sample] = (float) (y_signal[i_sample] * short_scaler * bandwidthGain);
            }
        } else {
            for (int i_sample = 0; i_sample < n_samples; i_sample++) {
                ret_signal[i_sample] = (float) (y_signal[i_sample] * short_scaler);
            }
        }
        return ret_signal;
    }

    private double[] getSOSgain(int n_band) {

        double[] G = {0d, 0d, 0d, 0d, 0d};
        switch (n_band) {

            case 1 - 1: { //25 Hz
                double[] Gtmp = {0.000412873803357, 0.000412873803357, 0.000412781593967, 0.000412781593967, 1d};
                G = Gtmp;
                break;
            }
            case 2 - 1: { //31.5 Hz
                double[] Gtmp = {0.000519756054748, 0.000519756054748, 0.000519609943325, 0.000519609943325, 1d};
                G = Gtmp;
                break;
            }
            case 3 - 1: { //40 Hz
                double[] Gtmp = {0.000654300398642, 0.000654300398642, 0.000654068888700, 0.000654068888700, 1d};
                G = Gtmp;
                break;
            }
            case 4 - 1: { //50 Hz
                double[] Gtmp = {0.000823661979661, 0.000823661979661, 0.000823295182909, 0.000823295182909, 1d};
                G = Gtmp;
                break;
            }
            case 5 - 1: { //63 Hz
                double[] Gtmp = {0.001036844337790, 0.001036844337790, 0.001036263246976, 0.001036263246976, 1d};
                G = Gtmp;
                break;
            }
            case 6 - 1: { //80 Hz
                double[] Gtmp = {0.001305175518318, 0.001305175518318, 0.001304255035710, 0.001304255035710, 1d};
                G = Gtmp;
                break;
            }
            case 7 - 1: { //100 Hz
                double[] Gtmp = {0.001642906005235, 0.001642906005235, 0.001641448104093, 0.001641448104093, 1d};
                G = Gtmp;
                break;
            }
            case 8 - 1: { //125 Hz
                double[] Gtmp = {0.002067959172850, 0.002067959172850, 0.002065650479725, 0.002065650479725, 1d};
                G = Gtmp;
                break;
            }
            case 9 - 1: { //160 Hz
                double[] Gtmp = {0.002602872401991, 0.002602872401991, 0.002599217205002, 0.002599217205002, 1d};
                G = Gtmp;
                break;
            }
            case 10 - 1: { //200 Hz
                double[] Gtmp = {0.003275976100557, 0.003275976100557, 0.003270190643597, 0.003270190643597, 1d};
                G = Gtmp;
                break;
            }
            case 11 - 1: { //315 Hz
                double[] Gtmp = {0.004122868860256, 0.004122868860256, 0.004113714742306, 0.004113714742306, 1d};
                G = Gtmp;
                break;
            }
            case 12 - 1: { //400 Hz
                double[] Gtmp = {0.005188260098519, 0.005188260098519, 0.005173782078541, 0.005173782078541, 1d};
                G = Gtmp;
                break;
            }
            case 13 - 1: { //500 Hz
                double[] Gtmp = {0.006528266912773, 0.006528266912773, 0.006505381023576, 0.006505381023576, 1d};
                G = Gtmp;
                break;
            }
            case 14 - 1: { //630 Hz
                double[] Gtmp = {0.008213269444507, 0.008213269444507, 0.008177117448956, 0.008177117448956, 1d};
                G = Gtmp;
                break;
            }
            case 15 - 1: { //800 Hz
                double[] Gtmp = {0.010331448357445, 0.010331448357445, 0.010274388947512, 0.010274388947512, 1d};
                G = Gtmp;
                break;
            }
            case 16 - 1: { //1000 Hz
                double[] Gtmp = {0.012993147931897, 0.012993147931897, 0.012903186075665, 0.012903186075665, 1d};
                G = Gtmp;
                break;
            }
            case 17 - 1: { //1250 Hz
                double[] Gtmp = {0.016336226446585, 0.016336226446585, 0.016194579380661, 0.016194579380661, 1d};
                G = Gtmp;
                break;
            }
            case 18 - 1: { //1600 Hz
                double[] Gtmp = {0.020532567717026, 0.020532567717026, 0.020309915358601, 0.020309915358601, 1d};
                G = Gtmp;
                break;
            }
            case 19 - 1: { //2000 Hz
                double[] Gtmp = {0.025795926591436, 0.025795926591436, 0.025446678787579, 0.025446678787579, 1d};
                G = Gtmp;
                break;
            }
            case 20 - 1: { //2500 Hz
                double[] Gtmp = {0.032391254914464, 0.032391254914464, 0.031844870762748, 0.031844870762748, 1d};
                G = Gtmp;
                break;
            }
            case 21 - 1: { //3150 Hz
                double[] Gtmp = {0.0406455842044078, 0.0406455842044078, 0.0397935890797608, 0.0397935890797608, 1d};
                G = Gtmp;
                break;
            }
            case 22 - 1: { //4000 Hz
                double[] Gtmp = {0.050960399107565, 0.050960399107565, 0.049637274420367, 0.049637274420367, 1d};
                G = Gtmp;
                break;
            }
            case 23 - 1: { //5000 Hz
                double[] Gtmp = {0.063825182802070, 0.063825182802070, 0.061780814983462, 0.061780814983462, 1d};
                G = Gtmp;
                break;
            }
            case 24 - 1: { //6300 Hz
                double[] Gtmp = {0.079831404159211, 0.079831404159211, 0.076692438717978, 0.076692438717978, 1d};
                G = Gtmp;
                break;
            }
            case 25 - 1: { //8000 Hz
                double[] Gtmp = {0.099685601796990, 0.099685601796990, 0.094903200749767, 0.094903200749767, 1d};
                G = Gtmp;
                break;
            }
            case 26 - 1: { //10000 Hz
                double[] Gtmp = {0.124219395365960, 0.124219395365960, 0.117002161297607, 0.117002161297607, 1d};
                G = Gtmp;
                break;
            }
            case 27 - 1: { //12500 Hz
                double[] Gtmp = {0.154393331815538, 0.154393331815538, 0.143627509403026, 0.143627509403026, 1d};
                G = Gtmp;
                break;
            }
            case 28 - 1: { //16000 Hz
                double[] Gtmp = {0.191290842501608, 0.191290842501608, 0.175456640286265, 0.175456640286265, 1d};
                G = Gtmp;
                break;
            }
            case 29 - 1: { //20000 Hz
                double[] Gtmp = {0.236099156564906, 0.236099156564906, 0.213203584998636, 0.213203584998636, 1d};
                G = Gtmp;
                break;
            }
        }
        return G;
    }

    /**
     * Initiate second-order section digital filter parameters for every one
     * third octave band. The values are valid for signals with fsamp=44100 Hz.
     *
     * @param n_band
     */
    private double[][] getSOSmatrix(int n_band) {

        double[][] SOS = new double[4][6];

        switch (n_band) {
            case 1 - 1: { //25 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.99963463125335, 0.999650476862053},
                        {1d, 0d, -1d, 1d, -1.99970717493653, 0.999717524451329},
                        {1d, 0d, -1d, 1d, -1.99918947596733, 0.999203468009659},
                        {1d, 0d, -1d, 1d, -1.99925937609849, 0.999271091289235}};
                SOS = SOStmp;

                break;
            }
            case 2 - 1: { //31.5 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.99953488435599, 0.999559996903546},
                        {1d, 0d, -1d, 1d, -1.999627994684, 0.999644396845669},
                        {1d, 0d, -1d, 1d, -1.99897514577177, 0.998997318979166},
                        {1d, 0d, -1d, 1d, -1.99906388895745, 0.999082454906502}};
                SOS = SOStmp;

                break;
            }
            case 3 - 1: { //40 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.9994063031649, 0.999446101692079},
                        {1d, 0d, -1d, 1d, -1.99952634730477, 0.999552341624749},
                        {1d, 0d, -1d, 1d, -1.99870272393019, 0.99873786141229},
                        {1d, 0d, -1d, 1d, -1.99881559648915, 0.998845018101141}};
                SOS = SOStmp;

                break;
            }
            case 4 - 1: { //50 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.99923966219729, 0.999302733929199},
                        {1d, 0d, -1d, 1d, -1.99939526791581, 0.99943646373587},
                        {1d, 0d, -1d, 1d, -1.99835563444887, 0.998411313870214},
                        {1d, 0d, -1d, 1d, -1.998499564937, 0.998546188560923}};
                SOS = SOStmp;

                break;
            }
            case 5 - 1: { //63 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.99902232210452, 0.999122274882323},
                        {1d, 0d, -1d, 1d, -1.99922531440098, 0.99929060033537},
                        {1d, 0d, -1d, 1d, -1.99791214780116, 0.998000375456704},
                        {1d, 0d, -1d, 1d, -1.99809622314032, 0.998170102443921}};
                SOS = SOStmp;

                break;
            }
            case 6 - 1: { //80 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.99873674152365, 0.998895137264422},
                        {1d, 0d, -1d, 1d, -1.99900353785804, 0.999106999233341},
                        {1d, 0d, -1d, 1d, -1.99734348047712, 0.997483275153744},
                        {1d, 0d, -1d, 1d, -1.99757977610785, 0.997696838747295}};
                SOS = SOStmp;

                break;
            }
            case 7 - 1: { //100 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.99835826059378, 0.998609263082313},
                        {1d, 0d, -1d, 1d, -1.99871195050986, 0.998875905978711},
                        {1d, 0d, -1d, 1d, -1.99661117923754, 0.996832665191486},
                        {1d, 0d, -1d, 1d, -1.99691587735011, 0.997101352883885}};
                SOS = SOStmp;

                break;
            }
            case 8 - 1: { //125 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.99785175351061, 0.998249489200278},
                        {1d, 0d, -1d, 1d, -1.99832523788846, 0.998585049924046},
                        {1d, 0d, -1d, 1d, -1.99566331743591, 0.996014201490738},
                        {1d, 0d, -1d, 1d, -1.99605833465218, 0.996352180772192}};
                SOS = SOStmp;

                break;
            }
            case 9 - 1: { //160 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.99716653923615, 0.997796752846239},
                        {1d, 0d, -1d, 1d, -1.99780730502965, 0.998218998779618},
                        {1d, 0d, -1d, 1d, -1.99442895792009, 0.994984775777281},
                        {1d, 0d, -1d, 1d, -1.99494433158748, 0.995409820076007}};
                SOS = SOStmp;

                break;
            }
            case 10 - 1: { //200 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.99622859323929, 0.997227099236846},
                        {1d, 0d, -1d, 1d, -1.99710602022742, 0.99775834740838},
                        {1d, 0d, -1d, 1d, -1.99281000780459, 0.993690325383978},
                        {1d, 0d, -1d, 1d, -1.9934874118398, 0.994224707149738}};
                SOS = SOStmp;

                break;
            }
            case 11 - 1: { //315 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.99492856051177, 0.996510441499026},
                        {1d, 0d, -1d, 1d, -1.99614516572139, 0.997178701998392},
                        {1d, 0d, -1d, 1d, -1.99066910352353, 0.992063116831325},
                        {1d, 0d, -1d, 1d, -1.99156708731366, 0.992734708361072}};
                SOS = SOStmp;

                break;
            }
            case 12 - 1: { //400 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.99310322914237, 0.995609017471259},
                        {1d, 0d, -1d, 1d, -1.99481204219882, 0.996449406666243},
                        {1d, 0d, -1d, 1d, -1.98781144771062, 0.990018398370095},
                        {1d, 0d, -1d, 1d, -1.98901329889455, 0.990862013634153}};
                SOS = SOStmp;

                break;
            }
            case 13 - 1: { //500 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.99050680670828, 0.994475475479041},
                        {1d, 0d, -1d, 1d, -1.99293829986016, 0.995531949270623},
                        {1d, 0d, -1d, 1d, -1.98395738982897, 0.987450296416661},
                        {1d, 0d, -1d, 1d, -1.98558300940737, 0.988509309260325}};
                SOS = SOStmp;

                break;
            }
            case 14 - 1: { //630 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.98676630134718, 0.993050513225421},
                        {1d, 0d, -1d, 1d, -1.9902702023199, 0.994377965770798},
                        {1d, 0d, -1d, 1d, -1.97870082579015, 0.98422682662828},
                        {1d, 0d, -1d, 1d, -1.9809247376751, 0.985555085645678}};
                SOS = SOStmp;

                break;
            }
            case 15 - 1: { //800 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.98131216847172, 0.991259988360334},
                        {1d, 0d, -1d, 1d, -1.98642240920404, 0.992926741391776},
                        {1d, 0d, -1d, 1d, -1.9714458839295, 0.980183893516583},
                        {1d, 0d, -1d, 1d, -1.97452460597994, 0.981847920665762}};
                SOS = SOStmp;

                break;
            }
            case 16 - 1: { //1000 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.97327061291269, 0.989011423812757},
                        {1d, 0d, -1d, 1d, -1.98080609784611, 0.99110207751502},
                        {1d, 0d, -1d, 1d, -1.96131047213282, 0.975118178675675},
                        {1d, 0d, -1d, 1d, -1.96562410492596, 0.977199568266252}};
                SOS = SOStmp;

                break;
            }
            case 17 - 1: { //1250 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.96129684137274, 0.986189857434693},
                        {1d, 0d, -1d, 1d, -1.97251728116345, 0.988808349882964},
                        {1d, 0d, -1d, 1d, -1.94697959369175, 0.96877888707181},
                        {1d, 0d, -1d, 1d, -1.95309480860277, 0.971376675148598}};
                SOS = SOStmp;

                break;
            }
            case 18 - 1: { //1600 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.94331838743223, 0.982653056622724},
                        {1d, 0d, -1d, 1d, -1.96016379721327, 0.985925515164084},
                        {1d, 0d, -1d, 1d, -1.92648342773831, 0.960858460382354},
                        {1d, 0d, -1d, 1d, -1.93524818291937, 0.964090948697723}};
                SOS = SOStmp;

                break;
            }
            case 19 - 1: { //2000 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.91614399676791, 0.978226279249737},
                        {1d, 0d, -1d, 1d, -1.94159887356544, 0.982302705061909},
                        {1d, 0d, -1d, 1d, -1.8968649616478, 0.950982627988732},
                        {1d, 0d, -1d, 1d, -1.90954910934814, 0.954987595875618}};
                SOS = SOStmp;

                break;
            }
            case 20 - 1: { //2500 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.87487775337986, 0.972697099787666},
                        {1d, 0d, -1d, 1d, -1.91351506048818, 0.977749829390522},
                        {1d, 0d, -1d, 1d, -1.8536909024226, 0.938700630788609},
                        {1d, 0d, -1d, 1d, -1.87219045406778, 0.943631816425165}};
                SOS = SOStmp;

                break;
            }
            case 21 - 1: { //3150 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.81206659266995d, 0.965811507388085d},
                        {1d, 0d, -1d, 1d, -1.87083621916567d, 0.972026182391755d},
                        {1d, 0d, -1d, 1d, -1.79035335182561d, 0.923477258473934d},
                        {1d, 0d, -1d, 1d, -1.81747657879499d, 0.929492982583517d}};
                SOS = SOStmp;

                break;
            }
            case 22 - 1: { //4000 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.71652228964464, 0.957273877539435},
                        {1d, 0d, -1d, 1d, -1.80583449568253, 0.96482416356056},
                        {1d, 0d, -1d, 1d, -1.69712462062022, 0.904689735711706},
                        {1d, 0d, -1d, 1d, -1.73696762469691, 0.911925666518042}};
                SOS = SOStmp;

                break;
            }
            case 23 - 1: { //5000 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.57185027436493, 0.946756296691085},
                        {1d, 0d, -1d, 1d, -1.70691683103663, 0.955744299253964},
                        {1d, 0d, -1d, 1d, -1.56000474500507, 0.88163497931864},
                        {1d, 0d, -1d, 1d, -1.61838446053098, 0.89014537903767}};
                SOS = SOStmp;

                break;
            }
            case 24 - 1: { //6300 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.35501677406497, 0.933928929478821},
                        {1d, 0d, -1d, 1d, -1.55713350304722, 0.944253272731431},
                        {1d, 0d, -1d, 1d, -1.35963565720317, 0.853557535695661},
                        {1d, 0d, -1d, 1d, -1.44444104426036, 0.863193449940804}};
                SOS = SOStmp;

                break;
            }
            case 25 - 1: { //8000 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -1.03607428913417, 0.918537693413136},
                        {1d, 0d, -1d, 1d, -1.33281123956173, 0.929605283015337},
                        {1d, 0d, -1d, 1d, -1.19222371404469, 0.829876183237224},
                        {1d, 0d, -1d, 1d, -1.07114643078151, 0.819719136762785}};
                SOS = SOStmp;

                break;
            }
            case 26 - 1: { //10000 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, -0.58196549003164, 0.900594811528767},
                        {1d, 0d, -1d, 1d, -1.00364577059532, 0.910674212151042},
                        {1d, 0d, -1d, 1d, -0.834813256480064, 0.788635819389091},
                        {1d, 0d, -1d, 1d, -0.667098481285678, 0.779559978840824}};
                SOS = SOStmp;

                break;
            }
            case 27 - 1: { //12500 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, 0.0292134012608121, 0.880878860084649},
                        {1d, 0d, -1d, 1d, -0.537780584404988, 0.885530534428226},
                        {1d, 0d, -1d, 1d, -0.128150898838565, 0.73310662970785},
                        {1d, 0d, -1d, 1d, -0.349119121227023, 0.737212849422089}};
                SOS = SOStmp;

                break;
            }
            case 28 - 1: { //16000 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, 0.0800121378149414, 0.850081811955327},
                        {1d, 0d, -1d, 1d, 0.77255037323491, 0.862539267907511},
                        {1d, 0d, -1d, 1d, 0.530760327257635, 0.682318058965299},
                        {1d, 0d, -1d, 1d, 0.261943740525187, 0.671485598914933}};
                SOS = SOStmp;

                break;
            }
            case 29 - 1: { //20000 Hz
                double[][] SOStmp
                        = {{1d, 0d, -1d, 1d, 0.79931077062477, 0.791393116133809},
                        {1d, 0d, -1d, 1d, 1.51174765823124, 0.859182283094185},
                        {1d, 0d, -1d, 1d, 1.21472436975713, 0.639232016349079},
                        {1d, 0d, -1d, 1d, 0.91658389987536, 0.578701963128202}};
                SOS = SOStmp;

                break;
            }
        }
        return SOS;
    }

}
