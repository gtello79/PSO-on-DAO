package com.company;

import Swarms.Particle;
import Swarms.Swarm;
import Test.EvaluationAlg;
import source.Collimator;
import source.EvaluationFunction;
import Utils.Gurobi_Solver;

import java.io.IOException;
import java.util.ArrayList;

import java.util.HashMap;

import robust.EscenarioController;
import SRCDAO.ApertureEnum;

public class Main {

    public static String INSTANCE_FILE = "./data/index_instances.txt";
    public static String DATA_FILE = "./data/";

    public static HashMap<String, String> mappingArg(String[] args) {

        HashMap<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            String param = args[i];
            String value = args[i + 1];
            params.put(param, value);
        }
        return params;
    }

    public static void main(String[] args) throws IOException {

        HashMap<String, String> params = mappingArg(args);
        ArrayList<Integer> maxApertures = new ArrayList<>();

        // MLC Configuration
        int instanceId = 92;

        int max_intensity = 5;
        int minIntensity = 0;
        int initial_intensity = 4;
        int step_intensity = 2;
        int open_apertures = -1;
        int max_apertures = 5;

        // Particle configuration
        int setup = ApertureEnum.RAND_RAND_SETUP.getValue();
        int diffSetup = 4;
        int nThreads = 3;
        boolean optimizedIntensity = true;
        boolean postCheck = true;

        /*
         * OPEN_MIN_SETUP = 0; OPEN_MAX_SETUP = 1;
         * CLOSED_MIN_SETUP = 2; CLOSED_MAX_SETUP = 3;
         * RAND_RAND_SETUP = 4;
         */

        // Parametros PSO
        int IRACESIZE = 518;
        int IRACESIZEROBUST = 200;
        int size = IRACESIZEROBUST; // SWARM size
        int iter = 40000 / size; // Pso Iterations

        double c1Aperture = 1.8751; // Coef Global
        double c2Aperture = 0.2134; // Coef Personal
        double wMaxAperture = 0.9;
        double wMinAperture = 0.4;
        double cnAperture = 1.6641; // constriction Factor

        double c1Intensity = 0.3158; // Coef Global
        double c2Intensity = 1.7017; // Coef Personal
        double wMaxIntensity = 0.9;
        double wMinIntensity = 0.4;
        double cnIntensity = 1.2389; // constriction Factor

        if (params.containsKey("size"))
            size = Integer.parseInt(params.get("size"));
        if (params.containsKey("iter"))
            iter = Integer.parseInt(params.get("iter"));
        if (params.containsKey("c1Aperture"))
            c1Aperture = Double.parseDouble(params.get("c1Aperture"));
        if (params.containsKey("c2Aperture"))
            c2Aperture = Double.parseDouble(params.get("c2Aperture"));
        if (params.containsKey("cnAperture"))
            cnAperture = Double.parseDouble(params.get("cnAperture"));

        if (params.containsKey("c1Intensity"))
            c1Intensity = Double.parseDouble(params.get("c1Intensity"));
        if (params.containsKey("c2Intensity"))
            c2Intensity = Double.parseDouble(params.get("c2Intensity"));
        if (params.containsKey("cnIntensity"))
            cnIntensity = Double.parseDouble(params.get("cnIntensity"));
        if (params.containsKey("i"))
            instanceId = Integer.parseInt(params.get("i"));
        if (params.containsKey("nThreads")) {
            nThreads = Integer.parseInt(params.get("nThreads"));
        }
        if (params.containsKey("intensityOptimized")) {
            optimizedIntensity = true;
        }
        if (params.containsKey("max_intensity")) {
            max_intensity = Integer.parseInt(params.get("max_intensity"));
        }
        if (params.containsKey("wMaxAperture")) {
            wMaxAperture = Double.parseDouble(params.get("wMaxAperture"));
        }
        if (params.containsKey("wMinAperture")) {
            wMinAperture = Double.parseDouble(params.get("wMinAperture"));
        }
        if (params.containsKey("wMaxIntensity")) {
            wMaxIntensity = Double.parseDouble(params.get("wMaxIntensity"));
        }
        if (params.containsKey("wMinIntensity")) {
            wMinIntensity = Double.parseDouble(params.get("wMinIntensity"));
        }

        // Print parameters configurate on experiments
        System.out.println("Instance " + instanceId);
        System.out.println("Size: " + size + "- iter: " + iter);
        System.out.println("Aperture  - c1: " + c1Aperture + "- c2: " + c2Aperture);
        System.out.println("Intensity - c1: " + c1Intensity + "- c2: " + c2Intensity);
        System.out.println("Optimization: " + optimizedIntensity + " - nThreads: " + nThreads);

        // Considering the weights for each volumen
        ArrayList<Double> w = new ArrayList<>();
        w.add(1.0);
        w.add(1.0);
        w.add(5.0);

        ArrayList<Double> Zmin = new ArrayList<>();
        Zmin.add(0.0);
        Zmin.add(0.0);
        Zmin.add(76.0);

        ArrayList<Double> Zmax = new ArrayList<>();
        Zmax.add(65.0);
        Zmax.add(65.0);
        Zmax.add(76.0);

        // Instantiate the scenario controller
        EscenarioController.startScenario(instanceId);

        // Convert Zmax from an ArrayList to an array
        double[] dd = new double[Zmax.size()];
        for (int i = 0; i < Zmax.size(); i++) {
            dd[i] = Zmax.get(i);
        }
        Gurobi_Solver.activateModel(dd, w);

        // Informacion del Collimator
        Collimator collimator = EscenarioController.getCollimatorFromNominalScenario();

        EvaluationFunction.ActivateEvaluationFunction(w, Zmin, Zmax);

        // Build the maxApertures by Beam Angle
        for (int a = 0; a < collimator.getNbAngles(); a++) {
            maxApertures.add(max_apertures);
        }
        // Creating the swarm
        Swarm swarm = new Swarm(w, Zmin, Zmax, maxApertures, max_intensity, minIntensity, initial_intensity,
            step_intensity, open_apertures, setup, diffSetup,
            c1Aperture, c2Aperture, cnAperture, c1Intensity, c2Intensity, cnIntensity,
            size, iter, wMaxAperture, wMinAperture, wMaxIntensity, wMinIntensity,
            nThreads, optimizedIntensity
            
            );

        swarm.MoveSwarms();
        
        if (postCheck) {
            int robustInstance = 85;
            EvaluationAlg evaluationAlgorithm = new EvaluationAlg(
                EscenarioController.getDDMFromNominalScenario(), swarm.getBestGlobalParticle(), w, Zmin, Zmax);

            Particle p = swarm.getBestGlobalParticle();
            
            EscenarioController.startScenario(robustInstance);

            System.out.println(p.evaluateInAllScenarios());
        }
    }
}
