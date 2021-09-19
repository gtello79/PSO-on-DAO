package Swarms;
import source.Collimator;
import source.Volumen;

import java.util.ArrayList;
import java.util.Vector;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;


public class Swarm {
    private Particle bestGlobalParticle;
    private double bestGlobalEval;
    private double c1Aperture;
    private double c2Aperture;
    private double innerAperture;
    private double cnAperture;

    private double c1Intensity;
    private double c2Intensity;
    private double innerIntensity;
    private double cnIntensity;

    private double firstSolution;
    private final ArrayList<Particle> swarm;
    private int iter;
    private int globalUpdateCount = 0;
    private int lastChange = 0;
    private int threadsToUse = 1;

    private boolean callablefunctions = false;

    /*---------------------------------------METHODS ---------------------------------------------------------------------------*/
    public Swarm(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, ArrayList<Integer> max_apertures, int max_intensity , int initial_intensity, int step_intensity,
                 int open_apertures, int setup, int diffSetup ,Vector<Volumen> volumen, Collimator collimator,
                 double c1Aperture, double c2Aperture, double innerAperture, double cnAperture, 
                 double c1Intensity, double c2Intensity, double innerIntensity, double cnIntensity, int size, int iter, int nThreads) {

        setThreadsToUse(nThreads);
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

            /*Only used by Threads*/
            if(callablefunctions){
                newParticle.setC1ApertureThread(c1Aperture);
                newParticle.setC2ApertureThread(c2Aperture);
                newParticle.setInnerApertureThread(innerAperture);
                newParticle.setCnApertureThread(cnAperture);

                newParticle.setC1IntensityThread(c1Intensity);
                newParticle.setC2IntensityThread(c2Intensity);
                newParticle.setInnerIntensityThread(innerIntensity);
                newParticle.setCnIntensityThread(cnIntensity);
            }

            swarm.add(newParticle);
            System.out.println();
        }

        //Seteando parametros de PSO
        setC1Aperture(c1Aperture);
        setC2Aperture(c2Aperture);
        setInnerAperture(innerAperture);
        setCnAperture(cnAperture);

        setC1Intensity(c1Intensity);
        setC2Intensity(c2Intensity);
        setInnerIntensity(innerIntensity);
        setCnIntensity(cnIntensity);

        CalculateNewBestGlobal();
    }


    /*Running PSO Algorithm*/
    public void MoveSwarms(){

        System.out.println("MOVING SWARMS");
        for(int i = 0; i < getIter(); i++){
            boolean change;
            // Habilitacion de función por paralelismo
            if(callablefunctions){
                ParticlesMovementThreads();
                change = evalParticlesThread();
            }
            else{
                calculateVelocity();
                calculatePosition();
                change = evalParticles();
            }

            if(i%10 == 0){
                System.out.println("Optimizacion de intensidad");
                //OptimizateIntensity();
            }
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
            particle.CalculateVelocity(getC1Aperture(), getC2Aperture(),getInnerAperture(),getCnAperture(),
                                        getC1Intensity(),getC2Intensity(),getInnerIntensity(), getCnIntensity(), getBestGlobalParticle());
        }
    }

    public void calculatePosition(){
        for(Particle particle: swarm){
            particle.CalculatePosition();
        }
    }

    public void OptimizateIntensites(){
        for(Particle particle: swarm){
            particle.OptimizateIntensities();
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

    public boolean evalParticlesThread(){
        int i = 0;
        boolean changeGlobal = false;
        int threadsToUse = 4;

        // Guardar objetos ejecutados en hilos (particulas)
        List<Callable<Object>> calls = new ArrayList<Callable<Object>>();

        // Pool ejecutora de las funciones
        ExecutorService pool2 = Executors.newFixedThreadPool(threadsToUse);

        //Guardar objetos ejecutados
        for(int j=0; j<swarm.size(); j++) {
            Particle p = swarm.get(j);
            p.setBestGlobal(p);
            calls.add(Executors.callable(swarm.get(j)));
        }

        // Da inicio a la ejecucion de los hilos
        try {
            pool2.invokeAll(calls);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        // Permite manejar el termino de los metodos llamados por los threads
        pool2.shutdown();

        //Calculate new Best Global
        for(Particle particle: swarm){
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

    public void ParticlesMovementThreads(){

        // Guardar objetos ejecutados en hilos (particulas)
        List<Callable<Object>> calls = new ArrayList<Callable<Object>>();

        // Pool ejecutora de las funciones
        ExecutorService pool2 = Executors.newFixedThreadPool(threadsToUse);

        //Guardar objetos ejecutados
        for(int j=0; j<swarm.size(); j++) {
            calls.add(Executors.callable(swarm.get(j)));
        }

        // Da inicio a la ejecucion de los hilos
        try {
            pool2.invokeAll(calls);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        // Permite manejar el termino de los metodos llamados por los threads
        pool2.shutdown();
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

    public void setCnIntensity(double cnIntensity) {
        this.cnIntensity = cnIntensity;
    }

    public double getCnAperture() {
        return cnAperture;
    }

    public void setCnAperture(double cnAperture) {
        this.cnAperture = cnAperture;
    }

    public double getCnIntensity() {
        return cnIntensity;
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

    private void setThreadsToUse(int threadsToUse){
        this.threadsToUse = threadsToUse;
        this.callablefunctions = threadsToUse > 1;
    }
}
