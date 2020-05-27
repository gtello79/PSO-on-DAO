package SRCDAO;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Random;
import java.util.SortedMap;

import javafx.util.Pair;
import source.*;

public class Beam {
    private Collimator collimator;
    private Matrix I;
    private int angle;
    private int max_apertures;
    private int max_intensity;
    private int initial_intensity;
    private int step_intensity;
    private int open_apertures;
    private int setup;
    private SortedMap<Integer, Matrix>D;
    private Vector<Vector<Pair<Integer,Integer>>> A;
    private SortedMap<Pair<Integer,Integer>, Integer> pos2beam;
    private SortedMap<Integer, Pair<Integer,Integer>> beam2pos;

    public Beam(int angle, int max_apertures, int max_intensity, int initial_intensity, int step_intensity, int open_apertures, int setup){
        this.angle = angle;
        this.max_apertures = max_apertures;
        this.max_intensity = max_intensity;
        this.initial_intensity = initial_intensity;
        this.step_intensity = step_intensity;
        this.open_apertures = open_apertures;
        this.setup = setup;

    };

    public void CalculateVelocity(double c1, double c2, double w, Beam BGlobal, Beam BPersonal){
        /*Instaurar semilla*/
        double r1 = (new Random()).nextDouble();
        double r2 = (new Random()).nextDouble();
    }
    public void CalculatePosition(){

    }

    public ArrayList<Integer> openBeamlets(int a){
        ArrayList<Integer> ob = new ArrayList<>();
        Pair<Integer,Integer> aux;

        for(int i = 0; i < collimator.getxDim(); i++){
            aux = collimator.getActiveRange(i,angle);

            if(aux.getKey()<0) continue;
            for(int j = aux.getKey();  j<=aux.getValue();j++){
                if(j >= (A.get(a)).get(i).getKey() && j <= (A.get(a)).get(i).getValue())
                    ob.add(pos2beam.get(new Pair(i,j)));
            }
        }
        return ob;
    }



    public int getId_beam() {
        return angle;
    }

    public void setId_beam(int angle) {
        this.angle = angle;
    }

    public Matrix getDepositionMatrix(int o){
        int index = 0;
        for(Matrix m: D.values()){
            if(index == o) return m;
            index++;
        }
        return null;
    }

    public int getNbApertures(){
        return max_apertures;
    }

    public int getNbBeamlets(){
        return collimator.getNangleBeamlets(angle);
    }

    public double getIntensity(int beam){
        Pair<Integer,Integer> p = getPos(beam);
        return I.getPos(p.getKey(),p.getValue());
    }

    public Pair<Integer,Integer> getPos(int index){
        if(!beam2pos.containsValue(index)){
            beam2pos.put(index,collimator.indexToPos(index,angle));
            pos2beam.put(beam2pos.get(index),index);
            return beam2pos.get(index);
        }
        else return beam2pos.get(index);
    };
}
