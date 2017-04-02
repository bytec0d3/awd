package awd;

import core.*;
import interfaces.AutonomousGroupInterface;
import movement.MovementModel;
import routing.MessageRouter;

import java.util.*;

public abstract class AutonomousHost extends DTNHost implements Comparable<DTNHost>{

    public static final String SETTINGS_NAMESPACE = "AutonomousHost";
    private static final String SETTINGS_DECISION_TIME_S = "decisionTime";
    public static final String SETTINGS_MAX_CLIENTS = "maxClients_";

    private double decisionTimeS;
    private double lastDecision = 0;

//    private static final String GROUP_INFO_MESSAGE_ID = "g";
//    private static final String GROUP_BYE_MESSAGE_ID = "gb";
//    static final String VISIBILITY_QUERY_MESSAGE_ID = "vq";
//    static final String VISIBILITY_RESPONSE_MESSAGE_ID = "vqr";
//    static final String MERGE_INTENT_MESSAGE_ID = "mi";

    public enum HOST_STATUS {CONNECTED, BUSY, AP, OFFLINE}

    private static final float RES_TO_GO_OFFLINE = 0.0f;

    private HOST_STATUS currentStatus;
    ContextManager contextManager;

    private Set<Integer> fakeMessagesCache;

    //Group infos
    private String groupName;
    private DTNHost currentAP;
    private Set<DTNHost> group;

    //Logs
    public int dischargeTime;

    boolean groupByeSent = false;

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
    public AutonomousHost(List<MessageListener> msgLs,
                          List<MovementListener> movLs,
                          String groupId,
                          List<NetworkInterface> interf,
                          ModuleCommunicationBus comBus,
                          MovementModel mmProto,
                          MessageRouter mRouterProto) {
        super(msgLs, movLs, groupId, interf, comBus, mmProto, mRouterProto);

        this.currentStatus = HOST_STATUS.AP;
        this.contextManager = new ContextManager();
        this.groupName = this.toString();
        this.currentAP = this;

        parseSettings();
    }

    private void parseSettings(){

        Settings s = new Settings(SETTINGS_NAMESPACE);
        //Common settings
        this.decisionTimeS = s.getDouble(SETTINGS_DECISION_TIME_S);
        if(getInterfaces().size() != 0) {
            if (s.contains(SETTINGS_MAX_CLIENTS + this.name))
                this.getInterface().setMaxClients(s.getInt(SETTINGS_MAX_CLIENTS + this.name));
            else
                this.getInterface().setRandomMaxClients();
        }

        if(this.getClass().getName().compareTo(CompleteAutonomousHost.class.getName()) == 0)
            ((CompleteAutonomousHost)this).parseSettings(s);
    }

    public String toString(){
        return this.name;
    }

    public AutonomousGroupInterface getInterface(){

        return (AutonomousGroupInterface) getInterface(1);
    }

    public ContextManager getContextManager(){return this.contextManager;}


    //------------------------------------------------------------------------------------------------------------------
    // UPDATE METHOD
    //------------------------------------------------------------------------------------------------------------------
    public void update(boolean simulateConnections) {

        super.update(simulateConnections);

        if(groupByeSent && this.group == null) groupByeSent = false;

        if(this.fakeMessagesCache == null){
            fakeMessagesCache = new HashSet<>(SimScenario.getInstance().getHosts().size());
            fakeMessagesCache.add(this.getAddress());
        }

        if(getInterfaces().size() != 0) {

            this.contextManager.updateContext(getCurrentStatus(), getGroup(), getInterface().getNearbyNodes());

            checkMyResources();

            if (SimClock.getTime() - lastDecision >= decisionTimeS && this.getCurrentStatus() != HOST_STATUS.OFFLINE) {
                takeDecision();
                this.lastDecision = SimClock.getIntTime();
            }
        }
    }

    boolean haveFreeSlots(){
        return (this.getGroup() == null || this.getGroup().size() < this.getInterface().getMaxClients());
    }

    //------------------------------------------------------------------------------------------------------------------
    // ABSTRACT METHODS
    //------------------------------------------------------------------------------------------------------------------
    abstract void takeDecision();
    public abstract void evaluateNearbyNodes(Collection<NetworkInterface> nodes);

