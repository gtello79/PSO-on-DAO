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
        String file = "src/data/test_instance_0_70_140_210_280.txt";
        String file2 = "src/data/test_instance_coordinates.txt";
        
        HashMap<String, String> params = mappingArg(args);

        //MLC Configuration    
        int max_intensity = 10; //10 x apertura - probar este parametro
        int initial_intensity = 4;
        int step_intensity = 2;
        int open_apertures = -1;

        //Particle configuration
        int setup = 4;
        int diffSetup = 4;

        /*
            OPEN_MIN_SETUP = 0; OPEN_MAX_SETUP = 1; 
            CLOSED_MIN_SETUP = 2; CLOSED_MAX_SETUP = 3;
            RAND_RAND_SETUP = 4;
        */

        //Parametros PSO
        String [] parameters = {"","","c2Aperture","","c1Intensity","c2Intensity","inerIntensity"};
        int size = 400; //Particle size
        int iter = 100; //Pso Iterations
        
        double c1Aperture = 1;          // Coef Global
        double c2Aperture = 1;          // Coef Personal
        double inerAperture = 1;        // Inner

        double c1Intensity = 1;         // Coef Global
        double c2Intensity = 1;         // Coef Personal
        double inerIntensity = 1;       // Inner
        
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
        
        if(params.containsKey("c1Intensity")) 
            c1Intensity = Double.parseDouble(params.get("c1Intensity"));
        if(params.containsKey("c2Intensity")) 
            c2Intensity = Double.parseDouble(params.get("c2Intensity"));
        if(params.containsKey("inerIntensity")) 
            inerIntensity = Double.parseDouble(params.get("inerIntensity"));
        

        System.out.println("Size: "+ size+ "- iter: "+ iter); 
        System.out.println("Aperture  - c1: "+ c1Aperture  + "- c2: "+ c2Aperture  + "- w: " + inerAperture); 
        System.out.println("Intensity - c1: "+ c1Intensity + "- c2: "+ c2Intensity + "- w: " + inerIntensity); 


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
        Zmax.add(65.0); //Anterior 60
        Zmax.add(76.0);

        Vector<Integer> angles = get_angles(file);
        Collimator collimator = new Collimator(file2, angles);
        Vector<Volumen> volumes = createVolumes(file);

        ArrayList<Integer> maxApertures = new ArrayList<>();
        for (int a = 0; a < angles.size(); a++) {
            maxApertures.add(5);
        }

        
        // Creating the swarm
        Swarm swarm = new Swarm(w, Zmin, Zmax, maxApertures, max_intensity, initial_intensity, step_intensity, open_apertures, setup, diffSetup, volumes, collimator,
                                c1Aperture, c2Aperture, inerAperture, c1Intensity, c2Intensity, inerIntensity, size, iter);
        swarm.MoveSwarms();

        //Get the Solution of the algorithm
        //Particle particle = swarm.getBestGlobalParticle();

        //Used to calculate the execution minutes
        // Long finalClock = (System.currentTimeMillis() - initialClock);
        // long totalMinutes = TimeUnit.MILLISECONDS.toMinutes(finalClock);
        // System.out.println("Final time :"+ totalMinutes);

        //Stadistics of the solution
        // CreateCSV createCSV = new CreateCSV(particle);
        // createCSV.collimatorIndex(collimator);
    }
}
