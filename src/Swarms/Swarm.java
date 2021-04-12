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
    private ArrayList<Particle> swarm;
    private int iter;

    /*---------------------------------------METHODS ---------------------------------------------------------------------------*/
    public Swarm(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, ArrayList<Integer> max_apertures, int max_intensity , int initial_intensity, int step_intensity,
                 int open_apertures, int setup, int diffSetup ,Vector<Volumen> volumen, Collimator collimator, double c1, double c2, double iner, int size, int iter) {
        setC1(c1);
        setC2(c2);
        setIner(iner);
        setIter(iter);
        this.swarm = new ArrayList<>();

        /*A Particles set will be created*/
        for(int i = 0; i < size ; i++){
            Particle newParticle;
            System.out.println("Creating Particle: "+ i);
            if(i == 0){
                newParticle = new Particle(w, Zmin, Zmax, max_apertures, max_intensity,initial_intensity, step_intensity, open_apertures, diffSetup, volumen, collimator);
            }else{
                newParticle = new Particle(w, Zmin, Zmax, max_apertures, max_intensity,initial_intensity, step_intensity, open_apertures, setup, volumen, collimator);
            }
            swarm.add(newParticle);

            if(i == 0){
                setBestGlobalParticle(newParticle);
                setBestGlobalEval(newParticle.getFitness());
            }
            System.out.println();
        }
        CalculateNewBestGlobal();
    }

    /*Running PSO Algorithm*/
    public void MoveSwarms(){

        System.out.println("MOVING SWARMS");
        for(int i = 0; i < getIter(); i++){
            CalculateVelocity();
            CalculatePosition();

            CalculateNewBestPersonal();
            CalculateNewBestGlobal();

            System.out.println("Iter "+ i +" best solution: "+ bestGlobalEval);
        }
    }


    public void CalculateVelocity(){
        for (Particle particle : swarm) {
            particle.CalculateVelocity(getC1(), getC2(), getInner(), getBestGlobalParticle() );
        }
    }

    public void CalculatePosition(){
        int i = 0;
        for(Particle particula: swarm){
            particula.CalculatePosition();
            System.out.println("Particle "+i+": "+particula.getFitness());
            i++;
        }
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

    public void setBestGlobalParticle(Particle bestGlobalParticle) {
        this.bestGlobalParticle = new Particle(bestGlobalParticle);
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

    public void setIner(double inner) {
        this.inner = inner;
    }

    public void setIter(int iter) {
        this.iter = iter;
    }

    /*--------------------------------------------------------- PRINTERS -----------------------------------------------------*/

    public void printIntensityMatrixSwarm(){
        for(Particle p : swarm){
            p.printIntensityMatrix();
        }
    }

    public void printIntensityMatrixParticle(int i){
        Particle p = swarm.get(i);
        p.printIntensityMatrix();
    }

    public void printAperture(){
        int x = 0;
        for(Particle p : swarm){
            System.out.println("Particle " + x);
            p.printAperture();
            x++;
        }
    }
    public void printApertureParticle(int x){
        Particle p = swarm.get(x);
        p.printAperture();

    }

    public void printApertureBeam(int x){
        int r = 0;
        for(Particle p : swarm){
            System.out.println("Particle " + r);
            p.printApertureBeam(x);
            x++;
        }
    }

    public void printApertureBeamByParticle(int x, int y){
        Particle p = swarm.get(y);
        p.printApertureBeam(x);
    }


}
