package Swarms;

import SRCDAO.Plan;
import source.Collimator;

import java.util.ArrayList;
import java.util.Collections;
import java.text.DecimalFormat;

public class Particle extends Thread {
    int idParticle;
    private double fitness;
    private double bestFitness;
    private double bestPersonalRobust;
    private double robustFitnessValue;

    public static DecimalFormat df = new DecimalFormat("#.00");

    private ArrayList<Double> robustFitness;
    private int setupRunnerThread;
    private double c1Aperture;
    private double c2Aperture;
    private double innerAperture;
    private double cnAperture;

    private double c1Intensity;
    private double c2Intensity;
    private double innerIntensity;
    private double cnIntensity;

    private Particle bestGlobal;
    private Plan bestPersonal;
    private Plan currentPlan;
    private double beamOnTime;

    static final int MOVEMENT_THREAD = 0;
    static final int EVAL_THREAD = 1;
    static final int OPTIMIZE_THREAD = 2;
    static final int REPAIR_SOLUTION = 3;

    /*-------------------------------------------------------------METHODS -------------------------------------------*/
    public Particle(int idParticle, ArrayList<Double> w, ArrayList<Double> Zmin, ArrayList<Double> Zmax,
            ArrayList<Integer> max_apertures,
            int max_intensity, int minIntensity, int initial_intensity, int step_intensity, int open_apertures,
            int setup, Collimator collimator) {

        this.idParticle = idParticle;
        this.currentPlan = new Plan(w, Zmin, Zmax, max_apertures, max_intensity, minIntensity, initial_intensity,
                step_intensity, open_apertures, setup, collimator);

        // Update Plan Stats
        setFitness(currentPlan.getEval());
        setBeamOnTime(this.currentPlan.getBeamOnTime());
        setRobustFitness(this.currentPlan.getRobustEval());
        updateRobustFitnessValue();

        // Set Best Personal
        setBestPersonal(this.currentPlan);
        setBestFitness(currentPlan.getEval());
        setBestPersonalRobust(this.robustFitnessValue);
        
    }

    public Particle(Particle p) {
        this.idParticle = p.idParticle;
        this.fitness = p.fitness;
        this.bestFitness = p.bestFitness;
        this.beamOnTime = p.beamOnTime;
        this.robustFitness = new ArrayList<>(p.robustFitness);
        this.robustFitnessValue = p.robustFitnessValue;
        this.bestPersonalRobust = p.bestPersonalRobust;

        this.currentPlan = new Plan(p.currentPlan);
        this.bestPersonal = new Plan(p.bestPersonal);
    }

    public void evalParticle() {
        double lastFitness = this.fitness;
        double newFitness = this.currentPlan.evalFunction();

        // Update Particule Stats
        setFitness(newFitness);
        setBeamOnTime(this.currentPlan.getBeamOnTime());
        setRobustFitness(this.currentPlan.getRobustEval());
        updateRobustFitnessValue();

        CalculateBestPersonal();

        System.out.println(idParticle + ": " + df.format(lastFitness) + "\t->\t " + df.format(this.fitness) + "\t"
                + df.format(this.getRobustFitnessValue()));
    }

    public void OptimizateIntensities() {

        // Se realiza la optimizacion de intensidades
        this.currentPlan.OptimizateIntensities();
        this.evalParticle();
    }

    public void regenerateApertures() {
        double lastFitness = this.fitness;
        this.currentPlan.regenerateApertures();
        this.currentPlan.OptimizateIntensities();

        // Update Particule Stats
        setFitness(this.currentPlan.getEval());
        setBeamOnTime(this.currentPlan.getBeamOnTime());
        setRobustFitness(this.currentPlan.getRobustEval());
        updateRobustFitnessValue();

        // Actualizacion del best personal
        CalculateBestPersonal();
        System.out.println(idParticle + ": " + df.format(lastFitness) + "\t->\t " + df.format(this.fitness) + "\t"
                + df.format(this.getRobustFitnessValue()));

    }

    /*---------------------------------------------------- PSO METHODS--------------------------------------------------------------------------------*/

