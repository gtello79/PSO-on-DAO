package SRCDAO;

import Utils.Gurobi_Solver;
import robust.EscenarioController;

import com.gurobi.gurobi.GRBException;
import source.Collimator;

import java.security.KeyException;
import java.util.ArrayList;

public class Plan {
    private double eval; // Valor del Plan dentro de la funcion de evaluacion
    private ArrayList<Double> robustEval; // Valor de la funcion de evaluacion robusta
    private int nBeam; // Cantidad total de angulos en BAC
    private int totalBeamLet; // Total de beamlets activos en BAC
    private int maxIntensityByAperture; // Intensidad Maxima por apertura
    private int totalAperturesUnsed; // Cantidad de aperturas inutilizadas
    private double beamOnTime;

    private final Collimator collimator; // Informacion del Collimator
    private ArrayList<Double> w;
    private ArrayList<Double> zMin;
    private ArrayList<Double> zMax;
    private ArrayList<Integer> maxApertures; // Contiene la cantidad de aperturas por beam angle

    private ArrayList<Beam> Angle_beam; // Contiene los beam angle
    private ArrayList<Double> fluenceMap;
    private int[] beamIndex; // ID de cada beam
    private int[] beamletsByBeam; // Cantidad de beamlets x beam

    /*---------------------------------------------- METHODS -----------------------------------------------------------------------*/

    public Plan(ArrayList<Double> w, ArrayList<Double> zMin, ArrayList<Double> zMax, ArrayList<Integer> maxApertures,
            int max_intensity, int minIntensity, int initial_intensity, int step_intensity, int open_apertures,
            int setup, Collimator collimator) {

        setNBeam(collimator.getNbAngles());
        setW(w);
        setZMin(zMin);
        setZMax(zMax);

        this.Angle_beam = new ArrayList<>();
        this.totalBeamLet = collimator.getNbBeamlets();
        this.collimator = collimator;

        this.maxApertures = new ArrayList<>(maxApertures);
        this.maxIntensityByAperture = max_intensity;
        this.beamIndex = new int[getNBeam()];
        this.beamletsByBeam = new int[nBeam];

        this.totalAperturesUnsed = 0;
        this.beamOnTime = 0.0;
        this.robustEval = new ArrayList<>();

        // Creacion de los beam en BAC
        for (int i = 0; i < nBeam; i++) {
            Beam new_beam = new Beam(collimator.getAngle(i), maxApertures.get(i), max_intensity, minIntensity,
                    initial_intensity, step_intensity, open_apertures, setup, collimator);
            this.Angle_beam.add(new_beam);
            this.beamIndex[i] = new_beam.getIdBeam();
            this.beamletsByBeam[i] = new_beam.getTotalBeamlets();
        }

        // Evaluate the plan
        evalFunction();

        // Report information about the plan
        System.out.println("--Created " + Angle_beam.size() + " Stations Beams");
        System.out.println("--Initial Evaluation: " + getEval());
    }

    // Constructor de copia de un Treatment Plan
    public Plan(Plan p) {

        setNBeam(p.nBeam);
        setTotalBeamlet(p.totalBeamLet);
        setTotalAperturesUnUsed(p.totalAperturesUnsed);

        setW(p.w);
        setZMin(p.zMin);
        setZMax(p.zMax);
        setFluenceMap(p.getFluenceMap());

        this.Angle_beam = new ArrayList<>();
        this.maxApertures = new ArrayList<>(p.maxApertures);
        this.collimator = new Collimator(p.collimator);

        this.totalBeamLet = p.totalBeamLet;
        this.maxIntensityByAperture = p.maxIntensityByAperture;
        this.beamIndex = new int[getNBeam()];
        this.totalAperturesUnsed = p.totalAperturesUnsed;
        this.beamOnTime = p.beamOnTime;

        this.setAngle_beam(p.getAngle_beam());

        // Evaluate the robust fluence map
        this.setEval(p.eval);
        this.robustEval = EscenarioController.evaluateFluenceMap(p.fluenceMap, w, zMin, zMax);
    }

    public void buildTreatmentPlan() {
        this.beamOnTime = 0.0;
        for (int i = 0; i < Angle_beam.size(); i++) {
            Beam b = Angle_beam.get(i);

            b.generateIntensities();
            this.beamOnTime += b.getBeamOnTime();
        }
    }

    /* Funcion de evaluacion */
    public double evalFunction() {

        // Eval objetive function
        this.fluenceMap = getFluenceMap();
        double val = EscenarioController.evaluateOverFeatureEscenario(fluenceMap, w, zMin, zMax);
        this.setEval(val);

        // Evaluate the robust fluence map
        this.robustEval = EscenarioController.evaluateFluenceMap(this.fluenceMap, w, zMin, zMax);

        return val;
    }

    public void OptimizateIntensities() {
        // Optimizate Intensities
        double[] dd = new double[3];
        dd[0] = zMax.get(0);
        dd[1] = zMax.get(1);
        dd[2] = zMax.get(2);

        Gurobi_Solver newModel;
        try {
            newModel = new Gurobi_Solver(this, beamIndex, dd, w);
            double objFunction = newModel.objVal;
            setEval(objFunction); // Recuperar valor de la funcion objetivo
            setIntensity(newModel.newIntensity); // Cambia intensidades obtenidas en cada apertura
        } catch (GRBException e) {
            e.printStackTrace();
        }

        buildTreatmentPlan();
        evalFunction();
    }

