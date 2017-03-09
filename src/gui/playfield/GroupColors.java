package gui.playfield;

import java.awt.*;
import java.util.HashMap;
import java.util.Random;

public class GroupColors {

    private static GroupColors instance;

    private HashMap<String, Color> groupColors;

    private GroupColors(){
        this.groupColors = new HashMap<>();
    }

    public static GroupColors getInstance(){
        if(instance == null) instance = new GroupColors();
        return instance;
    }

    public Color getGroupColor(String group){

        if(!this.groupColors.containsKey(group)){
            this.groupColors.put(group, getRandomColor());
        }

        return this.groupColors.get(group);
    }

    private Color getRandomColor(){

        return new Color(new Random().nextInt(255),
                new Random().nextInt(255),
                new Random().nextInt(255),
                100);

    }
}
