package report;

import awd.AutonomousHost;
import core.DTNHost;
import core.SimScenario;

/**
 * Created by mattia on 18/03/17.
 */
public class Autonomous_EndSimBatteryStatus extends Report {

    public void done() {

        for(DTNHost h : SimScenario.getInstance().getHosts()){
            write(h+" "+((AutonomousHost)h).getContextManager().getBatteryLevel());
        }

        super.done();
    }
}
