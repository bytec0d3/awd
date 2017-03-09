package awd;

import core.*;
import movement.MovementModel;
import routing.MessageRouter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by mattia on 02/03/17.
 */
public class CompleteAutonomousHost extends AutonomousHost {

    //Merge
    private AutonomousHost hostToMerge;
    private int mergePositiveResponses;


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
    }

    @Override
    public void evaluateNearbyNodes(Collection<NetworkInterface> nodes) {

    }



    //------------------------------------------------------------------------------------------------------------------
    // MESSAGE MENAGEMENT
    //------------------------------------------------------------------------------------------------------------------

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

    public void sendVisibilityQuery(AutonomousHost host){

        this.hostToMerge = host;
        this.mergePositiveResponses = 0;

        if(this.getGroup() != null){

            for (DTNHost node : getGroup()){

                String messageId = VISIBILITY_QUERY_MESSAGE_ID + ":" + System.currentTimeMillis() + ":" +host.getAddress();
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
                String messageId = MERGE_INTENT_MESSAGE_ID + ":" + System.currentTimeMillis() + ":" + String.valueOf(nodeAddress);
                Message message = new Message(this, host, messageId, 0);
                createNewMessage(message);
                sendMessage(messageId, host);
            }
        }
    }


    private void handleVisibilityQueryMessage(Message message, AutonomousHost host){

        int address = Integer.parseInt(message.getId().split(":")[2]);

        boolean visible = false;

        for(DTNHost nearbyHost : getInterface().getNearbyNodes()){
            if(nearbyHost.getAddress() == address){
                visible = true;
                this.hostToMerge = (AutonomousHost)nearbyHost;
                break;
            }
        }

        sendVisibilityResponse(visible, host);
    }

    private void handleVisibilityResponse(Message message, AutonomousHost host){

        int visibility = Integer.parseInt(message.getId().split(":")[2]);
        if(visibility == 1) this.mergePositiveResponses++;

        if(this.mergePositiveResponses >= (this.getGroup().size() / 2) + 1){
            sendMergeIntent(this.hostToMerge.getAddress());
            destroyGroup();
            this.getInterface().connect(this.hostToMerge.getInterface());
            Logger.print(this, "Merge to "+this.hostToMerge);
        }
    }

    private void handleMergeIntent(Message message, AutonomousHost host){

        if(this.hostToMerge != null) {
            this.getInterface().connect(this.hostToMerge.getInterface());
            Logger.print(this, "Following the AP and merge to "+this.hostToMerge);
            destroyGroup();
        }
    }
}
