package Swarms;
import SRCDAO.*;
import source.Collimator;
import source.Volumen;

import java.util.ArrayList;

public class Particle extends Thread{
    int idParticle;
    private double fitness;
    private double bestFitness;
    private Plan bestPersonal;
    private Plan currentPlan;
    private Particle bestGlobal;
    private int setupRunnerThread;
    private double c1ApertureThread;
    private double c2ApertureThread;
    private double innerApertureThread;
    private double cnApertureThread;

    private double c1IntensityThread;
    private double c2IntensityThread;
    private double innerIntensityThread;
    private double cnIntensityThread;

    static final int MOVEMENT_THREAD = 0;
    static final int EVAL_THREAD = 1;

    /*-------------------------------------------------------------METHODS -------------------------------------------*/
    public Particle(int idParticle, ArrayList<Double> w, ArrayList<Double> Zmin, ArrayList<Double> Zmax, ArrayList<Integer> max_apertures,
                    int max_intensity, int initial_intensity, int step_intensity,  int open_apertures, int setup,
                    ArrayList<Volumen> volumen, Collimator collimator)
    {
        this.idParticle = idParticle;
        this.currentPlan = new Plan(w, Zmin, Zmax, max_apertures, max_intensity,initial_intensity, step_intensity,
                                    open_apertures, setup, volumen, collimator);
        setFitness(currentPlan.getEval());

        setBestPersonal(this.currentPlan);
        setBestFitness(currentPlan.getEval());
        setupRunnerThread = 0;
    }

    public Particle(Particle p){
        this.currentPlan = new Plan(p.currentPlan);
        this.bestPersonal = new Plan(p.bestPersonal);
        this.fitness = p.fitness;
        this.bestFitness = p.bestFitness;
        this.setupRunnerThread = 0;
    }

    public void evalParticle(){
        double lastFitness = this.fitness;
        this.fitness = currentPlan.eval();
        //System.out.println("Particle "+idParticle+": " +  lastFitness + " -> " + fitness);
        CalculateBestPersonal();

    }

    public void CalculateVelocity(double c1Aperture,  double c2Aperture, double wAperture, double cnAperture,
                                  double c1Intensity, double c2Intensity, double wIntensity, double cnIntensity, Particle bGlobal){

        currentPlan.CalculateVelocity(c1Aperture, c2Aperture, wAperture, cnAperture, c1Intensity, c2Intensity, wIntensity, cnIntensity, bGlobal.getCurrentPlan(), bestPersonal);
    }

    public void CalculatePosition(){
        currentPlan.CalculatePosition();
    }

    public void OptimizateIntensities(){
        currentPlan.OptimizateIntensities();
    }

    public void CalculateBestPersonal(){
        if(this.fitness < bestFitness){
            setBestPersonal(this.currentPlan);
            setBestFitness(this.fitness);
        }
    }

    @Override
    public void run(){

        switch (setupRunnerThread){

            case MOVEMENT_THREAD:
                // Calcular velocidad
                CalculateVelocity(c1ApertureThread, c2ApertureThread, innerApertureThread, cnApertureThread,
                                    c1IntensityThread, c2IntensityThread, innerIntensityThread, cnIntensityThread, bestGlobal);
                // Calcular posicion
                CalculatePosition();
                setupRunnerThread = 1;
                break;
            case EVAL_THREAD:
                // Evaluar particula
                evalParticle();
                setupRunnerThread = 0;
                break;
        }
    }

    /*-------------------------------------------- GETTER AND SETTERS ----------------------------------------------*/
    public ArrayList<Integer> getTotalUnUsedApertures(){
        return this.currentPlan.getAperturesUnUsed();
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

    public double getC1ApertureThread() {
        return c1ApertureThread;
    }

    public void setC1ApertureThread(double c1ApertureThread) {
        this.c1ApertureThread = c1ApertureThread;
    }

    public double getC2ApertureThread() {
        return c2ApertureThread;
    }

    public void setC2ApertureThread(double c2ApertureThread) {
        this.c2ApertureThread = c2ApertureThread;
    }

    public double getInnerApertureThread() {
        return innerApertureThread;
    }

    public void setInnerApertureThread(double innerApertureThread) {
        this.innerApertureThread = innerApertureThread;
    }

    public double getCnApertureThread() {
        return cnApertureThread;
    }

    public void setCnApertureThread(double cnApertureThread) {
        this.cnApertureThread = cnApertureThread;
    }

    public double getC1IntensityThread() {
        return c1IntensityThread;
    }

    public void setC1IntensityThread(double c1IntensityThread) {
        this.c1IntensityThread = c1IntensityThread;
    }

    public double getC2IntensityThread() {
        return c2IntensityThread;
    }

    public void setC2IntensityThread(double c2IntensityThread) {
        this.c2IntensityThread = c2IntensityThread;
    }

    public double getInnerIntensityThread() {
        return innerIntensityThread;
    }

    public void setInnerIntensityThread(double innerIntensityThread) {
        this.innerIntensityThread = innerIntensityThread;
    }

    public double getCnIntensityThread() {
        return cnIntensityThread;
    }

    public void setCnIntensityThread(double cnIntensityThread) {
        this.cnIntensityThread = cnIntensityThread;
    }

    public void setBestGlobal(Particle p){
        this.bestGlobal = new Particle(p);
    }

}
