package Swarms;
import SRCDAO.*;
import source.Collimator;
import source.Volumen;

import java.util.ArrayList;
import java.util.Vector;

public class Particle {
    private double fitness;
    private double bestFitness;
    private Plan bestPersonal;
    private final Plan currentPlan;

    /*-------------------------------------------------------------METHODS -------------------------------------------*/
    public Particle(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, ArrayList<Integer> max_apertures, int max_intensity, int initial_intensity, int step_intensity,
                    int open_apertures, int setup, Vector<Volumen> volumen, Collimator collimator)
    {
        this.currentPlan = new Plan(w, Zmin, Zmax, max_apertures, max_intensity,initial_intensity, step_intensity, open_apertures, setup, volumen, collimator);
        setFitness(currentPlan.getEval());

        setBestPersonal(this.currentPlan);
        setBestFitness(currentPlan.getEval());
    }

    public Particle(Particle p){
        this.currentPlan = new Plan(p.currentPlan);
        this.bestPersonal = new Plan(p.bestPersonal);
        this.fitness = p.fitness;
        this.bestFitness = p.bestFitness;

    }

    public void CalculateVelocity(double c1Aperture, double c2Aperture, double wAperture,double c1Intensity, double c2Intensity, double wIntensity, Particle bGlobal){
        currentPlan.CalculateVelocity(c1Aperture, c2Aperture, wAperture, c1Intensity, c2Intensity, wIntensity, bGlobal.getCurrentPlan(), bestPersonal);
    }

    public void CalculatePosition(){
        currentPlan.CalculatePosition();
    }

    public void CalculateBestPersonal(){
        if(this.fitness < bestFitness){
            setBestPersonal(this.currentPlan);
            setBestFitness(this.fitness);
        }
    }

    public void evalParticle(){
        this.fitness = currentPlan.eval();
    }
    /*-------------------------------------------- GETTER AND SETTERS ----------------------------------------------*/
    public double getFitness() {
        return this.fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public void setBestFitness(double bestFitness) {
        this.bestFitness = bestFitness;
    }

    public void setBestPersonal(Plan bPersonal) {
        bestPersonal = new Plan(bPersonal);
    }

    public Plan getCurrentPlan() {
        return this.currentPlan;
    }

    public void printFluenceMapByBeam(){
        currentPlan.printFluenceMapByBeam();
    }

}
