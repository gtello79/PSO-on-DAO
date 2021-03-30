package source;

import SRCDAO.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


public class EvaluationFunction {

    private double F;

    private int n_evaluations;

    //Matrix of derivatives for each organ and voxel
    //How much increase/decrease F increasing the voxel in one unity.
    private Vector<Vector<Double>> D;

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
        setN_evaluations(0);
        setF(0.0);
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
        F = 0.0;

        //Se genera el Vector Z (efecto del beamlet i sobre el voxel v, en el organo r)
        for(int o = 0 ; o < nb_organs; o++){
            double totalBeamlets = nb_beamlets.get(o);
            Vector<Double> d = new Vector<Double>();

            //Tomo la DDM asociada a un organo o
            Matrix doseDeposition = DDM.get(o);
            //doseDeposition.printShape();

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
            F += pen/nb_voxels.get(o);
            //System.out.println("Region "+ o +": " +pen/nb_voxels.get(o));
        }
        return F;
    }


    /* -------------------------------------------------- GETTER AND SETTERS ----------------------------------------------------------- */

    public void setF(double f) {
        F = f;
    }

    public void setN_evaluations(int n_evaluations) {
        this.n_evaluations = n_evaluations;
    }

    public void setNb_organs(int nb_organs) {
        this.nb_organs = nb_organs;
    }

    /*---------------------------------- METODOS PENDIENTES ---------------------------------------------------------------------------*/

    public double eval(Plan p, Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax) {
        generate_Z(p);
        F = 0.0;
        //Se recorren los organos
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

    //Genera la dosis de cada beamlet en cada angulo sobre un voxel
    public void generate_Z(Plan p) {
        //Se obtiene cada beam
        Vector<Beam> stations = p.getBeams();

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
            //Se estima usando el error cuadratico medio
            //considering 2*Xmid, Xext
            //Se actualiza la matriz Z para la distribucion de dosis con la dosis suministrada por beam
            //we update the dose distribution matrices Z with the dose delivered by the actual beam
            for (int o = 0; o < nb_organs; o++) {
                //Dosis distribuida por cada organo desde los beamlets
                Matrix Depo = station.getDepositionMatrix(o);

                for (int k = 0; k < nb_voxels.get(o); k++) {
                    double dose = 0.0;
                    //Se calcula la dosis de cada beamlet en el actual beam
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

    /***
     * Metodo implementado para cargar un vector te intensidad externa nuestros propositos
     * utilizado netamente para
     */
    public Vector<Double> importIntensityVector(String VectorName){
        String path = "import/intensityVector/";
        String fileName = VectorName;
        String delimiter = " ";
        File archivo = new File(path+fileName);
        Vector<Double> intensityVector = new Vector<>();
        int beamletsbybeam = 0;

        if(!archivo.exists()) {
            System.out.println("ERROR: NO SE ENCUENTRA EL ARCHIVO "+fileName);
        }else{
            Scanner reader = null;
            try {
                reader = new Scanner(archivo);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            while ( reader.hasNextLine() ){
                String datLine = reader.nextLine();

                String[] x = datLine.split(delimiter);
                if(!x[0].equals("") && !x[0].equals(";")) {

                    String[] y = x[x.length-1].split("\t");
                    double intensity = Double.parseDouble(y[y.length - 1]);
                    intensityVector.add(intensity);
                    beamletsbybeam++;
                }
                else{
                    if(x[0].equals(";")){
                        beamletsbybeam = 0;
                    }
                }

            }
        }
        return intensityVector;
    }

}