    //------------------------------------------------------------------------------------------------------------------
    // CONNECTIONS
    //------------------------------------------------------------------------------------------------------------------
    /**
     * Informs the router of this host about state change in a connection
     * object.
     * @param con  The connection object whose state changed
     */
    public void connectionUp(Connection con) {
        super.connectionUp(con);

        //Logger.print(this, "CON UP: "+con.fromNode+" ---> " + con.toNode);

        //If this is an incoming connection
        if(con.fromNode != this){

            if(getGroup() != null && getGroup().size() == this.getInterface().getMaxClients()) {

                Logger.print(this, "Refuse connection from "+con.fromNode);

                if(con.fromNode instanceof CompleteAutonomousHost){
                    CompleteAutonomousHost h = (CompleteAutonomousHost)con.fromNode;
                    h.connectionFailed(this);
                }
                getInterface().disconnect(con, ((AutonomousHost) con.fromNode).getInterface());
                getInterface().connections.remove(con);

            }else {
                setCurrentStatus(HOST_STATUS.AP);
                addGroupMember(con.fromNode);
                sendGroupInfo();
            }

        } else {
            // else, I requested to connect to another host
            setCurrentStatus(HOST_STATUS.CONNECTED);
            addGroupMember(con.toNode);
            setCurrentAP(con.toNode);
        }
    }

