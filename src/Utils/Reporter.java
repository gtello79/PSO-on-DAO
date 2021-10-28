package Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import SRCDAO.Aperture;
import SRCDAO.Beam;
import SRCDAO.Plan;
import Swarms.Particle;
import source.Collimator;
import source.Matrix;
import source.Pair;

public class Reporter {
    private final Integer id = (int)(80*Math.random());

    private String UID = String.valueOf(id);
    private String intensityFolderPath = "./OutPlots/intensityFolder/";
    private String apertureFolderPath = "./OutPlots/apertureFolder/";
    private String generalFolderPath = "./OutPlots/";
    private static final int INTENSITY_MATRIX_CSV = 1;
    private static final int INTENSITY_MATRIX_TXT = 2;
    private static final int ALL_APERTURES_CSV = 3;
    private static final int ALL_APERTURES_TXT = 4;
    private static final int ALL_APERTURES_AMPL = 5;
    private static final int ALL_COMPONENTS_TXT = 6;


    public Reporter(Particle particle, int function){
        switch (function){
            case INTENSITY_MATRIX_CSV:
                intensityMatrixToCSV(particle);
                break;

            case INTENSITY_MATRIX_TXT:
                break;

            case ALL_APERTURES_CSV:
                apertureMatrix(particle);
                break;

            case ALL_APERTURES_TXT:

                break;

            case ALL_APERTURES_AMPL:
                printAperturesToAMPL(particle);
                break;

            case ALL_COMPONENTS_TXT:
                exportIntensityMatrixAndApertures(particle);
                break;
        }
    }


    private void intensityMatrixToCSV(Particle particle){
        final String NextLine = "\n";
        final String delimiter = ",";
        Plan plan = particle.getCurrentPlan();

        for(Beam beam: plan.getAngle_beam()){

            Matrix matrix = beam.getIntensitisMatrix();
            String fileName = UID + "-intensityMatrix"+beam.getIdBeam();
            String filePath = intensityFolderPath+ fileName+ ".csv";

            try{
                FileWriter matrixCSV = new FileWriter(filePath);
                matrixCSV.append(String.valueOf(beam.getIdBeam()));
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
        System.out.println("DONE - UID Experiment: " + id);
    }

    private void apertureMatrix(Particle particle){
        final String NextLine = "\n";
        final String delimiter = ",";

        Plan plan = particle.getCurrentPlan();

        for(Beam beam: plan.getAngle_beam()){
            int gDim = beam.getCollimatorDim();
            Vector<Aperture> apertureVector = beam.getApertures();
            String fileName2 = UID + "-Apertures"+beam.getIdBeam();

            try{
                FileWriter matrixCSV = new FileWriter(apertureFolderPath+fileName2+ ".csv");
                matrixCSV.append(String.valueOf(beam.getIdBeam()));
                matrixCSV.append(NextLine);
                for(Aperture a : apertureVector){
                    String apertureChain = "";
                    ArrayList<Pair<Integer, Integer>> shapes = a.getApertures();
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
        System.out.println("DONE - UID Experiment: " + id);
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

    private void exportIntensityMatrixAndApertures(Particle particle){
        final String NextLine = "\n";
        final String delimiter = ",";
        Plan tp = particle.getCurrentPlan();
        ArrayList<Beam> beams = tp.getAngle_beam();
        for (int b = 0; b < tp.getNBeam(); b++){
            Beam beam = beams.get(b);
            String fileName = generalFolderPath+UID + "-Desc_Beam"+beam.getIdBeam()+".txt";
            Matrix intensityMatrix = beam.getIntensitisMatrix();
            Vector<Aperture> aperturesSet = beam.getApertures();

            try{
                FileWriter beamTXT = new FileWriter(fileName);
                beamTXT.append("Beam,"+String.valueOf(beam.getIdBeam()));
                beamTXT.append(NextLine);

                // Writting Intensity Matrix on File
                for(int i = 0; i < intensityMatrix.getX(); i++){
                    for(int j = 0; j < intensityMatrix.getY(); j++){
                        String value = String.valueOf(intensityMatrix.getPos(i,j));
                        beamTXT.append(value).append(delimiter);
                    }
                    beamTXT.append(NextLine);
                }
                beamTXT.append(NextLine);

                // Writting Apertures on file
                for(int a = 0; a < aperturesSet.size(); a++){
                    Aperture aperture = aperturesSet.get(a);
                    double intensity = aperture.getIntensity();
                    ArrayList<Pair<Integer,Integer>> shapes = aperture.getApertures();

                    beamTXT.append("Aperture,"+intensity);
                    beamTXT.append(NextLine);

                    for(int i = 0; i < intensityMatrix.getX(); i++){
                        Pair<Integer, Integer> pair = shapes.get(i);
                        for(int j = 0; j < intensityMatrix.getY(); j++){
                            String value;
                            // Cerrada permanentemente
                            if(intensityMatrix.getPos(i,j) == -1){
                                value = String.valueOf(-1);
                            }else{
                                // Abierta
                                if( j > pair.getFirst() && j < pair.getSecond() ){
                                    value = String.valueOf(1);
                                }else{
                                    // Cerrada
                                    value = String.valueOf(0);
                                }
                            }
                            beamTXT.append(value).append(delimiter);
                        }
                        beamTXT.append(NextLine);
                    }
                    beamTXT.append(NextLine);
                }
                beamTXT.flush();
                beamTXT.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        System.out.println("DONE - UID Experiment: " + id);
    }

    public void printAperturesToAMPL(Particle particle){
        Plan tp = particle.getCurrentPlan();
        int[] beamAngles = tp.getBeamletsByBeam();
        for(int i = 0; i < beamAngles.length; i++){
            int totalBeamlets = beamAngles[i];
            for(int a = 0; a < 5; a++){
                System.out.println("param x"+(i+1)+(a+1)+" :=");
                for(int j = 0; j < totalBeamlets; j++){
                    System.out.println((j+1)+"    "+ tp.getProyectedBeamLetByApertureOnBeam(i,a,j));
                }
                System.out.println(";");
                System.out.println("");
            }
        }
    }

}