package Swarms;

import java.util.logging.Logger;
import SRCDAO.Plan;
import robust.EscenarioController;
import source.Collimator;

import java.util.ArrayList;
import java.util.Collections;
import java.text.DecimalFormat;

public class Particle extends Thread {
    int particleId;

    private static DecimalFormat df = new DecimalFormat("#.00");

    private int setupRunnerThread;
    private double c1Aperture;
    private double c2Aperture;
    private double innerAperture;
    private double cnAperture;

    private double c1Intensity;
    private double c2Intensity;
    private double innerIntensity;
    private double cnIntensity;

    private double fitness;
    private double bestFitness;

    private Particle bestGlobal;
    private Particle bestPersonal;
    private int indexToScenario = 0;
    private ArrayList<Plan> plansScenarios;
    private ArrayList<Double> fitnessScenarios;
    private ArrayList<Double> beamOnTimeScenarios;
    private ArrayList<Integer> nApertureByScenario;

    // MOVEMENT_THREAD: Used for updating particle velocity and position in PSO.
    static final int MOVEMENT_THREAD = 0;
    // EVAL_THREAD: Used for evaluating the fitness of the particle.
    static final int EVAL_THREAD = 1;
    // OPTIMIZE_THREAD: Used for optimizing the intensities of the particle's plan.
    static final int OPTIMIZE_THREAD = 2;
    // REPAIR_SOLUTION: Used for regenerating apertures and repairing the solution.
    static final int REPAIR_SOLUTION = 3;

    static Logger logger = Logger.getLogger(Particle.class.getName());

    /*-------------------------------------------------------------METHODS -------------------------------------------*/
    public Particle(int particleId,
            ArrayList<Integer> max_apertures,
            int max_intensity, int minIntensity, int initial_intensity, int step_intensity, int open_apertures,
            int setup) {

        this.particleId = particleId;
        this.plansScenarios = new ArrayList<>();
        this.fitnessScenarios = new ArrayList<>();
        this.beamOnTimeScenarios = new ArrayList<>();
        this.nApertureByScenario = new ArrayList<>();

        // Create a plan for each scenario
        for (int s = 0; s < EscenarioController.getnScenarios(); s++) {
            Collimator collimator = EscenarioController.getCollimatorFromIndexScenario(s);

            // Plan creation for the scenario
            Plan planScenario = new Plan(max_apertures, max_intensity, minIntensity,
                    initial_intensity, step_intensity, open_apertures, setup, collimator, s);
            plansScenarios.add(planScenario);
            fitnessScenarios.add(planScenario.getEval());
            beamOnTimeScenarios.add(planScenario.getBeamOnTime());
            nApertureByScenario.add(planScenario.getTotalAperturesUnsed());
        }

        // Update Plan Stats
        this.evalParticle();
        System.out.println("Particle-" + particleId + " created with fitness: " + this.fitnessScenarios);
    }

    public Particle(Particle p) {
        // Copy constructor
        this.particleId = p.particleId;
        this.bestFitness = p.bestFitness;
        this.fitness = p.fitness;
        this.plansScenarios = new ArrayList<>();

        for (Plan plan : p.plansScenarios) {
            this.plansScenarios.add(new Plan(plan));
        }
        this.fitnessScenarios = new ArrayList<>(p.fitnessScenarios);
        this.beamOnTimeScenarios = new ArrayList<>(p.beamOnTimeScenarios);
        this.nApertureByScenario = new ArrayList<>(p.nApertureByScenario);
        this.indexToScenario = p.indexToScenario;

        this.bestPersonal = p.bestPersonal == null ? null : new Particle(p.bestPersonal);
        this.setupRunnerThread = p.setupRunnerThread;
    }

    public void evalParticle() {
        double lastFitness = this.fitness;
        for (int s = 0; s < EscenarioController.getnScenarios(); s++) {
            if(s == this.indexToScenario) {
                Plan planScenario = this.plansScenarios.get(s);
                double eval = planScenario.getEval();
                this.fitnessScenarios.set(s, eval);
                this.beamOnTimeScenarios.set(s, planScenario.getBeamOnTime());
                this.nApertureByScenario.set(s, planScenario.getTotalAperturesUnsed());
            }
        }

        // Update Plan Stats
        this.fitness = Collections.max(fitnessScenarios);
        this.indexToScenario = fitnessScenarios.indexOf(this.fitness);

        // Set Best Personal
        CalculateBestPersonal();

        System.out.println(particleId + ": " + lastFitness + "\t->\t "  + this.fitnessScenarios + "\t->\t " + df.format(this.getFitness()));
    }

    public void OptimizeIntensities() {
        // Se realiza la optimizacion de intensidades
        for (int s = 0; s < EscenarioController.getnScenarios(); s++) {
            if(s == this.indexToScenario) {
                this.plansScenarios.get(s).OptimizeIntensities();
                break;
            }
        }
        this.evalParticle();
        CalculateBestPersonal();
    }

