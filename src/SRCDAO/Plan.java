package SRCDAO;
import Utils.Gurobi_Solver;
import source.*;

import java.util.ArrayList;
import java.util.Vector;

public class Plan {
    private double eval;
    private int nBeam;
    private int totalBeamLet;
    private int maxIntensityByAperture;

    private Vector<Double> w;
    private Vector<Double> zMin;
    private Vector<Double> zMax;
    private Vector<Integer> beamletsByAngle;
    private ArrayList<Integer> maxApertures;

    private Vector<Beam> Angle_beam;
    private Vector<Double> fluenceMap;
    private EvaluationFunction ev;

    private Gurobi_Solver gurobiSolver;
    private int[] beamIndex;
    /*---------------------------------------------- METHODS -----------------------------------------------------------------------*/

    public Plan(Vector<Double> w, Vector<Double> zMin, Vector<Double> zMax, ArrayList<Integer> maxApertures, int max_intensity,
                int initial_intensity, int step_intensity, int open_apertures, int setup, Vector<Volumen> volumen, Collimator collimator) {

        setNBeam(collimator.getNbAngles());
        setW(w);
        setZMin(zMin);
        setZMax(zMax);

        this.Angle_beam = new Vector<>();
        this.ev = new EvaluationFunction(volumen);
        this.totalBeamLet = collimator.getNbBeamlets();
        this.maxApertures = new ArrayList(maxApertures);
        this.maxIntensityByAperture = max_intensity;
        this.beamIndex = new int[getNBeam()];

        System.out.println("-------- Initilizing plan.-----------");
        for (int i = 0; i < nBeam; i++) {

            Beam new_beam = new Beam(collimator.getAngle(i), maxApertures.get(i) , max_intensity, initial_intensity, step_intensity, open_apertures, setup, collimator);
            Angle_beam.add(new_beam);
            beamIndex[i] = new_beam.getIdBeam();
        }

        //this.gurobiSolver = new Gurobi_Solver(this, volumen, beamIndex, [1.0,2.0], w);

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
        setFluenceMap(p.getFluenceMap());

        this.Angle_beam = new Vector();
        this.maxApertures = new ArrayList(p.maxApertures);
        this.ev = p.ev;
        this.totalBeamLet = p.totalBeamLet;
        this.maxIntensityByAperture = p.maxIntensityByAperture;
        this.setAngle_beam(p.getAngle_beam());
        this.beamIndex = new int[getNBeam()];
        /*
        //Copiando cada beam del Plan P en la nueva instancia
        for(Beam b: p.getAngle_beam()){
            Beam newBeam = new Beam(b);
            Angle_beam.add(newBeam);
        }
        */

        setEval(p.eval);
    }

    /*Funcion de evaluacion */
    public double eval() {
        this.fluenceMap = getFluenceMap();

        double val = ev.evalIntensityVector(fluenceMap, w, zMin, zMax);
        setEval(val);
        return val;
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

    public void OptimizateIntensities(){
        //Optimizate Intensities
        //Gurobi_Solver(this,)
    }

    /*--------------------------------------------------------- GETTER AND SETTERS -----------------------------------------------------*/
    public Vector<Double> getFluenceMap(){
        Vector<Double> intensityVector = new Vector<Double>();
        for(Beam pivote: Angle_beam){
            Vector<Double> v =  pivote.getIntensityVector();
            for(Double i: v){
                intensityVector.add(i);
            }
        }
        return intensityVector;
    }

    public Beam getByID(int to_search){
        for(Beam b: Angle_beam){
            if(b.getIdBeam() == to_search)
                return b;
        }
        System.out.println("ALERTA, BEAM "+ to_search +" NO ENCONTRADO");
        return null;
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

    public void setW(Vector<Double> w) {
        this.w = w;
    }

    public void setZMin(Vector<Double> zMin) {
        this.zMin = new Vector(zMin);
    }

    public void setZMax(Vector<Double> zMaxVector) {
        this.zMax = new Vector<>(zMaxVector);
    }

    public void setTotalBeamlet(int totalBeamLet) {
        this.totalBeamLet = totalBeamLet;
    }

    public Vector<Beam> getAngle_beam() {
        return Angle_beam;
    }

    public void setAngle_beam(Vector<Beam> angle_beam) {
        Vector<Beam> newAngleBeam = new Vector<>();
        for(Beam beam: angle_beam){
            Beam beamAngle = new Beam(beam);
            newAngleBeam.add(beamAngle);
            //this.Angle_beam.add(beamAngle);
        }
        this.Angle_beam = new Vector(newAngleBeam);
    }

    public void setFluenceMap(Vector<Double> fluenceMap) {
        this.fluenceMap = new Vector(fluenceMap);
    }

    public ArrayList<Matrix> getIntensitiesMatrix(){
        ArrayList<Matrix> matrixArrayList = new ArrayList<>();
        for(Beam beam: Angle_beam){
            matrixArrayList.add(beam.getIntensitisMatrix());
        }
        return  matrixArrayList;
    }

    public Vector<Integer> getBeamletsByAngle(){
        return this.beamletsByAngle;
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


    /*--------------------------------------------------------- PRINTERS -----------------------------------------------------*/

    public void printIntensityMatrix(){
        for(Beam b: Angle_beam){
            b.printIntensityMatrix();
        }
    }

    public void printFluenceMap(){
        for (Double i: fluenceMap){
            System.out.print(i + " ");
        }
        System.out.println();
    }

    public void printFluenceMapByBeam(){
        for(Beam b : Angle_beam){
            b.printFluenceMapOnBeam();
        }
    }

}
