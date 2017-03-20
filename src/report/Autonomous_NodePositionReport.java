package report;

import core.*;

import java.util.*;

/**
 * Created by mattia on 03/03/17.
 */
public class Autonomous_NodePositionReport extends Report implements UpdateListener, MovementListener {

    private double minX = Double.MAX_VALUE;
    private double minY = Double.MAX_VALUE;
    private double maxX = 0;
    private double maxY = 0;

    @Override
    public void updated(List<DTNHost> hosts) {

        if(!isWarmup()) {
            String time = formatTime();

            for (DTNHost host : hosts) {
                double x = host.getLocation().getX();
                double y = host.getLocation().getY();

                if(x<minX) minX = x;
                if(x>maxX) maxX = x;
                if(y<minY) minY = y;
                if(y>maxY) maxY = y;

                write(time+" "+host.name+" "+x+" "+y);
            }

        }
    }

    private static String formatTime(){
        return String.format("%.2f", SimClock.getTime());
    }

    public void done() {

        /*double maxTime = SimScenario.getInstance().getEndTime();
        double minX = 0, maxX = SimScenario.getInstance().getWorldSizeX();
        double minY = 0, maxY = SimScenario.getInstance().getWorldSizeY();

        write("0 " + maxTime + " " + minX + " " + maxX + " " + minY + " " + maxY);

        for(Loc loc : locations) write(loc.toString());*/

        String time = formatTime();

        for (DTNHost host : SimScenario.getInstance().getHosts()) {
            double x = host.getLocation().getX();
            double y = host.getLocation().getY();

            write(time+" "+host.name+" "+x+" "+y);
        }

        write("0 "+time+" "+minX+" "+maxX+" "+minY+" "+maxY);

        super.done();
    }

    @Override
    public void newDestination(DTNHost host, Coord destination, double speed) {}

    @Override
    public void initialLocation(DTNHost host, Coord location) {
        write("0 "+host.name+" "+location.getX()+" "+location.getY());
        //locations.add(new Loc("0", host.name, location.getX(), location.getY()));
    }

    class Loc{
        String time, node;
        double x,y;

        Loc(String time, String node, double x, double y){
            this.time = time;
            this.node = node;
            this.x = x;
            this.y = y;
        }

        public String toString(){
            return time+" "+node+" "+x+" "+y;
        }
    }
}
