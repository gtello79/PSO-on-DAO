package SRCDAO;
import Utils.Gurobi_Solver;
import gurobi.GRBException;
import source.*;

import java.util.ArrayList;
import java.util.Vector;

public class Plan {
    private double eval;
    private int nBeam;
    private int totalBeamLet;
    private int maxIntensityByAperture;
    private int totalAperturesUnsed;

    private ArrayList<Volumen> volumen;
    private ArrayList<Double> w;
    private ArrayList<Double> zMin;
    private ArrayList<Double> zMax;
    private ArrayList<Integer> maxApertures;

    private ArrayList<Beam> Angle_beam;
    private ArrayList<Double> fluenceMap;
    private EvaluationFunction ev;

    private Gurobi_Solver gurobiSolver;
    private int[] beamIndex;

    private int[] beamletsByBeam;
    /*---------------------------------------------- METHODS -----------------------------------------------------------------------*/

    public Plan(ArrayList<Double> w, ArrayList<Double> zMin, ArrayList<Double> zMax, ArrayList<Integer> maxApertures, int max_intensity,
                int initial_intensity, int step_intensity, int open_apertures, int setup, ArrayList<Volumen> volumen, Collimator collimator) {

        System.out.println("------------ Initilizing plan. -----------------");
        setNBeam(collimator.getNbAngles());
        setW(w);
        setZMin(zMin);
        setZMax(zMax);

        this.Angle_beam = new ArrayList<>();
        this.maxApertures = new ArrayList(maxApertures);

        this.ev = new EvaluationFunction(volumen);
        this.totalBeamLet = collimator.getNbBeamlets();
        this.maxIntensityByAperture = max_intensity;
        this.beamIndex = new int[getNBeam()];
        this.volumen = volumen;
        this.beamletsByBeam = new int [nBeam];
        this.totalAperturesUnsed = 0;

        for (int i = 0; i < nBeam; i++) {
            Beam new_beam = new Beam(collimator.getAngle(i), maxApertures.get(i) , max_intensity, initial_intensity, step_intensity, open_apertures, setup, collimator);
            Angle_beam.add(new_beam);
            beamIndex[i] = new_beam.getIdBeam();
            this.beamletsByBeam[i] = new_beam.getTotalBeamlets();
        }

        System.out.println("--Created " + Angle_beam.size() + " Stations Beams");
        eval();
        System.out.println("--Initial Evaluation: " + getEval());

    }

    //Constructor de copia de un Treatment Plan
    public Plan(Plan p){

        setEval(p.eval);
        setNBeam(p.nBeam);
        setTotalBeamlet(p.totalBeamLet);

        setW(p.w);
        setZMax(p.zMax);
        setZMin(p.zMin);
        setFluenceMap(p.getFluenceMap());

        this.Angle_beam = new ArrayList();
        this.maxApertures = new ArrayList(p.maxApertures);
        this.ev = p.ev;
        this.totalBeamLet = p.totalBeamLet;
        this.maxIntensityByAperture = p.maxIntensityByAperture;
        this.setAngle_beam(p.getAngle_beam());
        this.beamIndex = new int[getNBeam()];

        ArrayList<Volumen> newOrgs = new ArrayList<>();
        for(int i = 0; i < p.volumen.size(); i++){
            Volumen v = new Volumen(p.volumen.get(i));
            newOrgs.add(v);

        }
        this.volumen = newOrgs;
        setEval(p.eval);
    }

    /*Funcion de evaluacion */
    public double eval() {
        this.fluenceMap = getFluenceMap();
        double val = ev.evalIntensityVector(fluenceMap, w, zMin, zMax);
        setEval(val);
        return val;
    }

