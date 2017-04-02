package awd;

import core.SimClock;
import core.SimScenario;

import java.util.*;

public class ConnectivityMonitor {

    private HashMap<String, LinkStats> data;
    private static ConnectivityMonitor instance = new ConnectivityMonitor();

    private ConnectivityMonitor(){}

    public static ConnectivityMonitor getInstance(){
        return instance;
    }

    public void newConnection(int node1, int node2){

        if(node1 != node2) {

            if (data == null) data = new HashMap<>(SimScenario.getInstance().getHosts().size());

            String key = node1 + " " + node2;
            String reverseKey = node2 + " " + node1;

            LinkStats linkStats;

            if (data.containsKey(key) || data.containsKey(reverseKey)) {

                linkStats = data.get(key);
                if (linkStats == null) {
                    linkStats = data.get(reverseKey);
                    key = reverseKey;
                }

            } else linkStats = new LinkStats();

            if (linkStats.startTime == -1) {
                linkStats.startTime = SimClock.getIntTime();
                data.put(key, linkStats);
                //System.out.println("CON MONITOR: connect "+key);
            }
        }
    }

    public void newDisconnection(int node1, int node2){

        if(node1 != node2) {

            String key = node1 + " " + node2;
            String reverseKey = node2 + " " + node1;

            LinkStats linkStats = data.get(key);
            if (linkStats == null) {
                linkStats = data.get(reverseKey);
                key = reverseKey;
            }

            if (linkStats.startTime != -1) {
                linkStats.total += (SimClock.getIntTime() - linkStats.startTime);
                linkStats.numberOfTime += 1;
                linkStats.startTime = -1;

                data.put(key, linkStats);
                //System.out.println("CON MONITOR: disconnect "+key);
            }
        }
    }

    public HashMap<String, LinkStats> getData() {
        return data;
    }

    public class LinkStats{
        public int startTime = -1, total;
        public int numberOfTime;
    }
}
