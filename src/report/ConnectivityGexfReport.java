package report;

import awd.AutonomousHost;
import core.ConnectionListener;
import core.DTNHost;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ConnectivityGexfReport extends Report implements ConnectionListener {

    private HashMap<String, NodeEdges> edgeList = new HashMap<>();
    private Set<String> printedEdges = new HashSet<>();

    public void hostsConnected(DTNHost host1, DTNHost host2) {
        if (isWarmup()) {
            return;
        }

        newEvent();

        AutonomousHost h1 = (AutonomousHost)host1;
        AutonomousHost h2 = (AutonomousHost)host2;

        NodeEdges nodeEdges = new NodeEdges(h1.name);

        if(this.edgeList.containsKey(h1.name)) nodeEdges = this.edgeList.get(h1.name);

        nodeEdges.addEdge(h2.name);

        this.edgeList.put(h1.name, nodeEdges);
    }

    // 	Nothing to do here..
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {}

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

        public String getStringEdges(Set<String> printedEdges){
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