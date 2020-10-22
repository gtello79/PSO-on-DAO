package Swarms;
import source.Collimator;
import source.Volumen;

import java.util.ArrayList;
import java.util.Vector;

public class Swarm {
    private Particle best_global;
    private double best_solution;
    private double c1;
    private double c2;
    private double iner;
    private ArrayList<Particle> Poblacion;
    private int iter;

    /*---------------------------------------METHODS ---------------------------------------------------------------------------*/
    public Swarm(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, int max_apertures, int max_intensity , int initial_intensity, int step_intensity,
                 int open_apertures, int setup, Vector<Volumen> volumen, Collimator collimator, double c1, double c2, double iner, int size, int iter) {
        setC1(c1);
        setC2(c2);
        setIner(iner);
        setIter(iter);

        this.Poblacion = new ArrayList<>();
        /*A set of particles will be created*/
        for(int i = 0; i < size ; i++){
            System.out.println("Creating Particle: "+ i);
            Particle NewParticle = new Particle(w, Zmin, Zmax, max_apertures, max_intensity,initial_intensity, step_intensity, open_apertures, setup, volumen, collimator);
            Poblacion.add(NewParticle);
            if(i == 0){
                setBest_global(NewParticle);
                setBest_solution(NewParticle.getFitness());
                //NewParticle.printIntensityMatrix();
                //NewParticle.printAperture();
            }
            System.out.println();
        }
    }

    /*Running PSO Algorithm*/
    public void MoveSwarms(){
        CalculateNewBestGlobal();
        System.out.println("MOVING SWARM");
        for(int i = 0; i < getIter(); i++){
            CalculateVelocity();
            CalculatePosition();
            CalculateNewBestPersonal();
            CalculateNewBestGlobal();
            System.out.println("Iter "+ i +" best solution: "+ best_solution);
        }
        best_global.printIntensityMatrix();
    };

    public void CalculateVelocity(){
        for (Particle particula : Poblacion) {
            particula.CalculateVelocity(getC1(), getC2(), getIner(), getBest_global() );
        }
    }

    public void CalculatePosition(){
        for(Particle particula: Poblacion){
            particula.CalculatePosition();
        }
    }

    public void CalculateNewBestGlobal() {
        for (int i = 0; i < Poblacion.size() ; i++) {
            Particle particula = Poblacion.get(i);
            //System.out.println(particula.getFitness() +" " + getBest_solution());
            if (Double.compare(particula.getFitness(), getBest_solution()) == - 1) {
                System.out.println(particula.getFitness() + " " + getBest_solution() +" DENTRO");
                setBest_global(particula);
                setBest_solution(particula.getFitness());
                //System.out.println(best_global.getFitness() + " " + getBest_solution() +" CAMBIADA");
            };
        };
    };


    public void CalculateNewBestPersonal() {
        for(Particle particula: Poblacion){
            particula.CalculateBestPersonal();
        }
    }
    /*--------------------------------------------------------- PRINTERS -----------------------------------------------------*/

    public void printIntensityMatrixSwarm(){
        for(Particle p : Poblacion){
            p.printIntensityMatrix();
        }
    }

    public void printIntensityMatrixParticle(int i){
        Particle p = Poblacion.get(i);
        p.printIntensityMatrix();
    }

    public void printAperture(){
        int x = 0;
        for(Particle p : Poblacion){
            System.out.println("Particle " + x);
            p.printAperture();
            x++;
        }
    }
    public void printApertureParticle(int x){
        Particle p = Poblacion.get(x);
        p.printAperture();

    }

    public void printApertureBeam(int x){
        int r = 0;
        for(Particle p : Poblacion){
            System.out.println("Particle " + r);
            p.printApertureBeam(x);
            x++;
        }
    }

    public void printApertureBeambyParticle(int x, int y){
        Particle p = Poblacion.get(y);
        p.printApertureBeam(x);
    }

    /*-------------------------------------------- GETTER AND SETTERS ----------------------------------------------*/
    public double getBest_solution() {
        return best_solution;
    }

    public double getC1() {
        return c1;
    }

    public double getC2() {
        return c2;
    }

    public double getIner() {
        return iner;
    }

    public int getIter() {
        return iter;
    }

    public void setBest_global(Particle best_global) {
        this.best_global = best_global;
    }

    public void setBest_solution(double new_solution) {
        this.best_solution = new_solution;
    }

    public Particle getBest_global() {
        return best_global;
    }

    public void setC1(double c1) {
        this.c1 = c1;
    }

    public void setC2(double c2) {
        this.c2 = c2;
    }

    public void setIner(double iner) {
        this.iner = iner;
    }

    public void setIter(int iter) {
        this.iter = iter;
    }



}
