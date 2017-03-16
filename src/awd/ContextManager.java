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
    private List<Integer> previousNearbynodes; //List of addresses
    private List<Double> jaccards = new ArrayList<>();
    private double lastWeight = 0.5;
    private double currentWeight = 0.5;


    private float battery = 1.0f;
    private int reachableNodes;


    private Integer lastUpdate = null;
    private AutonomousHost.HOST_STATUS lastStatus = null;
    private Integer lastGroupSize = null;

    private double resourcesWeight, reachableNodesWeight, nearbyNodesWeight, stabilityWeight;

    void setUtilityFunctionWeights(double resources, double reachable, double nearby, double stability){

        this.resourcesWeight = resources;
        this.reachableNodesWeight = reachable;
        this.nearbyNodesWeight = nearby;
        this.stabilityWeight = stability;

    }
    void setStabilityWindowSize(double stabilityWindowSize){this.stabilityWindowSize = stabilityWindowSize;}

    private void updateStability(List<Integer> currentNearbyNodes){

        if(previousNearbynodes == null) this.previousNearbynodes = new ArrayList<>();
        else {

            this.jaccards.add(jaccard(this.previousNearbynodes, currentNearbyNodes));
            this.previousNearbynodes = currentNearbyNodes;

            double currentTime = SimClock.getTime();

            if (currentTime - lastStabilityUpdate >= stabilityWindowSize) {
                //update the stability value
                this.lastStabilityUpdate = currentTime;

                OptionalDouble average = jaccards.stream().mapToDouble(a -> a).average();
                double avg = average.isPresent() ? average.getAsDouble() : 0;

                this.lastStabilityValue = this.lastStabilityValue * this.lastWeight + avg * this.currentWeight;

                this.jaccards = new ArrayList<>();
            }
        }
    }

    private double jaccard(Collection<Integer> previous, Collection<Integer> current){

        double jac = 0;
        if(previous.size() != 0 || current.size() != 0)
            jac = CollectionUtils.intersection(previous, current).size() / CollectionUtils.union(previous, current).size();

        //Logger.print(null, "Jac: "+lastStabilityValue);
        return jac;
    }

    void updateContext(AutonomousHost.HOST_STATUS currentStatus,
                              Set<DTNHost> group,
                              List<Integer> nearbyNodes){

        int currentTime = (int) SimClock.getTime();
        double drop = 0;

        this.reachableNodes = (group != null) ? group.size() : 0;
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

    Double evalContext(){
        return this.battery*resourcesWeight+
                this.reachableNodes*reachableNodesWeight+
                ((this.previousNearbynodes == null) ? 0 : this.previousNearbynodes.size())*nearbyNodesWeight
                +this.lastStabilityValue*stabilityWeight;
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
