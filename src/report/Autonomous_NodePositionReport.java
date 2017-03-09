package report;

import core.*;

import java.util.*;

/**
 * Created by mattia on 03/03/17.
 */
public class Autonomous_NodePositionReport extends Report implements UpdateListener, MovementListener {

    private List<Loc> locations = new ArrayList<>();

    @Override
    public void updated(List<DTNHost> hosts) {

        if(!isWarmup()) {
            for (DTNHost host : hosts) {
                locations.add(new Loc(formatTime(), host.name, host.getLocation().getX(), host.getLocation().getY()));
            }

        }
    }

    private static String formatTime(){
        return String.format("%.2f", SimClock.getTime());
    }

    public void done() {

        double maxTime = SimScenario.getInstance().getEndTime();
        double minX = 0, maxX = SimScenario.getInstance().getWorldSizeX();
        double minY = 0, maxY = SimScenario.getInstance().getWorldSizeY();

        write("0 " + maxTime + " " + minX + " " + maxX + " " + minY + " " + maxY);

        for(Loc loc : locations) write(loc.toString());

        super.done();
    }

    @Override
    public void newDestination(DTNHost host, Coord destination, double speed) {}

    @Override
    public void initialLocation(DTNHost host, Coord location) {
        locations.add(new Loc("0", host.name, location.getX(), location.getY()));
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
