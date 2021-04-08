package source;

import SRCDAO.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


public class EvaluationFunction {

    private double evaluation;

    //dosis de distribucion por cada organano
    private Vector<Vector<Double>> Z;

    //Numero de organos, incluyendo el tumor
    private int nb_organs;

    //Numero de voxels por cada organo
    private Vector<Integer> nb_voxels;

    //Doses Deposition Matrix
    private Vector<Matrix> DDM = new Vector<>();

    //Numero de beamlets x tejido (hasta el momento se espera que todos sean iguales)
    private Vector<Integer> nb_beamlets;


    public EvaluationFunction(Vector<Volumen> volumes)
    {
        setEvaluation(0.0);
        setNb_organs( volumes.size() );

        nb_voxels = new Vector<>();
        nb_beamlets = new Vector<>();
        Z = new Vector<>(nb_organs);

        for(Volumen v: volumes) {
            DDM.add(v.getDDM());
            nb_beamlets.add(v.getNb_beamlets());
        }
        for (int i = 0; i < nb_organs; i++)
            this.nb_voxels.add(volumes.get(i).getNb_voxels());

        //Inicializando la 'matriz' o mapas Z con 0.0
        for(int i = 0; i < nb_organs; i++){
            Vector this_row = new Vector<Double>();
            for(int j = 0; j < nb_voxels.get(i) ; j++){
                this_row.add(0.0);
            }
            (this.Z).add(this_row);

        }

    }

    /* ----------------------------------------------------------- METHODS --------------------------------------------------------*/

    public double evalIntensityVector(Vector<Double> p, Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax){
        //Valor de Funcion objetivo
        setEvaluation(0.0);

        //Se genera el Vector Z (efecto del beamlet i sobre el voxel v, en el organo r)
        for(int o = 0 ; o < nb_organs; o++){
            double totalBeamlets = nb_beamlets.get(o);
            Vector<Double> d = new Vector<Double>();

            //Tomo la DDM asociada a un organo o
            Matrix doseDeposition = DDM.get(o);

            //Recorrer todos los voxels de ese organo
            for(int v = 0; v < nb_voxels.get(o) ; v++){
                double dosis_v = 0.0;

                //Dosis para el voxel v
                for(int i = 0; i < totalBeamlets; i++) {
                    dosis_v += doseDeposition.getPos(v, i) * p.get(i);
                }
                d.add(dosis_v);
            }
            //Se agrega el vector Z asociaco al organo R
            Z.set(o,d);
        }

        //Se calcula la penalizacion a partir de la dosis estimada
        for (int o = 0; o < nb_organs; o++) {
            double pen = 0.0;
            double diff;
            for (int k = 0; k < nb_voxels.get(o); k++) {
                if (Z.get(o).get(k) < Zmin.get(o)) {
                    diff = Zmin.get(o) - Z.get(o).get(k);
                    pen += w.get(o) * Math.pow(Math.max(diff,0),2);
                }
                if (Z.get(o).get(k) > Zmax.get(o)) {
                    diff = Z.get(o).get(k) - Zmax.get(o);
                    pen += w.get(o) * Math.pow(Math.max(diff,0),2);
                }
            }
            evaluation += pen/nb_voxels.get(o);
            //System.out.println("Region "+ o +": " +pen/nb_voxels.get(o));
        }

        setEvaluation(evaluation);
        return evaluation;
    }


    /* -------------------------------------------------- GETTER AND SETTERS ----------------------------------------------------------- */

    public void setEvaluation(double f) {
        this.evaluation = f;
    }

    public void setNb_organs(int nb_organs) {
        this.nb_organs = nb_organs;
    }

    /*---------------------------------- METODOS PENDIENTES ---------------------------------------------------------------------------*/

}