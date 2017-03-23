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

    private List<Pair<Double, Double>> avgs = new ArrayList<>();
    private boolean first=true;

    @Override
    public void createSnapshot(List<DTNHost> hosts) {

        if(first){
            avgs.add(new Pair<>(0.0, 0.0));
            first = false;
        }

        double avg = 0;

        for(DTNHost host : hosts){

            if(host instanceof AutonomousHost) {
                avg += (((AutonomousHost) host).getFakeMessagesCache() / (float)hosts.size());
            }
        }

        avgs.add(new Pair<>(SimClock.getTime(), avg/(float)hosts.size()));
    }

    @Override
    protected void writeSnapshot(DTNHost host) {

    }

    public void done() {
        for (Pair<Double, Double> avg : avgs)
            write(String.format("%.2f", avg.getKey()) + " " + String.format("%.2f", avg.getValue()));

        super.done();
    }
}
