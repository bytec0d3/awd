package report;

import awd.AutonomousHost;
import core.DTNHost;
import core.SimClock;
import core.UpdateListener;

import java.util.*;

/**
 * Created by mattia on 03/03/17.
 */
public class Autonomous_ConnectivityGraphReport extends Report implements UpdateListener {

    private HashMap<String, HashMap<String, NodeStats>> nodesData = new HashMap<>();

    private Set<String> printedEdges = new HashSet<>();
    private boolean initialized = false;

    private void genKeys(List<DTNHost> hosts){

        for(DTNHost host1 : hosts){

            HashMap<String, NodeStats> stats = new HashMap<>();

            for(DTNHost host2 : hosts){
                if(host1 != host2) stats.put(host2.name, new NodeStats());
            }

            nodesData.put(host1.name, stats);
        }

    }

    @Override
    public void updated(List<DTNHost> hosts) {

        if(!initialized){
            genKeys(hosts);
            initialized = true;
        }

        for(DTNHost host : hosts){

            AutonomousHost h = (AutonomousHost)host;

            HashMap<String, NodeStats> stats = new HashMap<>();
            if(nodesData.keySet().contains(h.name)) stats = nodesData.get(h.name);

            logNodesInGroup(h, stats);

            checkNodesNotInGroup(h, stats);
        }

    }

    private void logNodesInGroup(AutonomousHost h, HashMap<String, NodeStats> stats){

        if(h.getGroup() != null) {
            for (DTNHost hostInGroup : h.getGroup()) {
                if (hostInGroup != h) {

                    NodeStats ns = new NodeStats();

                    if(stats.containsKey(hostInGroup.name)) ns = stats.get(hostInGroup.name);

                    if (!stats.containsKey(hostInGroup.name) || (stats.containsKey(hostInGroup.name) && stats.get(hostInGroup.name).startTime == 0)) {
                        ns.startTime = SimClock.getTime();
                        stats.put(hostInGroup.name, ns);
                    }
                }
            }
        }

        nodesData.put(h.name, stats);
    }

    private void checkNodesNotInGroup(AutonomousHost h, HashMap<String, NodeStats> stats){

        Set<String> hostGroup = getHostGroup(h.getGroup());

        for(String nodeInStat : stats.keySet()){

            if(!hostGroup.contains(nodeInStat)) {
                NodeStats ns = stats.get(nodeInStat);

                if (ns.startTime != 0) {
                    ns.total += (SimClock.getTime() - ns.startTime);
                    ns.startTime = 0;
                    ns.numberOfTime+=1;
                }

                stats.put(nodeInStat, ns);
            }
        }
    }

    private void finish(){

        for(String node : nodesData.keySet()){

            Iterator it = nodesData.get(node).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();

                String otherNode = (String)pair.getKey();
                NodeStats ns = (NodeStats) pair.getValue();

                if (ns.startTime != 0) {
                    ns.total += (SimClock.getTime() - ns.startTime);
                    ns.startTime = 0;
                    ns.numberOfTime+=1;
                }

                nodesData.get(node).put(otherNode, ns);
            }
        }

    }

    private Set<String> getHostGroup(Set<DTNHost> group){

        Set<String> groupNames = new HashSet<>();

        if(group != null)
            for(DTNHost host : group)
                groupNames.add(host.name);

        return groupNames;
    }

    public void done() {

        finish();

        for(String node : nodesData.keySet()){

            HashMap<String, NodeStats> nsHm = nodesData.get(node);

            for(String otherNode : nsHm.keySet()){

                NodeStats ns = nsHm.get(otherNode);

                if(!printedEdges.contains(otherNode+"-"+node)) {

                    double avg = (ns.numberOfTime == 0) ? 0 : ns.total / ns.numberOfTime;

                    write(node + " " + otherNode + " " + String.format("%.2f", ns.total) + " "
                            + String.format("%.2f", avg));

                    printedEdges.add(node+"-"+otherNode);
                }
            }
        }

        super.done();

    }

    private static String formatTime(){
        return String.format("%.2f", SimClock.getTime());
    }


    class NodeStats{
        double startTime, total;
        int numberOfTime;
    }
}