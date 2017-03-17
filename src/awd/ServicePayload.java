package awd;

import core.DTNHost;

import java.util.HashSet;
import java.util.Set;

public class ServicePayload {

    private AutonomousHost.HOST_STATUS hostStatus;
    private double utilityValue;
    private Set<DTNHost> groupMembers;
    private int maxClients;


    public ServicePayload(AutonomousHost.HOST_STATUS status, double utilityValue, Set<DTNHost> group, int maxClients){
        this.hostStatus = status;
        this.utilityValue = utilityValue;
        this.maxClients = maxClients;
        this.groupMembers = new HashSet<>();
        if(group != null) groupMembers = group;
    }

    public ServicePayload(AutonomousHost.HOST_STATUS status, Set<DTNHost> group){
        this.hostStatus = status;
        this.groupMembers = new HashSet<>();
        if(group != null) groupMembers = group;
    }

    //-----------------------------------------------------------------------------------------------
    // GETTERS
    //-----------------------------------------------------------------------------------------------
    public AutonomousHost.HOST_STATUS getHostStatus(){ return this.hostStatus; }
    public int getAvailableSlots() {
        return (this.groupMembers != null) ? this.maxClients - this.groupMembers.size() : this.maxClients;
    }
    public Set<DTNHost> getGroupMembers(){ return this.groupMembers; }
    public Double getUtilityValue(){return this.utilityValue;}


}
