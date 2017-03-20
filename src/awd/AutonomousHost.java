package awd;

import core.*;
import interfaces.AutonomousGroupInterface;
import movement.MovementModel;
import routing.MessageRouter;

import java.util.*;

public abstract class AutonomousHost extends DTNHost implements Comparable<DTNHost>{

    private static final String SETTINGS_NAMESPACE = "AutonomousHost";
    private static final String SETTINGS_DECISION_TIME_S = "decisionTime";
    public static final String SETTINGS_MAX_CLIENTS = "maxClients_";

    private double decisionTimeS;
    private double lastDecision = 0;

    private static final String GROUP_INFO_MESSAGE_ID = "g";
    private static final String GROUP_BYE_MESSAGE_ID = "gb";
    static final String VISIBILITY_QUERY_MESSAGE_ID = "vq";
    static final String VISIBILITY_RESPONSE_MESSAGE_ID = "vqr";
    static final String MERGE_INTENT_MESSAGE_ID = "mi";

    public enum HOST_STATUS {CONNECTED, BUSY, AP, OFFLINE}

    private static final float RES_TO_GO_OFFLINE = 0.0f;

    private HOST_STATUS currentStatus;
    ContextManager contextManager;

    //Group infos
    private String groupName;
    private DTNHost currentAP;
    private Set<DTNHost> group;

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

        if(getInterfaces().size() != 0) {

            this.contextManager.updateContext(getCurrentStatus(), getGroup(), getInterface().getNearbyNodes());

            checkMyResources();

            if (SimClock.getTime() - lastDecision >= decisionTimeS) {
                takeDecision();
                this.lastDecision = SimClock.getIntTime();
            }
        }
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

        //Logger.print(this, con.fromNode + " ---> " + con.toNode);

        //If this is an incoming connection
        if(con.fromNode != this){

            if(getGroup() != null && getGroup().size() == this.getInterface().getMaxClients()) {
                Logger.print(this, "Rejecting connection from "+con.fromNode);
                getInterface().disconnect(con, ((AutonomousHost) con.fromNode).getInterface());
                getInterface().connections.remove(con);
            }else {
                setCurrentStatus(HOST_STATUS.AP);
                addGroupMember(con.fromNode);
                sendGroupInfo();
            }

        }
        // else, I requested to connect to another host
        else {

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

        //Logger.print(this, con.fromNode + " -X-> " + con.toNode);

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
    private void clearMessageQueue(){
        this.getRouter().messages = new HashMap<>();
    }

    /**
     * Sends a message from this host to another host
     * @param id Identifier of the message
     * @param to Host the message should be sent to
     */
    public void sendMessage(String id, DTNHost to) {
        getRouter().sendMessage(id, to);
        getRouter().messages.clear();
    }

    /**
     * Send the list of peers connected to the AP to every member of the group
     */
    private void sendGroupInfo(){

        if(this.getGroup() != null && this.getGroup().size() != 0) {

            for (DTNHost node : group) {

                if (node != this) {
                    //Logger.print(this, "Send GI to "+node);
                    int rand = new Random().nextInt();
                    String messageId = GROUP_INFO_MESSAGE_ID+":"+System.currentTimeMillis()+":"+rand;
                    Message message = new Message(this, node, messageId, this.group.size());
                    int i = 0;
                    for (DTNHost member : group) {
                        message.addProperty(String.valueOf(i), member);
                        i++;
                    }

                    createNewMessage(message);
                    sendMessage(messageId, node);
                }
            }
        }
    }

    private void sendGroupBye(){

        if(group != null) {
            for (DTNHost node : group) {
                if (node != this) {
                    Logger.print(this, "Sending GROUP_BYE to "+node.name);
                    int rand = new Random().nextInt();
                    String messageId = GROUP_BYE_MESSAGE_ID+":"+System.currentTimeMillis()+":"+rand;
                    Message message = new Message(this, node, messageId, this.group.size());
                    createNewMessage(message);
                    sendMessage(messageId, node);
                }
            }
        }
    }

    /**
     * Start receiving a message from another host
     * @param m The message
     * @param from Who the message is from
     * @return The value returned by
     * {@link MessageRouter#receiveMessage(Message, DTNHost)}
     */
    public int receiveMessage(Message m, DTNHost from) {
        int retVal = getRouter().receiveMessage(m, from);

        if (retVal == MessageRouter.RCV_OK) {
            m.addNodeOnPath(this);	// add this node on the messages path
            getRouter().messageTransferred(m.getId(),from);

            if(m.getId().contains(GROUP_INFO_MESSAGE_ID+":"))
                handleGroupInfoMessage(m, (AutonomousHost) from);
            else if(m.getId().contains(GROUP_BYE_MESSAGE_ID+":"))
                handleGroupBye(m, (AutonomousHost)from);
            else if(m.getId().contains(VISIBILITY_QUERY_MESSAGE_ID+":"))
                handleVisibilityQueryMessage(m, (AutonomousHost)from);
            else if(m.getId().contains(VISIBILITY_RESPONSE_MESSAGE_ID +":"))
                handleVisibilityResponse(m, (AutonomousHost)from);
            else if(m.getId().contains(MERGE_INTENT_MESSAGE_ID+":"))
                handleMergeIntent(m, (AutonomousHost) from);
        }

        return retVal;
    }

    private void handleGroupInfoMessage(Message message, AutonomousHost host){

        Set<AutonomousHost> members = new HashSet<>();
        for(int i=0; i<message.getSize(); i++)
            members.add((AutonomousHost)message.getProperty(String.valueOf(i)));

        updateGroup(members);
    }

    private void handleGroupBye(Message message, AutonomousHost host){

        if(host == this.currentAP){
            Logger.print(this, "Received GROUP_BYE from the AP ("+host+")");
            this.getInterface().addPreviousAPInBlacklist(host);
        }
    }

    void handleVisibilityQueryMessage(Message message, AutonomousHost host){}
    void handleVisibilityResponse(Message message, AutonomousHost host){}
    void handleMergeIntent(Message message, AutonomousHost host){}

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

    private void updateGroup(Set<AutonomousHost> members){
        if(this.currentStatus != HOST_STATUS.AP) {
            this.group = new HashSet<>();
            for (AutonomousHost member : members) if (member != this) group.add(member);
            group.add(this.getCurrentAP());
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
    }

    private void removeGroupMember(DTNHost host){
        if(this.group != null && this.group.contains(host)) {
            this.group.remove(host);
        }

        if(group != null && group.size() == 0){
            clearGroup();
        }
    }

    void destroyGroup(){
        if(this.getCurrentStatus() == HOST_STATUS.AP) sendGroupBye();
        this.getInterface().leaveCurrentGroup();
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

        if(getCurrentStatus() != HOST_STATUS.OFFLINE && this.contextManager.getBatteryLevel() <= RES_TO_GO_OFFLINE) {

            Logger.print(this, "Turning off Wifi");
            destroyGroup();
            getInterface().stopScan();
            setCurrentStatus(HOST_STATUS.OFFLINE);
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