    public int getProyectedBeamLetByApertureOnBeam(int indexBeam, int idAperture, int indexBeamlet) {
        Beam beam = Angle_beam.get(indexBeam);
        boolean projectionBeamLet = beam.getProyectedBeamLetByAperture(idAperture, indexBeamlet);

        return projectionBeamLet ? 1 : 0;
    }

    public void setIntensity(double[][] newIntensities) {
        for (int b = 0; b < Angle_beam.size(); b++) {
            Beam beam = Angle_beam.get(b);
            double[] apertureIntensities = newIntensities[b];
            beam.setIntensityByAperture(apertureIntensities);
        }
    }

    public void regenerateApertures() {
        for (Beam actual : Angle_beam) {
            actual.regenerateApertures();
        }
    }

    /*
     * PSO METHODS
     */

    // Funcion que realiza la actualizacion de la velocidad de la particula
    public void CalculateVelocity(double c1Aperture, double c2Aperture, double wAperture, double cnAperture,
            double c1Intensity, double c2Intensity, double wIntensity, double cnIntensity, Plan Bsolution,
            Plan Bpersonal) {
        // Bsolution: Best Global solution ; 
        // Bpersonal: Best Personal solution
        for (Beam beam_i : Angle_beam) {
            Beam B_Bsolution = Bsolution.getByID(beam_i.getIdBeam());
            Beam B_BPersonal = Bpersonal.getByID(beam_i.getIdBeam());
            beam_i.CalculateVelocity(c1Aperture, c2Aperture, wAperture, cnAperture, c1Intensity, c2Intensity,
                    wIntensity, cnIntensity, B_Bsolution, B_BPersonal);
        }
    }

    // Funcion que recalcula la posicion de la particula luego de calcular la velocidad
    public void CalculatePosition() {
        for (Beam actual : Angle_beam) {
            actual.CalculatePosition();
        }
    }

    /*--------------------------------------------------------- GETTER AND SETTERS -----------------------------------------------------*/
    public ArrayList<Double> getFluenceMap() {
        this.beamOnTime = 0.0;
        ArrayList<Double> intensityVector = new ArrayList<Double>();
        // Concatena los vectores de intensidad al Fluence Map
        for (Beam pivote : Angle_beam) {
            ArrayList<Double> v = pivote.getIntensityVector();
            intensityVector.addAll(v);
            this.beamOnTime += pivote.getBeamOnTime();
        }

        return intensityVector;
    }

    public ArrayList<Integer> getAperturesUnUsed() {
        this.totalAperturesUnsed = 0;
        ArrayList<Integer> unUsedByBeam = new ArrayList<>();
        for (int b = 0; b < this.nBeam; b++) {
            int unUsed = Angle_beam.get(b).getAperturesUnused();
            this.totalAperturesUnsed += unUsed;
            unUsedByBeam.add(unUsed);
        }
        return unUsedByBeam;
    }

    public Beam getByID(int idBeamToSearch) {
        for (Beam b : Angle_beam) {
            if (b.getIdBeam() == idBeamToSearch)
                return b;
        }
        System.exit(1);
        return null;
    }

    public Double getIntensityByAperture(int indexBeam, int indexAperture) {
        double intensity = 0.0;
        try {
            intensity = Angle_beam.get(indexBeam).getIntensityByAperture(indexAperture);
        } catch (KeyException e) {
            e.printStackTrace();
        }
        return intensity;
    }

    public void setAngle_beam(ArrayList<Beam> angle_beam) {
        ArrayList<Beam> newAngleBeam = new ArrayList<>();
        for (Beam beam : angle_beam) {
            Beam beamAngle = new Beam(beam);
            newAngleBeam.add(beamAngle);

        }
        this.Angle_beam = new ArrayList<>(newAngleBeam);
    }

    public Integer getTotalApertureByBeam(int indexBeam) {
        int apertures = -1;
        try {
            apertures = this.maxApertures.get(indexBeam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return apertures;
    }

    public int getTotalAperturesUnsed() {
        return this.totalAperturesUnsed;
    }

    public int[] getBeamletsByBeam() {
        return this.beamletsByBeam;
    }

    public Integer getMaxIntensityByAperture() {
        return maxIntensityByAperture;
    }

    public double getEval() {
        return eval;
    }

    public double getBeamOnTime() {
        return this.beamOnTime;
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
        this.zMin = new ArrayList<>(zMin);
    }

    public void setZMax(ArrayList<Double> zMaxVector) {
        this.zMax = new ArrayList<>(zMaxVector);
    }

    public void setTotalBeamlet(int totalBeamLet) {
        this.totalBeamLet = totalBeamLet;
    }

    public ArrayList<Beam> getAngle_beam() {
        return Angle_beam;
    }

    public void setFluenceMap(ArrayList<Double> fluenceMap) {
        this.fluenceMap = new ArrayList<>(fluenceMap);
    }

    public void setTotalAperturesUnUsed(int totalAperturesUnUsed) {
        this.totalAperturesUnsed = totalAperturesUnUsed;
    }

    public ArrayList<Double> getRobustEval() {
        return this.robustEval;
    }
}