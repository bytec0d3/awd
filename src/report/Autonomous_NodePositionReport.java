package report;

import core.*;

import java.util.*;

/**
 * Created by mattia on 03/03/17.
 */
public class Autonomous_NodePositionReport extends Report implements MovementListener {

    private double minX = Double.MAX_VALUE;
    private double minY = Double.MAX_VALUE;
    private double maxX = 0;
    private double maxY = 0;

    private List<String> data = new ArrayList<>();

    private static String formatTime(){
        return String.format("%.2f", SimClock.getTime());
    }

    public void done() {

        write("0 "+SimClock.getIntTime()+" "+minX+" "+maxX+" "+minY+" "+maxY);

        for(String d : data) write(d);

        super.done();
    }

    @Override
    public void newDestination(DTNHost host, Coord destination, double speed) {

        if(!isWarmup()) {
            double x = host.getLocation().getX();
            double y = host.getLocation().getY();

            if(x<minX) minX = x;
            if(x>maxX) maxX = x;
            if(y<minY) minY = y;
            if(y>maxY) maxY = y;

            data.add(SimClock.getIntTime()+" "+host.name+" "+x+" "+y);

        }
    }

    @Override
    public void initialLocation(DTNHost host, Coord location) {

        double x = location.getX();
        double y = location.getY();

        if(x<minX) minX = x;
        if(x>maxX) maxX = x;
        if(y<minY) minY = y;
        if(y>maxY) maxY = y;

        data.add("0 "+host.name+" "+location.getX()+" "+location.getY());
    }
}
