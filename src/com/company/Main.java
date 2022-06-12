package com.company;
import Swarms.*;
import Utils.Reporter;

import Utils.Reporter;
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
        for(int i = 0; i < args.length  ; i+=2){
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

        String indexFile = "./data/index_instances.txt";
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
        
        folder_coord = "./data/"+folder_coord+"/";
        String instance_file = folder_coord + "Instance.txt";
        String coordinate_file = folder_coord + "coordinates_instance.txt";
        System.out.println(folder_coord);
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
    public static ArrayList<Volumen> createVolumes(String org_filename) throws FileNotFoundException {
        Vector<String> orgFiles = new Vector<>();
        ArrayList<Volumen> Volumes = new ArrayList<>();
        String line;
        int nline = 0;
        System.out.println("ORG FILE: "+ org_filename);
        File of = new File(org_filename);
        if(!of.exists()){
            System.out.println("ERROR: NO SE ENCUENTRA EL ARCHIVO " + org_filename);
            System.exit(-1);
        }
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
        int instanceId = 73;

        //MLC Configuration    
        int max_intensity = 10; //10 x apertura - probar este parametro
        int minIntensity = 1;
        int initial_intensity = 4;
        int step_intensity = 2;
        int open_apertures = -1;
        int max_apertures = 5;

        //Particle configuration
        int setup = 4;
        int diffSetup = 4;
        int nThreads = 3;
        boolean optimizedIntensity = false;
        /*
            OPEN_MIN_SETUP = 0; OPEN_MAX_SETUP = 1; 
            CLOSED_MIN_SETUP = 2; CLOSED_MAX_SETUP = 3;
            RAND_RAND_SETUP = 4;
        */

        //Parametros PSO
        int size = 10;                                  //Particle size
        int iter = 21;                                  //Pso Iterations
        
        double c1Aperture = 1.8751; //0.9321;           // Coef Global
        double c2Aperture = 0.2134; //0.9949;           // Coef Personal
        double innerAperture = 0.5774; //0.1314;        // Inner
        double cnAperture = 1.6641; //1.4638;           // constriction Factor

        double c1Intensity = 0.3158; //0.48;            // Coef Global
        double c2Intensity = 1.7017; //1.4577;          // Coef Personal
        double innerIntensity = 0.5331; //0.8432;       // Inner
        double cnIntensity =  1.2389; //0.9911;         // constriction Factor
        
        if(params.containsKey("size")) 
            size = Integer.parseInt(params.get("size"));
        if(params.containsKey("iter"))
            iter = Integer.parseInt(params.get("iter"));
        if(params.containsKey("c1Aperture")) 
            c1Aperture = Double.parseDouble(params.get("c1Aperture"));
        if(params.containsKey("c2Aperture")) 
            c2Aperture = Double.parseDouble(params.get("c2Aperture"));
        if(params.containsKey("inerAperture")) 
            innerAperture = Double.parseDouble(params.get("inerAperture"));
        if(params.containsKey("cnAperture")) 
            cnAperture = Double.parseDouble(params.get("cnAperture"));
        
        if(params.containsKey("c1Intensity")) 
            c1Intensity = Double.parseDouble(params.get("c1Intensity"));
        if(params.containsKey("c2Intensity")) 
            c2Intensity = Double.parseDouble(params.get("c2Intensity"));
        if(params.containsKey("inerIntensity")) 
            innerIntensity = Double.parseDouble(params.get("inerIntensity"));
        if(params.containsKey("cnIntensity")) 
            cnIntensity = Double.parseDouble(params.get("cnIntensity"));
        if(params.containsKey("i")) 
            instanceId = Integer.parseInt(params.get("i"));
        if(params.containsKey("nThreads")){
            nThreads = Integer.parseInt(params.get("nThreads"));
        }
        if(params.containsKey("intensityOptimized")){
            optimizedIntensity = false;
        }

        //iter = 40000/size;

        System.out.println("Instance " + instanceId );
        System.out.println("Size: "+ size+ "- iter: "+ iter); 
        System.out.println("Aperture  - c1: "+ c1Aperture  + "- c2: "+ c2Aperture  + "- w: " + innerAperture + "- cn: "+ cnAperture);
        System.out.println("Intensity - c1: "+ c1Intensity + "- c2: "+ c2Intensity + "- w: " + innerIntensity + "- cn: "+ cnIntensity);
        System.out.println("Optimization: " + optimizedIntensity  +" - nThreads: " + nThreads);
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
        
        Pair<String,String> dataToLoad = getInstanceById(instanceId);
        String file = dataToLoad.getFirst();
        String file2 = dataToLoad.getSecond();

        Vector<Integer> angles = get_angles(file);

        // Informacion del Collimator
        Collimator collimator = new Collimator(file2, angles);

        // Informacion de los organos desde DDM
        ArrayList<Volumen> volumes = createVolumes(file);
        
        ArrayList<Integer> maxApertures = new ArrayList<>();
        for (int a = 0; a < angles.size(); a++) {
            maxApertures.add(max_apertures);
        }

        // Creating the swarm
        Swarm swarm = new Swarm(w, Zmin, Zmax, maxApertures, max_intensity, minIntensity, initial_intensity, step_intensity, open_apertures, setup, diffSetup, volumes, collimator,
                                c1Aperture, c2Aperture, innerAperture, cnAperture,
                                c1Intensity, c2Intensity, innerIntensity, cnIntensity, size, iter, nThreads, optimizedIntensity);

        swarm.MoveSwarms();

        //Get the Solution of the algorithm
        Particle particle = swarm.getBestGlobalParticle();

        Reporter r = new Reporter(particle, 6);
    }
}
