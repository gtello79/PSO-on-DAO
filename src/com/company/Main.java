package com.company;
import Swarms.*;
import Utils.CreateCSV;
import source.*;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Scanner;

public class Main {

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

        int max_intensity = 10; //10 x apertura - probar este parametro
        int initial_intensity = 4;
        int step_intensity = 2;
        int open_apertures = -1;

        ArrayList<Integer> maxApertures = new ArrayList<>();

        int setup = 4;
        int diffSetup = 0;

        /*
            OPEN_MIN_SETUP = 0;
            OPEN_MAX_SETUP = 1;
            CLOSED_MIN_SETUP = 2 ;
            CLOSED_MAX_SETUP = 3;
            RAND_RAND_SETUP = 4;
        */

        //Parametros PSO
        int size = 1; //Particle size
        int iter = 1; //Pso Iterations

        double generalValue = 1;
        double c1Aperture = generalValue;
        double c2Aperture = generalValue;
        double inerAperture = 1;

        double c1Intensity = 1;
        double c2Intensity = 1;
        double inerIntensity = 1;

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

        for (int a = 0; a < angles.size(); a++) {
            maxApertures.add(5);
        }

        Swarm swarm = new Swarm(w, Zmin, Zmax, maxApertures, max_intensity, initial_intensity, step_intensity, open_apertures, setup, diffSetup, volumes, collimator,
                                c1Aperture, c2Aperture, inerAperture, c1Intensity, c2Intensity, inerIntensity, size, iter);
        swarm.MoveSwarms();


        Particle particle = swarm.getBestGlobalParticle();

        CreateCSV createCSV = new CreateCSV(particle);

        createCSV.collimatorIndex(collimator);
    }
}
