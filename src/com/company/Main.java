package com.company;
import Swarms.*;
import Utils.CreateCSV;
import source.*;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import java.util.Scanner;
import java.util.HashMap;

public class Main {

    public static HashMap<String, String> mappingArg(String [] args){
        
        HashMap<String, String> params = new HashMap<>();

        for(int i = 0; i < args.length ; i+=2){
            String param = args[i];
            String value = args[i+1];
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

    public static Pair<String,String> getInstanceById(Integer index) throws FileNotFoundException {

        String indexFile = "src/data/index_instances.txt";
        String folder_coord = "1_1";
        File of = new File(indexFile);

        Scanner reading = new Scanner(of);
        
        int nLine = 1;
        while(reading.hasNextLine()){
            String linea = reading.nextLine();
            if (nLine == index){
                folder_coord = linea;
                break;
            }    
            nLine++;
        }
        
        folder_coord = "src/data/"+folder_coord+"/";
        String instance_file = folder_coord + "Instance.txt";
        String coordinate_file = folder_coord + "coordinates_instance.txt";
        
        reading.close();
        return new Pair(instance_file, coordinate_file);
    }


    public static Vector<Integer> get_angles(String nameFile) throws FileNotFoundException {
        File testInstance = new File(nameFile);
        Vector<Integer> angles = new Vector<>();
        Scanner reading = new Scanner(testInstance);
        int nLine = 0;
        while(reading.hasNextLine()){
            String linea = reading.nextLine();
            char [] arreglo = linea.toCharArray();
            String to_add = "";
            int angle;
            if (nLine == 0){
                for(char index : arreglo){
                    if (index == ' '){
                        angle = Integer.parseInt(to_add);
                        angles.add(angle);
                        to_add = "";
                    }else {
                        to_add += Character.toString(index);
                    }
                }
                if(isNumeric(to_add)){
                    angle = Integer.parseInt(to_add);
                    angles.add(angle);
                }
           }
            nLine++;
        }
        reading.close();
        return angles;
    }

    //Se pasa el archivo src/data/test_instance_0_70_140_210_280.txt
    public static Vector<Volumen> createVolumes(String org_filename) throws FileNotFoundException {
        Vector<String> orgFiles = new Vector<>();
        Vector<Volumen> Volumes = new Vector<>();
        String line;
        int nline = 0;
        File of = new File(org_filename);
        if(!of.exists())
            System.out.println("ERROR: NO SE ENCUENTRA EL ARCHIVO " + org_filename);
        System.out.println("READING VOLUMEN FILES");
        Scanner reading = new Scanner(of);
        while(reading.hasNextLine()){
            line = reading.nextLine();
            if(nline > 0){
                if(line.isEmpty()) continue;
                orgFiles.add(line);
            }
            nline++;
        }
        reading.close();

        for(String orgFile: orgFiles){
            Volumen to_add = new Volumen(orgFile);
            Volumes.add(to_add);
        }

        return Volumes;
    }

    public static void main(String[] args) throws FileNotFoundException {

        HashMap<String, String> params = mappingArg(args);

        //Instance 0-70-140-210-280 CERR PACKAGE
        int instanceId = 71;

        //MLC Configuration    
        int max_intensity = 10; //10 x apertura - probar este parametro
        int initial_intensity = 4;
        int step_intensity = 2;
        int open_apertures = -1;

        //Particle configuration
        int setup = 4;
        int diffSetup = 4;
        int nThreads = 1;
        /*
            OPEN_MIN_SETUP = 0; OPEN_MAX_SETUP = 1; 
            CLOSED_MIN_SETUP = 2; CLOSED_MAX_SETUP = 3;
            RAND_RAND_SETUP = 4;
        */

        //Parametros PSO
        int size = 20; //Particle size
        int iter = 20; //Pso Iterations
        
        double c1Aperture = 1;          // Coef Global
        double c2Aperture = 1;          // Coef Personal
        double inerAperture = 1;        // Inner
        double cnAperture = 1; // constriction Factor

        double c1Intensity = 1;         // Coef Global
        double c2Intensity = 1;         // Coef Personal
        double inerIntensity = 1;       // Inner
        double cnIntensity = 1; // constriction Factor
        
        if(params.containsKey("size")) 
            size = Integer.parseInt(params.get("size"));
        if(params.containsKey("iter")) 
            iter = Integer.parseInt(params.get("iter"));
        
        if(params.containsKey("c1Aperture")) 
            c1Aperture = Double.parseDouble(params.get("c1Aperture"));
        if(params.containsKey("c2Aperture")) 
            c2Aperture = Double.parseDouble(params.get("c2Aperture"));
        if(params.containsKey("inerAperture")) 
            inerAperture = Double.parseDouble(params.get("inerAperture"));
        if(params.containsKey("cnAperture")) 
            cnAperture = Double.parseDouble(params.get("cnAperture"));
        
        if(params.containsKey("c1Intensity")) 
            c1Intensity = Double.parseDouble(params.get("c1Intensity"));
        if(params.containsKey("c2Intensity")) 
            c2Intensity = Double.parseDouble(params.get("c2Intensity"));
        if(params.containsKey("inerIntensity")) 
            inerIntensity = Double.parseDouble(params.get("inerIntensity"));
        if(params.containsKey("cnIntensity")) 
            cnIntensity = Double.parseDouble(params.get("cnIntensity"));
        if(params.containsKey("i")) 
            instanceId = Integer.parseInt(params.get("i"));
        if(params.containsKey("nThreads"))
            nThreads = Integer.parseInt(params.get("nThreads"));
            if(nThreads > 3){
                nThreads = 3;
            }

        System.out.println("Size: "+ size+ "- iter: "+ iter); 
        System.out.println("Aperture  - c1: "+ c1Aperture  + "- c2: "+ c2Aperture  + "- w: " + inerAperture); 
        System.out.println("Intensity - c1: "+ c1Intensity + "- c2: "+ c2Intensity + "- w: " + inerIntensity); 
        System.out.println("nThreads: " + nThreads);
        Vector<Double> w = new Vector<>();
        w.add(1.0);
        w.add(1.0);
        w.add(5.0); //valor 5.0

        Vector<Double> Zmin = new Vector<>();
        Zmin.add(0.0);
        Zmin.add(0.0);
        Zmin.add(76.0);


        Vector<Double> Zmax = new Vector<>();
        Zmax.add(65.0);
        Zmax.add(65.0);
        Zmax.add(76.0);
        
        Pair<String,String> dataToLoad = getInstanceById(instanceId);
        String file = dataToLoad.getFirst();
        String file2 = dataToLoad.getSecond();

        Vector<Integer> angles = get_angles(file);

        // Informacion del Collimator
        Collimator collimator = new Collimator(file2, angles);

        // Informacion de los organos desde DDM
        Vector<Volumen> volumes = createVolumes(file);
        
        ArrayList<Integer> maxApertures = new ArrayList<>();
        for (int a = 0; a < angles.size(); a++) {
            maxApertures.add(5);
        }


        Long initialAlgorithmTime = System.currentTimeMillis();
        // Creating the swarm
        Swarm swarm = new Swarm(w, Zmin, Zmax, maxApertures, max_intensity, initial_intensity, step_intensity, open_apertures, setup, diffSetup, volumes, collimator,
                                c1Aperture, c2Aperture, inerAperture, cnAperture,
                                c1Intensity, c2Intensity, inerIntensity, cnIntensity, size, iter, nThreads);
        
        swarm.MoveSwarms();
        Long finalAlgorithmTime = System.currentTimeMillis();
        System.out.println("Processing Time: " + ((finalAlgorithmTime - initialAlgorithmTime) / 1000) + " [seg]");
        /*
        //Get the Solution of the algorithm
        Particle particle = swarm.getBestGlobalParticle();
        */

    }
}