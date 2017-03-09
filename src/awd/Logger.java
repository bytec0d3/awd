package awd;

import core.DTNHost;
import core.SimClock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Logger {


    public static void print(DTNHost node, String event){

        System.out.println(formatTime() + " " +node+ " " +event);

    }

    public static void printMembership(DTNHost h){

        AutonomousHost source = (AutonomousHost) h;

        if(source.getGroup() != null){

            for(DTNHost m : source.getGroup()){

                AutonomousHost member = (AutonomousHost)m;

                if(!member.getGroup().contains(source))
                    Logger.print(source, "MORTE MORTE MORTE MORTE MORTE MORTE MORTE");
            }

        }

        AutonomousHost host = (AutonomousHost)h;

        List<String> members = new ArrayList<>();
        if(host.getGroup() != null){
            for(DTNHost member : host.getGroup()) members.add(member.name);
        }

        System.out.println(formatTime() + " " +h+ " " + Arrays.toString(members.toArray()));
    }

    private static String formatTime(){
        return String.format("%.2f", SimClock.getTime());
    }
}