    public void CalculateVelocity(double c1Aperture, double c2Aperture, double wAperture, double cnAperture,
            double c1Intensity, double c2Intensity, double wIntensity, double cnIntensity, Particle bGlobal) {

        this.currentPlan.CalculateVelocity(c1Aperture, c2Aperture, wAperture, cnAperture, c1Intensity, c2Intensity,
                wIntensity, cnIntensity, bGlobal.getCurrentPlan(), bestPersonal);
    }

    public void CalculatePosition() {
        this.currentPlan.CalculatePosition();
    }

    public void CalculateBestPersonal() {
        if (this.robustFitnessValue < this.bestPersonalRobust) {
            setBestPersonal(this.currentPlan);
            setBestFitness(this.fitness);
            setBestPersonalRobust(this.robustFitnessValue);
        }
    }

    /*---------------------------------------------------- GETTER AND SETTERS ----------------------------------------------*/
    public ArrayList<Integer> getTotalUnUsedApertures() {
        return this.currentPlan.getAperturesUnUsed();
    }

    public int getAperturesUnUsed() {
        return this.currentPlan.getTotalAperturesUnsed();
    }

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

    public void setC1Aperture(double c1Aperture) {
        this.c1Aperture = c1Aperture;
    }

    public void setC2Aperture(double c2Aperture) {
        this.c2Aperture = c2Aperture;
    }

    public void setInnerAperture(double innerAperture) {
        this.innerAperture = innerAperture;
    }

    public void setCnAperture(double cnAperture) {
        this.cnAperture = cnAperture;
    }

    public void setC1Intensity(double c1Intensity) {
        this.c1Intensity = c1Intensity;
    }

    public void setC2Intensity(double c2Intensity) {
        this.c2Intensity = c2Intensity;
    }

    public void setInnerIntensity(double innerIntensity) {
        this.innerIntensity = innerIntensity;
    }

    public void setCnIntensity(double cnIntensity) {
        this.cnIntensity = cnIntensity;
    }

    public void setBestGlobal(Particle p) {
        this.bestGlobal = new Particle(p);
    }

    public void setSetupRunnerThread(int idSetup) {
        this.setupRunnerThread = idSetup;
    }

    public double setBeamOnTime(double beamOnTime) {
        return this.beamOnTime = this.currentPlan.getBeamOnTime();
    }

    public double getBeamOnTime() {
        return this.beamOnTime;
    }

    // Optimization Robusts Methods
    public void setRobustFitness(ArrayList<Double> robustFitness) {
        this.robustFitness = new ArrayList<>(robustFitness);
    }

    public ArrayList<Double> getRobustFitness() {
        return this.robustFitness;
    }

    public double getRobustFitnessValue() {
        return this.robustFitnessValue;
    }

    public double getBestPersonalRobust() {
        return bestPersonalRobust;
    }

    public void setBestPersonalRobust(double bestPersonalRobust) {
        this.bestPersonalRobust = bestPersonalRobust;
    }
    public void updateRobustFitnessValue() {
        this.robustFitnessValue = getRobustFitnessValueFromList(this.robustFitness);
    }

    public double getRobustFitnessValueFromList(ArrayList<Double> robustFitnessList){
        return Collections.max(robustFitnessList);
    }

    //---------- THREADS METHODS (NO TOCAR)
    // ----------------------------------
    @Override
    public void run() {
        switch (setupRunnerThread) {

            case MOVEMENT_THREAD:
                // Calcular velocidad
                this.CalculateVelocity(c1Aperture, c2Aperture, innerAperture, cnAperture,
                        c1Intensity, c2Intensity, innerIntensity, cnIntensity, bestGlobal);
                // Calcular posicion
                this.CalculatePosition();

                break;

            case EVAL_THREAD:
                // Evaluar particula
                this.evalParticle();
                break;

            case OPTIMIZE_THREAD:

                this.OptimizateIntensities();

                break;

            case REPAIR_SOLUTION:

                this.regenerateApertures();

                break;
        }
    }

}
