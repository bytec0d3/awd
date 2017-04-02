package report;

import awd.ConnectivityMonitor;
import core.SimClock;

import java.util.*;

/**
 * Created by mattia on 03/03/17.
 */
public class Autonomous_ConnectivityGraphReport extends Report {

    @Override
    public void done() {

        HashMap<String, ConnectivityMonitor.LinkStats> data = ConnectivityMonitor.getInstance().getData();

        for(String link : data.keySet()){

            if(data.get(link).startTime != -1){

                data.get(link).total += (SimClock.getIntTime() - data.get(link).startTime);
                data.get(link).numberOfTime+=1;
                data.get(link).startTime = -1;
            }

            write(link+" "+data.get(link).total + " " + (float)data.get(link).total / (float)data.get(link).numberOfTime);
        }

        super.done();
    }
}
