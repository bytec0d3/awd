package report;

import awd.AutonomousHost;
import core.DTNHost;
import core.SimClock;
import util.AutonomousUtils;

import java.util.List;

public class ReachabilityAvgReport extends SnapshotReport {

    protected void createSnapshot(List<DTNHost> hosts) {
        float reachabilityAvg = 0.0f;

        for(DTNHost host : hosts)
            reachabilityAvg += getReachabilityAvg((AutonomousHost) host);

        reachabilityAvg /= (float)hosts.size();

        write("" + String.format("%.2f", SimClock.getTime()) + " "
                + String.format("%.2f", reachabilityAvg));
    }

    @Override
    protected void writeSnapshot(DTNHost host) {

    }

    private float getReachabilityAvg(AutonomousHost host){

        float jaccardIndex = 0.0f;

        //if(host.getGroup() != null && host.getInterface().getNearbyNodes() != null)
            //jaccardIndex = AutonomousUtils.jaccardIndex(host.getInterface().getNearbyNodes(), host.getGroup());

        return jaccardIndex;
    }
}
