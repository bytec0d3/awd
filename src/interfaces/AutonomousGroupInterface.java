package interfaces;

import awd.AutonomousHost;
import awd.Logger;
import core.*;

import java.util.*;

public class AutonomousGroupInterface extends SimpleBroadcastInterface {

    //Settings fields
    private static final String BLACKLIST_PREVIOUS_AP_TIME = "blackListPreviousAPTime";
    private static final String REWIRING_PROB = "rewiringProbability";
    private static final String MAX_CLIENTS_SIZE_ENTRY = "maxClients";
    private static final String MAX_TIME_TO_CONNECT = "maxTimeToConnect";
    private static final String SCAN_INTERVAL_IN_GROUP = "scanIntervalInGroup";
    private static final String SCAN_INTERVAL_SEARCHING = "scanIntervalSearching";
    private static final String MAX_FIRST_SCAN_DELAY = "maxFirstScanDelay";

    private static final double SCAN_INTERVAL_NEVER = Double.MAX_VALUE;
    private double scanIntervalInGroup = 300;       // 5 minutes
    private double scanIntervalSearching = 60;      // 1 minute
    private int blackListPreviousAPTime = 300;      // 5 minutes
    private double maxFirstScanDelay;
    private boolean firstScanDelay = false;

    private double rewiringProbability = 0.8;

    private int maxClients = 10;

    private AutonomousHost myHost = (AutonomousHost) this.host;
    private HashMap<DTNHost, Double> blackList = new HashMap<>();

    private double scanInterval;
    private double lastScanTime;

    private Random random = new Random();

    private List<DTNHost> nearbyNodes = new ArrayList<>();

    //Lazy connect
    private double maxTimeToConnect = 0;
    private double connectAt;
    private NetworkInterface interfaceToConnect;
    private Random lazyConnectionRandom = new Random();

    /**
     * Reads the interface settings from the Settings file
     */
    public AutonomousGroupInterface(Settings s)	{

        super(s);

        readSettings(s);
    }

    private void readSettings(Settings s){
        if(s.contains(BLACKLIST_PREVIOUS_AP_TIME))
            this.blackListPreviousAPTime = s.getInt(BLACKLIST_PREVIOUS_AP_TIME);

        if(s.contains(MAX_CLIENTS_SIZE_ENTRY))
            this.maxClients = s.getInt(MAX_CLIENTS_SIZE_ENTRY);

        if(s.contains(MAX_TIME_TO_CONNECT))
            this.maxTimeToConnect = s.getDouble(MAX_TIME_TO_CONNECT);

        if(s.contains(SCAN_INTERVAL_SEARCHING))
            this.scanIntervalSearching = s.getDouble(SCAN_INTERVAL_SEARCHING);

        if(s.contains(SCAN_INTERVAL_IN_GROUP))
            this.scanIntervalInGroup = s.getDouble(SCAN_INTERVAL_IN_GROUP);

        //if(scanIntervalSearching != 0)
            //this.scanInterval = new Random().nextInt((int)scanIntervalSearching);
        if(s.contains(MAX_FIRST_SCAN_DELAY)) {
            this.firstScanDelay = true;
            this.maxFirstScanDelay = s.getDouble(MAX_FIRST_SCAN_DELAY);
            this.scanInterval = new Random().nextInt((int) maxFirstScanDelay);
        }

        if(s.contains(REWIRING_PROB))
            this.rewiringProbability = s.getDouble(REWIRING_PROB);
    }

    /**
     * Copy constructor
     * @param ni the copied network interface object
     */
    public AutonomousGroupInterface(AutonomousGroupInterface ni) {
        super(ni);

        this.blackListPreviousAPTime = ni.blackListPreviousAPTime;
        this.maxClients = ni.maxClients;
        this.maxTimeToConnect = ni.maxTimeToConnect;
        this.scanIntervalSearching = ni.scanIntervalSearching;
        this.scanIntervalInGroup = ni.scanIntervalInGroup;
        this.maxFirstScanDelay = ni.maxFirstScanDelay;
        if(ni.firstScanDelay) {
            this.firstScanDelay = true;
            this.scanInterval = new Random().nextInt((int)maxFirstScanDelay);
        }
        this.rewiringProbability = ni.rewiringProbability;
    }

    public NetworkInterface replicate()	{
        return new AutonomousGroupInterface(this);
    }

    public List<DTNHost> getNearbyNodes(){return this.nearbyNodes; }
    public int getMaxClients(){return this.maxClients;}

