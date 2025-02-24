package com.company;

import Swarms.*;

import source.Volumen;
import source.Collimator;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import java.util.Scanner;
import java.util.HashMap;
import javafx.util.Pair;

public class Main {

    public static String EXPERIMENT_PATH = "./ExperimentsFiles/";
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

    public static boolean isNumeric(String cadena) {

        boolean resultado;

        try {
            Integer.parseInt(cadena);
            resultado = true;
        } catch (NumberFormatException excepcion) {
            resultado = false;
        }

        return resultado;
    }

    public static Pair<String, String> getInstanceById(Integer index) throws FileNotFoundException {

        String folder_coord = "1_1";
        File of = new File(INSTANCE_FILE);
        Scanner reading = new Scanner(of);

        int nLine = 1;
        while (reading.hasNextLine()) {
            String linea = reading.nextLine();
            if (nLine == index) {
                folder_coord = DATA_FILE.concat(linea).concat("/");
                break;
            }
            nLine++;
        }
        reading.close();

        String instance_file = folder_coord.concat("Instance.txt");
        String coordinate_file = folder_coord.concat("coordinates_instance.txt");
        return new Pair<>(instance_file, coordinate_file);
    }

    public static Vector<Integer> get_angles(String nameFile) throws FileNotFoundException {
        File testInstance = new File(nameFile);
        Vector<Integer> angles = new Vector<>();
        Scanner reading = new Scanner(testInstance);
        int nLine = 0;
        while (reading.hasNextLine()) {
            if (nLine == 0) {
                String linea = reading.nextLine();
                String[] angles_list_str = linea.split(" ");
                for(String angle_str: angles_list_str){
                    Integer angle = Integer.parseInt(angle_str);
                    angles.add(angle);
                }
            }else{
                break;
            }
            nLine++;
        }
        reading.close();
        return angles;
    }

    // Se pasa el archivo src/data/test_instance_0_70_140_210_280.txt
    public static ArrayList<Volumen> createVolumes(String org_filename) throws FileNotFoundException {
        Vector<String> orgFiles = new Vector<>();
        ArrayList<Volumen> Volumes = new ArrayList<>();
        String line;
        int nline = 0;
        System.out.println("ORG FILE: " + org_filename);
        File of = new File(org_filename);
        if (!of.exists()) {
            throw new FileNotFoundException("ERROR: NO SE ENCUENTRA EL ARCHIVO " + org_filename);
        }

        Scanner reading = new Scanner(of);
        while (reading.hasNextLine()) {
            line = reading.nextLine();
            if (nline > 0) {
                if (line.isEmpty())
                    continue;
                orgFiles.add(line);
            }
            nline++;
        }
        reading.close();

        for (String orgFile : orgFiles) {
            Volumen to_add = new Volumen(orgFile);
            Volumes.add(to_add);
        }

        return Volumes;
    }

    public static void main(String[] args) throws FileNotFoundException {

        HashMap<String, String> params = mappingArg(args);
        ArrayList<Integer> maxApertures = new ArrayList<>();
        
        // MLC Configuration
        int instanceId = 71;
        int max_intensity = 20; // Apertura - probar este parametro
        int minIntensity = 0;
        int initial_intensity = 4;
        int step_intensity = 2;
        int open_apertures = -1;
        int max_apertures = 5;

        // Particle configuration
        int setup = 4;
        int diffSetup = 4;
        int nThreads = 3;
        boolean optimizedIntensity = false;

        /*
         * OPEN_MIN_SETUP = 0; OPEN_MAX_SETUP = 1;
         * CLOSED_MIN_SETUP = 2; CLOSED_MAX_SETUP = 3;
         * RAND_RAND_SETUP = 4;
         */

        // Parametros PSO
        int size = 10;                   // SWARM size
        int iter = 10;                  // Pso Iterations

        double c1Aperture = 1.8751;     // Coef Global
        double c2Aperture = 0.2134;     // Coef Personal
        double innerAperture = 0.5774;  // Inner
        double cnAperture = 1.6641;     // constriction Factor

        double c1Intensity = 0.3158;    // Coef Global
        double c2Intensity = 1.7017;    // Coef Personal
        double innerIntensity = 0.5331; // Inner
        double cnIntensity = 1.2389;    // constriction Factor

        if (params.containsKey("size"))
            size = Integer.parseInt(params.get("size"));
        if (params.containsKey("iter"))
            iter = Integer.parseInt(params.get("iter"));
        if (params.containsKey("c1Aperture"))
            c1Aperture = Double.parseDouble(params.get("c1Aperture"));
        if (params.containsKey("c2Aperture"))
            c2Aperture = Double.parseDouble(params.get("c2Aperture"));
        if (params.containsKey("inerAperture"))
            innerAperture = Double.parseDouble(params.get("inerAperture"));
        if (params.containsKey("cnAperture"))
            cnAperture = Double.parseDouble(params.get("cnAperture"));

        if (params.containsKey("c1Intensity"))
            c1Intensity = Double.parseDouble(params.get("c1Intensity"));
        if (params.containsKey("c2Intensity"))
            c2Intensity = Double.parseDouble(params.get("c2Intensity"));
        if (params.containsKey("inerIntensity"))
            innerIntensity = Double.parseDouble(params.get("inerIntensity"));
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
        
        // Print parameters configurate on experiments
        System.out.println("Instance " + instanceId);
        System.out.println("Size: " + size + "- iter: " + iter);
        System.out.println("Aperture  - c1: " + c1Aperture + "- c2: " + c2Aperture + "- w: " + innerAperture + "- cn: "
                + cnAperture);
        System.out.println("Intensity - c1: " + c1Intensity + "- c2: " + c2Intensity + "- w: " + innerIntensity
                + "- cn: " + cnIntensity);
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

        Pair<String, String> dataToLoad = getInstanceById(instanceId);
        String volumenFile = dataToLoad.getKey();
        String collimatorFile = dataToLoad.getValue();

        Vector<Integer> angles = get_angles(volumenFile);

        // Informacion del Collimator
        Collimator collimator = new Collimator(collimatorFile, angles);

        // Informacion de los organos desde DDM
        ArrayList<Volumen> volumes = createVolumes(volumenFile);

        // Build the maxApertures by Beam Angle
        for (int a = 0; a < angles.size(); a++) {
            maxApertures.add(max_apertures);
        }

        // Creating the swarm
        Swarm swarm = new Swarm(w, Zmin, Zmax, maxApertures, max_intensity, minIntensity, initial_intensity,
                step_intensity, open_apertures, setup, diffSetup, volumes, collimator,
                c1Aperture, c2Aperture, innerAperture, cnAperture,
                c1Intensity, c2Intensity, innerIntensity, cnIntensity, size, iter, nThreads, optimizedIntensity);

        swarm.MoveSwarms();
        /** 
         * 
         Particle particle = swarm.getBestGlobalParticle();
         if (exportIntensityVector) {
            // Get the Solution of the algorithm
            Reporter r = new Reporter(particle, 6);
        }
        */

    }
}
