package report;

import awd.AutonomousHost;
import core.DTNHost;
import core.UpdateListener;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Autonomous_AverageStats extends Report implements UpdateListener {

    private List<Double> ratioStats = new ArrayList<>();

    @Override
    public void updated(List<DTNHost> hosts) {

        int i=0;
        for(DTNHost host : hosts){
            AutonomousHost autonomousHost = (AutonomousHost)host;

            if(host.getInterfaces().size() != 0) {
                int group = (autonomousHost.getGroup() != null) ? autonomousHost.getGroup().size() : 0;
                int nearby = autonomousHost.getInterface().getNearbyNodesIds().size();

                if(nearby != 0) ratioStats.add((double) group / (double) nearby);

            }
        }
    }

    public void done() {
        Stats s = new Stats(ratioStats);

        write(s.mean + " " + s.median + " " + s.variance + " " + s.std);

        super.done();

    }

    class Stats{
        double mean, median, variance, std;

        Stats(List<Double> d){

            double[] data = d.stream().mapToDouble(i->i).toArray();

            mean = getMean(data);
            variance = getVariance(mean, data);
            std = getStdDev(variance);

            Median m = new Median();
            median = m.evaluate(data);
        }

        private double getMean(double[] data) {
            double sum = 0.0;
            for(double a : data)
                sum += a;
            return sum/data.length;
        }

        private double getVariance(double mean, double[] data)
        {
            double temp = 0;
            for(double a :data)
                temp += (a-mean)*(a-mean);
            return temp/data.length;
        }

        private double getStdDev(double variance)
        {
            return Math.sqrt(variance);
        }
    }
}
