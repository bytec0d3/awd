package report;

import awd.AutonomousHost;
import core.DTNHost;
import core.SimScenario;

/**
 * Created by mattia on 17/03/17.
 */
public class Autonomous_MaxClientsReport extends Report {

    @Override
    public void done() {

        for(DTNHost h : SimScenario.getInstance().getHosts()){

            if(h.getInterfaces().size() != 0) {
                AutonomousHost host = (AutonomousHost) h;
                write(AutonomousHost.SETTINGS_NAMESPACE + "." + AutonomousHost.SETTINGS_MAX_CLIENTS + host.name
                        + "=" + host.getInterface().getMaxClients());
            }
        }

        super.done();
    }
}
