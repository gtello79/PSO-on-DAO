package source;
import java.util.ArrayList;
import SRCDAO.Beamlet;

public class EvaluationFunction {

    // Valor de Fluence Map en la funcion de evaluacion
    private double evaluation;

    // Dosis de distribucion por cada organano
    private final ArrayList<ArrayList<Double>> Z;

    // Cantidad de organos, incluyendo el tumor
    private int nbOrgans;

    // Numero de voxels por cada organo
    private final ArrayList<Integer> nbVoxels;

    // Doses Deposition Matrix
    private final ArrayList<Matrix> DDM = new ArrayList<>();

    // N beamlets x tejido
    private final ArrayList<Integer> nbBeamLets;

    // Distribucion de la radiacion en cada organo
    private double[] distribution;

    private static ArrayList<Double> w;
    private static ArrayList<Double> Zmin;
    private static ArrayList<Double> Zmax;

    public static void ActivateEvaluationFunction(ArrayList<Double> weight, ArrayList<Double> Z_min,
        ArrayList<Double> Z_max) {
        // This method can be used to initialize any static resources if needed
        // For now, it does nothing as the EvaluationFunction is instance-based
        EvaluationFunction.w = weight;
        EvaluationFunction.Zmin = Z_min;
        EvaluationFunction.Zmax = Z_max;

    }

    public EvaluationFunction(ArrayList<Volumen> volumes) {
        setEvaluation(0.0);
        setNb_organs(volumes.size());
        this.distribution = new double[this.nbOrgans];

        this.nbVoxels = new ArrayList<>();
        this.nbBeamLets = new ArrayList<>();
        this.Z = new ArrayList<>(nbOrgans);

        for (Volumen v : volumes) {
            DDM.add(v.getDDM());
            nbBeamLets.add(v.getNb_beamlets());
        }

        for (int i = 0; i < nbOrgans; i++)
            this.nbVoxels.add(volumes.get(i).getNb_voxels());

        // Inicializando los vectores de dosis para cada organo
        for (int i = 0; i < nbOrgans; i++) {
            ArrayList<Double> thisRow = new ArrayList<Double>();
            for (int j = 0; j < nbVoxels.get(i); j++) {
                thisRow.add(0.0);
            }
            this.Z.add(thisRow);
        }

    }

    /*
     * ----------------------------------------------------------- METHODS
     * --------------------------------------------------------
     */

    public double evalIntensityVector(ArrayList<Double> p, Collimator collimator) {
        //Valor de Funcion objetivo
        setEvaluation(0.0);
        
        // Se genera el Vector Z (efecto del beamlet i sobre el voxel v, en el organo r)
        for (int o = 0; o < nbOrgans; o++) {
            ArrayList<Double> d = new ArrayList<Double>();

            // Tomo la DDM asociada a un organo o
            Matrix doseDeposition = DDM.get(o);

            // Recorrer todos los voxels de ese organo
            for (int v = 0; v < nbVoxels.get(o); v++) {
                double dosis_v = 0.0;

                // Dosis para el voxel v
                for (int a : collimator.getAngles()) {
                    ArrayList<Beamlet> beamletsAngle = collimator.getBeamletListByAngle(a);
                    for (Beamlet b : beamletsAngle) {
                        int beamIndex = b.getId();
                        try{
                            //double ration = p.get(beamIndex);
                            double ration = b.isUsedInIdeal() ? p.get(beamIndex) : 0.0;
                            dosis_v += doseDeposition.getPos(v, beamIndex) * ration;
                        }catch(IndexOutOfBoundsException e){
                            dosis_v += 0.0;
                            //throw new IndexOutOfBoundsException("Index out of bounds: " + e.getMessage());
                        }
                    }
                }

                d.add(dosis_v);
            }
            // Se agrega el vector Z asociado al organo O
            Z.set(o, d);

        }

        // Se calcula la penalizacion a partir de la dosis estimada
        for (int o = 0; o < nbOrgans; o++) {
            double pen = 0.0;
            double diff = 0.0;
            for (int k = 0; k < nbVoxels.get(o); k++) {

                if (Z.get(o).get(k) < Zmin.get(o)) {
                    diff = Zmin.get(o) - Z.get(o).get(k);
                    pen += w.get(o) * Math.pow(Math.max(diff, 0), 2);
                }

                if (Z.get(o).get(k) > Zmax.get(o)) {
                    diff = Z.get(o).get(k) - Zmax.get(o);
                    pen += w.get(o) * Math.pow(Math.max(diff, 0), 2);
                }

            }
            double penalizationOrg = pen / nbVoxels.get(o);

            distribution[o] = penalizationOrg;
            evaluation += penalizationOrg;
        }

        setEvaluation(evaluation);
        return evaluation;
    }

    /* ----------------------- GETTER AND SETTERS -----------------------*/

    public void setEvaluation(double f) {
        this.evaluation = f;
    }

    public void setNb_organs(int nbOrgans) {
        this.nbOrgans = nbOrgans;
    }

    public double[] getDistributionIntensity() {
        return distribution;
    }

}