    /**
     * Updates the state of current connections (i.e. tears down connections
     * that are out of range and creates new ones).
     */
    public void update() {

        checkBlackListTimers();

        if(myHost == null) myHost = (AutonomousHost) this.host;

        if (optimizer == null) {
            return; /* nothing to do */
        }

        // First break the old ones
        optimizer.updateLocation(this);

        for (int i=0; i<this.connections.size(); ) {
            Connection con = this.connections.get(i);
            NetworkInterface anotherInterface = con.getOtherInterface(this);

            // all connections should be up at this stage
            assert con.isUp() : "Connection " + con + " was down!";

            if (!isWithinRange(anotherInterface)) {
                disconnect(con,anotherInterface);
                connections.remove(i);
            }
            else {
                i++;
            }
        }

        if(isScanning()) myHost.evaluateNearbyNodes(optimizer.getNearInterfaces(this));

        /*
        if(this.interfaceToConnect != null) connect(this.interfaceToConnect);
        else if(myHost.getCurrentStatus() != AutonomousHost.HOST_STATUS.OFFLINE)
            // Then find new possible connections
            if(this.simpleNearbyEvaluation)
                simpleEvaluateNearbyInterfaces(optimizer.getNearInterfaces(this));
            else
                evaluateNearbyInterfaces(optimizer.getNearInterfaces(this));*/
    }

    public Set<String> getNearbyNodesIds(){

        Set<String> nodes = new HashSet<>();

        for(NetworkInterface ni : optimizer.getNearInterfaces(this)) {
            AutonomousHost autonomousHost = (AutonomousHost)ni.getHost();
            if(isWithinRange(ni) &&
                    ni != this &&
                    autonomousHost.getCurrentStatus() != AutonomousHost.HOST_STATUS.OFFLINE)
                nodes.add(ni.getHost().name);
        }

        return nodes;
    }

    //------------------------------------------------------------------------------------------------------------------
    // SCAN INTERVAL SERVICEMENAGEMENT
    //------------------------------------------------------------------------------------------------------------------
    public void setScanIntervalWhenInGroup(){
        this.scanInterval = this.scanIntervalInGroup;
    }
    public void setScanIntervalSearching(){
        this.scanInterval = this.scanIntervalSearching;
    }
    public void stopScan(){
        this.scanInterval = SCAN_INTERVAL_NEVER;
    }


    //------------------------------------------------------------------------------------------------------------------
    // BLACK LIST SERVICEMENAGEMENT
    //------------------------------------------------------------------------------------------------------------------

    public void addPreviousAPInBlacklist(DTNHost host){
        addToBlackList(host, this.blackListPreviousAPTime);
    }

    private void addToBlackList(DTNHost host, int duration){
        this.blackList.put(host, SimClock.getTime() + duration);
    }

    private void removeFromBlackList(DTNHost host){
        if(isInBlackList(host)) this.blackList.remove(host);
    }

    private boolean isInBlackList(DTNHost host){
        return this.blackList.keySet().contains(host);
    }

    private void checkBlackListTimers(){

        List<DTNHost> keys = new ArrayList<>();
        keys.addAll(this.blackList.keySet());

        double currentTime = SimClock.getTime();

        for(DTNHost host : keys){
            if(this.blackList.get(host) <= currentTime) {
                //Logger.print(this.getHost(), "Remove " + host + " from the blacklist.");
                removeFromBlackList(host);
            }
        }
    }

    /**
     * Checks if this interface is currently in the scanning mode
     * @return True if the interface is scanning; false if not
     */
    public boolean isScanning() {
        double simTime = SimClock.getTime();

        if (!isActive()) {
            return false;
        }

        //Logger.print(getHost(), "Scan interval: "+scanInterval);

        if (scanInterval > 0.0) {
            if (simTime < lastScanTime) {
                return false; /* not time for the first scan */
            }
            else if (simTime > lastScanTime + scanInterval) {
                lastScanTime = simTime; /* time to start the next scan round */
                //myHost.scanningEvent();

                if(this.scanInterval < scanIntervalSearching)
                    setScanIntervalSearching();

                //Logger.print(getHost(), "Scanning");

                return true;
            }
            else if (simTime != lastScanTime ){
                return false; /* not in the scan round */
            }
        }

        //Logger.print(getHost(), "Scanning");
        //myHost.scanningEvent();

        return true;
    }

