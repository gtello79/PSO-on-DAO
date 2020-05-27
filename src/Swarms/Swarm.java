package Swarms;
import java.util.ArrayList;
import java.util.Vector;

public class Swarm {
    private Particle best_global;
    private double best_solution;
    private double c1;
    private double c2;
    private double iner;
    private ArrayList<Particle> Poblacion;
    private int size;
    private int iter;
    private int max_intensity;

    public Swarm(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, int max_apertures, int max_intensity ,int initial_intensity, int step_intensity, int open_apertures, int setup) {
        this.c1 = 0;
        this.c2 = 0;
        this.iner = 0;
        this.size = 0;
        this.iter = 0;
        this.max_intensity = 0;
        Poblacion = new ArrayList<Particle>();
        /*A set of particles will be created*/
        for(int i = 0; i < size ; i++){
            Particle NewParticle = new Particle(w, Zmin, Zmax, max_apertures, max_intensity,initial_intensity, step_intensity, open_apertures, setup);
            Poblacion.add(NewParticle);
        }
    }

    public void MoveSwarms(double c1, double c2, double iner, int iter){
        /*Running PSO Algorithm*/
        for(int i = 0; i < iter; i++){
            CalculateVelocity(c1 ,c2 ,iner);
            CalculatePosition();
            CalculateNewBestGlobal();
        }
    };

    public void CalculateVelocity(double c1, double c2, double iner){
        for (Particle particula : Poblacion) {
            particula.CalculateVelocity(c1, c2, iner, best_global);
        }
    };

    public void CalculatePosition(){
        for(Particle particula: Poblacion){
            particula.CalculatePosition();
        }
    };

    public void CalculateNewBestGlobal(){
        for(Particle particula: Poblacion){
            if(particula.getFitness() < best_solution){
                setBest_global(particula);
                setBest_solution(particula.getFitness());
            }
        }
    }

    public Particle getBest_global() {
        return best_global;
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

    public int getSize() {
        return size;
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

    public void setC1(double c1) {
        this.c1 = c1;
    }

    public void setC2(double c2) {
        this.c2 = c2;
    }

    public void setIner(double iner) {
        this.iner = iner;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setIter(int iter) {
        this.iter = iter;
    }


}
