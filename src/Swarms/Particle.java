package Swarms;
import SRCDAO.*;
import source.Collimator;
import source.Volumen;

import java.util.ArrayList;

public class Particle extends Thread{
    int idParticle;
    private double fitness;
    private double bestFitness;

    private int setupRunnerThread;
    private double c1Aperture;
    private double c2Aperture;
    private double innerAperture;
    private double cnAperture;

    private double c1Intensity;
    private double c2Intensity;
    private double innerIntensity;
    private double cnIntensity;

    // PSO Stadistics for movement
    private Particle bestGlobal;
    private Plan bestPersonal;
    private Plan currentPlan;
    private double beamOnTime;
    private int unsedApertures;

    static final int MOVEMENT_THREAD = 0;
    static final int EVAL_THREAD = 1;
    static final int OPTIMIZE_THREAD = 2;
    static final int REPAIR_SOLUTION = 3;

    /*-------------------------------------------------------------METHODS -------------------------------------------*/
    public Particle(int idParticle, ArrayList<Double> w, ArrayList<Double> Zmin, ArrayList<Double> Zmax, ArrayList<Integer> max_apertures,
                    int max_intensity, int minIntensity, int initial_intensity, int step_intensity,  int open_apertures, int setup,
                    ArrayList<Volumen> volumen, Collimator collimator)
    {
        this.idParticle = idParticle;
        this.currentPlan = new Plan(w, Zmin, Zmax, max_apertures, max_intensity, minIntensity, initial_intensity, step_intensity,
                                    open_apertures, setup, volumen, collimator);

        // Update Plan Stats
        this.fitness = this.currentPlan.getEval();
        this.updateBeamOnTime();
        this.updateUnUserAperture();
        
        // Set Best Personal
        setBestPersonal(this.currentPlan);
        setBestFitness(this.fitness);
    }

    public Particle(Particle p){
        this.idParticle = p.idParticle;
        this.fitness = p.fitness;
        this.bestFitness = p.bestFitness;
        this.beamOnTime = p.beamOnTime;

        this.currentPlan = new Plan(p.currentPlan);
        this.bestPersonal = new Plan(p.bestPersonal);
    }

    public void evalParticle(){
        double lastFitness = this.fitness;
        this.fitness = this.currentPlan.eval();

        // Update Plan Stats
        this.updateBeamOnTime();
        this.updateUnUserAperture();
        
        //Actualizacion del best personal
        CalculateBestPersonal();

        System.out.println(idParticle+ ": "+ lastFitness + " -> " + this.fitness);

    }

    public void OptimizateIntensities(){

        // Se realiza la optimizacion de intensidades
        this.currentPlan.OptimizateIntensities();

        this.evalParticle();
    }

    public void regenerateApertures(){
        double lastFitness = this.fitness;
        this.currentPlan.regenerateApertures();
        this.currentPlan.OptimizateIntensities();
        this.fitness = this.currentPlan.getEval();
        //Actualizacion del best personal
        CalculateBestPersonal();
        System.out.println(idParticle+ ": "+ lastFitness + " -> " + this.fitness);

    }

    /*---------------------------------------------------- PSO METHODS--------------------------------------------------------------------------------*/

    public void CalculateVelocity(double c1Aperture,  double c2Aperture, double wAperture, double cnAperture,
                                  double c1Intensity, double c2Intensity, double wIntensity, double cnIntensity, Particle bGlobal){

        currentPlan.CalculateVelocity(c1Aperture, c2Aperture, wAperture, cnAperture, c1Intensity, c2Intensity,
                                        wIntensity, cnIntensity, bGlobal.getCurrentPlan(), bestPersonal);
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

    /*---------------------------------------------------- GETTER AND SETTERS ----------------------------------------------*/
    public ArrayList<Integer> getTotalUnUsedApertures(){
        return this.currentPlan.getAperturesUnUsed();
    }

    public int getAperturesUnUsed(){
        return this.currentPlan.getTotalAperturesUnsed();
    }

    public double getFitness() {
        return this.fitness;
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

    public void setC1ApertureThread(double c1Aperture) {
        this.c1Aperture = c1Aperture;
    }

    public void setC2ApertureThread(double c2Aperture) {
        this.c2Aperture = c2Aperture;
    }

    public void setInnerApertureThread(double innerAperture) {
        this.innerAperture = innerAperture;
    }

    public void setCnApertureThread(double cnAperture) {
        this.cnAperture = cnAperture;
    }

    public void setC1IntensityThread(double c1Intensity) {
        this.c1Intensity= c1Intensity;
    }

    public void setC2IntensityThread(double c2Intensity) {
        this.c2Intensity = c2Intensity;
    }

    public void setInnerIntensityThread(double innerIntensity) { 
        this.innerIntensity = innerIntensity; 
    }

    public void setCnIntensityThread(double cnIntensity) {
        this.cnIntensity = cnIntensity;
    }

    public void setBestGlobal(Particle p){
        this.bestGlobal = new Particle(p);
    }

    public void setSetupRunnerThread(int idSetup){
        this.setupRunnerThread=idSetup;
    }

    public void updateBeamOnTime(){
        this.beamOnTime = this.currentPlan.getBeamOnTime();
    }

    public void updateUnUserAperture(){
        this.unsedApertures = this.currentPlan.getTotalAperturesUnsed();
    }

    public double getBeamOnTime(){ 
        return this.beamOnTime;
    }

    // --------------------------------------- THREADS METHODS (NO TOCAR) ----------------------------------
    @Override
    public void run(){
        switch (setupRunnerThread){

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
