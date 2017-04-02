package report;

import awd.AutonomousHost;
import awd.Logger;
import core.DTNHost;
import core.SimClock;
import core.UpdateListener;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mattia on 22/03/17.
 */
public class Autonomous_FakeMessagesReport extends SnapshotReport  {

    private boolean first=true;

    @Override
    public void createSnapshot(List<DTNHost> hosts) {

        if(first){
            write("0.0 0.0");
            first = false;
        }

        double avg = 0;

        for(DTNHost host : hosts){

            if(host instanceof AutonomousHost) {
                avg += (((AutonomousHost) host).getFakeMessagesCache() / (float)hosts.size());
            }
        }

        write(String.format("%.2f", SimClock.getTime()) + " " + String.format("%.2f", avg/(float)hosts.size()));
    }

    @Override
    protected void writeSnapshot(DTNHost host) {

    }
}
