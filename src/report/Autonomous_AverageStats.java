package report;

import awd.AutonomousHost;
import core.DTNHost;
import core.SimClock;
import core.UpdateListener;
import javafx.util.Pair;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mattia on 03/03/17.
 */
public class Autonomous_AverageStats extends Report implements UpdateListener {

    private List<Pair<String, Stats>> ratioStats = new ArrayList<>();
    private List<Pair<String, Stats>> batteryStats = new ArrayList<>();

    @Override
    public void updated(List<DTNHost> hosts) {

        double[] ratios = new double[hosts.size()];
        double[] batteries = new double[hosts.size()];

        int i=0;
        for(DTNHost host : hosts){
            AutonomousHost autonomousHost = (AutonomousHost)host;

            int group = (autonomousHost.getGroup() != null) ? autonomousHost.getGroup().size() : 0;
            int nearby = autonomousHost.getInterface().getNearbyNodesIds().size();

            double ratio = (float)group / (float)nearby;

            //Logger.print(host, group+" - " + nearby + " - " + String.valueOf(ratio));
            ratios[i] = ratio;
            batteries[i] = autonomousHost.getContextManager().getBatteryLevel();
            i++;
        }
        ratioStats.add(new Pair<>(formatTime(), new Stats(ratios)));
        batteryStats.add(new Pair<>(formatTime(), new Stats(batteries)));

        //Logger.print(null, ""+s.mean);
    }

    public void done() {

        for(int i=0; i<ratioStats.size(); i++){
            String time = ratioStats.get(i).getKey();
            Stats rs = ratioStats.get(i).getValue();
            Stats bs = batteryStats.get(i).getValue();
            write(time + "," +
                    rs.mean + "," + rs.median + "," + rs.variance + "," + rs.std +
                    bs.mean + "," + bs.median + "," + bs.variance + "," + bs.std
            );
        }

        super.done();

    }

    class Stats{
        double mean, median, variance, std;

        Stats(double[] data){
            SummaryStatistics stats = new SummaryStatistics();
            for(double val : data) stats.addValue(val);

            mean = stats.getMean();
            std = stats.getStandardDeviation();
            variance = stats.getVariance();

            Median m = new Median();
            median = m.evaluate(data);
        }
    }

    private static String formatTime(){
        return String.format("%.2f", SimClock.getTime());
    }
}