    public List<AutonomousHost> getAvailableGroups(Collection<NetworkInterface> nodes){

        List<AutonomousHost> groups = new ArrayList<>();

        for (NetworkInterface i : nodes) {
            AutonomousHost otherHost = (AutonomousHost)i.getHost();

            if(this != i && isWithinRange(i) &&
                    otherHost.getService().getHostStatus() == AutonomousHost.HOST_STATUS.AP &&
                    otherHost.getService().getGroupMembers().size() > 0 &&
                    otherHost.getService().getMaxClients() - otherHost.getService().getGroupMembers().size() > 0 &&
                    isScanning() && i.getHost().isRadioActive() && !isConnected(i) && !isInBlackList(otherHost))

                groups.add(otherHost);
        }

        return (groups.size() == 0) ? null : groups;
    }

    public List<AutonomousHost> getAvailableSinglets(Collection<NetworkInterface> nodes){

        List<AutonomousHost> singlets = new ArrayList<>();

        for (NetworkInterface i : nodes) {
            AutonomousHost otherHost = (AutonomousHost)i.getHost();

            if(this != i && isWithinRange(i) &&
                    otherHost.getService().getHostStatus() == AutonomousHost.HOST_STATUS.AP &&
                    otherHost.getService().getGroupMembers().size() == 0 &&
                    isScanning() && i.getHost().isRadioActive() && !isConnected(i) && !isInBlackList(otherHost))

                singlets.add(otherHost);
        }

        return (singlets.size() == 0) ? null : singlets;
    }

    private Object[] extractGroups(Collection<NetworkInterface> nodes){

        this.nearbyNodes = new ArrayList<>();

        //myHost.setCurrentStatus(AutonomousHost.HOST_STATUS.BUSY);

        HashMap<Integer, NetworkInterface> nearbyGroups = new HashMap<>();
        List<AutonomousHost> nearbySinglets = new ArrayList<>();

        for (NetworkInterface i : nodes) {

            AutonomousHost otherHost = (AutonomousHost)i.getHost();

            if(this != i && isWithinRange(i)) this.nearbyNodes.add(otherHost);

            if (otherHost.getService().getHostStatus() == AutonomousHost.HOST_STATUS.AP
                    && isScanning()
                    && i.getHost().isRadioActive()
                    && isWithinRange(i)
                    && !isConnected(i)
                    && (this != i)
                    && !isInBlackList(otherHost))
            {

                Set<DTNHost> otherGroup = otherHost.getService().getGroupMembers();

                if(otherGroup != null && otherGroup.size() > 0 && otherGroup.size() < this.maxClients)
                    nearbyGroups.put(otherGroup.size(), i);
                else if(otherGroup == null || otherGroup.size() == 0)
                    nearbySinglets.add(otherHost);
            }
        }

        return new Object[]{nearbyGroups, nearbySinglets};
    }

    private NetworkInterface getBestCandidate(HashMap<Integer, NetworkInterface> nearbyGroups, List<AutonomousHost> nearbySinglets){

        NetworkInterface bestCandidate = null;

        // If there are groups in the nearby, connect to the greatest one
        if (nearbyGroups.size() > 0) {
            SortedSet<Integer> keys = new TreeSet<>(nearbyGroups.keySet());
            bestCandidate =  nearbyGroups.get(keys.last());

        } else {
            //else, choose the best node
            nearbySinglets.add((AutonomousHost) this.host);
            Collections.sort(nearbySinglets, AutonomousHost.Comparators.contextAndNameComparator);
            AutonomousHost candidateHost = nearbySinglets.get(nearbySinglets.size() - 1);
            if (candidateHost != this.host) bestCandidate = candidateHost.getInterface();
        }

        return bestCandidate;
    }

    /**
     * Evaluate visible services.
     *
     * @param nodes     List of visible interfaces (nodes)
     */
    private void evaluateNearbyInterfaces(Collection<NetworkInterface> nodes){

        /*Object[] groups = extractGroups(nodes);
        HashMap<Integer, NetworkInterface> nearbyGroups = (HashMap<Integer, NetworkInterface>) groups[0];
        List<AutonomousHost> nearbySinglets = (List<AutonomousHost>) groups[1];

        if(this.connections.size() > 0 &&
                myHost.getCurrentStatus() == AutonomousHost.HOST_STATUS.AP
                && nearbyGroups.size() > 0){
            evaluateMarge(nearbyGroups);
        }else {
            if (rewireConnections(nearbyGroups)) return;

            if (this.connections.size() == 0) {
                NetworkInterface bestCandidate = getBestCandidate(nearbyGroups, nearbySinglets);
                if(bestCandidate != null) connect(bestCandidate);
            }
        }*/
    }

