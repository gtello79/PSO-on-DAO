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
    private int max_intensity;

    public Swarm(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, int max_apertures, int max_intensity , int initial_intensity, int step_intensity,
                 int open_apertures, int setup, Vector<Volumen> volumen, Collimator collimator, double c1, double c2, double iner, int size, int iter) {
        this.c1 = c1;
        this.c2 = c2;
        this.iner = iner;
        this.iter = iter;
        Poblacion = new ArrayList<Particle>();
        /*A set of particles will be created*/
        for(int i = 0; i < size ; i++){
            Particle NewParticle = new Particle(w, Zmin, Zmax, max_apertures, max_intensity,initial_intensity, step_intensity, open_apertures, setup, volumen, collimator);
            Poblacion.add(NewParticle);
        }
    }

    public void MoveSwarms(){
        /*Running PSO Algorithm*/
        for(int i = 0; i < getIter(); i++){
            CalculateVelocity();
            CalculatePosition();
            CalculateNewBestGlobal();
        }
    };

    public void CalculateVelocity(){
        for (Particle particula : Poblacion) {
            particula.CalculateVelocity(getC1(), getC2(), getIner(), best_global);
        }
    };

    public void CalculatePosition(){
        for(Particle particula: Poblacion){
            particula.CalculatePosition();
        }
    };

    public void CalculateNewBestGlobal(){
        for(Particle particula: Poblacion){
            if(particula.getFitness() < getBest_solution()){
                setBest_global(particula);
                setBest_solution(particula.getFitness());
            }
        }
    }

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

    public void setBest_solution(double best_solution) {
        this.best_solution = best_solution;
    }

}
