package report;

import awd.AutonomousHost;
import core.DTNHost;
import core.SimClock;
import core.UpdateListener;

import java.util.HashMap;
import java.util.List;

/**
 * Created by mattia on 06/03/17.
 */
public class Autonomous_BatteryReport extends Report implements UpdateListener {

    private HashMap<String, String> offTimes = new HashMap<>();

    @Override
    public void updated(List<DTNHost> hosts) {

        String time = formatTime();

        for(DTNHost h : hosts)
            if(((AutonomousHost)h).getCurrentStatus() == AutonomousHost.HOST_STATUS.OFFLINE && !offTimes.containsKey(h.name))
                offTimes.put(h.name, time);
    }

    public void done() {

        for(String node : offTimes.keySet()) write(node+" "+offTimes.get(node));

        super.done();
    }

    private static String formatTime(){
        return String.format("%.2f", SimClock.getTime());
    }
}
