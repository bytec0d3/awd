package report;

import core.*;

/**
 * Created by mattia on 10/11/16.
 */
public class NodesMovementReport extends Report implements MovementListener {

    private double maxTime = 0;
    private double minX = Integer.MAX_VALUE, maxX = 0;
    private double minY = Integer.MAX_VALUE, maxY = 0;

    @Override
    public void newDestination(DTNHost host, Coord destination, double speed) {

        if(!isWarmup()) {

            double time = SimClock.getTime();

            if (time > maxTime) maxTime = time;

            if (destination.getX() < minX) minX = destination.getX();
            if (destination.getX() > maxX) maxX = destination.getX();

            if (destination.getY() < minY) minY = destination.getY();
            if (destination.getY() > maxY) maxY = destination.getY();

            write(time + " " + host.name + " " + destination.getX() + " " + destination.getY());
        }
    }

    @Override
    public void initialLocation(DTNHost host, Coord location) {
        write("0 " + host.name + " " + location.getX() + " " + location.getY());
    }

    public void done(){
        write("0 " + maxTime + " " + minX + " " + maxX + " " + minY + " " + maxY);
        super.done();
    }
}
