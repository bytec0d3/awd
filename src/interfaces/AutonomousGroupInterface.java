package interfaces;

import awd.AutonomousHost;
import core.*;
import util.RandomGen;

import java.util.*;

public class AutonomousGroupInterface extends SimpleBroadcastInterface {

    //Settings fields
    private static final String BLACKLIST_PREVIOUS_AP_TIME = "blacklistTime";
    private static final String SETTINGS_GENERAL_MAX_CLIENTS = "generalMaxClients";
    private static final String MAX_TIME_TO_CONNECT = "maxTimeToConnect";
    private static final String MAX_FIRST_SCAN_DELAY = "maxFirstScanDelay";

    private static final double SCAN_INTERVAL_NEVER = Double.MAX_VALUE;
    private int blackListPreviousAPTime;
    private double maxFirstScanDelay;
    private boolean firstScanDelay = false;
    private double timeToStartScan = 0;

    private int generalMinClients;
    private int generalMaxClients;
    private int maxClients;

    private AutonomousHost myHost = (AutonomousHost) this.host;
    private Collection<NetworkInterface> nearbyInterfaces;
    private HashMap<DTNHost, Double> blackList = new HashMap<>();

    private double scanInterval;
    private double lastScanTime;

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

        this.blackListPreviousAPTime = s.getInt(BLACKLIST_PREVIOUS_AP_TIME);

        int[] maxClientsRange = s.getCsvInts(SETTINGS_GENERAL_MAX_CLIENTS);
        this.generalMinClients = maxClientsRange[0];
        this.generalMaxClients = maxClientsRange[1];

        if(s.contains(MAX_TIME_TO_CONNECT))
            this.maxTimeToConnect = s.getDouble(MAX_TIME_TO_CONNECT);

        if(s.contains(MAX_FIRST_SCAN_DELAY)) {
            this.firstScanDelay = true;
            this.maxFirstScanDelay = s.getDouble(MAX_FIRST_SCAN_DELAY);
            this.timeToStartScan = new Random().nextInt((int) maxFirstScanDelay);
        }
    }

    /**
     * Copy constructor
     * @param ni the copied network interface object
     */
    public AutonomousGroupInterface(AutonomousGroupInterface ni) {
        super(ni);

        this.blackListPreviousAPTime = ni.blackListPreviousAPTime;
        this.generalMinClients = ni.generalMinClients;
        this.generalMaxClients = ni.generalMaxClients;
        this.maxTimeToConnect = ni.maxTimeToConnect;
        this.maxFirstScanDelay = ni.maxFirstScanDelay;
        if(ni.firstScanDelay) {
            this.firstScanDelay = true;
            this.timeToStartScan = new Random().nextInt((int)maxFirstScanDelay);
        }
    }

    public NetworkInterface replicate()	{
        return new AutonomousGroupInterface(this);
    }

    public Collection<NetworkInterface> getNearbyInterfaces(){return this.nearbyInterfaces; }
    public List<Integer> getNearbyNodes(){
        List<Integer> hosts = new ArrayList<>();

        if(nearbyInterfaces != null)
            for(NetworkInterface nI : this.nearbyInterfaces) hosts.add(nI.getHost().getAddress());

        return hosts;
    }
    public Collection<DTNHost> getNearbyHosts(){

        Collection<DTNHost> nearbyHosts = new ArrayList<>();

        if(this.nearbyInterfaces != null) {
            for (NetworkInterface networkInterface : this.nearbyInterfaces) {
                nearbyHosts.add(networkInterface.getHost());
            }
        }

        return nearbyHosts;
    }
    public int getMaxClients(){return this.maxClients;}
    public void setMaxClients(int maxClients){this.maxClients = maxClients;}
    public void setRandomMaxClients(){
        this.maxClients = RandomGen.getRandomIntInRange(this.generalMinClients, this.generalMaxClients);
    }

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

        if(isScanning()){
            this.nearbyInterfaces = new ArrayList<>();

            for(NetworkInterface ni : optimizer.getNearInterfaces(this)){
                AutonomousHost autonomousHost = (AutonomousHost)ni.getHost();
                if(isWithinRange(ni) &&
                        ni != this &&
                        autonomousHost.getCurrentStatus() != AutonomousHost.HOST_STATUS.OFFLINE)
                    this.nearbyInterfaces.add(ni);
            }
        }
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
    // SCAN INTERVAL
    //------------------------------------------------------------------------------------------------------------------
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

        if(SimClock.getTime() <= this.timeToStartScan) return false;

        //Logger.print(getHost(), "Scan interval: "+scanInterval);

        if (scanInterval > 0.0) {
            if (simTime < lastScanTime) {
                return false; /* not time for the first scan */
            }
            else if (simTime > lastScanTime + scanInterval) {
                lastScanTime = simTime; /* time to start the next scan round */
                //myHost.scanningEvent();

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
                    otherHost.getService().getAvailableSlots() > 0 &&
                    isScanning() && i.getHost().isRadioActive() && !isInBlackList(otherHost))

                groups.add(otherHost);
        }

        return (groups.size() == 0) ? null : groups;
    }

    public List<AutonomousHost> getAvailableGOs(Collection<NetworkInterface> nodes){

        List<AutonomousHost> groups = new ArrayList<>();

        for (NetworkInterface i : nodes) {
            AutonomousHost otherHost = (AutonomousHost)i.getHost();

            if(this != i && isWithinRange(i) &&
                    otherHost.getService().getHostStatus() == AutonomousHost.HOST_STATUS.AP &&
                    otherHost.getService().getAvailableSlots() > 0 &&
                    isScanning() && i.getHost().isRadioActive() && !isInBlackList(otherHost))

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

    public void leaveCurrentGroup(){

        for (int i=0; i<this.connections.size(); ) {

            Connection con = this.connections.get(i);
            NetworkInterface anotherInterface = con.getOtherInterface(this);

            disconnect(con,anotherInterface);
            connections.remove(i);
        }
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
