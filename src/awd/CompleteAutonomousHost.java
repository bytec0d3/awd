package awd;

import core.*;
import javafx.util.Pair;
import movement.MovementModel;
import org.apache.commons.collections4.CollectionUtils;
import routing.MessageRouter;

import java.util.*;

/**
 * Created by mattia on 02/03/17.
 */
public class CompleteAutonomousHost extends AutonomousHost {

    private static final String SETTINGS_TRAVELLING_PROB = "travellingProb";
    private static final String SETTINGS_MAX_RES_GROUP = "destroyGroupAfterPercRes";
    private static final String SETTINGS_PREV_STABILITY_WEIGHT = "prevStabilityWeight";
    private static final String SETTINGS_CURRENT_STABILITY_WEIGHT = "currentStabilityWeight";
    private static final String SETTINGS_STABILITY_WINDOW_S = "stabilityWindowSize";
    private static final String SETTINGS_UTILITY_RESOURCES_WEIGHT = "utilityResourcesWeight";
    private static final String SETTINGS_UTILITY_NEARBY_NODES_WEIGHT = "utilityNearbyNodesWeight";
    private static final String SETTINGS_UTILITY_CAPACITY_NEARBY_WEIGHT = "utilityCapacityNearbyWeight";
    private static final String SETTINGS_UTILITY_STABILITY_WEIGHT = "utilityStabilityWeight";

    private double resourcesWeight, capacityNearbyWeight, nearbyNodesWeight, stabilityWeight;

    // The maximum percentage of resources used by the AP to maintain the group active
    private double maxGroupResources;
    private double startGroupResources;

    private double travellingProb;

    private int travelling, merges;

    private Random random;

    /**
     * Creates a new DTNHost.
     *
     * @param msgLs        Message listeners
     * @param movLs        Movement listeners
     * @param groupId      GroupID of this host
     * @param interf       List of NetworkInterfaces for the class
     * @param comBus       Module communication bus object
     * @param mmProto      Prototype of the movement model of this host
     * @param mRouterProto Prototype of the message router of this host
     */
    public CompleteAutonomousHost(List<MessageListener> msgLs, List<MovementListener> movLs, String groupId,
                                  List<NetworkInterface> interf, ModuleCommunicationBus comBus, MovementModel mmProto,
                                  MessageRouter mRouterProto) {
        super(msgLs, movLs, groupId, interf, comBus, mmProto, mRouterProto);

        this.random = new Random();
    }

    @Override
    public void evaluateNearbyNodes(Collection<NetworkInterface> nodes) {

        if(getCurrentStatus() == HOST_STATUS.AP && this.getGroup() == null) {

            AutonomousHost candidate = null;

            if (nodes != null) {
                //List<AutonomousHost> availableGroups = getInterface().getAvailableGroups(nodes);

                Pair<List<AutonomousHost>, List<AutonomousHost>> available = getInterface().getAvailableNodes(nodes);
                List<AutonomousHost> availableGroups = available.getKey();
                List<AutonomousHost> availableSinglets = available.getValue();

                if (availableGroups != null) {

                    availableGroups.sort(Comparators.groupComparator);
                    candidate = availableGroups.get(availableGroups.size() - 1);

                } else {

                    if (availableSinglets != null) {
                        availableSinglets.add(this);
                        availableSinglets.sort(Comparators.contextAndNameComparator);
                        candidate = availableSinglets.get(availableSinglets.size() - 1);
                    }
                }

                if (candidate != null && candidate != this && candidate.haveFreeSlots()) {
                    forceConnection(candidate, candidate.getInterface().getInterfaceType(), true);
                }
            }
        }
    }

    void connectionFailed(AutonomousHost from){
        getInterface().addPreviousAPInBlacklist(from);
    }

    /**
     * Evaluate if the node should become a traveller or not.
     * If it becomes a traveller, first it adds the previous AP in blacklist, and then it performs the disconnection
     * from the current group.
     */
    private void evaluateTravelling(){

        double r = random.nextDouble();
        if(r <= this.travellingProb / this.getGroup().size()){
            getInterface().addPreviousAPInBlacklist(this.getCurrentAP());
            destroyGroup();
            this.travelling++;
        }
    }

    private void evaluateMerge(){

        int myCapacity = this.getService().getAvailableSlots();
        int myCardinality = (this.getGroup() != null) ? this.getGroup().size() : 0;
        int myMinSize = (this.getGroup().size() / 2) + 1;

        List<AutonomousHost> availableGos = getInterface().getAvailableGOs(getInterface().getNearbyInterfaces());

        if(availableGos != null) {

            List<AutonomousHost> candidates = new ArrayList<>();

            for(AutonomousHost autonomousHost : availableGos){

                int otherCardinality = autonomousHost.getService().getGroupMembers().size();
                int otherCapacity = autonomousHost.getService().getAvailableSlots();
                int otherMinSize = (autonomousHost.getService().getGroupMembers().size() / 2) + 1;

                if(otherCapacity > myCapacity && otherCardinality > myCardinality ||
                        (otherCapacity > myMinSize && otherCardinality <= myCapacity && myCapacity < otherMinSize))
                    candidates.add(autonomousHost);
            }

            if(candidates.size() > 0) {
                AutonomousHost bestCandidate = candidates.stream()
                        .max(Comparator.comparing(c -> c.getService().getUtilityValue()))
                        .orElse(null);

                if(bestCandidate.getService().getUtilityValue() > this.getService().getUtilityValue()) {
                    sendVisibilityQuery(bestCandidate);
                }
            }
        }
    }

    public void update(boolean simulateConnections) {

        super.update(simulateConnections);

        if(this.getCurrentStatus() == HOST_STATUS.AP && this.getGroup() != null && this.getGroup().size() > 0)
            checkGroupResources();
    }

