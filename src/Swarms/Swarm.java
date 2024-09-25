package Swarms;

import source.Collimator;
import source.Volumen;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Vector;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class Swarm {
    private Particle bestGlobalParticle;
    public static DecimalFormat df = new DecimalFormat("#.00");

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
    private int iter;
    private int globalUpdateCount = 0;
    private int lastChange = 0;
    private int threadsToUse = 1;
    private double beamOnTime;
    private boolean optimizedIntensity;

    private ArrayList<Particle> swarm;
    private Vector<double[]> evalTrack;

    static final int MOVEMENT_THREAD = 0;
    static final int EVAL_THREAD = 1;
    static final int OPTIMIZE_THREAD = 2;
    static final int REPAIR_SOLUTION = 3;

    /*---------------------------------------METHODS ---------------------------------------------------------------------------*/
    public Swarm(ArrayList<Double> w, ArrayList<Double> Zmin, ArrayList<Double> Zmax, ArrayList<Integer> max_apertures,
            int max_intensity, int minIntensity,
            int initial_intensity, int step_intensity, int open_apertures, int setup, int diffSetup,
            ArrayList<Volumen> volumen, Collimator collimator,
            double c1Aperture, double c2Aperture, double innerAperture, double cnAperture,
            double c1Intensity, double c2Intensity, double innerIntensity, double cnIntensity, int size, int iter,
            int nThreads,
            boolean optimizedIntensity) {

        setThreadsToUse(nThreads);
        this.iter = iter;

        this.swarm = new ArrayList<>();
        this.evalTrack = new Vector<>();
        this.beamOnTime = 0.0;
        this.optimizedIntensity = optimizedIntensity;

        // Seteando parametros de PSO
        setC1Aperture(c1Aperture);
        setC2Aperture(c2Aperture);
        setInnerAperture(innerAperture);
        setCnAperture(cnAperture);

        setC1Intensity(c1Intensity);
        setC2Intensity(c2Intensity);
        setInnerIntensity(innerIntensity);
        setCnIntensity(cnIntensity);

        /* A Particles set will be created */
        for (int i = 0; i < size; i++) {
            System.out.println("Creating Particle: " + i);
            
            Particle newParticle;
            this.globalUpdateCount = 0;

            if (i == 0) {
                // Particula diferenciada
                newParticle = new Particle(i, w, Zmin, Zmax, max_apertures, max_intensity, minIntensity,
                        initial_intensity, step_intensity, open_apertures, diffSetup, volumen, collimator);
                setBestGlobalParticle(newParticle);
                setBestGlobalEval(newParticle.getFitness());
            } else {
                // Particula normal
                newParticle = new Particle(i, w, Zmin, Zmax, max_apertures, max_intensity, minIntensity,
                        initial_intensity, step_intensity, open_apertures, setup, volumen, collimator);
            }

            // Intensity optimization
            newParticle.OptimizateIntensities();

            // Only used by Threads
            newParticle.setC1ApertureThread(c1Aperture);
            newParticle.setC2ApertureThread(c2Aperture);
            newParticle.setInnerApertureThread(innerAperture);
            newParticle.setCnApertureThread(cnAperture);

            newParticle.setC1IntensityThread(c1Intensity);
            newParticle.setC2IntensityThread(c2Intensity);
            newParticle.setInnerIntensityThread(innerIntensity);
            newParticle.setCnIntensityThread(cnIntensity);
            
            swarm.add(newParticle);
            System.out.println();
        }

        CalculateNewBestGlobal();

        // Save the par iter-best value on each iteration
        double[] initialRecord = new double[2];
        initialRecord[0] = 0.0;
        initialRecord[1] = this.bestGlobalEval;

        // Save de evolution of the best solution
        evalTrack.add(initialRecord);
        this.firstSolution = bestGlobalEval;

    }

    /* Running PSO Algorithm */
    // Move particles using differents alternatives
    public void MoveSwarms() {
        System.out.println(" ------- MOVING SWARMS");

        double initialAlgorithmTime = (double) System.currentTimeMillis();
        
        // Move the swarm on the search space
        MoveSwarmsOnConcurrent();
        

        double finalAlgorithmTime = (double)System.currentTimeMillis();

        // Save algorithm statistics
        int totalAperturesUnUsed = this.bestGlobalParticle.getAperturesUnUsed();
        double bestBeamOnTime = this.bestGlobalParticle.getBeamOnTime();
        double final_time = finalAlgorithmTime - initialAlgorithmTime;

        System.out.println("----- ESTADISTICAS FINALES");
        System.out.println("Processing Time: " + df.format(final_time) + " [seg]");
        System.out.println("Best Fitness - #Ap Unused - Best BoT");
        System.out.println(bestGlobalEval + " " + totalAperturesUnUsed + " " + df.format(bestBeamOnTime));

    }

    // Move the particles with concurrent process
    public void MoveSwarmsOnConcurrent() {

        for (int i = 0; i < this.iter; i++) {
            boolean change = false, change_1 = false, change_2 = false, change_3 = false;

            // Movimiento de particulas en paralelo
            this.caseParticlesThread(MOVEMENT_THREAD);
            this.caseParticlesThread(EVAL_THREAD);
            change_1 = CalculateNewBestGlobal();

            // Optimizacion de particulas en paralelo
            if (i % 10 == 0 && optimizedIntensity) {
                System.out.println(" ------- Optimizacion de intensidades ---------");
                this.caseParticlesThread(OPTIMIZE_THREAD);
                change_2 = CalculateNewBestGlobal();

            }
            
            // Reparacion de particulas en paralelo
            if (i % 10 == 0 && optimizedIntensity) {
                System.out.println(" ------- Reparación de solución ---------");
                this.caseParticlesThread(REPAIR_SOLUTION);
                change_3 = CalculateNewBestGlobal();
            }

            // Se verifica si hubo cambios en la solución
            change = change_1 || change_2 || change_3;
            if (change) {
                this.globalUpdateCount++;
                setLastChange(i);

                double[] finalRecord = new double[2];
                finalRecord[0] = (double) i;
                finalRecord[1] = this.bestGlobalEval;
                evalTrack.add(finalRecord);
            }

            System.out.println(" Iter " + i + " best solution: " + bestGlobalEval + ". Update count: " + this.getGlobalUpdateCount());
        }
    }

    public void repairSolutions() {
        System.out.println(" ------- Reparación de solución ---------");
        for (Particle particle : swarm) {
            particle.regenerateApertures();
        }
    }

    public void OptimizateIntensities() {
        System.out.println(" ------- Optimizacion de intensidades ---------");
        for (Particle particle : swarm) {
            particle.OptimizateIntensities();
        }
    }

    public void evalParticles() {
        for (Particle particle : swarm) {
            particle.evalParticle();
        }
    }

    /*---------------------------------------------------- PSO METHODS--------------------------------------------------------------------------------*/

    public boolean CalculateNewBestGlobal() {
        boolean changeGlobal = false;

        for (Particle particle : swarm) {
            if (particle.getFitness() < getBestGlobalEval()) {
                setBestGlobalParticle(particle);
                setBestGlobalEval(particle.getFitness());
                changeGlobal = true;
            }
        }
        return changeGlobal;
    }

    /*-------------------------------------------- GETTER AND SETTERS ----------------------------------------------*/
    private void setThreadsToUse(int threadsToUse) {
        this.threadsToUse = threadsToUse;
        System.out.println("Using " + this.threadsToUse + " Threads");

    }

    public double getBestGlobalEval() {
        return bestGlobalEval;
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


    public int getGlobalUpdateCount() {
        return globalUpdateCount;
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

    public void setLastChange(int lastChange) {
        this.lastChange = lastChange;
    }

    // --------------------------------------- THREADS METHODS (NO TOCAR)
    // ----------------------------------

    public void caseParticlesThread(int idSetup) {
        // Guardar objetos ejecutados en hilos (particulas)
        List<Callable<Object>> calls = new ArrayList<Callable<Object>>();
        // Pool ejecutora de las funciones
        ExecutorService pool2 = Executors.newFixedThreadPool(threadsToUse);

        // Guardar objetos ejecutados - Establece configuracion de ejecucion
        for (int j = 0; j < swarm.size(); j++) {
            Particle p = swarm.get(j);
            if (idSetup == MOVEMENT_THREAD) {
                p.setBestGlobal(this.bestGlobalParticle);
            }

            p.setSetupRunnerThread(idSetup);
            calls.add(Executors.callable(p));
        }

        // Da inicio a la ejecucion de los hilos - llama a metodo run en cada particle
        try {
            pool2.invokeAll(calls);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        // Permite manejar el termino de los metodos llamados por los threads
        pool2.shutdown();
    }

}
