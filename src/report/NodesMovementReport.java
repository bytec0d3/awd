package report;

import awd.AutonomousHost;
import com.sun.deploy.util.StringUtils;
import core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by mattia on 10/11/16.
 */
public class NodesMovementReport extends Report implements MovementListener {

    private double maxTime = 0;
    private double minX = Integer.MAX_VALUE, maxX = 0;
    private double minY = Integer.MAX_VALUE, maxY = 0;

    private String maxId = "";

    private HashMap<String, List<String>> positions = new HashMap<>();

    @Override
    public void newDestination(DTNHost host, Coord destination, double speed) {

        if(!isWarmup()) {

            double time = SimClock.getTime();

            if (time > maxTime) maxTime = time;

            if (destination.getX() < minX) minX = destination.getX();
            if (destination.getX() > maxX) maxX = destination.getX();

            if (destination.getY() < minY) minY = destination.getY();
            if (destination.getY() > maxY) maxY = destination.getY();

            positions.get(host.name).add(time+","+destination.getX()+","+destination.getY());
        }
    }

    @Override
    public void initialLocation(DTNHost host, Coord location) {

        if(host.name.compareTo(maxId) > 0) maxId = host.name;

        List<String> data = new ArrayList<>();
        data.add(host.name);
        data.add("0,"+location.getX()+","+location.getY());

        positions.put(host.name, data);
    }

    public void done(){
        write(maxId+" 0 " + maxTime + " " + minX + " " + maxX + " " + minY + " " + maxY);
        for(String key : positions.keySet())
            write(key+" "+StringUtils.join(positions.get(key), " "));
        super.done();
    }
}
