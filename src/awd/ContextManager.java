package awd;

import core.DTNHost;
import core.SimClock;

import java.util.Set;

public class ContextManager {


    private static final double GO_SLOPE = -0.006802;
    private static final double GO_INTERCEPT = -0.03356;

    private static final double CLIENT_SLOPE = -0.003365;
    private static final double CLIENT_INTERCEPT = -0.04075;

    private static final double SCAN_SLOPE = -0.04;
    private static final double SCAN_INTERCEPT = 1.0;


    private float battery, cpu, memory;


    private Integer lastUpdate = null;
    private AutonomousHost.HOST_STATUS lastStatus = null;
    private Integer lastGroupSize = null;

    public ContextManager(){

        this.battery = this.cpu = this.memory = 1.0f;
    }

    //public void updateContextAfterScanning(){
    //this.battery -= BATTERY_DROP_SCANNING;
    //}

    public void updateContext(AutonomousHost.HOST_STATUS currentStatus, Set<DTNHost> group){

        int currentTime = (int) SimClock.getTime();
        double drop = 0;

        if(this.lastUpdate == null || currentTime > this.lastUpdate) {

            if(lastStatus != null && this.lastUpdate != null) {

                float hourDelta = ((((float) currentTime - (float)lastUpdate) / 60.0f)/60.0f);

                switch (lastStatus) {

                    case AP:
                        if (group == null || group.size() == 0) drop = getScanningDrop(hourDelta);
                        else drop = getDropInGroup(hourDelta, true, lastGroupSize + 1);
                        break;

                    case CONNECTED:
                        drop = getDropInGroup(hourDelta, false, lastGroupSize + 1);
                        break;
                }

                //Logger.print(null, ((lastGroupSize == 0 || lastGroupSize == null) ? "SCAN" : lastStatus) +
                 //       " : " + String.format("%2.2f",drop) + " ("+String.format("%2.2f",battery*100)+"%)");

                this.battery -= drop;
            }

            this.lastUpdate = currentTime;
            this.lastStatus = currentStatus;
            this.lastGroupSize = (group == null) ? 0 : group.size();
        }
    }

    public Float evalContext(){
        return this.battery*0.33f*this.cpu*0.33f*this.memory*0.33f;
    }
    public float getBatteryLevel(){ return this.battery; }

    private double getScanningDrop(float hourDelta){

        return 1.0 - (SCAN_SLOPE*hourDelta + SCAN_INTERCEPT);
    }

    private double getDropInGroup(float hourDelta, boolean isGO, int groupSize){

        double slope;

        if (isGO) slope = getGoSlope(groupSize);
        else slope = getClientSlope(groupSize);

        return 1.0 - (slope*hourDelta + 1.0);
    }

    private double getGoSlope(int groupSize){

        return GO_SLOPE*groupSize + GO_INTERCEPT;
    }

    private double getClientSlope(int groupSize){

        return CLIENT_SLOPE*groupSize + CLIENT_INTERCEPT;
    }
}
