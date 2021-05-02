package Swarms;
import source.Collimator;
import source.Volumen;

import java.util.ArrayList;
import java.util.Vector;

public class Swarm {
    private Particle bestGlobalParticle;
    private double bestGlobalEval;
    private double c1;
    private double c2;
    private double inner;
    private double firstSolution;
    private final ArrayList<Particle> swarm;
    private int iter;
    private int globalUpdateCount = 0;


    /*---------------------------------------METHODS ---------------------------------------------------------------------------*/
    public Swarm(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, ArrayList<Integer> max_apertures, int max_intensity , int initial_intensity, int step_intensity,
                 int open_apertures, int setup, int diffSetup ,Vector<Volumen> volumen, Collimator collimator, double c1, double c2, double iner, int size, int iter) {
        setC1(c1);
        setC2(c2);
        setInner(iner);
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
            evalParticles();

            CalculateNewBestPersonal();
            CalculateNewBestGlobal();

            System.out.println("Iter "+ i +" best solution: "+ bestGlobalEval + ". Update count: " + this.getGlobalUpdateCount() );
        }

        bestGlobalParticle.printFluenceMapByBeam();
        System.out.println("Initial solution: " + firstSolution );
    }


    public void calculateVelocity(){
        for (Particle particle : swarm) {
            particle.CalculateVelocity(getC1(), getC2(), getInner(), getBestGlobalParticle() );
        }
    }

    public void calculatePosition(){
        int i = 0;
        for(Particle particle: swarm){
            particle.CalculatePosition();
            System.out.println("Particle "+i+": "+particle.getFitness());
            i++;
        }
    }

    public void evalParticles(){
        for(Particle particle: swarm){
            particle.evalParticle();
        }
    }

    public void CalculateNewBestGlobal() {
        boolean changeGlobal = false;
        for (Particle particle: swarm){
            if (particle.getFitness() < getBestGlobalEval() ) {
                setBestGlobalParticle(particle);
                setBestGlobalEval(particle.getFitness());
                changeGlobal = true;
            }
        }

        if(changeGlobal) setGlobalUpdateCount();

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

    public double getC1() {
        return c1;
    }

    public double getC2() {
        return c2;
    }

    public double getInner() {
        return inner;
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

    public void setC1(double c1) {
        this.c1 = c1;
    }

    public void setC2(double c2) {
        this.c2 = c2;
    }

    public void setInner(double inner) {
        this.inner = inner;
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

}
