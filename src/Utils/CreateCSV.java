package Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import SRCDAO.Aperture;
import SRCDAO.Beam;
import SRCDAO.Plan;
import Swarms.Particle;
import source.Collimator;
import source.Matrix;
import source.Pair;

public class CreateCSV {
    private final Integer id = (int)(50*Math.random());

    String UID = String.valueOf(id);

    public CreateCSV(Particle particle) {
        String intensityFolderPath = "./intensityFolder/";
        String apertureFolderPath = "./apertureFolder/";
        Plan plan = particle.getCurrentPlan();

        for(Beam beam: plan.getAngle_beam()){
            String fileName1 = UID + "-intensityMatrix"+beam.getIdBeam();
            String fileName2 = UID + "-Apertures"+beam.getIdBeam();
            Matrix matrix = beam.getIntensitisMatrix();
            intensityMatrixToCSV(intensityFolderPath, fileName1, beam.getIdBeam(), matrix);
            int gdim = beam.getCollimatorDim();
            apertureMatrix(apertureFolderPath, fileName2, beam.getIdBeam(), beam.getApertures(), gdim);
        }

        System.out.println("DONE - UID Experiment: " + id);
        //System.out.println("Aperture Folder on " + apertureFolderPath + " - Intensity Folder on " + intensityFolderPath );
        IntensityVector(plan);
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

    private void apertureMatrix(String path, String fileName, int angle, Vector<Aperture> apertureVector, int gDim){
        final String NextLine = "\n";
        final String delimiter = ",";
        try{
            FileWriter matrixCSV = new FileWriter(path+fileName+ ".csv");
            matrixCSV.append(String.valueOf(angle));
            matrixCSV.append(NextLine);
            for(Aperture a : apertureVector){
                String apertureChain = "";
                Vector<Pair<Integer, Integer>> shapes = a.getApertures();
                apertureChain += Double.toString(a.getIntensity()) + '\n';

                //Filas de aperturas
                for(int i = 0; i < gDim; i++){
                    //Columnas de las aperturas
                    for(int j = 0; j < gDim; j++){

                        if(j > shapes.get(i).getFirst() && j < shapes.get(i).getSecond() && shapes.get(i).getFirst() != -2 ){
                            apertureChain += 1;
                        }else if (shapes.get(i).getFirst() == -2){
                            apertureChain += -1;
                        }else{
                            apertureChain += 0;
                        }
                        apertureChain += delimiter;
                    }
                    apertureChain+="\n";
                }
                apertureChain+="\n";
                matrixCSV.append(apertureChain);
            }
            matrixCSV.flush();
            matrixCSV.close();
        }catch (IOException e){
            e.printStackTrace();
        }


    }

    public void collimatorIndex(Collimator collimator){
        String collimatorPath = "./import/collimatorIndex/";
        Vector<Integer> angles = collimator.getAngles();
        for(Integer angle: angles){
            Vector<Pair<Double,Double>> angleCoordMatr = collimator.getAngleCoordMatr().get(angle);
            Vector<Pair<Integer,Integer>> angleCoordNew = collimator.getAngleCoord().get(angle);
            if(angleCoordNew.size() != angleCoordMatr.size()){
                System.out.println("ERROR, LOS BEAMLETS NO TIENEN LA MISMA DIMENSION, REVISAR COLLIMATOR");
            }else{
                int Seamless = angleCoordNew.size();
                String fileName = UID + "- COLLIMATOR INDEX ROW "+angle;
                try{
                    FileWriter matrixCSV = new FileWriter(collimatorPath+fileName+ ".csv");
                    matrixCSV.append(String.valueOf(angle));
                    matrixCSV.append("\n");
                    for(int i = 0; i < Seamless; i++){
                        Pair<Double,Double> oldRow = angleCoordMatr.get(i);
                        Pair<Integer,Integer> newRow = angleCoordNew.get(i);
                        String rowString = (i+1) +" <"+oldRow.getFirst() + "," + oldRow.getSecond() +"> ---- <" + newRow.getFirst() + "," + newRow.getSecond() +"> \n";
                        matrixCSV.append(rowString);
                    }
                    matrixCSV.flush();
                    matrixCSV.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }

        }
    }

    public void IntensityVector(Plan p){
        Vector<Integer> nBeamLetsByBeam = new Vector<>();
        String IntensityVectorPath = "./import/";
        String fileName = UID +" - FluenceMap.csv";
        for(Beam b: p.getAngle_beam())
            nBeamLetsByBeam.add(b.getTotalBeamlets());
        System.out.println(nBeamLetsByBeam);
        int beamIndex = 0;
        try{
            FileWriter matrixCSV = new FileWriter(IntensityVectorPath+fileName+ ".csv");
            String vectorChain = "";
            int count = 0;
            for(double i: p.getFluenceMap()){
                vectorChain += i + " \n";
                count++;
                if(count == nBeamLetsByBeam.get(beamIndex)){
                    vectorChain += " \n";
                    beamIndex++;
                    count = 0;
                }


            }
            matrixCSV.append(vectorChain);
            matrixCSV.flush();
            matrixCSV.close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

}