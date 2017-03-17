package awd;

import core.*;
import movement.MovementModel;
import org.apache.commons.collections4.CollectionUtils;
import routing.MessageRouter;

import java.util.*;

/**
 * Created by mattia on 02/03/17.
 */
public class CompleteAutonomousHost extends AutonomousHost {

    //Merge
    private AutonomousHost hostToMerge;
    private int mergePositiveResponses;

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
                List<AutonomousHost> availableGroups = getInterface().getAvailableGroups(nodes);

                if (availableGroups != null) {

                    availableGroups.sort(Comparators.groupComparator);
                    candidate = availableGroups.get(availableGroups.size() - 1);

                } else {

                    List<AutonomousHost> availableSinglets = getInterface().getAvailableSinglets(nodes);
                    if (availableSinglets != null) {
                        availableSinglets.add(this);
                        availableSinglets.sort(Comparators.contextAndNameComparator);
                        candidate = availableSinglets.get(availableSinglets.size() - 1);
                    }
                }

                if (candidate != null && candidate != this) {
                    forceConnection(candidate, candidate.getInterface().getInterfaceType(), true);
                }
            }
        }
    }

    /**
     * Evaluate if the node should become a traveller or not.
     * If it becomes a traveller, first it adds the previous AP in blacklist, and then it performs the disconnection
     * from the current group.
     */
    private void evaluateTravelling(){

        double r = random.nextDouble();
        if(r <= this.travellingProb / this.getGroup().size()){
            //Logger.print(this, "R: "+r+" - th: "+this.travellingProb / this.getGroup().size());
            //Logger.print(this, "Ok, lets travel");
            getInterface().addPreviousAPInBlacklist(this.getCurrentAP());
            destroyGroup();
        }
    }

    private void evaluateMerge(){

        int minSize = (this.getGroup().size() / 2) + 1;

        List<AutonomousHost> availableGroups = getInterface().getAvailableGroups(getInterface().getNearbyInterfaces());
        if(availableGroups != null) {
            availableGroups.removeIf((AutonomousHost host) -> host.getService().getAvailableSlots() >= minSize);

            if (availableGroups.size() != 0) {
                AutonomousHost hostToMerge = Collections.max(availableGroups,
                        Comparator.comparing(host -> host.getService().getGroupMembers().size()));

                sendVisibilityQuery(hostToMerge);
            }
        }
    }

    public void update(boolean simulateConnections) {

        super.update(simulateConnections);

        if(this.getCurrentStatus() == HOST_STATUS.AP && this.getGroup() != null && this.getGroup().size() > 0)
            checkGroupResources();
    }

    private void checkGroupResources(){

        if(this.startGroupResources - this.contextManager.getBatteryLevel() >= this.maxGroupResources) {
            Logger.print(this, "Group resources limit reached");
            destroyGroup();
        }
    }

    @Override
    void takeDecision() {

        if(getCurrentStatus() == HOST_STATUS.AP && this.getGroup() == null){
            evaluateNearbyNodes(getInterface().getNearbyInterfaces());

        }else{

            Collection<DTNHost> currentGroup = new ArrayList<>();
            if(this.getGroup() != null) currentGroup.addAll(this.getGroup());
            if(CollectionUtils.subtract(getInterface().getNearbyHosts(), currentGroup).size() != 0) {
                // If I am a client and there are other groups in the nearby, evaluate to become a traveller
                if (getCurrentStatus() == HOST_STATUS.CONNECTED) {
                    evaluateTravelling();

                } else if (getCurrentStatus() == HOST_STATUS.AP) {
                    // If I am a GO, evaluate to merge my group with another one
                    evaluateMerge();
                }
            }
        }
    }


    //------------------------------------------------------------------------------------------------------------------
    // MESSAGE MENAGEMENT
    //------------------------------------------------------------------------------------------------------------------
    void sendVisibilityQuery(AutonomousHost host){

        this.hostToMerge = host;
        this.mergePositiveResponses = 0;

        if(this.getGroup() != null){

            for (DTNHost node : getGroup()){

                String messageId = VISIBILITY_QUERY_MESSAGE_ID+":"+System.currentTimeMillis()+":"+host.getAddress();
                Message message = new Message(this, node, messageId, 0);
                createNewMessage(message);
                sendMessage(messageId, node);

            }
        }
    }

    private void sendVisibilityResponse(boolean visibility, DTNHost node){

        int visible = (visibility) ? 1 : 0;

        String messageId = VISIBILITY_RESPONSE_MESSAGE_ID + ":" + System.currentTimeMillis() + ":" + visible;
        Message message = new Message(this, node, messageId, 0);
        createNewMessage(message);
        sendMessage(messageId, node);
    }

    private void sendMergeIntent(int nodeAddress){

        if(this.getGroup() != null){

            List<DTNHost> hostToNotify = new ArrayList<>();
            hostToNotify.addAll(this.getGroup());

            for(DTNHost host : hostToNotify){
                String messageId = MERGE_INTENT_MESSAGE_ID +":"+System.currentTimeMillis()+":"+nodeAddress;
                Message message = new Message(this, host, messageId, 0);
                createNewMessage(message);
                sendMessage(messageId, host);
            }
        }
    }


    void handleVisibilityQueryMessage(Message message, AutonomousHost host){

        int address = Integer.parseInt(message.getId().split(":")[2]);

        boolean visible = false;

        for(NetworkInterface nearbyInterface : getInterface().getNearbyInterfaces()){
            if(nearbyInterface.getHost().getAddress() == address){
                visible = true;
                this.hostToMerge = (AutonomousHost)nearbyInterface.getHost();
                break;
            }
        }

        sendVisibilityResponse(visible, host);
    }

    void handleVisibilityResponse(Message message, AutonomousHost host){

        int visibility = Integer.parseInt(message.getId().split(":")[2]);
        if(visibility == 1) this.mergePositiveResponses++;

        if(this.mergePositiveResponses >= (this.getGroup().size() / 2) + 1){
            sendMergeIntent(this.hostToMerge.getAddress());
            destroyGroup();
            this.getInterface().connect(this.hostToMerge.getInterface());
            Logger.print(this, "Merge to "+this.hostToMerge);
        }
    }

    void handleMergeIntent(Message message, AutonomousHost host){

        if(this.hostToMerge != null) {
            this.getInterface().connect(this.hostToMerge.getInterface());
            Logger.print(this, "Following the AP and merge to "+this.hostToMerge);
            destroyGroup();
        }
    }
}
