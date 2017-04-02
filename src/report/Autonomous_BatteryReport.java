package report;

import awd.AutonomousHost;
import core.DTNHost;
import core.SimScenario;

/**
 * Created by mattia on 06/03/17.
 */
public class Autonomous_BatteryReport extends Report {

    public void done() {

        for(DTNHost h : SimScenario.getInstance().getHosts()){

            AutonomousHost host = (AutonomousHost)h;
            write(Integer.toString(host.dischargeTime));

        }

        super.done();
    }
}
