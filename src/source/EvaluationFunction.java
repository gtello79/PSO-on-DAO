package source;

import javafx.util.Pair;
import SRCDAO.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.*;

import static java.lang.Math.pow;
import static java.lang.Math.abs;

public class EvaluationFunction {

    private double F;

    private int nVolumes;

    private int n_evaluations;

    //Evaluation of F before the last incremental evaluation
    private double prev_F;

    //Matrix of derivatives for each organ and voxel (may be useful for algorithms)
    //How much increase/decrease F increasing the voxel in one unity.
    private Vector<Vector<Double>> D;

    //dose distribution vectors for each organ
    private Vector<Vector<Double>> Z;


    //voxel_dose[o][d] indicates the number of voxels in organ o with a dose between d and d+1
    private Vector<Vector<Double>> voxel_dose;

    //number of organs, including the tumor
    private int nb_organs;

    //number of voxels for each organ
    private Vector<Integer> nb_voxels;

    private HashSet<Pair<Double, Pair<Integer, Integer>>> tumor_voxels;

    private Vector<Matrix> DDM = new Vector<>();

    private HashSet<Pair<Double, Pair<Integer, Integer>>> voxels;

    private ArrayList< Pair< Pair<Integer,Integer>, Double > > Z_diff;

    /* ----------------------------------------------------------- METHODS --------------------------------------------------------*/
    public EvaluationFunction(Vector<Volumen> volumes)
    {
        setN_evaluations(0);
        setPrev_F(0.0);
        setF(0.0);
        setNb_organs(volumes.size());
        setnVolumes(volumes.size());

        nb_voxels = new Vector<>();
        voxels = new HashSet<>();
        Z_diff = new ArrayList<>();
        D = new Vector<>(nb_organs);
        Z = new Vector<>(nb_organs);
        voxel_dose = new Vector<>(volumes.size());

        for(Volumen v: volumes)
            DDM.add(v.getDDM());

        for (int i = 0; i < nb_organs; i++)
            this.nb_voxels.add(volumes.get(i).getNb_voxels());

        //Inicializando las 'matrices' o mapas Z y D con 0.0
        for(int i = 0; i < nb_organs; i++){
            Vector this_row = new Vector<Double>();
            for(int j = 0; j < nb_voxels.get(i) ; j++){
                this_row.add(0.0);
            }
            (this.Z).add(this_row);
            (this.D).add(this_row);
        }


        for(int v = 0; v < volumes.size(); v++){
            Vector thisArrow = new Vector<>(150);
            for(int k = 0; k < 150; k++){
                thisArrow.add(0.0);
            }
            voxel_dose.add(thisArrow);
        }

    }

    public double eval2(Plan p, Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax){
        //Parametros para considerar
        Vector<Vector<Double>> t_doses = new Vector<>();
        F = 0.0;
        Vector<Double> IntensityVector = p.getIntensityVector();

        for(int o = 0 ; o < nb_organs; o++){
            Vector<Double> d = new Vector<Double>();
            Matrix doseDeposition = DDM.get(o);
            for(int v = 0; v < nb_voxels.get(o) ; v++){
                double dosis_v = 0.0;
                //Dosis para el voxel v
                for(int i = 0; i < p.getIntensityVector().size(); i++){
                    dosis_v += IntensityVector.get(i) * doseDeposition.getPos(v,i);
                //Radiation_from_DDM[o,v] //organo o en el beamlet v
                }
                d.add(dosis_v);
            }
            t_doses.add(d);
        }

        for (int o = 0; o < nb_organs; o++) {
            double pen = 0.0;
            for (int k = 0; k < nb_voxels.get(o); k++) {
                if (Z.get(o).get(k) < Zmin.get(o)) {
                    pen += w.get(o) * Math.pow(Math.max((Zmin.get(o)-t_doses.get(o).get(k)),0),2);
                }
                if (Z.get(o).get(k) > Zmax.get(o)) {
                    pen += w.get(o) * Math.pow(Math.max((t_doses.get(o).get(k) - Zmax.get(o)),0),2);
                }
            }
            F += pen / nb_voxels.get(o);
        }
        n_evaluations++;
        return F;
    }

