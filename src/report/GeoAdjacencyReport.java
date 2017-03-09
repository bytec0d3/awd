package report;

import awd.AutonomousHost;
import core.DTNHost;
import core.UpdateListener;

import java.util.*;

public class GeoAdjacencyReport extends Report implements UpdateListener {

    private HashMap<String, NodeEdges> edgeList = new HashMap<>();
    private Set<String> printedEdges = new HashSet<>();


    @Override
    public void updated(List<DTNHost> hosts) {

        if (isWarmup()) {
            return;
        }

        newEvent();

        for(DTNHost h : hosts){

            AutonomousHost host = (AutonomousHost)h;

            NodeEdges nodeEdges = new NodeEdges(host.name);

            if(this.edgeList.containsKey(host.name)) nodeEdges = this.edgeList.get(host.name);

            for(DTNHost r : host.getInterface().getGeoNearbyNodes()){
                AutonomousHost reachable = (AutonomousHost)r;
                nodeEdges.addEdge(reachable.name);
            }

            this.edgeList.put(host.name, nodeEdges);
        }
    }

    public void done() {

        setPrefix("\t"); // indent following lines by one tab

        write("<gexf xmlns=\"http://www.gexf.net/1.2draft\" version=\"1.2\">");
        write("<graph mode=\"static\" defaultedgetype=\"undirected\">");
        write("<nodes>");

        for(String node : this.edgeList.keySet())
            write("<node id=\""+node+"\" label=\""+node+"\" />");

        write("</nodes>");
        write("<edges>");
        for(String node : this.edgeList.keySet())
            write(this.edgeList.get(node).getStringEdges(this.printedEdges));
        write("</edges>");
        write("</graph>");
        write("</gexf>");

        super.done();
    }

    private class NodeEdges {

        private String node;
        private HashMap<String, Integer> edges = new HashMap<>();

        NodeEdges(String node){ this.node = node; }

        void addEdge(String otherNode){

            int edgeLabel = 1;

            if(this.edges.containsKey(otherNode))
                edgeLabel = this.edges.get(otherNode)+1;

            this.edges.put(otherNode, edgeLabel);
        }

        String getStringEdges(Set<String> printedEdges){
            String edges = "";
            for(String other : this.edges.keySet())
                if(!printedEdges.contains(node+"-"+other) && !printedEdges.contains(other+"-"+node)) {
                    edges += "<edge id=\"" + node + "-" + other + "\" source=\"" + this.node + "\" target=\"" + other +
                            "\" " + "weight=\"" + this.edges.get(other) + "\" label=\"" + this.edges.get(other) +
                            "\"/>\n";
                    printedEdges.add(node+"-"+other);
                }

            return edges;
        }
    }
}