    /**
     * Informs the router of this host about state change in a connection
     * object.
     * @param con  The connection object whose state changed
     */
    public void connectionDown(Connection con) {
        super.connectionDown(con);

        //Logger.print(this, "CON DOWN: "+con.fromNode+" -X-> " + con.toNode);

        //ConnectivityMonitor.getInstance().newDisconnection(con.fromNode.getAddress(), con.toNode.getAddress());

        switch (getCurrentStatus()){

            // I am a client member of a group
            case CONNECTED:
                clearGroup();
                setCurrentStatus(HOST_STATUS.AP);
                clearMessageQueue();
                break;

            // I am an AP of a group
            case AP:
                removeGroupMember((con.fromNode == this ? con.toNode : con.fromNode));
                sendGroupInfo();
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    // MESSAGES
    //------------------------------------------------------------------------------------------------------------------
    private void sendFakeMessages(AutonomousHost toHost){
        toHost.receiveFakeMessages(this.fakeMessagesCache);
    }
    private void receiveFakeMessages(Set<Integer> messages){
        this.fakeMessagesCache.addAll(messages);
    }
    public float getFakeMessagesCache(){return (float)this.fakeMessagesCache.size();}


    private void clearMessageQueue(){
        this.getRouter().messages = new HashMap<>();
    }

    private void sendGroupInfo(){

        if(this.getGroup() != null && !groupByeSent)
            for(DTNHost host : this.getGroup()) ((AutonomousHost)host).updateGroup(this.getGroup());
    }

    private void sendGroupBye(){
        this.groupByeSent = true;
        if(this.getGroup() != null)
            for(DTNHost host : this.getGroup())
                ((AutonomousHost)host).receiveGroupBye();
    }

    private void receiveGroupBye(){
        this.getInterface().addPreviousAPInBlacklist(this.currentAP);
    }

    //------------------------------------------------------------------------------------------------------------------
    // LOCAL SERVICE MANAGEMENT
    //------------------------------------------------------------------------------------------------------------------
    public abstract ServicePayload getService();

    //------------------------------------------------------------------------------------------------------------------
    // STATUS MANAGEMENT
    //------------------------------------------------------------------------------------------------------------------
    public HOST_STATUS getCurrentStatus(){return this.currentStatus; }

    public void setCurrentStatus(HOST_STATUS currentStatus){
        this.currentStatus = currentStatus;
        if(this.currentStatus == HOST_STATUS.AP) setGroupName(this.toString());
    }

    //------------------------------------------------------------------------------------------------------------------
    // GROUP MANAGEMENT
    //------------------------------------------------------------------------------------------------------------------
    private void setGroupName(String groupName){ this.groupName = groupName; }

    public String getGroupName(){ return this.groupName; }

    public Set<DTNHost> getGroup(){ return this.group; }

    private void updateGroup(Set<DTNHost> members){
        if(this.currentStatus != HOST_STATUS.AP) {


            //Disconnections
            for(DTNHost host : this.getGroup()){
                if(host != this && !members.contains(host) && host != this.getCurrentAP())
                ConnectivityMonitor.getInstance().newDisconnection(this.getAddress(), host.getAddress());
            }

            this.group = new HashSet<>();

            //New connections
            for (DTNHost member : members){
                if (member != this){
                    addGroupMember(member);
                    ConnectivityMonitor.getInstance().newConnection(this.getAddress(), member.getAddress());
                }
            }
            addGroupMember(this.getCurrentAP());
            ConnectivityMonitor.getInstance().newConnection(this.getAddress(), this.getCurrentAP().getAddress());

        }
    }

    private void addGroupMember(DTNHost host){

        if(this.group == null){
            this.group = new HashSet<>();

            // New group created and I am the AP of the group
            if(this.getCurrentStatus() == HOST_STATUS.AP &&
                    this.getClass().getName().compareTo(CompleteAutonomousHost.class.getName()) == 0)
                ((CompleteAutonomousHost)this).openGroupResources();
        }
        this.group.add(host);

        ConnectivityMonitor.getInstance().newConnection(this.getAddress(), host.getAddress());
        sendFakeMessages((AutonomousHost)host);
    }

    private void removeGroupMember(DTNHost host){
        if(this.group != null && this.group.contains(host)) {
            this.group.remove(host);
            ConnectivityMonitor.getInstance().newDisconnection(this.getAddress(), host.getAddress());
        }

        if(group != null && group.size() == 0){
            clearGroup();
        }
    }

    void destroyGroup(){
        if(this.getCurrentStatus() == HOST_STATUS.AP) sendGroupBye();
        this.getInterface().leaveCurrentGroup();

        if(this.getGroup() != null)
            for(DTNHost host : this.getGroup())
                ConnectivityMonitor.getInstance().newDisconnection(this.getAddress(), host.getAddress());

        clearGroup();
    }

    private void clearGroup(){
        this.group = null;

        setCurrentAP(this);
        setCurrentStatus(HOST_STATUS.AP);

        if(this.getClass().getName().compareTo(CompleteAutonomousHost.class.getName()) == 0)
            ((CompleteAutonomousHost)this).closeGroupResources();
    }

    //------------------------------------------------------------------------------------------------------------------
    // RESOURCES
    //------------------------------------------------------------------------------------------------------------------
    private void checkMyResources(){

        if(getCurrentStatus() != HOST_STATUS.OFFLINE && this.contextManager.getResources() <= RES_TO_GO_OFFLINE) {

            Logger.print(this, "Turning off Wifi");
            destroyGroup();
            getInterface().stopScan();
            setCurrentStatus(HOST_STATUS.OFFLINE);
            this.dischargeTime = SimClock.getIntTime();
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    // CURRENT_AP MANAGEMENT
    //------------------------------------------------------------------------------------------------------------------
    private void setCurrentAP(DTNHost host){
        this.currentAP = host;
        setGroupName(host.toString());
    }

    DTNHost getCurrentAP(){return this.currentAP; }


    //------------------------------------------------------------------------------------------------------------------
    // COMPARATORS
    //------------------------------------------------------------------------------------------------------------------
    static class Comparators {
        static Comparator<DTNHost> contextAndNameComparator = new Comparator<DTNHost>() {
            @Override
            public int compare(DTNHost o1, DTNHost o2) {

                CompleteAutonomousHost ah1 = (CompleteAutonomousHost) o1;
                CompleteAutonomousHost ah2 = (CompleteAutonomousHost) o2;

                int i = ah1.getService().getUtilityValue().compareTo(ah2.getService().getUtilityValue());
                if (i == 0) {
                    i = ((Integer) ah1.getAddress()).compareTo(ah2.getAddress());
                }
                return i;
            }
        };

        static Comparator<AutonomousHost> groupComparator = new Comparator<AutonomousHost>() {
            @Override
            public int compare(AutonomousHost o1, AutonomousHost o2) {

                Integer g1Size = o1.getService().getGroupMembers().size();
                Integer g2Size = o2.getService().getGroupMembers().size();

                return g1Size.compareTo(g2Size);
            }
        };

        static Comparator<AutonomousHost> addressComparator = new Comparator<AutonomousHost>() {
            @Override
            public int compare(AutonomousHost o1, AutonomousHost o2) {

                return ((Integer) o1.getAddress()).compareTo(o2.getAddress());
            }
        };
    }
}
