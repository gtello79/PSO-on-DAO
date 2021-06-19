package Swarms;
import source.Collimator;
import source.Volumen;

import java.util.ArrayList;
import java.util.Vector;

public class Swarm {
    private Particle bestGlobalParticle;
    private double bestGlobalEval;
    private double c1Aperture;
    private double c2Aperture;

    private double innerAperture;
    private double c1Intensity;
    private double c2Intensity;
    private double innerIntensity;
    private double firstSolution;
    private final ArrayList<Particle> swarm;
    private int iter;
    private int globalUpdateCount = 0;
    private int lastChange = 0;

    /*---------------------------------------METHODS ---------------------------------------------------------------------------*/
    public Swarm(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, ArrayList<Integer> max_apertures, int max_intensity , int initial_intensity, int step_intensity,
                 int open_apertures, int setup, int diffSetup ,Vector<Volumen> volumen, Collimator collimator,
                 double c1Aperture, double c2Aperture, double innerAperture, double c1Intensity, double c2Intensity, double innerIntensity, int size, int iter) {
        
        setC1Aperture(c1Aperture);
        setC2Aperture(c2Aperture);
        setInnerAperture(innerAperture);
        setC1Intensity(c1Intensity);
        setC2Intensity(c2Intensity);
        setInnerIntensity(innerIntensity);
        setIter(iter);
        this.swarm = new ArrayList<>();

        /*A Particles set will be created*/
        for(int i = 0; i < size ; i++){
            Particle newParticle;
            System.out.println("Creating Particle: "+ i);
            this.globalUpdateCount = 0;

            if(i == 0){
                newParticle = new Particle(w, Zmin, Zmax, max_apertures, max_intensity,initial_intensity, step_intensity, open_apertures, diffSetup, volumen, collimator);
                setBestGlobalParticle(newParticle);
                setBestGlobalEval(newParticle.getFitness());
                this.firstSolution = bestGlobalEval;
            }else{
                newParticle = new Particle(w, Zmin, Zmax, max_apertures, max_intensity,initial_intensity, step_intensity, open_apertures, setup, volumen, collimator);
            }

            swarm.add(newParticle);
            System.out.println();
        }
        CalculateNewBestGlobal();

    }

    /*Running PSO Algorithm*/
    public void MoveSwarms(){

        System.out.println("MOVING SWARMS");
        for(int i = 0; i < getIter(); i++){
            calculateVelocity();
            calculatePosition();
            boolean change = evalParticles();

            CalculateNewBestPersonal();

            if(change){
                setGlobalUpdateCount();
                setLastChange(i);
            }
                
            System.out.println("Iter "+ i +" best solution: "+ bestGlobalEval + ". Update count: " + this.getGlobalUpdateCount() );
        }

        //bestGlobalParticle.printFluenceMapByBeam();
        System.out.println("Initial solution: " + firstSolution  + " - Final solution: "+ bestGlobalEval + " - last Change: "+ lastChange);
        System.out.println(firstSolution  + " " + bestGlobalEval + " " + globalUpdateCount + " " + lastChange);
    }


    public void calculateVelocity(){
        for (Particle particle : swarm) {
            particle.CalculateVelocity(getC1Aperture(), getC2Aperture(),getInnerAperture(),getC1Intensity(),getC2Intensity(),getInnerIntensity(),getBestGlobalParticle());
        }
    }

    public void calculatePosition(){
        for(Particle particle: swarm){
            particle.CalculatePosition();
        }
    }

    public boolean evalParticles(){
        boolean changeGlobal = false;
        int i = 0;
        for(Particle particle: swarm){
            particle.evalParticle();

            //Calculate new Best Global
            if (particle.getFitness() < getBestGlobalEval() ) {
                setBestGlobalParticle(particle);
                setBestGlobalEval(particle.getFitness());
                changeGlobal = true;
            }
            System.out.println("Particle "+i+": "+particle.getFitness());
            i++;
        }
        return changeGlobal;
    }

    public void CalculateNewBestGlobal() {
        for (Particle particle: swarm){
            if (particle.getFitness() < getBestGlobalEval() ) {
                setBestGlobalParticle(particle);
                setBestGlobalEval(particle.getFitness());
            }
        }
    }

    public void CalculateNewBestPersonal() {
        for(Particle particle: swarm){
            particle.CalculateBestPersonal();
        }
    }

    /*-------------------------------------------- GETTER AND SETTERS ----------------------------------------------*/
    public double getBestGlobalEval() {
        return bestGlobalEval;
    }

    public void setBestGlobalParticle(Particle bestGlobalParticle) {
        this.bestGlobalParticle = new Particle(bestGlobalParticle);
    }

    public int getIter() {
        return iter;
    }

    public void setBestGlobalEval(double newSolution) {
        this.bestGlobalEval = newSolution;
    }

    public Particle getBestGlobalParticle() {
        return bestGlobalParticle;
    }

    public void setIter(int iter) {
        this.iter = iter;
    }

    public int getGlobalUpdateCount() {
        return globalUpdateCount;
    }

    public void setGlobalUpdateCount() {
        this.globalUpdateCount += 1;
    }

    public double getC1Aperture() {
        return c1Aperture;
    }

    public void setC1Aperture(double c1Aperture) {
        this.c1Aperture = c1Aperture;
    }

    public double getC2Aperture() {
        return c2Aperture;
    }

    public void setC2Aperture(double c2Aperture) {
        this.c2Aperture = c2Aperture;
    }

    public double getInnerAperture() {
        return innerAperture;
    }

    public void setInnerAperture(double innerAperture) {
        this.innerAperture = innerAperture;
    }

    public double getC1Intensity() {
        return c1Intensity;
    }

    public void setC1Intensity(double c1Intensity) {
        this.c1Intensity = c1Intensity;
    }

    public double getC2Intensity() {
        return c2Intensity;
    }

    public void setC2Intensity(double c2Intensity) {
        this.c2Intensity = c2Intensity;
    }

    public double getInnerIntensity() {
        return innerIntensity;
    }

    public void setInnerIntensity(double innerIntensity) {
        this.innerIntensity = innerIntensity;
    }

    public void setLastChange(int lastChange){
        this.lastChange = lastChange;
    }

    public int getLastChange(){
        return this.lastChange;
    }

}
