package report;

import awd.AutonomousHost;
import core.DTNHost;
import core.SimClock;
import core.UpdateListener;

import java.util.*;

public class VisibilityReport extends Report implements UpdateListener {

    private HashMap<DTNHost, MembershipGroup> nodesMembership = new HashMap<>();

    @Override
    public void updated(List<DTNHost> hosts) {

        if(!isWarmup()){

            for(DTNHost h : hosts){

                MembershipGroup membershipGroup = new MembershipGroup();
                if(nodesMembership.containsKey(h)) membershipGroup = nodesMembership.get(h);

                membershipGroup.update(((AutonomousHost)h).getInterface().getNearbyNodes());
                nodesMembership.put(h, membershipGroup);
            }
        }
    }

    @Override
    public void done(){

        for(DTNHost host : nodesMembership.keySet()) nodesMembership.get(host).done();

        write("<gexf xmlns=\"http://www.gexf.net/1.2draft\" version=\"1.2\">");
        write("<graph mode=\"static\" defaultedgetype=\"undirected\">");
        write("<nodes>");


        for(DTNHost host : nodesMembership.keySet())
            write("<node id=\""+host+"\" label=\""+host+"\" />");

        write("</nodes>");
        write("<edges>");

        List<String> wroteEdges = new ArrayList<>();

        for(DTNHost host : nodesMembership.keySet()){

            HashMap<DTNHost, NodeReachability> edgeList = nodesMembership.get(host).getNodeEdgeList();

            for(DTNHost host1 : edgeList.keySet()){
                if(!wroteEdges.contains(host+"_"+host1) && !wroteEdges.contains(host1+"_"+host)){
                    wroteEdges.add(host+"_"+host1);
                    write("<edge id=\"" + host + "_" + host1 + "\" source=\"" + host + "\" target=\"" + host1 +
                            "\" " + "weight=\"" + edgeList.get(host1).getMillis() + "\" label=\"" +
                            edgeList.get(host1).getMillis() + "\"/>\n");
                }
            }
        }

        write("</edges>");
        write("</graph>");
        write("</gexf>");

        super.done();
    }

    private class MembershipGroup{

        private HashMap<DTNHost, NodeReachability> group = new HashMap<>();

        public void update(List<DTNHost> currentGroup){

            if(currentGroup == null) currentGroup = new ArrayList<>();

            for (DTNHost node : currentGroup) {

                NodeReachability nodeReachability = new NodeReachability();
                if (group.containsKey(node)) nodeReachability = group.get(node);
                nodeReachability.isReachable();
                group.put(node, nodeReachability);

            }

            for (DTNHost host : group.keySet()) {
                if (!currentGroup.contains(host)) group.get(host).isNotReachable();
            }

        }

        public void done(){
            for(NodeReachability nodeReachability : group.values())
                nodeReachability.isNotReachable();
        }

        public HashMap<DTNHost, NodeReachability> getNodeEdgeList(){
            return this.group;
        }
    }

    private class NodeReachability{

        private double millis;
        private Double startTime;

        void isReachable(){
            if(startTime == null) startTime = SimClock.getTime();
        }

        void isNotReachable(){
            if(startTime != null) {
                millis += SimClock.getIntTime() - startTime;
                startTime = null;
            }
        }

        double getMillis(){return this.millis;}
    }
}