    public void OptimizateIntensities(){
        //Optimizate Intensities
        double [] dd = new double[3];
        dd[0] = zMax.get(0);
        dd[1] = zMax.get(1);
        dd[2] = zMax.get(2);

        try {
            Gurobi_Solver newModel;
            try {
                newModel = new Gurobi_Solver(this, volumen, beamIndex, dd, w);
                setIntensity(newModel.newIntensity);
                double objFunction = newModel.objVal;
                System.out.println("Solver: " + newModel.objVal);
                //setEval(newModel.objVal); // -> Recuperar valor de la funcion objetivo
            } catch (GRBException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        buildTreatmentPlan();
    }

    public void buildTreatmentPlan(){
        for(int i = 0; i < Angle_beam.size(); i++){
            // Tomar cada beam
            Beam b = Angle_beam.get(i);
            // Construirlo ( limpiar Intensity Map )
            b.generateIntensities();
        }
    }

    public int getProyectedBeamLetByApertureOnBeam(int indexBeam, int idAperture, int indexBeamlet ){
        Beam beam = Angle_beam.get(indexBeam);
        int coef = 0;
        if(beam.getProyectedBeamLetByAperture(idAperture, indexBeamlet)){
            coef+=1;
        }
        return coef;
    }

    public boolean setIntensity(double[][] newIntensities){
        boolean process = true;

        for(int b = 0; b < Angle_beam.size(); b++){
            Beam beam = Angle_beam.get(b);
            double[] apertureIntensities = newIntensities[b];
            boolean action = beam.setIntensityByAperture(apertureIntensities);
            if(!action)
                return false;
        }
        return process;
    }


    /* --------------------------------------- PSO METHODS ---------------------------------------- */

    //Funcion que realiza la actualizacion de la velocidad de la particula
    public void CalculateVelocity(double c1Aperture, double c2Aperture, double wAperture, double cnAperture, double c1Intensity, double c2Intensity, double wIntensity, double cnIntensity, Plan Bsolution, Plan Bpersonal) {
        //Bsolution: Best Global solution ; Bpersonal: Best Personal solution
        for (Beam actual : Angle_beam) {
            Beam B_Bsolution = Bsolution.getByID(actual.getIdBeam());
            Beam B_BPersonal = Bpersonal.getByID(actual.getIdBeam());
            actual.CalculateVelocity(c1Aperture, c2Aperture, wAperture, cnAperture, c1Intensity, c2Intensity, wIntensity, cnIntensity, B_Bsolution, B_BPersonal);
        }
    }

    //Funcion que recalcula la posicion de la particula luego de calcular la velocidad
    public void CalculatePosition() {
        for (Beam actual : Angle_beam) {
            actual.CalculatePosition();
        }
    }

    /*--------------------------------------------------------- GETTER AND SETTERS -----------------------------------------------------*/
    public ArrayList<Double> getFluenceMap(){
        ArrayList<Double> intensityVector = new ArrayList<Double>();

        for(Beam pivote: Angle_beam){
            ArrayList<Double> v =  pivote.getIntensityVector();
            intensityVector.addAll(v);
        }
        return intensityVector;
    }

    public ArrayList<Integer> getAperturesUnUsed(){
        ArrayList<Integer> unUsedByBeam = new ArrayList<>();
        int aperturesUnUsed = 0;
        for(int b = 0; b < this.nBeam; b++){
            int unUsed = Angle_beam.get(b).getAperturesUnused();
            aperturesUnUsed+=unUsed;
            unUsedByBeam.add(unUsed);
        }
        this.totalAperturesUnsed = aperturesUnUsed;
        return unUsedByBeam;
    }

    public Beam getByID(int idBeamToSearch){
        for(Beam b: Angle_beam){
            if(b.getIdBeam() == idBeamToSearch)
                return b;
        }
        System.out.println("ALERTA, BEAM "+ idBeamToSearch +" NO ENCONTRADO");
        return null;
    }

    public int[] getBeamletsByBeam(){
        return this.beamletsByBeam;
    }

    public Integer getMaxIntensityByAperture(){
        return maxIntensityByAperture;
    }

    public double getEval() {
        return eval;
    }

    public void setEval(double eval) {
        this.eval = eval;
    }

    public void setNBeam(int nBeam) {
        this.nBeam = nBeam;
    }

    public Integer getNBeam() {
        return nBeam;
    }

    public void setW(ArrayList<Double> w) {
        this.w = w;
    }

    public void setZMin(ArrayList<Double> zMin) {
        this.zMin = new ArrayList(zMin);
    }

    public void setZMax(ArrayList<Double> zMaxVector) {
        this.zMax = new ArrayList(zMaxVector);
    }

    public void setTotalBeamlet(int totalBeamLet) {
        this.totalBeamLet = totalBeamLet;
    }

    public ArrayList<Beam> getAngle_beam() {
        return Angle_beam;
    }

    public void setAngle_beam(ArrayList<Beam> angle_beam) {
        ArrayList<Beam> newAngleBeam = new ArrayList<>();
        for(Beam beam: angle_beam){
            Beam beamAngle = new Beam(beam);
            newAngleBeam.add(beamAngle);
            //this.Angle_beam.add(beamAngle);
        }
        this.Angle_beam = new ArrayList(newAngleBeam);
    }

    public void setFluenceMap(ArrayList<Double> fluenceMap) {
        this.fluenceMap = new ArrayList(fluenceMap);
    }

    public ArrayList<Integer> getMaxApertures(){
        return maxApertures;
    }

    public Double getIntensityByAperture(int indexBeam, int indexAperture) throws Exception {
        double intensity = 0.0;
        try{
            intensity = Angle_beam.get(indexBeam).getIntensityByAperture(indexAperture);
        }catch (Exception e){
            e.printStackTrace();
        }
        return intensity;
    }

    public Integer getTotalApertureByBeam(int indexBeam){
        int apertures = -1;
        try{
            apertures = this.maxApertures.get(indexBeam);
        }catch (Exception e){
            e.printStackTrace();
        }
        return apertures;
    }

}
