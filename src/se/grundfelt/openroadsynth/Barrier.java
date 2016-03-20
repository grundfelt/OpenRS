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
 * 
 * List of variables Start Position along the road axis where the
 * barrier begins End Position along the road axis where the barrier begins
 * Height Barrier Hight relative the ground IsActive Boolean barDistFromRoad
 * Distance from road where screen is located carIsBehindScreenStartAt Position
 * along the road axis where the car is no longer visible / behind screen
 * carIsBehindScreenEndAt Position along the road axis where the car becomes
 * visible again / behind screen
 */
public class Barrier {

    public double start = 0;
    public double end = 0;
    public double height = 0;
    public boolean isActive;
    public double barDistFromRoad = 5; //Distance from road where screen is located
    public double carIsBehindScreenStartAt = 0;
    public double carIsBehindScreenEndAt = 0;

    //Constructor
    Barrier(double start, double end, double dist_from_road, double height, double listDistFromRoad) {
        this.start = start;
        this.end = end;
        this.barDistFromRoad = dist_from_road;
        this.height = height;

        //Calculate the LineOfSight
        this.carIsBehindScreenStartAt=this.start*(listDistFromRoad/(listDistFromRoad-this.barDistFromRoad));
        this.carIsBehindScreenEndAt=this.end*(listDistFromRoad/(listDistFromRoad-this.barDistFromRoad));
        
        //System.out.printf("start: %f  -  end: %f \n" , this.carIsBehindScreenStartAt,this.carIsBehindScreenEndAt);
    }

    public boolean getIfActiveHere(double carpos) {
        if ((carpos >= this.carIsBehindScreenStartAt) && (carpos <= this.carIsBehindScreenEndAt)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Calculate the barrierGain according to ISO9613 Sound ray Paths
     *
     * Left | Barrier | Right 
     * View from the listener.
     * @param freq
     * @param carpos
     * @param listDistFromRoad
     * @param carListDist
     * @return attenuation %
     */
    public double getReduction(double freq, double carpos, double listDistFromRoad, double carListDist) {

        double vertDz = 0d, leftDz = 0d, rightDz = 0d;
        double attentuation=1d;
        
        if (this.isActive && (carpos > this.carIsBehindScreenStartAt) && (carpos < this.carIsBehindScreenEndAt)) {

            double c = 343d;
            double lambda = c / freq;

            double listenerHeight = 1.75D;
            double sourceHeight = 0.5D;

            double C2 = 20d;
            double C3 = 1d;
            
            

            //Calc Vertical Diffraction
            double vertDss = Math.sqrt(Math.pow(this.barDistFromRoad, 2d) + Math.pow(this.height - sourceHeight, 2D)); //VERTICAL
            double vertDsr = Math.sqrt(Math.pow((listDistFromRoad - this.barDistFromRoad), 2d) + Math.pow(this.height - listenerHeight, 2D));
            double vertA = carpos;
            double vertZ = Math.sqrt(Math.pow(vertDss + vertDsr, 2d) + Math.pow(vertA, 2d)) - carListDist;
            double Vert_Kmet = Math.exp((-1d / 2000d) * Math.sqrt(vertDss * vertDsr * carListDist / (2d * vertZ)));
            if (vertZ < 0) {
                Vert_Kmet = 1;
            }
            vertDz = -10d * Math.log10(3d + (C2 / lambda) * C3 * vertZ * Vert_Kmet);

            //Calc Left Diffraction
            double leftDss = Math.sqrt(Math.pow(this.carIsBehindScreenStartAt, 2d) + Math.pow(carpos, 2D)); //VERTICAL
            double leftDsr = Math.sqrt(Math.pow((listDistFromRoad - this.barDistFromRoad), 2d) + Math.pow(this.carIsBehindScreenStartAt, 2D));
            double leftA = listenerHeight - sourceHeight;
            double leftZ = Math.sqrt(Math.pow(leftDss + leftDsr, 2d) + Math.pow(leftA, 2d)) - carListDist;
            double leftKmet = 1d;
            if (leftZ < 0) {
                leftZ = 0;
            }

            leftDz = -10 * Math.log10(3 + (C2 / lambda) * C3 * leftZ * leftKmet);

            //Calc Right Diffraction DOES NOT WORK
            double rightDss = Math.sqrt(Math.pow(this.carIsBehindScreenEndAt, 2d) + Math.pow(carpos, 2D)); //VERTICAL
            double rightDsr = Math.sqrt(Math.pow((listDistFromRoad - this.barDistFromRoad), 2d) + Math.pow(this.carIsBehindScreenEndAt, 2D));
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

            attentuation= Math.max(Math.max(leftGain, rightGain), vertGain);
            
            if(Double.isNaN(attentuation)){
                int stop=0;
            }
        }

        return attentuation;
    }
    

}
