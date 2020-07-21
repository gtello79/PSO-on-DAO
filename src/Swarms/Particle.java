package Swarms;
import SRCDAO.*;
import source.Collimator;
import source.Volumen;

import java.util.Vector;

public class Particle {
    private double fitness;
    private double Bfitness;
    private Plan BPersonal;
    private Plan Pcurrent;

    public Particle(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, int max_apertures, int max_intensity,int initial_intensity, int step_intensity,
                    int open_apertures, int setup, Vector<Volumen> volumen, Collimator collimator)
    {
        Pcurrent = new Plan(w, Zmin, Zmax, max_apertures, max_intensity,initial_intensity, step_intensity, open_apertures, setup, volumen, collimator);
        this.BPersonal = Pcurrent;
        this.fitness = Pcurrent.getEval();
        this.Bfitness = Pcurrent.getEval();
    };

    public void CalculateVelocity(double c1, double c2, double w, Particle Bglobal){
        Pcurrent.CalculateVelocity(c1, c2, w, Bglobal.getBPersonal() , BPersonal);
    };

    public void CalculatePosition(){
        Pcurrent.CalculatePosition();
    };

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public double getBfitness() {
        return Bfitness;
    }

    public void setBfitness(double bfitness) {
        Bfitness = bfitness;
    }

    public Plan getBPersonal() {
        return BPersonal;
    }

    public void setBPersonal(Plan bglobal) {
        BPersonal = bglobal;
    }

    public Plan getPcurrent() {
        return Pcurrent;
    }

    public void setPcurrent(Plan pcurrent) {
        Pcurrent = pcurrent;
    }


}
