package Swarms;

import source.Collimator;

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


    private int iter;
    private int globalUpdateCount = 0;
    private int threadsToUse = 1;
    private boolean optimizedIntensity;
    private double swarmMovementTime = 0.0;

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
            Collimator collimator,
            double c1Aperture, double c2Aperture, double innerAperture, double cnAperture,
            double c1Intensity, double c2Intensity, double innerIntensity, double cnIntensity, int size, int iter,
            int nThreads,
            boolean optimizedIntensity) {

        setThreadsToUse(nThreads);
        setIter(iter);

        this.swarm = new ArrayList<>();
        this.evalTrack = new Vector<>();
        this.optimizedIntensity = optimizedIntensity;

        /* A Particles set will be created */
        for (int i = 0; i < size; i++) {
            Particle newParticle;
            System.out.println("Creating Particle: " + i);
            this.globalUpdateCount = 0;

            if (i == 0) {
                // Particula diferenciada
                newParticle = new Particle(i, w, Zmin, Zmax, max_apertures, max_intensity, minIntensity,
                        initial_intensity, step_intensity, open_apertures, diffSetup, collimator);
                setBestGlobalParticle(newParticle);
                setBestGlobalEval(newParticle.getFitness());
            } else {
                // Particula normal
                newParticle = new Particle(i, w, Zmin, Zmax, max_apertures, max_intensity, minIntensity,
                        initial_intensity, step_intensity, open_apertures, setup, collimator);
            }

            // Intensity optimization
            newParticle.OptimizateIntensities();

            /* Only used by Threads */
            // PSO parameters settings
            newParticle.setC1Aperture(c1Aperture);
            newParticle.setC2Aperture(c2Aperture);
            newParticle.setInnerAperture(innerAperture);
            newParticle.setCnAperture(cnAperture);
            newParticle.setC1Intensity(c1Intensity);
            newParticle.setC2Intensity(c2Intensity);
            newParticle.setInnerIntensity(innerIntensity);
            newParticle.setCnIntensity(cnIntensity);

            swarm.add(newParticle);
            System.out.println();
        }

        CalculateNewBestGlobal();

        double[] initialRecord = new double[2];
        initialRecord[0] = 0.0;
        initialRecord[1] = this.bestGlobalEval;
        evalTrack.add(initialRecord);

    }

    /* Running PSO Algorithm */
    public void MoveSwarms() {
        System.out.println(" ------- MOVING SWARMS -------");

        MoveSwarmsOnConcurrent();

        int totalAperturesUnUsed = this.bestGlobalParticle.getAperturesUnUsed();
        double bestBeamOnTime = this.bestGlobalParticle.getBeamOnTime();

        System.out.println("Resultados");
        System.out.println("Processing Time: " + df.format(this.swarmMovementTime) + " [seg]");
        System.out.println("Best Fitness - #Ap Unused - Best BoT - Robust Fitness");
        System.out.println(df.format(bestGlobalEval) + " " + totalAperturesUnUsed + " " + df.format(bestBeamOnTime) + " " + this.bestGlobalParticle.getRobustFitness());

    }

    // Move the particles with concurrent process
    public void MoveSwarmsOnConcurrent() {
        double initialAlgorithmTime = (double) System.currentTimeMillis();

        for (int i = 0; i < this.getIter(); i++) {
            boolean change_1 = false, change_2 = false;

            // Efectua los movimientos de las particulas
            this.caseParticlesThread(MOVEMENT_THREAD);

            // Efectua la evaluación de las partículas
            this.caseParticlesThread(EVAL_THREAD);

            change_1 = CalculateNewBestGlobal();

            if (i % 10 == 0 && i > 1 && optimizedIntensity) {
                System.out.println(" ------- Optimizacion de intensidades ---------");
                this.caseParticlesThread(OPTIMIZE_THREAD);
                System.out.println(" ------- Reparación de solución ---------");
                this.caseParticlesThread(REPAIR_SOLUTION);
                change_2 = CalculateNewBestGlobal();
                
            }

            if (change_1 || change_2) {
                this.globalUpdateCount++;

                double[] finalRecord = new double[2];
                finalRecord[0] = (double) i;
                finalRecord[1] = this.bestGlobalEval;
                evalTrack.add(finalRecord);
            }
            System.out.println(" >>> Iter " + i + " best solution: " + df.format(bestGlobalEval) + " Robust " + df.format(bestGlobalParticle.getRobustFitnessValue()) + ". Update count: "
                    + this.getGlobalUpdateCount());
        }

        // Calculate the resolution time
        this.swarmMovementTime = (double) System.currentTimeMillis() - initialAlgorithmTime;
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
            if (particle.getRobustFitnessValue() < this.bestGlobalParticle.getRobustFitnessValue()) {
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

    public ArrayList<Double> getRobustBestGlobalEval() {
        return bestGlobalParticle.getRobustFitness();
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


    // --------------------------------------- THREADS METHODS (NO TOCAR) //
    // ----------------------------------
    public void caseParticlesThread(int idSetup) {
        // Guardar objetos ejecutados en hilos (particulas)
        List<Callable<Object>> calls = new ArrayList<Callable<Object>>();

        // Pool ejecutora de las funciones
        ExecutorService pool2 = Executors.newFixedThreadPool(threadsToUse);

        // Guardar objetos ejecutados
        for (int j = 0; j < swarm.size(); j++) {
            Particle p = swarm.get(j);
            if (idSetup == MOVEMENT_THREAD) {
                p.setBestGlobal(this.bestGlobalParticle);
            }

            p.setSetupRunnerThread(idSetup);
            calls.add(Executors.callable(p));
        }

        // Da inicio a la ejecucion de los hilos
        try {
            pool2.invokeAll(calls);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        // Permite manejar el termino de los metodos llamados por los threads
        pool2.shutdown();
    }

}
