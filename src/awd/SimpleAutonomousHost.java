package awd;

import core.*;
import javafx.util.Pair;
import movement.MovementModel;
import routing.MessageRouter;

import java.util.*;

/**
 * Created by mattia on 14/02/17.
 */
public class SimpleAutonomousHost extends AutonomousHost {

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
    public SimpleAutonomousHost(List<MessageListener> msgLs, List<MovementListener> movLs,
                                String groupId, List<NetworkInterface> interf, ModuleCommunicationBus comBus,
                                MovementModel mmProto, MessageRouter mRouterProto) {

        super(msgLs, movLs, groupId, interf, comBus, mmProto, mRouterProto);
    }

    public void evaluateNearbyNodes(Collection<NetworkInterface> nodes){

        // Look for an AP just if I'm not already connected to someone
        if(getCurrentStatus() == HOST_STATUS.AP && this.getGroup() == null) {

            AutonomousHost candidate = null;

            if (nodes != null) {
                Pair<List<AutonomousHost>, List<AutonomousHost>> available = getInterface().getAvailableNodes(nodes);
                List<AutonomousHost> availableGroups = available.getKey();
                List<AutonomousHost> availableSinglets = available.getValue();

                if (availableGroups != null) {

                    availableGroups.sort(Comparators.groupComparator);
                    candidate = availableGroups.get(availableGroups.size() - 1);

                } else {

                    if (availableSinglets != null) {
                        availableSinglets.add(this);
                        availableSinglets.sort(Comparators.addressComparator);
                        candidate = availableSinglets.get(availableSinglets.size() - 1);
                    }
                }

                if (candidate != null && candidate != this && candidate.haveFreeSlots()) {
                    forceConnection(candidate, candidate.getInterface().getInterfaceType(), true);
                }
            }
        }
    }

    @Override
    void takeDecision() {
        evaluateNearbyNodes(getInterface().getNearbyInterfaces());
    }

    public ServicePayload getService(){

        return new ServicePayload(this.getCurrentStatus(), this.getGroup(), this.getInterface().getMaxClients());
    }
}
