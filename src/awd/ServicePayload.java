package awd;

import core.DTNHost;

import java.util.HashSet;
import java.util.Set;

public class ServicePayload {

    private AutonomousHost.HOST_STATUS hostStatus;
    private DTNHost currentAp;
    private float resources;
    private Set<DTNHost> groupMembers;
    private int maxClients;


    public ServicePayload(AutonomousHost.HOST_STATUS status, DTNHost currentAp, float resources,
                          Set<DTNHost> group, int maxClients){
        this.hostStatus = status;
        this.currentAp = currentAp;
        this.resources = resources;
        this.maxClients = maxClients;
        groupMembers = new HashSet<>();
        if(group != null) groupMembers = group;
    }

    //-----------------------------------------------------------------------------------------------
    // GETTERS
    //-----------------------------------------------------------------------------------------------
    public AutonomousHost.HOST_STATUS getHostStatus(){ return this.hostStatus; }
    public DTNHost getCurrentAp(){ return this.currentAp; }
    public float getResources(){ return this.resources; }
    public int getMaxClients(){return this.maxClients;}
    public Set<DTNHost> getGroupMembers(){ return this.groupMembers; }


}