    public void regenerateApertures() {
        double lastFitness = this.fitness;

        for (int s = 0; s < EscenarioController.getnScenarios(); s++) {
            if(s == this.indexToScenario) {
                this.plansScenarios.get(s).regenerateApertures();
                this.plansScenarios.get(s).OptimizeIntensities();
            }
        }
        this.evalParticle();

        // Actualizacion del best personal
        CalculateBestPersonal();
        System.out.println(particleId + ": " + lastFitness + "\t->\t "  + this.fitnessScenarios + "\t->\t " + df.format(this.getFitness()));

    }

    /*---------------------------------------------------- PSO METHODS--------------------------------------------------------------------------------*/

    public void CalculateVelocity(double c1Aperture, double c2Aperture, double wAperture, double cnAperture,
            double c1Intensity, double c2Intensity, double wIntensity, double cnIntensity, Particle bGlobal) {

        for (int s = 0; s < this.plansScenarios.size(); s++) {
            if(s == this.indexToScenario){
                Plan bestPersonal = this.bestPersonal.plansScenarios.get(s);
                Plan bestGlobal = bGlobal.plansScenarios.get(s);
                this.plansScenarios.get(s).CalculateVelocity(c1Aperture, c2Aperture, wAperture, cnAperture,
                        c1Intensity, c2Intensity, wIntensity, cnIntensity, bestGlobal, bestPersonal);
            }
        }
    }

    public void CalculatePosition() {

        for (int s = 0; s < this.plansScenarios.size(); s++) {
            if(s == this.indexToScenario){
                this.plansScenarios.get(s).CalculatePosition();
            }
        }
    }

    public void CalculateBestPersonal() {
        if (this.fitness < this.bestFitness) {
            setBestPersonal();
            setBestFitness();
        }
    }

    public ArrayList<Double> evaluateInAllScenarios() {
        Plan p = this.plansScenarios.get(this.indexToScenario);
        return p.evaluateInAllScenarios();
    }

    /*---------------------------------------------------- GETTER AND SETTERS ----------------------------------------------*/
    public ArrayList<Integer> getTotalUnUsedApertures() {
        Plan currentPlan = this.plansScenarios.get(this.indexToScenario);
        return currentPlan.getAperturesUnUsed();
    }

    public Plan getPlanFromScenario(int indexScenario) {
        return this.plansScenarios.get(indexScenario);
    }

    public int getAperturesUnUsed() {
        Plan currentPlan = this.plansScenarios.get(this.indexToScenario);
        return currentPlan.getTotalAperturesUnsed();
    }

    public double getBeamOnTime() {
        Plan currentPlan = this.plansScenarios.get(this.indexToScenario);
        return currentPlan.getBeamOnTime();
    }

    public void setBestFitness() {
        this.bestFitness = this.fitness;
    }

    public void setBestPersonal() {
        this.bestPersonal = new Particle(this);
    }

    public Plan getCurrentPlan() {
        Plan currentPlan = this.plansScenarios.get(this.indexToScenario);
        return currentPlan;
    }

    public void setC1Aperture(double c1Aperture) {
        this.c1Aperture = c1Aperture;
    }

    public void setC2Aperture(double c2Aperture) {
        this.c2Aperture = c2Aperture;
    }

    public void setInertiaAperture(double innerAperture) {
        this.innerAperture = innerAperture;
    }

    public void setCnAperture(double cnAperture) {
        this.cnAperture = cnAperture;
    }

    public void setC1Intensity(double c1Intensity) {
        this.c1Intensity = c1Intensity;
    }

    public void setC2Intensity(double c2Intensity) {
        this.c2Intensity = c2Intensity;
    }

    public void setInertiaIntensity(double innerIntensity) {
        this.innerIntensity = innerIntensity;
    }

    public void setCnIntensity(double cnIntensity) {
        this.cnIntensity = cnIntensity;
    }

    public void setBestGlobal(Particle p) {
        this.bestGlobal = null;
        this.bestGlobal = new Particle(p);
    }

    public void setSetupRunnerThread(int idSetup) {
        this.setupRunnerThread = idSetup;
    }

    public double getFitness() {
        return this.fitness;
    }

    public double getBestFitness() {
        return this.bestFitness;
    }

    public ArrayList<Double> getFitnessScenarios() {
        return this.fitnessScenarios;
    }

    // ---------- THREADS METHODS (NO TOCAR)
    // ----------------------------------
    @Override
    public void run() {
        switch (setupRunnerThread) {

            case MOVEMENT_THREAD:
                // Prevent NullPointerException by checking bestGlobal and bestPersonal
                if (bestGlobal == null || bestPersonal == null) {
                    System.err.println("Error: bestGlobal or bestPersonal is not set for Particle " + particleId);
                    break;
                }
                // Calcular velocidad
                this.CalculateVelocity(c1Aperture, c2Aperture, innerAperture, cnAperture,
                        c1Intensity, c2Intensity, innerIntensity, cnIntensity, bestGlobal);
                // Calcular posicion
                this.CalculatePosition();

                this.bestGlobal = null;
                break;


            case EVAL_THREAD:
                // Evaluar particula
                this.evalParticle();
                break;

            case OPTIMIZE_THREAD:

                this.OptimizeIntensities();

                break;

            case REPAIR_SOLUTION:

                this.regenerateApertures();

                break;
        }
    }

}
