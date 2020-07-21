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
    //How much increase/decrease F increasing the voxe in one unity.
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

    private HashSet<Pair<Double, Pair<Integer, Integer>>> voxels;

    private ArrayList< Pair< Pair<Integer,Integer>, Double > > Z_diff;


    public EvaluationFunction(Vector<Volumen> volumes) {
        this.n_evaluations = 0;
        this.prev_F = 0.0;
        this.F = 0.0;
        this.Z = new Vector<>();
        this.D = new Vector<>();
        this.nb_voxels = new Vector<>();
        this.voxels = new HashSet<>();
        this.Z_diff = new ArrayList<>();
        this.nb_organs = volumes.size();
        this.nVolumes = volumes.size();
        for (int i = 0; i < nb_organs; i++) {
            this.nb_voxels.add(volumes.get(i).getNb_voxels());
        }
        D = new Vector<>(nb_organs);
        Z = new Vector<>(nb_organs);
        for(int i = 0; i < nb_organs; i++){
            Vector this_arrow = new Vector<Double>();
            for(int j = 0; j < nb_voxels.get(i) ; j++){
                this_arrow.add(0.0);
            }
            (this.Z).add(this_arrow);
            (this.D).add(this_arrow);
        }

        voxel_dose = new Vector<>(volumes.size());
        for(int v = 0; v < volumes.size(); v++){
            Vector thisArrow = new Vector<>(150);
            for(int k = 0; k < 150; k++){
                thisArrow.add(0.0);
            }
            voxel_dose.add(thisArrow);
        }

    }

    public double eval(Plan p, Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax) {
        voxels.clear();
        generate_Z(p);
        F = 0.0;
        for (int o = 0; o < nb_organs; o++) {
            double pen = 0.0;
            for (int k = 0; k < nb_voxels.get(o); k++) {
                if (Z.get(o).get(k) < Zmin.get(o)) {
                    pen += w.get(o) * (pow(Zmin.get(o) - Z.get(o).get(k), 2));
                }
                if (Z.get(o).get(k) > Zmax.get(o)) {
                    pen += w.get(o) * (pow(Z.get(o).get(k) - Zmax.get(o), 2));
                }
                update_sorted_voxels(w, Zmin, Zmax, o, k, false);
            }
            F += pen / nb_voxels.get(o);
        }
        n_evaluations++;
        return F;
    }

    public void generate_Z(Plan p) {
        ArrayList<Beam> stations = p.getBeams();
        for (int o = 0; o < nb_organs; o++) {
            Vector toAdd = new Vector(nb_voxels.get(o));
            for (int i = 0; i < nb_voxels.get(o); i++){
                toAdd.add(0.0);
            }
            Z.setElementAt(toAdd,o);
        }
        for (Beam station : stations) {
            //considering 2*Xmid, Xext
            //we update the dose distribution matrices Z with the dose delivered by the station
            for (int o = 0; o < nb_organs; o++) {
                Matrix Depo = station.getDepositionMatrix(o);
                for (int k = 0; k < nb_voxels.get(o); k++) {
                    double dose = 0.0;
                    for (int b = 0; b < station.getNbBeamlets(); b++) {
                        dose += Depo.getPos(k, b) * station.getIntensity(b);
                    }
                    double d = dose + Z.get(o).get(k);

                    Vector pivote = Z.get(o);

                    pivote.setElementAt(d,k);
                    Z.setElementAt(pivote,o);
                }
            }

        }
    }

    public void update_sorted_voxels(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, int o, int k, boolean erase) {
        if(erase) voxels.remove(new Pair(abs(D.get(o).get(k)),new Pair(o,k)));

        Vector pivote = D.get(o);
        if (Zmin.get(o) > 0) {
            if (Z.get(o).get(k) < Zmin.get(o)) {
                double val = w.get(o) * (Z.get(o).get(k) - Zmin.get(o)) / (nb_voxels.get(o));
                pivote.setElementAt(val,k);
                D.setElementAt(pivote,o);
            } else {
                pivote.setElementAt(0.0,o);
                D.set(o,pivote);
            }
        } else {
            if (Z.get(o).get(k) > Zmax.get(o)) {
                double val = w.get(o) * (Z.get(o).get(k) - Zmax.get(o)) / (nb_voxels.get(o));
                pivote.setElementAt(val,k);
                D.set(o, pivote);
            } else {
                pivote.setElementAt(0.0,o);
                D.set(o,pivote);
            }
        }
        if (D.get(o).get(k) != 0.0) {
            voxels.add(new Pair(abs(D.get(o).get(k)) ,new Pair(o, k) ));
        }
    }

    public double incremental_eval(Beam beam, Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, ArrayList<Pair<Integer,Double>> diff){
        prev_F = F;
        Z_diff.clear();
        double delta_f = 0.0;

        for(int o = 0; o < nb_organs; o++){
            Matrix D = beam.getDepositionMatrix(o);

            for(int k = 0; k < nb_voxels.get(o) ; k++){
                double delta = 0.0;

                for(Pair<Integer,Double> let: diff ){
                    int b = let.getKey();
                    if(D.getPos(k,b) == 0.0) continue;
                    delta+=D.getPos(k,b)*let.getValue();
                }

                if(delta == 0.0)continue; //no change in the voxel

                double pen = 0.0;

                if(Z.get(o).get(k) < Zmin.get(o) && Z.get(o).get(k)+delta < Zmin.get(o)){
                    pen += w.get(o) * delta * (delta+2*(Z.get(o).get(k) - Zmin.get(o)));
                }else if (Z.get(o).get(k) < Zmin.get(o)){
                    pen -= w.get(o) * (pow(Zmin.get(o) - Z.get(o).get(k),2));
                }else if (Z.get(o).get(k) + delta < Zmin.get(o)){
                    pen += w.get(o) * (pow(Zmin.get(o)-(Z.get(o).get(k)+delta),2));
                }

                if(Z.get(o).get(k) > Zmax.get(o) && Z.get(o).get(k) + delta > Zmax.get(o)){
                    pen += w.get(o)*delta*(delta+2*(-Zmax.get(o) + Z.get(o).get(k)));
                }else if(Z.get(o).get(k) > Zmax.get(o)){
                    pen -=  w.get(o) * ( pow(Z.get(o).get(k)-Zmax.get(o), 2) );
                }else if(Z.get(o).get(k) + delta > Zmax.get(o)){
                    pen += w.get(o) * ( pow(Z.get(o).get(k) + delta - Zmax.get(o), 2) );
                }

                delta_f += pen/nb_voxels.get(o);
                double valor = Z.get(o).get(k) + delta;
                Vector pivote = Z.get(o);
                pivote.set(k,valor);
                Z.set(o,pivote);

                update_sorted_voxels(w,Zmin,Zmax,o,k, true);

                //we save the last changes (see undo_last_eval)
                Z_diff.add(new Pair(new Pair(o,k),delta));
            }
        }
        F+=delta_f;
        n_evaluations++;
        return F;
    }

    public void undoLastEval(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax){
        for(Pair< Pair<Integer,Integer>, Double > z: Z_diff){
            int o = z.getKey().getKey();
            int k = z.getKey().getValue();
            double value = Z.get(o).get(k) - z.getValue();
            Vector pivote = Z.get(o);
            pivote.set(k,value);
            Z.set(o,pivote);

            update_sorted_voxels(w, Zmin, Zmax, o, k,true);
        }
        F = prev_F;
        prev_F = F;
        Z_diff.clear();
    }

    public void generateVoxelDoseFunction(){
        for(int o = 0; o < nb_organs; o++){
            Vector<Double> dose = new Vector<Double>();
            Vector<Double> to_fill = new Vector<Double>();
            for(int i = 0; i < voxel_dose.get(o).size(); i++){
                to_fill.add(0.0);
            }
            voxel_dose.add(o,to_fill);
            for(int k = 0; k < nb_voxels.get(o); k++){
                dose.add(Z.get(o).get(k));
                if(Z.get(o).get(k) > 150){
                    int index = (Z.get(o).get(k)).intValue();
                    double val = voxel_dose.get(o).get(index) + 1;
                    Vector pivote = voxel_dose.get(o);
                    pivote.set(index,val);
                    voxel_dose.set(o,pivote);
                }

            }
            FileWriter fichero = null;
            PrintWriter pw = null;
            try{
                fichero = new FileWriter("plotter/organ_xls"+o+".dat");
                pw = new PrintWriter(fichero);
                for (Double v: dose){
                    pw.println(v);
                }
            }catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (null != fichero)
                        fichero.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }

        for(int o = 0; o < nb_organs; o++){
            FileWriter fichero = null;
            PrintWriter pw = null;
            try{
                fichero = new FileWriter("plotter/organ_xls"+ o +".dat");
                pw = new PrintWriter(fichero);
                double cum = 0.0;
                for (int k = 149; k >= 0; k--){
                    cum+=voxel_dose.get(o).get(k);
                    pw.println((k+1)+","+cum/nb_voxels.get(o));
                }
            }catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (null != fichero)
                        fichero.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    public void generateLineaSystem(Plan p, Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax) {
        boolean flag = false;
        for (int o = 0; o < nb_organs; o++) {
            double pen = 0.0;
            for (int k = 0; k < nb_voxels.get(o); k++) {
                if (flag) System.out.println(" + ");
                flag = true;
                if (k == 0) System.out.println("(");
                if (Zmin.get(o) == 0) System.out.print("max(Z_" + o + "_" + k + "-" + Zmax.get(o) + ",0)^2");
                else System.out.print("max(Z_" + Zmin.get(o) + "- Z_" + o + "_" + k + ",0)^2");
                if (k == nb_voxels.get(o) - 1) System.out.print(")");
            }
            System.out.print("/" + nb_voxels.get(o));
        }
        System.out.println("");
        for (int o = 0; o < nb_organs; o++) {
            for (int k = 0; k < nb_voxels.get(o); k++) {
                System.out.print("Z_" + o + "_" + k + "=");
                //Aperture variable id
                int ap = 0;
                flag = false;
                for (Beam s : p.getBeams()) {
                    Matrix D = s.getDepositionMatrix(o);
                    for (int a = 0; a < s.getNbApertures(); a++) {
                        double apDosePerIntensity = 0.0;
                        for (Integer b : s.openBeamlets(a))
                            apDosePerIntensity += D.getPos(k, b);
                        if (apDosePerIntensity > 0.0) {
                            if (flag) System.out.print('+');
                            System.out.print(apDosePerIntensity + "* I_" + ap);
                            flag = true;
                        }
                    }
                }
                System.out.println("");
            }
        }
    }
}