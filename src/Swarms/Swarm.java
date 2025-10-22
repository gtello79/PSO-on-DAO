package Swarms;

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
    private int size;

    private double wMaxAperture;
    private double wMinAperture;
    private double wMaxIntensity;
    private double wMinIntensity;

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
            double c1Aperture, double c2Aperture, double cnAperture, double c1Intensity, double c2Intensity, double cnIntensity, int size, int iter,
            double wMaxAperture, double wMinAperture, double wMaxIntensity, double wMinIntensity,
            int nThreads,
            boolean optimizedIntensity) {

        setThreadsToUse(nThreads);
        setIter(iter);

        this.swarm = new ArrayList<>();
        this.evalTrack = new Vector<>();
        this.optimizedIntensity = optimizedIntensity;
        this.wMaxAperture = wMaxAperture;
        this.wMinAperture = wMinAperture;
        this.wMaxIntensity = wMaxIntensity;
        this.wMinIntensity = wMinIntensity;
        this.size = size;

        /* A Particles set will be created */
        this.globalUpdateCount = 0;
        for (int i = 0; i < this.size; i++) {
            System.out.println("Creating Particle: " + i);
            Particle newParticle = new Particle(i, max_apertures, max_intensity, minIntensity,
                initial_intensity, step_intensity, open_apertures, setup);
            
            newParticle.setBestPersonal();
            newParticle.setBestFitness();
            
            /* Only used by Threads */
            // PSO parameters settings
            newParticle.setC1Aperture(c1Aperture);
            newParticle.setC2Aperture(c2Aperture);
            newParticle.setCnAperture(cnAperture);
            newParticle.setC1Intensity(c1Intensity);
            newParticle.setC2Intensity(c2Intensity);
            newParticle.setCnIntensity(cnIntensity);

            if (i == 0) {
                setBestGlobalParticle(newParticle);
                setBestGlobalEval(newParticle.getFitness());
            }
            
            // Intensity optimization
            newParticle.OptimizeIntensities();

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
        System.out.println(df.format(bestGlobalEval) + " " + totalAperturesUnUsed + " " + df.format(bestBeamOnTime)
                + " " + this.bestGlobalParticle.getFitness());

    }

    // Move the particles with concurrent process
    public void MoveSwarmsOnConcurrent() {
        double initialAlgorithmTime = (double) System.currentTimeMillis();

        for (int iter = 0; iter < this.getIter(); iter++) {
            boolean change_1 = false, change_2 = false;

            // Efectua los movimientos de las particulas
            this.caseParticlesThread(MOVEMENT_THREAD, iter);

            // Efectua la evaluación de las partículas
            this.caseParticlesThread(EVAL_THREAD, iter);

            change_1 = CalculateNewBestGlobal();

            if (iter % 10 == 0 && iter > 1 && optimizedIntensity) {
                System.out.println(" ------- Optimizacion de intensidades ---------");
                this.caseParticlesThread(OPTIMIZE_THREAD, iter);
                System.out.println(" ------- Reparación de solución ---------");
                this.caseParticlesThread(REPAIR_SOLUTION, iter);
                change_2 = CalculateNewBestGlobal();

            }

            if (change_1 || change_2) {
                this.globalUpdateCount++;

                double[] finalRecord = new double[2];
                finalRecord[0] = (double) iter;
                finalRecord[1] = this.bestGlobalEval;
                evalTrack.add(finalRecord);
            }
            System.out.println(" >>> Iter " + iter + " best solution: " + df.format(bestGlobalEval) + " Robust "
                    + bestGlobalParticle.getFitness() + ". Update count: "
                    + this.getGlobalUpdateCount());
        }

        // Calculate the resolution time
        this.swarmMovementTime = ((double) System.currentTimeMillis() - initialAlgorithmTime) / 1000.0;
    }

    public void repairSolutions() {
        System.out.println(" ------- Reparación de solución ---------");
        for (Particle particle : swarm) {
            particle.regenerateApertures();
        }
    }

    public void OptimizeIntensities() {
        System.out.println(" ------- Optimizacion de intensidades ---------");
        for (Particle particle : swarm) {
            particle.OptimizeIntensities();
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

        for (int i = 0; i < swarm.size(); i++) {
            Particle particle = swarm.get(i);
            if (particle.getFitness() < this.bestGlobalParticle.getFitness()) {
                setBestGlobalParticle(particle);
                setBestGlobalEval(particle.getFitness());
                changeGlobal = true;
            }
        }
        return changeGlobal;
    }

    public double updateInertiaControl(double wMax, double wMin, int iteration) {
        double w = wMax - ((wMax - wMin) * iteration) / this.iter;
        return w;
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
    public void caseParticlesThread(int idSetup, int iter) {
        // Guardar objetos ejecutados en hilos (particulas)
        List<Callable<Object>> calls = new ArrayList<Callable<Object>>();

        // Pool ejecutora de las funciones
        ExecutorService pool2 = Executors.newFixedThreadPool(threadsToUse);

        // Guardar objetos ejecutados
        for (int j = 0; j < swarm.size(); j++) {
            Particle p = swarm.get(j);
            if (idSetup == MOVEMENT_THREAD) {
                // Calculate cf for aperture and intensity
                double wAperture = this.updateInertiaControl(this.wMaxAperture, this.wMinAperture, iter);
                double wIntensity = this.updateInertiaControl(this.wMaxIntensity, this.wMinIntensity, iter);

                p.setInertiaIntensity(wIntensity);
                p.setInertiaAperture(wAperture);
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
