package Swarms;
import SRCDAO.*;
import Utils.Reporter;
import source.Collimator;
import source.Volumen;

import java.util.ArrayList;

public class Particle extends Thread{
    int idParticle;
    private double fitness;
    private double bestFitness;

    private int setupRunnerThread;
    private double c1ApertureThread;
    private double c2ApertureThread;
    private double innerApertureThread;
    private double cnApertureThread;

    private double c1IntensityThread;
    private double c2IntensityThread;
    private double innerIntensityThread;
    private double cnIntensityThread;

    private Particle bestGlobal;
    private Plan bestPersonal;
    private Plan currentPlan;

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
        setFitness(currentPlan.getEval());

        setBestPersonal(this.currentPlan);
        setBestFitness(currentPlan.getEval());

    }

    public Particle(Particle p){
        this.idParticle = p.idParticle;
        this.fitness = p.fitness;
        this.bestFitness = p.bestFitness;

        this.currentPlan = new Plan(p.currentPlan);
        this.bestPersonal = new Plan(p.bestPersonal);
    }

    public void evalParticle(){
        double lastFitness = this.fitness;
        this.fitness = currentPlan.eval();
        CalculateBestPersonal();
        System.out.println(idParticle+ ": "+ lastFitness + " -> " + this.fitness);
    }

    public void OptimizateIntensities(){
        double lastFitness = this.fitness;
        currentPlan.OptimizateIntensities();

        this.fitness = this.currentPlan.getEval();
        CalculateBestPersonal();
        System.out.println(idParticle+ ": "+ lastFitness + " -> " + this.fitness);
    }

    public void regenerateApertures(){
        double lastFitness = this.fitness;
        this.currentPlan.regenerateApertures();     // Se regenera la solucion
        currentPlan.OptimizateIntensities();        // Se optimiza las intensidades
        this.fitness = this.currentPlan.getEval();  // Se evalua la solucion

        CalculateBestPersonal();                    //Actualizacion del best personal
        System.out.println(idParticle+ ": "+ lastFitness + " -> " + this.fitness);

    }

    /*---------------------------------------------------- PSO METHODS--------------------------------------------------------------------------------*/

    public void CalculateVelocity(double c1Aperture,  double c2Aperture, double wAperture, double cnAperture,
                                  double c1Intensity, double c2Intensity, double wIntensity, double cnIntensity, Particle bGlobal){

        currentPlan.CalculateVelocity(c1Aperture, c2Aperture, wAperture, cnAperture, c1Intensity, c2Intensity, wIntensity, cnIntensity, bGlobal.getCurrentPlan(), bestPersonal);
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

    public void setC1ApertureThread(double c1ApertureThread) {
        this.c1ApertureThread = c1ApertureThread;
    }

    public void setC2ApertureThread(double c2ApertureThread) {
        this.c2ApertureThread = c2ApertureThread;
    }

    public void setInnerApertureThread(double innerApertureThread) {
        this.innerApertureThread = innerApertureThread;
    }

    public void setCnApertureThread(double cnApertureThread) {
        this.cnApertureThread = cnApertureThread;
    }

    public void setC1IntensityThread(double c1IntensityThread) {
        this.c1IntensityThread = c1IntensityThread;
    }

    public void setC2IntensityThread(double c2IntensityThread) {
        this.c2IntensityThread = c2IntensityThread;
    }

    public void setInnerIntensityThread(double innerIntensityThread) { this.innerIntensityThread = innerIntensityThread; }

    public void setCnIntensityThread(double cnIntensityThread) {
        this.cnIntensityThread = cnIntensityThread;
    }

    public void setBestGlobal(Particle p){
        this.bestGlobal = new Particle(p);
    }

    public void setSetupRunnerThread(int idSetup){
        this.setupRunnerThread=idSetup;
    }

    // --------------------------------------- THREADS METHODS (NO TOCAR) ----------------------------------
    @Override
    public void run(){
        switch (setupRunnerThread){

            case MOVEMENT_THREAD:
                // Calcular velocidad
                this.CalculateVelocity(c1ApertureThread, c2ApertureThread, innerApertureThread, cnApertureThread,
                        c1IntensityThread, c2IntensityThread, innerIntensityThread, cnIntensityThread, bestGlobal);
                // Calcular posicion
                this.CalculatePosition();

                break;

            case EVAL_THREAD:
                // Evaluar particula
                this.evalParticle();
                break;

            case OPTIMIZE_THREAD:
                if(this.idParticle == 0){
                    //Reporter r = new Reporter(this, 6);
                    //System.out.println("Movement 1 "+ r.getUID());
                }

                this.OptimizateIntensities();

                if(this.idParticle == 0){
                    //Reporter r = new Reporter(this, 6);
                    //System.out.println("Optimization "+ r.getUID());
                }
                break;

            case REPAIR_SOLUTION:

                if(this.idParticle == 0){
                    //Reporter r = new Reporter(this, 9);
                    //System.out.println("Movement 2 "+ r.getUID());
                }

                this.regenerateApertures();

                if(this.idParticle == 0){
                    //Reporter r = new Reporter(this, 6);
                    //System.out.println("Reparacion "+ r.getUID());
                }
                break;
        }
    }

}