    private void checkGroupResources(){

        if(this.startGroupResources - this.contextManager.getResources() >= this.maxGroupResources) {
            //Logger.print(this, "Group resources limit reached");
            destroyGroup();
        }
    }

    @Override
    void takeDecision() {

        long time = 0;

        if(getCurrentStatus() == HOST_STATUS.AP && this.getGroup() == null){

            time = new Date().getTime();
            evaluateNearbyNodes(getInterface().getNearbyInterfaces());
            ExecTime.getInstance().setEvalNearbyNodesAvg(new Date().getTime() - time);

        }else{

            Collection<DTNHost> currentGroup = new ArrayList<>();
            if(this.getGroup() != null) currentGroup.addAll(this.getGroup());
            if(CollectionUtils.subtract(getInterface().getNearbyHosts(), currentGroup).size() != 0) {
                // If I am a client and there are other groups in the nearby, evaluate to become a traveller
                if (getCurrentStatus() == HOST_STATUS.CONNECTED) {
                    time = new Date().getTime();
                    evaluateTravelling();
                    ExecTime.getInstance().setEvalTravellingAvg(new Date().getTime() - time);

                } else if (getCurrentStatus() == HOST_STATUS.AP && this.getGroup() != null) {
                    // If I am a GO, evaluate to merge my group with another one
                    time = new Date().getTime();
                    evaluateMerge();
                    ExecTime.getInstance().setEvalMergeAvg(new Date().getTime() - time);
                }
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    // SERVICE MENAGEMENT
    //------------------------------------------------------------------------------------------------------------------
    public ServicePayload getService(){

        return new ServicePayload(this.getCurrentStatus(), getUtilityValue(), this.getGroup(),
                getInterface().getMaxClients());
    }

    private Double getUtilityValue(){

        int nearby = ((this.getContextManager().getPreviousNearbyNodes() == null) ? 0 :
                this.getContextManager().getPreviousNearbyNodes().size());

        int actualCapacity = getInterface().getMaxClients() - ((this.getGroup() != null) ? this.getGroup().size() : 0);

        return this.getContextManager().getResources()*resourcesWeight +
                nearby*nearbyNodesWeight +
                ((actualCapacity <= nearby) ? actualCapacity : nearby) * this.capacityNearbyWeight +
                +this.getContextManager().getLastStabilityValue()*stabilityWeight;
    }

    //------------------------------------------------------------------------------------------------------------------
    // MERGE MENAGEMENT
    //------------------------------------------------------------------------------------------------------------------
    boolean visibilityQuery(AutonomousHost host){

        return this.getInterface().getNearbyHosts().contains(host);
    }

    void mergeIntent(AutonomousHost hostToMerge){

        if(this.visibilityQuery(hostToMerge)) {
            destroyGroup();
            if(hostToMerge.haveFreeSlots())
                forceConnection(hostToMerge, hostToMerge.getInterface().getInterfaceType(), true);
                //this.getInterface().connect(hostToMerge.getInterface());
        }

    }

    //------------------------------------------------------------------------------------------------------------------
    // MESSAGE MENAGEMENT
    //------------------------------------------------------------------------------------------------------------------
    private void sendVisibilityQuery(AutonomousHost hostToMerge) {

        int positive = 0;

        for (DTNHost h : this.getGroup()) {

            CompleteAutonomousHost hostInGroup = (CompleteAutonomousHost) h;
            if (hostInGroup.visibilityQuery(hostToMerge)) positive++;
        }

        if (positive >= (this.getGroup().size() / 2) + 1) {

            List<DTNHost> myGroup = new ArrayList<>();
            myGroup.addAll(this.getGroup());

            for (DTNHost h : myGroup) {
                CompleteAutonomousHost hostInGroup = (CompleteAutonomousHost) h;
                hostInGroup.mergeIntent(hostToMerge);
            }

            destroyGroup();
            if(hostToMerge.haveFreeSlots())
                forceConnection(hostToMerge, hostToMerge.getInterface().getInterfaceType(), true);
                //this.getInterface().connect(hostToMerge.getInterface());
            this.merges++;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    // GROUP RESOURCES
    //------------------------------------------------------------------------------------------------------------------
    void openGroupResources(){
        this.startGroupResources = this.contextManager.getResources();
    }

    void closeGroupResources(){
        this.startGroupResources = 0;
    }

    //------------------------------------------------------------------------------------------------------------------
    // SETTINGS
    //------------------------------------------------------------------------------------------------------------------
    void parseSettings(Settings s){
        this.travellingProb = s.getDouble(SETTINGS_TRAVELLING_PROB);
        this.maxGroupResources = s.getDouble(SETTINGS_MAX_RES_GROUP);

        this.resourcesWeight = s.getDouble(SETTINGS_UTILITY_RESOURCES_WEIGHT);
        this.nearbyNodesWeight = s.getDouble(SETTINGS_UTILITY_NEARBY_NODES_WEIGHT);
        this.capacityNearbyWeight = s.getDouble(SETTINGS_UTILITY_CAPACITY_NEARBY_WEIGHT);
        this.stabilityWeight = s.getDouble(SETTINGS_UTILITY_STABILITY_WEIGHT);

        this.contextManager.setStabilityWeights(s.getDouble(SETTINGS_PREV_STABILITY_WEIGHT),
                s.getDouble(SETTINGS_CURRENT_STABILITY_WEIGHT));

        this.contextManager.setStabilityWindowSize(s.getDouble(SETTINGS_STABILITY_WINDOW_S));
    }

    public int getTravelling() {
        return travelling;
    }

    public int getMerges() {
        return merges;
    }
}
