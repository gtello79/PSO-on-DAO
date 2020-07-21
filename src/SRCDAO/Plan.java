package SRCDAO;
import source.*;

import java.util.ArrayList;
import java.util.Vector;

public class Plan {
    private double eval;
    private int n_beam;
    private Vector<Double> w;
    private Vector<Double> Zmin;
    private Vector<Double> Zmax;
    private int max_apertures;
    private int max_intensity;
    private int initial_intensity;
    private int step_intensity;
    private int open_apertures;
    private int setup;
    private ArrayList<Beam> Angle_beam;
    private EvaluationFunction ev;

    public Plan(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, int max_apertures, int max_intensity, int initial_intensity, int step_intensity, int open_apertures, int setup, Vector<Volumen> volumen, Collimator collimator){
        Angle_beam = new ArrayList<Beam>();
        System.out.println("--Initilizing plan.");
        this.n_beam = collimator.getNbAngles();
        this.w = w;
        this.Zmin = Zmin;
        this.Zmax = Zmax;
        this.max_apertures = max_apertures;
        this.max_intensity = max_intensity;
        this.initial_intensity = initial_intensity;
        this.step_intensity = step_intensity;
        this.open_apertures = open_apertures;
        this.setup = setup;
        this.Angle_beam = new ArrayList<Beam>();
        this.ev = new EvaluationFunction(volumen);
        for(int i = 0; i < n_beam ; i++){
            Beam new_beam = new Beam(collimator.getAngle(i), max_apertures, max_intensity, initial_intensity, step_intensity, open_apertures, setup, volumen, collimator);
            Angle_beam.add(new_beam);
        }
        System.out.println("--Created " + Angle_beam.size() + " Stations Beams");
        eval();
        System.out.println("--Initial Evaluation: "+getEval());
    }

    public Plan(Plan p){
        this.eval = p.getEval();
        this.n_beam = p.getN_beam();
        this.w = p.getW();
        this.Zmin = p.getZmin();
        this.Zmax = p.getZmax();
        this.max_apertures = p.getMax_apertures();
        this.max_intensity = p.getMax_intensity();
        this.initial_intensity = p.getInitial_intensity();
        this.step_intensity =  p.getStep_intensity();
        this.open_apertures = p.getOpen_apertures();
        this.setup = p.getSetup();
        this.Angle_beam = new ArrayList<Beam>();
        this.ev = p.getEv();
        for(Beam b : p.getAngle_beam()){
            this.Angle_beam.add(b);
        }
    }

    public void eval(){
        //Aqui se pone la funcion de evaluacion
        double val = ev.eval(this,w,Zmin,Zmax);
        ev.generateVoxelDoseFunction();
        setEval(val);
    }

    public void CalculateVelocity(double c1, double c2, double w, Plan Bsolution, Plan Bpersonal){
        for (Beam actual: Angle_beam){
            Beam B_Bsolution = getByID(actual.getId_beam());
            Beam B_BPersonal = getByID(actual.getId_beam());
            actual.CalculateVelocity(c1 ,c2 ,w ,B_Bsolution ,B_BPersonal);
        }
    }

    public void CalculatePosition(){
        for (Beam actual: Angle_beam){
            actual.CalculatePosition();
        }
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
        Zmin = zmin;
    }

    public Vector<Double> getZmax() {
        return Zmax;
    }

    public void setZmax(Vector<Double> zmax) {
        Zmax = zmax;
    }

    public int getMax_apertures() {
        return max_apertures;
    }

    public void setMax_apertures(int max_apertures) {
        this.max_apertures = max_apertures;
    }

    public int getMax_intensity() {
        return max_intensity;
    }

    public void setMax_intensity(int max_intensity) {
        this.max_intensity = max_intensity;
    }

    public int getInitial_intensity() {
        return initial_intensity;
    }

    public void setInitial_intensity(int initial_intensity) {
        this.initial_intensity = initial_intensity;
    }

    public int getStep_intensity() {
        return step_intensity;
    }

    public void setStep_intensity(int step_intensity) {
        this.step_intensity = step_intensity;
    }

    public int getOpen_apertures() {
        return open_apertures;
    }

    public void setOpen_apertures(int open_apertures) {
        this.open_apertures = open_apertures;
    }

    public int getSetup() {
        return setup;
    }

    public void setSetup(int setup) {
        this.setup = setup;
    }

    public ArrayList<Beam> getAngle_beam() {
        return Angle_beam;
    }

    public void setAngle_beam(ArrayList<Beam> angle_beam) {
        Angle_beam = angle_beam;
    }

    public EvaluationFunction getEv() {
        return ev;
    }

    public void setEv(EvaluationFunction ev) {
        this.ev = ev;
    }

}
