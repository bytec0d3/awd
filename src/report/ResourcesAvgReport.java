package report;

import awd.AutonomousHost;
import core.DTNHost;
import core.SimClock;

import java.util.List;

/**
 * Created by mattia on 04/11/16.
 */
public class ResourcesAvgReport extends SnapshotReport{

    protected void createSnapshot(List<DTNHost> hosts) {

        float resourcesAvg = 0, minResources = Float.MAX_VALUE, maxResources = 0;

        for(DTNHost h : hosts){
            AutonomousHost host = (AutonomousHost)h;

            float hostResources = host.getContextManager().getBatteryLevel();

            if(minResources > hostResources) minResources = hostResources;
            if(maxResources < hostResources) maxResources = hostResources;
            resourcesAvg += hostResources;

        }

        resourcesAvg /= (float)hosts.size();

        write(String.format("%.2f", SimClock.getTime()) + " " + String.format("%.2f", minResources)
        + " " + String.format("%.2f", resourcesAvg) + " " + String.format("%.2f", maxResources));
    }

    @Override
    protected void writeSnapshot(DTNHost host) {

    }
}
