package Utils;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import gurobi.*;

import SRCDAO.Plan;
import source.Volumen;

/**
 * @author Mauricio Moyano
 * @coauthor: Gonzalo Tello Valenzuela gonzalotello79@gmail.com
 */

public class Gurobi_Solver {

    public int max_intensity;
    public int organs;                          //number of organ
    public int beams;                           //number of organ
    public int []R;                             //number of voxels by region
    public int aperture;                        //number of aperture

    public int[] bmlts;                         // number of beamlets by angles
    public ArrayList<Double> weight;               //weights of objective

    public int totalBmlts;                      //total beamlets
    public int[] angles;

    public double[][] newIntensity;

    public double[] LB;
    public double[] UB;
    public Boolean[] isTarget;
    public double epsilon;
    public double[] x;
    public String jobThreadID;
    public String solver;
    public double maxIntensity;
    public double minIntensity;
    public double objVal;

    public int[] eud;
    GRBEnv env;
    GRBModel model;

    ArrayList<Volumen> M;
    //DDM M;
    Plan sol;

    public Gurobi_Solver(Plan sol, ArrayList<Volumen> volumen, int[] selAngles, double[] dd, ArrayList<Double> weight) throws Exception {

        this.sol=sol;
        this.beams = sol.getNBeam();
        this.eud = doubletoint(dd);
        this.organs = volumen.size();
        this.R = new int[organs];
        this.bmlts = sol.getBeamletsByBeam();
        this.weight = weight;
        this.bmlts = sol.getBeamletsByBeam();
        this.aperture = sol.getTotalApertureByBeam(0);

        this.minIntensity = 0;
        this.maxIntensity = sol.getMaxIntensityByAperture()*this.aperture;
        this.M = volumen;

        for(int i = 0; i<R.length; i++) {
            this.R[i] = M.get(i).getNb_voxels();
        }
        setEnv();
        setModelPen();
        //setModelORPen();
        //writeModel();
        model.dispose();
        env.dispose();
    }



    public int[] doubletoint(double[] doubleArray) {
        int[] intArray = new int[doubleArray.length];
        for (int i=0; i<intArray.length; ++i)
            intArray[i] = (int) doubleArray[i];
        return intArray;
    }

