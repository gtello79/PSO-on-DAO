package source;

import java.util.*;


public class EvaluationFunction {

    private double evaluation;

    //dosis de distribucion por cada organano
    private final ArrayList<ArrayList<Double>> Z;

    //Numero de organos, incluyendo el tumor
    private int nbOrgans;

    //Numero de voxels por cada organo
    private final ArrayList<Integer> nbVoxels;

    //Doses Deposition Matrix
    private final ArrayList<Matrix> DDM = new ArrayList<>();

    //Numero de beamlets x tejido (hasta el momento se espera que todos sean iguales)
    private final ArrayList<Integer> nbBeamLets;


    public EvaluationFunction(ArrayList<Volumen> volumes)
    {
        setEvaluation(0.0);
        setNb_organs( volumes.size() );

        nbVoxels = new ArrayList<>();
        nbBeamLets = new ArrayList<>();
        Z = new ArrayList<>(nbOrgans);

        for(Volumen v: volumes) {
            DDM.add(v.getDDM());
            nbBeamLets.add(v.getNb_beamlets());
        }

        for (int i = 0; i < nbOrgans; i++)
            this.nbVoxels.add(volumes.get(i).getNb_voxels());

        //Inicializando la 'matriz' o mapas Z con 0.0
        for(int i = 0; i < nbOrgans; i++){
            ArrayList thisRow = new ArrayList<Double>();
            for(int j = 0; j < nbVoxels.get(i) ; j++){
                thisRow.add(0.0);
            }
            this.Z.add(thisRow);

        }

    }

    /* ----------------------------------------------------------- METHODS --------------------------------------------------------*/

    public double evalIntensityVector(ArrayList<Double> p, ArrayList<Double> w, ArrayList<Double> Zmin, ArrayList<Double> Zmax){
        //Valor de Funcion objetivo
        setEvaluation(0.0);

        //Se genera el Vector Z (efecto del beamlet i sobre el voxel v, en el organo r)
        for(int o = 0 ; o < nbOrgans; o++){
            double totalBeamlets = nbBeamLets.get(o);
            ArrayList<Double> d = new ArrayList<Double>();

            //Tomo la DDM asociada a un organo o
            Matrix doseDeposition = DDM.get(o);

            //Recorrer todos los voxels de ese organo
            for(int v = 0; v < nbVoxels.get(o) ; v++){
                double dosis_v = 0.0;

                //Dosis para el voxel v
                for(int i = 0; i < totalBeamlets; i++) {
                    dosis_v += doseDeposition.getPos(v, i) * p.get(i);
                }
                d.add(dosis_v);
            }
            //Se agrega el vector Z asociado al organo O
            Z.set(o,d);

        }

        //Se calcula la penalizacion a partir de la dosis estimada
        for (int o = 0; o < nbOrgans; o++) {
            double pen = 0.0;
            double diff = 0.0;
            for (int k = 0; k < nbVoxels.get(o); k++) {

                if (Z.get(o).get(k) < Zmin.get(o)) {
                    diff = Zmin.get(o) - Z.get(o).get(k);
                    pen += w.get(o) * Math.pow(Math.max(diff,0),2);
                }

                if (Z.get(o).get(k) > Zmax.get(o)) {
                    diff = Z.get(o).get(k) - Zmax.get(o);
                    pen += w.get(o) * Math.pow(Math.max(diff,0),2);
                }

            }
            evaluation += pen/nbVoxels.get(o);

        }

        setEvaluation(evaluation);
        return evaluation;
    }


    /* -------------------------------------------------- GETTER AND SETTERS ----------------------------------------------------------- */

    public void setEvaluation(double f) {
        this.evaluation = f;
    }

    public void setNb_organs(int nbOrgans) {
        this.nbOrgans = nbOrgans;
    }


}