package report;

import awd.CompleteAutonomousHost;
import core.DTNHost;
import core.SimScenario;

/**
 * Created by mattia on 18/03/17.
 */
public class Autonomous_MergeTravels extends Report {

    public void done() {

        int totalTravels = 0, totalMerges = 0;

        for(DTNHost h : SimScenario.getInstance().getHosts()){

            if(h instanceof CompleteAutonomousHost) {

                totalTravels += ((CompleteAutonomousHost) h).getTravelling();
                totalMerges += ((CompleteAutonomousHost) h).getMerges();
            }
        }

        write(totalTravels+" "+totalMerges);

        super.done();
    }
}
