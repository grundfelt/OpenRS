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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 *
 * @author Gustav Grundfelt
 */
public class SignalToolbox {

    SignalToolbox() {

    }

    /**
     * Get the rms value of a signal float array*
     */
    static double rms(float[] samples) {
        double square = 0f, mean = 0f, sum = 0f, rms;
        for (int i_sample = 0; i_sample < samples.length; i_sample++) {
            square = (double) (samples[i_sample] * samples[i_sample]);
            sum += square;
        }
        mean = sum / (double) samples.length;
        rms = Math.sqrt(mean);
        return rms;
    }

    /**
     * Get the rms value of a signal short array*
     */
    static double rms(short[] samples) {
        double n_samples = (double) samples.length;
        double square = 0, mean = 0, sum = 0, rms;
        for (int i_sample = 0; i_sample < n_samples; i_sample++) {
            square = (double) (samples[i_sample] * samples[i_sample]);
            sum += square;
        }
        mean = sum / samples.length;
        rms = Math.sqrt(mean);
        return rms;
    }

    /**
     * Get the max value of a signal float array
     */
    static double max(float[] samples) {
        double n_samples = (double) samples.length;
        double max_value = 0, tmp_value = 0;
        for (int i_sample = 0; i_sample < n_samples; i_sample++) {
            tmp_value = Math.abs((double) samples[i_sample]);
            if (max_value < tmp_value) {
                max_value = tmp_value;
            }
        }
        return max_value;
    }

    /**
     * Get the max value of a signal short array
     */
    static double max(short[] samples) {
        double n_samples = (double) samples.length;
        double max_value = 0, tmp_value = 0;
        for (int i_sample = 0; i_sample < n_samples; i_sample++) {
            tmp_value = Math.abs((double) samples[i_sample]);
            if (max_value < tmp_value) {
                max_value = tmp_value;
            }
        }
        return max_value;

    }

    /**
     * Returns the logarithmic sum of an double array
     *
     * @param arrayDB
     * @return
     */
    static double getDecibels(double[] arrayDB) {
        int n = arrayDB.length;
        double sum = 0;
        for (int i_number = 0; i_number < n; i_number++) {
            sum += Math.pow(10d, (arrayDB[i_number] / 10d));
        }
        return 10 * Math.log10(sum);
    }

    /**
     * Logarithmic addition of two values.
     *
     * @param value1
     * @param value2
     * @return
     */
    static double addDecibels(double value1, double value2) {
        return 10d * Math.log10(Math.pow(10d, value1 / 10d) + Math.pow(10d, value2 / 10d));
    }

    /**
     * Logarithmic addition of two values.
     *
     * @param scalar
     * @param dBvalue
     * @return
     */
    static double multiplyDecibels(double scalar, double dBvalue) {
        return 10d * Math.log10(scalar * Math.pow(10d, dBvalue / 10d));
    }

    /**
     * Fades in and out a sound signal
     *
     * @param signalIn
     * @param seconds to fade in and out
     * @return
     */
    static float[] fadeInAndOut(float[] signalIn, double seconds) {
        double fadeGain;
        float[] signalOut = signalIn;
        int n_samples = signalIn.length;

        int samplesToFade = (int) (seconds * Globals.SAMPLING_RATE);

        for (int i1 = 0; i1 < samplesToFade; i1++) {
            fadeGain = (double) i1 / (double) samplesToFade;

            //Fade in
            signalOut[i1] = (short) (fadeGain * (double) signalOut[i1]);
            signalOut[i1] = (short) (fadeGain * (double) signalOut[i1]);

            //Fade out
            signalOut[n_samples - i1 - 1] = (short) (fadeGain * (double) signalOut[n_samples - i1 - 1]);
            signalOut[n_samples - i1 - 1] = (short) (fadeGain * (double) signalOut[n_samples - i1 - 1]);
        }
        return signalOut;
    }

    /**
     * Will write an stereo 16 bit PCN wave file from an array of floats. Values
     * between 2^-15 and 2^15
     *
     * @param signalLeft signal to be written
     * @param signalRight signal to be written
     * @param file where it is to be written
     *
     */
    public static void exportSignalToWav(float[] signalLeft, float[] signalRight, File file) {
        //Get the number of samples to write.
        int numberOfSamplesToWrite = Math.min(signalLeft.length, signalRight.length);

        //Create the buffer
        AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;

        //Create the buffer
        ByteBuffer audioBuffer = ByteBuffer.allocate(2 * 2 * numberOfSamplesToWrite); //2 channels x 2 bytes x number of samples to write
        AudioFormat audioFormat = new AudioFormat(Globals.SAMPLING_RATE, 16, 2, true, true);
        int n;

        for (n = 0; n < numberOfSamplesToWrite; n++) {
            audioBuffer.putShort((short) (signalLeft[n] * Globals.DYN_RANGE_GAIN));
            audioBuffer.putShort((short) (signalRight[n] * Globals.DYN_RANGE_GAIN));
        }

        AudioInputStream outputAIS;
        ByteArrayInputStream BAIS = new ByteArrayInputStream(audioBuffer.array());
        outputAIS = new AudioInputStream(BAIS, audioFormat, numberOfSamplesToWrite);
        try {
            AudioSystem.write(outputAIS, fileType, file);
        } catch (IOException ex) {
            Logger.getLogger(OpenRoadSynth.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

}
