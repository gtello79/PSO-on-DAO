package Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import SRCDAO.Aperture;
import SRCDAO.Beam;
import SRCDAO.Plan;
import Swarms.Particle;
import source.Matrix;

public class CreateCSV {
    private final Integer id = (int)(50*Math.random());

    String UID = String.valueOf(id);

    public CreateCSV(Particle particle) {
        String folderPath = "./Results/";
        Plan plan = particle.getCurrentPlan();

        for(Beam beam: plan.getAngle_beam()){
            String fileName1 = UID+"-intensityMatrix"+beam.getIdBeam();
            Matrix matrix = beam.getIntensitisMatrix();
            intensityMatrixToCSV(folderPath, fileName1, beam.getIdBeam(), matrix);
        }

        System.out.println("DONE - UID Experiment: " + id);
    }

    private void intensityMatrixToCSV(String path ,String fileName, int angle, Matrix matrix){
        final String NextLine = "\n";
        final String delimiter = ",";
        try{
            FileWriter matrixCSV = new FileWriter(path+fileName+ ".csv");
            matrixCSV.append(String.valueOf(angle));
            matrixCSV.append(NextLine);
            for(int i = 0; i < matrix.getX(); i++){
                for(int j = 0; j < matrix.getY(); j++){
                    String value = String.valueOf(matrix.getPos(i,j));
                    matrixCSV.append(value).append(delimiter);
                }
                matrixCSV.append(NextLine);
            }

            matrixCSV.flush();
            matrixCSV.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }
/*
    private void apertureMatrix(String path, String fileName, int angle, Vector<Aperture> apertureVector){
        final String NextLine = "\n";
        final String delimiter = ",";
        try{
            FileWriter matrixCSV = new FileWriter(path+fileName);
            matrixCSV.append(String.valueOf(angle));
            matrixCSV.append(NextLine);
            for(int i = 0; i < matrix.getX(); i++){
                for(int j = 0; j < matrix.getY(); j++){
                    String value = String.valueOf(matrix.getPos(i,j));
                    matrixCSV.append(value).append(delimiter);
                }
                matrixCSV.append(NextLine);
            }

            matrixCSV.flush();
            matrixCSV.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }
*/


}