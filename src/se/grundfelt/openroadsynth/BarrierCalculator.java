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

import static se.grundfelt.openroadsynth.BarrierCalculator.barrier;

/**
 *
 *  @author Gustav Grundfelt
 */
public class BarrierCalculator {

    public static Barrier[] barrier = new Barrier[3];

    /**
     * Function to init the barriers
     *
     * @param index
     * @param start
     * @param end
     * @param barDistFromRoad
     * @param height
     * @param listDistFromRoad
     */
    public static void initBarrier(int index, double start, double end, double barDistFromRoad, double height, double listDistFromRoad) {

        barrier[index].start = start;
        barrier[index].end = end;
        barrier[index].barDistFromRoad = barDistFromRoad;
        barrier[index].height = height;

        //Calculate the LineOfSight
        barrier[index].carIsBehindScreenStartAt = start * (listDistFromRoad / (listDistFromRoad - barDistFromRoad));
        barrier[index].carIsBehindScreenEndAt = end * (listDistFromRoad / (listDistFromRoad - barDistFromRoad));

    }

    public double getTotalReduction(double freq, double carpos, double listDistFromRoad, double carListDist) {

        //Get the two most reducing barriers
        double[] barRed = new double[3];
        
        int maxRedBarrierIndex = 0; //Index of the most reducing barrier
        int minRedBarrierIndex = 0; //Index of the secont most reducing barrier
        int FirstBarrierIndex;
        int SecondBarrierIndex;
        double maxRed=0;
        double minRed=Double.MAX_VALUE;
        
        for (int iBar = 0; iBar < barRed.length; iBar++) {
            barRed[iBar] = getOneScreenReduction(iBar, freq, carpos, listDistFromRoad, carListDist);
            if(barRed[iBar] > maxRed){
                maxRed=barRed[iBar];
                maxRedBarrierIndex=iBar;
            }
            if(barRed[iBar] < minRed){
                minRed=barRed[iBar];
                minRedBarrierIndex=iBar;
            }
        }
        
        //Get barrier which reduces the sound at most is
        FirstBarrierIndex=maxRedBarrierIndex;
        
        //Get barrier which reduces the sound second most
        for (int iBar = 0; iBar < barRed.length; iBar++) {
            if(iBar != maxRedBarrierIndex && iBar != minRedBarrierIndex){
                SecondBarrierIndex=iBar;
            }
        }
        
        //Check if the barriers both breaks the line of sight
        
        

        return 0;
    }

    private double getOneScreenReduction(int index, double freq, double carpos, double listDistFromRoad, double carListDist) {

        double vertDz = 0d, leftDz = 0d, rightDz = 0d;
        double attentuation = 1d;

        if (barrier[index].isActive && (carpos > barrier[index].carIsBehindScreenStartAt) && (carpos < barrier[index].carIsBehindScreenEndAt)) {

            double c = 343d;
            double lambda = c / freq;

            double listenerHeight = 1.75D;
            double sourceHeight = 0.5D;

            double C2 = 20d;
            double C3 = 1d;

            //Calc Vertical Diffraction
            double vertDss = Math.sqrt(Math.pow(barrier[index].barDistFromRoad, 2d) + Math.pow(barrier[index].height - sourceHeight, 2D)); //VERTICAL
            double vertDsr = Math.sqrt(Math.pow((listDistFromRoad - barrier[index].barDistFromRoad), 2d) + Math.pow(barrier[index].height - listenerHeight, 2D));
            double vertA = carpos;
            double vertZ = Math.sqrt(Math.pow(vertDss + vertDsr, 2d) + Math.pow(vertA, 2d)) - carListDist;
            double Vert_Kmet = Math.exp((-1d / 2000d) * Math.sqrt(vertDss * vertDsr * carListDist / (2d * vertZ)));
            if (vertZ < 0) {
                Vert_Kmet = 1;
            }
            vertDz = -10d * Math.log10(3d + (C2 / lambda) * C3 * vertZ * Vert_Kmet);

            //Calc Left Diffraction
            double leftDss = Math.sqrt(Math.pow(barrier[index].carIsBehindScreenStartAt, 2d) + Math.pow(carpos, 2D)); //VERTICAL
            double leftDsr = Math.sqrt(Math.pow((listDistFromRoad - barrier[index].barDistFromRoad), 2d) + Math.pow(barrier[index].carIsBehindScreenStartAt, 2D));
            double leftA = listenerHeight - sourceHeight;
            double leftZ = Math.sqrt(Math.pow(leftDss + leftDsr, 2d) + Math.pow(leftA, 2d)) - carListDist;
            double leftKmet = 1d;
            if (leftZ < 0) {
                leftZ = 0;
            }

            leftDz = -10 * Math.log10(3 + (C2 / lambda) * C3 * leftZ * leftKmet);

            //Calc Right Diffraction
            double rightDss = Math.sqrt(Math.pow(barrier[index].carIsBehindScreenEndAt, 2d) + Math.pow(carpos, 2D)); //VERTICAL
            double rightDsr = Math.sqrt(Math.pow((listDistFromRoad - barrier[index].barDistFromRoad), 2d) + Math.pow(barrier[index].carIsBehindScreenEndAt, 2D));
            double rightA = listenerHeight - sourceHeight;
            double rightZ = Math.sqrt(Math.pow(rightDss + rightDsr, 2d) + Math.pow(rightA, 2d)) - carListDist;
            double rightKmet = 1d;
            if (rightZ < 0d) {
                rightZ = 0;
            }

            rightDz = -10d * Math.log10(3 + (C2 / lambda) * C3 * rightZ * rightKmet);
            double vertGain = Math.pow(10d, vertDz / 20d);
            double leftGain = Math.pow(10d, leftDz / 20d);
            double rightGain = Math.pow(10d, rightDz / 20d);

            attentuation = Math.max(Math.max(leftGain, rightGain), vertGain);

            if (Double.isNaN(attentuation)) {
                int stop = 0;
            }
        }

        return attentuation;
    }

}
