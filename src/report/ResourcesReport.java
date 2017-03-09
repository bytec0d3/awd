package report;

import awd.AutonomousHost;
import core.DTNHost;
import core.SimClock;

import java.util.List;

public class ResourcesReport extends SnapshotReport{

    protected void createSnapshot(List<DTNHost> hosts) {

        for(DTNHost h : hosts){
            AutonomousHost host = (AutonomousHost)h;

            write("" + String.format("%.2f", SimClock.getTime()) + " " + host.name
                    + " " + String.format("%.2f", host.getContextManager().getBatteryLevel()));

        }
    }

    @Override
    protected void writeSnapshot(DTNHost host) {

    }
}
