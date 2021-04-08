package SRCDAO;
import source.*;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Vector;

public class Plan {
    private double eval;
    private int nBeam;
    private int totalBeamLet;

    private Vector<Double> w;
    private Vector<Double> zMin;
    private Vector<Double> zMax;

    private Vector<Beam> Angle_beam;
    private Vector<Double> fluenceMap;
    private EvaluationFunction ev;


    /*---------------------------------------------- METHODS -----------------------------------------------------------------------*/

    public Plan(Vector<Double> w, Vector<Double> zMin, Vector<Double> zMax, ArrayList<Integer> maxApertures, int max_intensity, int initial_intensity, int step_intensity, int open_apertures, int setup, Vector<Volumen> volumen, Collimator collimator) {
        setNBeam(collimator.getNbAngles());
        setW(w);
        setZMin(zMin);
        setZMax(zMax);

        this.Angle_beam = new Vector<>();
        this.ev = new EvaluationFunction(volumen);
        this.totalBeamLet = collimator.getNbBeamlets();

        System.out.println("-------- Initilizing plan.-----------");
        for (int i = 0; i < nBeam; i++) {

            Beam new_beam = new Beam(collimator.getAngle(i), maxApertures.get(i) , max_intensity, initial_intensity, step_intensity, open_apertures, setup, collimator);
            Angle_beam.add(new_beam);

        }

        System.out.println("--Created " + Angle_beam.size() + " Stations Beams");
        eval();
        System.out.println("--Initial Evaluation: " + getEval());
    }


    public Plan(Plan p){
        setEval(p.eval);
        setNBeam(p.nBeam);
        setTotalBeamlet(p.totalBeamLet);

        setW(p.w);
        setZMax(p.zMax);
        setZMin(p.zMin);
        setAngle_beam(p.getAngle_beam());
        setFluenceMap(p.getFluenceMap());

        this.Angle_beam = new Vector<>();
        this.ev = p.ev;
        this.totalBeamLet = p.totalBeamLet;

        for(Beam b: p.getBeams()){
            Beam newBeam = new Beam(b);
            Angle_beam.add(newBeam);
        }

        setEval(p.eval);
    }

    /*Funcion de evaluacion */
    public void eval() {
        //Esta es la nueva forma de evaluar nuestro vector
        fluenceMap = getFluenceMap();
        double val = ev.evalIntensityVector(fluenceMap, w, zMin, zMax);
        setEval(val);
    }

    /* --------------------------------------- PSO METHODS ---------------------------------------- */

    //Funcion que realiza la actualizacion de la velocidad de la particula
    public void CalculateVelocity(double c1, double c2, double w, Plan Bsolution, Plan Bpersonal) {
        //Bsolution: Best Global solution ; Bpersonal: Best Personal solution
        for (Beam actual : Angle_beam) {
            Beam B_Bsolution = Bsolution.getByID(actual.getIdBeam());
            Beam B_BPersonal = Bpersonal.getByID(actual.getIdBeam());
            actual.CalculateVelocity(c1, c2, w, B_Bsolution, B_BPersonal);
        }
    }

    //Funcion que recalcula la posicion de la particula luego de calcular la velocidad
    public void CalculatePosition() {
        for (Beam actual : Angle_beam) {
            actual.CalculatePosition();
        }
        eval();
    }

    /*--------------------------------------------------------- GETTER AND SETTERS -----------------------------------------------------*/
    public Vector<Double> getFluenceMap(){
        Vector<Double> intensityVector = new Vector<Double>();
        for(Beam b: Angle_beam){
            Vector<Double> v =  b.getIntensityVector();
            for(Double i: v){
                intensityVector.add(i);
            }
        }
        return intensityVector;
    }

    public Beam getByID(int to_search){
        for(Beam pivote: Angle_beam){
            if(pivote.getIdBeam() == to_search){
                return pivote;
            }
        }
        return null;
    }

    public double getEval() {
        return eval;
    }

    public void setEval(double eval) {
        this.eval = eval;
    }

    public Vector<Beam> getBeams(){
        return Angle_beam;
    }

    public void setNBeam(int nBeam) {
        this.nBeam = nBeam;
    }

    public void setW(Vector<Double> w) {
        this.w = w;
    }

    public void setZMin(Vector<Double> zMin) {
        this.zMin = new Vector<>();
        this.zMin.addAll(zMin);
    }

    public void setZMax(Vector<Double> zMaxVector) {
        this.zMax = new Vector<>();
        this.zMax.addAll(zMaxVector);
    }

    public void setTotalBeamlet(int totalBeamLet) {
        this.totalBeamLet = totalBeamLet;
    }

    public Vector<Beam> getAngle_beam() {
        return Angle_beam;
    }

    public void setAngle_beam(Vector<Beam> angle_beam) {
        Angle_beam = angle_beam;
    }

    public void setFluenceMap(Vector<Double> fluenceMap) {
        this.fluenceMap = fluenceMap;
    }

    /*--------------------------------------------------------- PRINTERS -----------------------------------------------------*/
    public void printIdBeam(){
        System.out.println("TOTAL BEAMLET: " + totalBeamLet);
        for(Beam x: Angle_beam)
            x.printIdBeam();
    }

    public void printIdBeamtoVector(){
        System.out.println("TOTAL BEAMLET: " + totalBeamLet);
        for(Beam x: Angle_beam)
            x.printIdBeamtoVector();
    }

    public void printIntensityMatrix(){
        for(Beam b: Angle_beam){
            b.printIntensityMatrix();
        }
    }

    public void printApertures(){
        for(Beam b: Angle_beam){
            b.printApertures();
        }
    }

    public void printAperturesBeam(int x){
        Beam b = Angle_beam.get(x);
        b.printApertures();
    }

    //Se exporta un vector de intensidad en un formato para AMPL
    public void exportintensityVector(){
        FileWriter intensityVector = null;
        PrintWriter pw = null;
        try{
            intensityVector = new FileWriter("IntensityVector.txt");
            pw = new PrintWriter(intensityVector);
            int param = 1;
            for(Beam b: Angle_beam) {
                int index = 1;
                Vector<Double> intensity = b.getIntensityVector();
                pw.print("param x" + param + " := ");
                for (Double i : intensity){
                    pw.println(index + "\t" + i);
                    index++;
                }
                pw.println("; \n");
                param++;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            // Nuevamente aprovechamos el finally para
            // asegurarnos que se cierra el fichero.
            try {
                if (intensityVector != null)
                    intensityVector.close();
            }catch (Exception e2){
                e2.printStackTrace();
            }
        }
    }

    public void printFluenceMap(){
        for (Double i: fluenceMap){
            System.out.print(i + " ");
        }
        System.out.println();
    }

}
