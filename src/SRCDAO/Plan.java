package SRCDAO;
import source.*;

import java.util.ArrayList;
import java.util.Vector;

public class Plan {
    private double eval;
    private int n_beam;

    private int totalbeamlet;
    private Vector<Double> w;
    private Vector<Double> Zmin;
    private Vector<Double> Zmax;
    private ArrayList<Beam> Angle_beam;
    private EvaluationFunction ev;


    /*---------------------------------------METHODS -------------------------------------------*/
    public Plan(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, int max_apertures, int max_intensity, int initial_intensity, int step_intensity, int open_apertures, int setup, Vector<Volumen> volumen, Collimator collimator) {
        setN_beam(collimator.getNbAngles());
        setW(w);
        setZmin(Zmin);
        setZmax(Zmax);

        this.Angle_beam = new ArrayList<>();
        this.ev = new EvaluationFunction(volumen);
        int x = 0;
        System.out.println("--Initilizing plan.");
        for (int i = 0; i < n_beam; i++) {

            Beam new_beam = new Beam(collimator.getAngle(i), max_apertures, max_intensity, initial_intensity, step_intensity, open_apertures, setup, volumen, collimator);
            Angle_beam.add(new_beam);
            x += new_beam.getNbBeamlets();
        }
        settotalbeamlet(x);
        System.out.println("--Created " + Angle_beam.size() + " Stations Beams");
        eval();
        System.out.println("--Initial Evaluation: " + getEval());
    }

    public Plan(Plan p) {
        setEval(p.getEval());
        setN_beam(p.getN_beam());
        setW(p.getW());
        setZmin(p.getZmin());
        setZmax(p.getZmax());
        setEv(p.getEv());
        setAngle_beam(p.getAngle_beam());

    }


    /*Evaluation Function */
    public void eval() {
        double val = ev.eval(this, w, Zmin, Zmax);
        ev.generateVoxelDoseFunction();
        setEval(val);
        //System.out.println("EVAL 2:" +ev.eval2(this, w, Zmin, Zmax));
    }

    /* --------------------------------------- PSO METHODS ---------------------------------------- */
    public void CalculateVelocity(double c1, double c2, double w, Plan Bsolution, Plan Bpersonal) {
        //Bsolution: Best Global solution
        //Bpersonal: Best Personal solution
        for (Beam actual : Angle_beam) {
            Beam B_Bsolution = Bsolution.getByID(actual.getId_beam());
            Beam B_BPersonal = Bpersonal.getByID(actual.getId_beam());
            actual.CalculateVelocity(c1, c2, w, B_Bsolution, B_BPersonal);
        }
    }

    public void CalculatePosition() {
        for (Beam actual : Angle_beam) {
            actual.CalculatePosition();
        }
        eval();
    }

    /*--------------------------------------------------------- GETTER AND SETTERS -----------------------------------------------------*/
    public Vector<Double> getIntensityVector(){
        Vector<Double> intensityVector = new Vector<Double>();
        for(Beam b: Angle_beam){
            Vector<Double> v =  b.getIntensityVector();
            for(Double i: v){
                intensityVector.add(i);
            }
        }
        return intensityVector;
    }

    public Vector<Vector<Double>> getIntensityVectors(){
        Vector<Vector<Double>> intensityVector = new Vector<Vector<Double>>();
        for(Beam b: Angle_beam){
            Vector<Double> piv =  b.getIntensityVector();
            intensityVector.add(piv);
        }
        return intensityVector;
    }

    public Beam getByID(int to_search){
        for(Beam pivote: Angle_beam){
            if(pivote.getId_beam() == to_search){
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

    public ArrayList<Beam> getBeams(){
        return Angle_beam;
    }

    public int getN_beam() {
        return n_beam;
    }

    public void setN_beam(int n_beam) {
        this.n_beam = n_beam;
    }

    public Vector<Double> getW() {
        return w;
    }

    public void setW(Vector<Double> w) {
        this.w = w;
    }

    public Vector<Double> getZmin() {
        return Zmin;
    }

    public void setZmin(Vector<Double> zmin) {
        Zmin = new Vector<>();
        Zmin.addAll(zmin);
    }

    public Vector<Double> getZmax() {
        return Zmax;
    }

    public void setZmax(Vector<Double> zmax) {
        Zmax = new Vector<>();
        Zmax.addAll(zmax);
    }

    public ArrayList<Beam> getAngle_beam() {
        return Angle_beam;
    }

    public void setAngle_beam(ArrayList<Beam> angle_beam) {
        Angle_beam = new ArrayList<>();
        this.Angle_beam.addAll(angle_beam);
    }

    public EvaluationFunction getEv() {
        return ev;
    }

    public void setEv(EvaluationFunction ev) {
        this.ev = ev;
    }

    public int gettotalbeamlet() {
        return totalbeamlet;
    }

    public void settotalbeamlet(int nbeamlet) {
        this.totalbeamlet = nbeamlet;
    }

    /*--------------------------------------------------------- PRINTERS -----------------------------------------------------*/
    public void printIdBeam(){
        System.out.println("TOTAL BEAMLET: " + totalbeamlet);
        for(Beam x: Angle_beam)
            x.printIdBeam();
    }

    public void printIdBeamtoVector(){
        System.out.println("TOTAL BEAMLET: " + totalbeamlet);
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

}
