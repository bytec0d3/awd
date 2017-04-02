package awd;

/**
 * Created by mattia on 29/03/17.
 */
public class ExecTime {

    private static final long TH_TIME = 60*1000;

    private static ExecTime instance = new ExecTime();

    private double updateContextAvg = 0;
    private int updateContextCounter = 0;

    private double evalNearbyNodesAvg = 0;
    private int evalNearbyNodesCounter = 0;

    private double evalTravellingAvg = 0;
    private int evalTravellingCounter = 0;

    private double evalMergeAvg = 0;
    private int evalMergeCounter = 0;

    private double takeDecisionAvg = 0;
    private int takeDecisionCounter = 0;

    private ExecTime(){

        /*new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    while(true) {
                        Thread.sleep(TH_TIME);
                        printAvgs();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();*/

    }

    public static ExecTime getInstance(){
        return instance;
    }

    public void setUpdateContextAvg(long time){
        updateContextAvg = ((this.updateContextAvg * this.updateContextCounter) + time) / (++ this.updateContextCounter);
    }

    public void setEvalNearbyNodesAvg(long time){
        evalNearbyNodesAvg = ((this.evalNearbyNodesAvg * this.evalNearbyNodesCounter) + time) / (++ this.evalNearbyNodesCounter);
    }

    public void setEvalTravellingAvg(long time){
        evalTravellingAvg = ((this.evalTravellingAvg * this.evalTravellingCounter) + time) / (++ this.evalTravellingCounter);
    }

    public void setEvalMergeAvg(long time){
        evalMergeAvg = ((this.evalMergeAvg * this.evalMergeCounter) + time) / (++ this.evalMergeCounter);
    }

    public void setTakeDecisionAvg(long time){
        takeDecisionAvg = ((this.takeDecisionAvg * this.takeDecisionCounter) + time) / (++ this.takeDecisionCounter);
    }

    private void printAvgs(){

        System.out.println("Update context avg : "+ this.updateContextAvg);
        System.out.println("Eval nearby nodes avg : "+ this.evalNearbyNodesAvg);
        System.out.println("Eval travelling avg : "+ this.evalTravellingAvg);
        System.out.println("Eval merge avg : "+ this.evalMergeAvg);
        System.out.println("Take decision avg : "+ this.takeDecisionAvg);

        this.updateContextAvg = 0;
        this.updateContextCounter = 0;

        this.evalNearbyNodesAvg = 0;
        this.evalNearbyNodesCounter = 0;

        this.evalTravellingAvg = 0;
        this.evalTravellingCounter = 0;

        this.evalMergeAvg = 0;
        this.evalMergeCounter = 0;

        this.takeDecisionAvg = 0;
        this.takeDecisionCounter = 0;

    }




}
