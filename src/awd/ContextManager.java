package awd;

import core.DTNHost;
import core.SimClock;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

public class ContextManager {


    private static final double GO_SLOPE = -0.006802;
    private static final double GO_INTERCEPT = -0.03356;

    private static final double CLIENT_SLOPE = -0.003365;
    private static final double CLIENT_INTERCEPT = -0.04075;

    private static final double SCAN_SLOPE = -0.04;
    private static final double SCAN_INTERCEPT = 1.0;

    //Stability index
    private double stabilityWindowSize;
    private double lastStabilityUpdate;
    private double lastStabilityValue;
    private List<Integer> previousNearbyNodes; //List of addresses
    private double jaccardAvg = 0;
    private int jaccardCounts = 0;

    private double lastStabilityWeight;
    private double currentStabilityWeight;


    private float battery = 1.0f;


    private Integer lastUpdate = null;
    private AutonomousHost.HOST_STATUS lastStatus = null;
    private Integer lastGroupSize = null;

    void setStabilityWeights(double prev, double current){
        this.lastStabilityWeight = prev;
        this.currentStabilityWeight = current;
    }

    void setStabilityWindowSize(double stabilityWindowSize){this.stabilityWindowSize = stabilityWindowSize;}

    private void updateStability(List<Integer> currentNearbyNodes){

        if(previousNearbyNodes == null) this.previousNearbyNodes = new ArrayList<>();
        else {

            //this.jaccards.add(jaccard(this.previousNearbyNodes, currentNearbyNodes));
            this.jaccardAvg = (this.jaccardAvg * this.jaccardCounts) + jaccard(this.previousNearbyNodes, currentNearbyNodes);
            this.jaccardCounts++;
            this.jaccardAvg /= this.jaccardCounts;

            this.previousNearbyNodes = currentNearbyNodes;

            double currentTime = SimClock.getTime();

            if (currentTime - lastStabilityUpdate >= stabilityWindowSize) {
                //update the stability value
                this.lastStabilityUpdate = currentTime;

                //OptionalDouble average = jaccards.stream().mapToDouble(a -> a).average();
                //double avg = average.isPresent() ? average.getAsDouble() : 0;
                double avg = this.jaccardAvg;

                this.lastStabilityValue = this.lastStabilityValue * this.lastStabilityWeight + avg * this.currentStabilityWeight;

                this.jaccardAvg = 0;
                this.jaccardCounts = 0;

                //this.jaccards = new ArrayList<>();
            }
        }
    }

    private double jaccard(List<Integer> previous, List<Integer> current){

        double jac = 0;
        if(previous.size() != 0 || current.size() != 0)
            jac = CollectionUtils.intersection(previous, current).size() / CollectionUtils.union(previous, current).size();
        return jac;
    }

    void updateContext(AutonomousHost.HOST_STATUS currentStatus,
                              Set<DTNHost> group, List<Integer> nearbyNodes){

        int currentTime = (int) SimClock.getTime();
        double drop = 0;

        updateStability(nearbyNodes);

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

                this.battery -= drop;
            }

            this.lastUpdate = currentTime;
            this.lastStatus = currentStatus;
            this.lastGroupSize = (group == null) ? 0 : group.size();
        }
    }

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

    List<Integer> getPreviousNearbyNodes() {
        return previousNearbyNodes;
    }

    public float getResources() {
        return battery;
    }

    double getLastStabilityValue() {
        return lastStabilityValue;
    }
}