    public double eval(Plan p, Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax) {
        generate_Z(p);
        F = 0.0;
        for (int o = 0; o < nb_organs; o++) {
            double pen = 0.0;
            for (int k = 0; k < nb_voxels.get(o); k++) {
                if (Z.get(o).get(k) < Zmin.get(o)) {
                    pen += w.get(o) * Math.pow(Math.max((Zmin.get(o)-Z.get(o).get(k)),0),2);
                }
                if (Z.get(o).get(k) > Zmax.get(o)) {
                    pen += w.get(o) * Math.pow(Math.max((Z.get(o).get(k) - Zmax.get(o)),0),2);

                }
            }
            F += pen / nb_voxels.get(o);
        }
        n_evaluations++;
        return F;
    }

    public void generate_Z(Plan p) {
        ArrayList<Beam> stations = p.getBeams();

        //Setea el vector Z
        for (int o = 0; o < nb_organs; o++) {
            Vector toAdd = new Vector(nb_voxels.get(o));
            for (int i = 0; i < nb_voxels.get(o); i++){
                toAdd.add(0.0);
            }
            Z.setElementAt(toAdd,o);
        }

        //Generacion del vector de dosis por cada angulo
        for (Beam station : stations) {
            //considering 2*Xmid, Xext
            //we update the dose distribution matrices Z with the dose delivered by the station
            for (int o = 0; o < nb_organs; o++) {
                Matrix Depo = station.getDepositionMatrix(o);

                for (int k = 0; k < nb_voxels.get(o); k++) {
                    double dose = 0.0;
                    for (int b = 0; b < station.getNbBeamlets(); b++) {
                        dose += Depo.getPos(k, b)*station.getIntensity(b);
                    }
                    double d = dose + Z.get(o).get(k);

                    Vector pivote = Z.get(o);
                    pivote.setElementAt(d, k);
                    Z.setElementAt(pivote, o);
                }
            }
        }
    }

    public void generateVoxelDoseFunction(){

        for(int o = 0; o < nb_organs; o++){
            Vector<Double> dose = new Vector<>();
            Vector<Double> to_fill = new Vector<>();

            for(int i = 0; i < voxel_dose.get(o).size(); i++) {
                to_fill.add(0.0);
            }
            voxel_dose.add(o,to_fill);

            for(int k = 0; k < nb_voxels.get(o); k++){
                dose.add(Z.get(o).get(k));
                if(Z.get(o).get(k) < 150){
                    int index = (Z.get(o).get(k)).intValue();
                    double val = voxel_dose.get(o).get(index) + 1;

                    Vector pivote = voxel_dose.get(o);
                    pivote.set(index,val);
                    voxel_dose.set(o,pivote);
                }
            }
        }
    }

    /* -------------------------------------------------- GETTER AND SETTERS ----------------------------------------------------------- */
    public double getF() {
        return F;
    }

    public void setF(double f) {
        F = f;
    }

    public void setnVolumes(int nVolumes) {
        this.nVolumes = nVolumes;
    }

    public int getnVolumes() {
        return nVolumes;
    }

    public int getN_evaluations() {
        return n_evaluations;
    }

    public void setN_evaluations(int n_evaluations) {
        this.n_evaluations = n_evaluations;
    }

    public double getPrev_F() {
        return prev_F;
    }

    public void setPrev_F(double prev_F) {
        this.prev_F = prev_F;
    }

    public int getNb_organs() {
        return nb_organs;
    }

    public void setNb_organs(int nb_organs) {
        this.nb_organs = nb_organs;
    }
    /* --------------------------------- PRINTERS ------------------------------- */
}