    private void simpleEvaluateNearbyInterfaces(Collection<NetworkInterface> nodes){

        /*Object[] groups = extractGroups(nodes);
        HashMap<Integer, NetworkInterface> nearbyGroups = (HashMap<Integer, NetworkInterface>) groups[0];
        List<AutonomousHost> nearbySinglets = (List<AutonomousHost>) groups[1];

        if (this.connections.size() == 0) {
            NetworkInterface bestCandidate = getBestCandidate(nearbyGroups, nearbySinglets);
            if(bestCandidate != null) connect(bestCandidate);
        }*/
    }

    /*private void evaluateMarge(HashMap<Integer, NetworkInterface> nearbyGroups){

        //Choose the AP with less clients
        SortedSet<Integer> keys = new TreeSet<>(nearbyGroups.keySet());
        NetworkInterface candidateInterface = nearbyGroups.get(keys.first());
        AutonomousHost candidateHost = (AutonomousHost) candidateInterface.getHost();

        if (candidateHost.getService().getGroupMembers().size() > myHost.getGroup().size() &&
                candidateHost.getService().getGroupMembers().size() + myHost.getGroup().size() < maxGroupSize) {
            myHost.sendVisibilityQuery(candidateHost);
        }
    }*/

    private boolean rewireConnections(HashMap<Integer, NetworkInterface> groups){
        boolean rewired = false;

        AutonomousHost myHost = (AutonomousHost) this.host;

        if(this.connections.size() > 0
                && groups.size() > 0 &&
                this.host != myHost.getCurrentAP() &&
                random.nextDouble() <= this.rewiringProbability){

            List<Integer> groupsKeys = new ArrayList<>();
            groupsKeys.addAll(groups.keySet());

            int key = groupsKeys.get(new Random().nextInt(groupsKeys.size()));
            if(groups.get(key).getHost() != myHost.getCurrentAP()){
                leaveCurrentGroup();
                connect(groups.get(key));
                Logger.print(this.host, "REWIRING");
                rewired = true;
            }

        }

        return rewired;
    }

    public void leaveCurrentGroup(){

        for (int i=0; i<this.connections.size(); ) {

            Connection con = this.connections.get(i);
            NetworkInterface anotherInterface = con.getOtherInterface(this);

            disconnect(con,anotherInterface);
            connections.remove(i);
        }

        this.setScanIntervalSearching();

    }

    /**
     * Tries to connect this host to another host. The other host must be
     * active and within range of this host for the connection to succeed.
     * @param anotherInterface The interface to connect to
     */
    public void connect(NetworkInterface anotherInterface){

        myHost.setCurrentStatus(AutonomousHost.HOST_STATUS.BUSY);

        double currentTime = SimClock.getTime();

        //Lazy connect
        if(this.interfaceToConnect == null){

            this.interfaceToConnect = anotherInterface;
            double waitTime = this.lazyConnectionRandom.nextDouble() * this.maxTimeToConnect;

            this.connectAt = currentTime + waitTime;
        }

        if(anotherInterface == interfaceToConnect && currentTime >= connectAt){
            AutonomousHost otherHost = (AutonomousHost) anotherInterface.getHost();

            if (otherHost.isRadioActive()
                    && isWithinRange(anotherInterface)
                    && !isConnected(anotherInterface)
                    && (this != anotherInterface)
                    && otherHost.getCurrentStatus() == AutonomousHost.HOST_STATUS.AP) {

                // new contact within range
                // connection speed is the lower one of the two speeds
                int conSpeed = anotherInterface.getTransmitSpeed(this);
                if (conSpeed > this.transmitSpeed) conSpeed = this.transmitSpeed;

                Connection con = new CBRConnection(this.host, this,
                        anotherInterface.getHost(), anotherInterface, conSpeed);
                connect(con, anotherInterface);
            }

            this.interfaceToConnect = null;
        }
    }

    /**
     * Just for GeoAdjencyReport
     *
     * @return  the Collection of DTNHost in the communication range
     */
    public Collection<DTNHost> getGeoNearbyNodes(){

        Collection<DTNHost> nearbyHosts = new ArrayList<>();

        for(NetworkInterface networkInterface : optimizer.getNearInterfaces(this)){

            if(isWithinRange(networkInterface) && networkInterface != this) nearbyHosts.add(networkInterface.getHost());

        }

        return nearbyHosts;

    }

}