    public void reset() {
        try {
            model.reset();
        } catch (GRBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setEnv() throws GRBException {
        this.env = new GRBEnv(true);
        env.set("OutputFlag", "0");
        env.set("logFile", "mip1.log");

        //env.set(GRB.IntParam.Threads, 4);
        env.set("Aggregate","0");
        env.set("Method", "2");
        env.set("NormAdjust", "2");
        env.start();
    }

    /**
     * Setea el modelo que minimiza la penalizacion de todos los organos,
     * no tiene restricciones
     */
    public void setModelPen() throws Exception {
        this.model = new GRBModel(env);
        model.set(GRB.StringAttr.ModelName, "Direct Aperture Optimization");

        // set variables for intensity an V
        GRBVar[][] intensity= new GRBVar[beams][aperture];//intensity for beam,for aperture
        GRBVar[][] voxel=new GRBVar[R.length][];
        int indexI = 0;
        int indexJ = 0;

        for (int i = 0; i < this.beams; ++i) {
            for (int j = 0; j < this.aperture; ++j) {
                indexI=i+1;
                indexJ=j+1;
                intensity[i][j] = model.addVar(
                    minIntensity,20,0.0, GRB.CONTINUOUS ,
                    "Intensity" +"["+ indexI + "." + indexJ+"]"
                );
                intensity[i][j].set(GRB.DoubleAttr.Start, sol.getIntensityByAperture(i,j));
            }
        }

        for (int i = 0; i < organs; ++i) {
            voxel[i]=new GRBVar[R[i]];
            for (int j = 0; j < R[i]; ++j) {
                indexI=i+1;
                indexJ=j+1;

                if(i==2){
                    voxel[i][j] = model.addVar(-GRB.INFINITY,GRB.INFINITY,0.0, GRB.CONTINUOUS ,"v" + indexI + "[" + indexJ+"]");
                }else{
                    voxel[i][j] = model.addVar(0.0,100.0,0.0, GRB.CONTINUOUS ,"v" + indexI + "[" + indexJ+"]");
                }
            }
        }

        //set constraints
        Hashtable<Integer, ArrayList<Integer>> aux_index;
        Hashtable<String, Double> aux_values;
        Enumeration<Integer> keys;
        ArrayList<Integer> beams;
        String valueIndexKey;
        int key;
        int beamblet;
        int totalBeamblets;
        int beamIndex;
        int count_voxel;
        double radiation, coefficent;
        int diffBeamblets = 0;

        for (int o = 0;  o < organs; o++) {
            //Recuperacion de index de voxels del organo o. Por organo (elemento del arrayList) -> id_voxel -> {id_beamlet}
            aux_index = M.get(o).getIndexDAODDM();
            //Mapa del organo {id_voxel-id_beamlet -> radiacion}
            aux_values = M.get(o).getValueDAODDM();
            keys = aux_index.keys();

            //Recorremos claves de voxel por organo para su evaluación
            count_voxel = 0;
            while(keys.hasMoreElements()){
                GRBLinExpr voxelRadiation= new GRBLinExpr();
                key = keys.nextElement();
                beams = aux_index.get(key);
                // Vamos a sacar el beam (indice del angulo)
                for(int b = 0; b < beams.size(); b++){
                    valueIndexKey = key + "-" + beams.get(b);
                    radiation = aux_values.get(valueIndexKey);

                    beamblet = beams.get(b);

                    totalBeamblets = 0;
                    beamIndex = 0;
                    diffBeamblets=0;

                    for(int z = 0; z < bmlts.length; z++) {
                        totalBeamblets+= bmlts[z];
                        if(beamblet < totalBeamblets){
                            beamIndex = z;
                            break;
                        }
                        diffBeamblets+=bmlts[z];
                    }

                    for(int a = 0; a < aperture; a++) {
                        int localBeamLet = beamblet-diffBeamblets;
                        coefficent = (double)sol.getProyectedBeamLetByApertureOnBeam(beamIndex, a, localBeamLet) * radiation;

                        if(coefficent != 0 && o==2) {

                            coefficent=coefficent*-1;
                            voxelRadiation.addTerm(coefficent,intensity[beamIndex][a] );

                        }
                        else {
                            voxelRadiation.addTerm(coefficent,intensity[beamIndex][a] );
                        }
                    }
                }

                if(o==2) {
                    //Si el organo es el tumor
                    voxelRadiation.addConstant(eud[o]);
                }else{
                    //Si el organo es OAR
                    int constEud=eud[o]*-1;
                    voxelRadiation.addConstant(constEud);
                }

                GRBLinExpr V=new GRBLinExpr();
                V.addTerm(1,voxel[o][count_voxel]);

                if(o==2) {
                    //Si el organo es el tumor
                    model.addConstr(V, GRB.EQUAL, voxelRadiation, "voxelRadiation"+o+"["+(count_voxel+1)+"]");
                }else {
                    //Si el organo es OAR
                    model.addConstr(V, GRB.GREATER_EQUAL, voxelRadiation, "voxelRadiation"+o+"["+(count_voxel+1)+"]");
                }
                count_voxel++;
            }
        }

        //set model
        GRBQuadExpr objFunc= new GRBQuadExpr();
        for(int o = 0;  o < organs; o++) {
            double coef=(double)((weight.get(o)/R[o]));

            for (int j = 0; j < R[o]; ++j) {
                objFunc.addTerm(coef, voxel[o][j],voxel[o][j]);
                System.out.print("");
            }
        }

        model.setObjective(objFunc, GRB.MINIMIZE);
        model.update();
        writeModel();
        model.optimize();
        model.update();
        // model.computeIIS();
        //model.write("mod.ilp");
        double[][]getIntensity = new double[this.beams][this.aperture];
        for (int i = 0 ; i < this.beams; ++i) {
            for (int j = 0 ; j < this.aperture; ++j) {
                getIntensity[i][j]=intensity[i][j].get(GRB.DoubleAttr.X);
            }
        }

        newIntensity=getIntensity;
        objVal=model.get(GRB.DoubleAttr.ObjVal);

    }


    public void writeModel() throws GRBException {
        model.write("out1.lp");
    }

}