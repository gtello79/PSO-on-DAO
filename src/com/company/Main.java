package com.company;
import Swarms.*;
import source.*;

import java.io.FileNotFoundException;
import java.io.File;
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
        Vector<Integer> angles = new Vector<Integer>();
        Scanner reading = new Scanner(testInstance);
        int nLine = 0;
        while(reading.hasNextLine()){
            String linea = reading.nextLine();
            char [] arreglo = linea.toCharArray();
            String to_add = "";
            Integer angle = 0;
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
    };

    //Se pasa el archivo src/data/test_instance_0_70_140_210_280.txt
    public static Vector<Volumen> createVolumes(String org_filename, Collimator collimator) throws FileNotFoundException {
        Vector<String> orgn_files = new Vector<String>();
        Vector<Volumen> Volumes = new Vector<Volumen>();
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
                orgn_files.add(line);
            }
            nline++;
        }
        reading.close();
        for(int i = 0; i < orgn_files.size(); i++){
            String actual_file = orgn_files.get(i);
                Volumen to_add = new Volumen(collimator,actual_file);
            Volumes.add(to_add);
        }
        return Volumes;
    }

    public static void main(String[] args) throws FileNotFoundException {
        /* Debo hacer que lea todo esto a partir de un archivo de prueba */
        String file = "src/data/test_instance_0_70_140_210_280.txt";
        String file2 = "src/data/test_instance_coordinates.txt";
        String sDirectorioTrabajo = System.getProperty("user.dir");
        System.out.println("El directorio de trabajo es " + sDirectorioTrabajo);
        int size = 10;
        int iter = 10;
        double c1 = 1;
        double c2 = 1;
        double iner = 1;
        Vector <Double> w  = new Vector<Double>();
        Vector <Double> Zmin = new Vector<Double>();
        Vector <Double> Zmax = new Vector<Double>();;
        Vector <Integer> angles = get_angles(file);
        Collimator collimator = new Collimator(file2,angles);
        Vector <Volumen> volumes = createVolumes(file,collimator);
        int max_apertures = 0;
        int max_intensity = 0;
        int initial_intensity = 0;
        int step_intensity = 0;
        int open_apertures = 0;
        int setup = 0;

        Swarm poblacion = new Swarm(w, Zmin, Zmax, max_apertures, max_intensity, initial_intensity, step_intensity, open_apertures, setup);
        poblacion.MoveSwarms(c1, c2, iner, iter);

    }
}
