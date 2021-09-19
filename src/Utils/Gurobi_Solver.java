package Utils;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import SRCDAO.Plan;
import gurobi.*;
import source.Volumen;


/**
 *
 * @author Moyano
 * @coautho: Gonzalo Tello Valenzuela gonzalotello79@gmail.com
 */

public class Gurobi_Solver {

    public int organs;                          //number of organ
    public int beams;                           //number of organ
    public int []R;                             //number of voxels by region
    public int aperture;                        //number of aperture

    public int[] bmlts;                         // number of beamlets by angles
    public Vector<Double> weight;                     //weights of objective

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

    Vector<Volumen> M;
    //DDM M;
    Plan sol;

    public Gurobi_Solver(Plan sol, Vector<Volumen> volumen, int[] selAngles, double[] dd, Vector<Double> weight) throws GRBException {

        this.beams = sol.getNBeam();
        this.eud = doubletoint(dd);
        this.organs = volumen.size();
        this.R = new int[organs];
        this.bmlts = new int[beams];
        this.weight = weight;
        this.bmlts = selAngles;
        this.aperture = sol.getTotalApertureByBeam(0);

        this.minIntensity = 1;
        this.maxIntensity = sol.getMaxIntensityByAperture()*this.aperture;
        this.M = volumen;
        this.sol=sol;

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

    public Gurobi_Solver(Plan sol, Vector<Volumen> volumen, int[] selAngles, double[] dd, Vector<Double> weight, int a, int n) throws GRBException {
        this.sol=sol;
        this.weight=weight;
        this.beams = sol.getNBeam();
        this.eud=doubletoint(dd);
        this.organs= volumen.size();
        this.aperture = sol.getTotalApertureByBeam(0);
        this.bmlts=new int[beams];
        this.bmlts=selAngles;

        this.aperture = sol.getMaxApertures().get(0);
        this.minIntensity = n;
        this.maxIntensity = sol.getMaxIntensityByAperture()*this.aperture;
        this.M = volumen;

        this.R=new int[organs];
        for(int i=0;i<R.length;i++) {
            this.R[i] = M.get(i).getNb_voxels();
        }
        setEnv();
        setModelPenAllApertures();
        //setModelORPen();
        writeModel();
        model.dispose();
        env.dispose();

    }

    public Gurobi_Solver(Plan sol, Vector<Volumen> volumen, int[] selAngles, double[] dd, Vector<Double> weight, int min) throws GRBException {
        this.sol = new Plan(sol);
        this.beams = sol.getNBeam();
        this.eud = doubletoint(dd);
        this.organs = volumen.size();
        this.R = new int[organs];
        this.aperture = sol.getTotalApertureByBeam(0);

        this.bmlts = new int[beams];
        this.weight = weight;
        this.bmlts = selAngles;
        this.maxIntensity = sol.getMaxIntensityByAperture()*this.aperture;
        this.minIntensity = min;

        this.M = volumen;

        for(int i=0;i<R.length;i++) {
            this.R[i] = M.get(i).getNb_voxels();
        }
        setEnv();
        setModelPen();
        //setModelORPen();
        //writeModel();
        model.dispose();
        env.dispose();
    }

    public Gurobi_Solver(Plan sol, Vector<Volumen> volumen, int[] selAngles, int[] dd, Vector<Double> weight) throws GRBException {
        this.sol = new Plan(sol);
        this.beams = sol.getNBeam();
        this.eud = dd;
        this.organs = volumen.size();
        this.R = new int[organs];
        this.bmlts = new int[beams];
        this.weight = weight;
        this.bmlts = selAngles;
        this.aperture = sol.getTotalApertureByBeam(0);
        this.minIntensity = 0;
        this.maxIntensity = sol.getMaxIntensityByAperture()*this.aperture;
        this.M = volumen;

        for(int i=0;i<R.length;i++) {
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

    public void setNewModel(Plan sol) {
        //Todas los los beams tienen la misma cantidad de aperturas
        aperture = sol.getTotalApertureByBeam(0);
        this.sol = new Plan(sol);
        boolean error=true;
        do {
            try {
                setEnv();
                setModelORPen();
                //setModel();
                //model.dispose();
                //env.dispose();
                error=false;
            } catch (GRBException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }while(error);
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

        // Set Variables
        // set variables for intensity an V
        GRBVar[][] intensity= new GRBVar[beams][aperture];//intensity for beam,for aperture
        GRBVar[][] voxel=new GRBVar[R.length][];
        int indexI = 0;
        int indexJ = 0;

        for (int i = 0; i < this.beams; ++i) {
            for (int j = 0; j < this.aperture; ++j) {
                indexI=i+1;
                indexJ=j+1;
                intensity[i][j] = model.addVar(minIntensity,20,0.0, GRB.CONTINUOUS ,"Intensity" +"["+ indexI + "." + indexJ+"]");
                intensity[i][j].set(GRB.DoubleAttr.Start, sol.getIntensityByAperture(i,j));
            }
        }

        for (int i = 0; i < this.R.length; ++i) {
            voxel[i]=new GRBVar[R[i]];
            for (int j = 0; j < R[i]; ++j) {
                indexI=i+1;
                indexJ=j+1;
                voxel[i][j] = model.addVar(0.0,100.0,0.0, GRB.CONTINUOUS ,"v" + indexI + "[" + indexJ+"]");

            }
        }

        //set constraints
        ArrayList<Hashtable<Integer, ArrayList<Integer>>> index_dao_ddm = M.index_dao_ddm;
        ArrayList<Hashtable<String, Double>> value_dao_ddm = M.value_dao_ddm;
        Hashtable<Integer, ArrayList<Integer>> aux_index;
        Hashtable<String, Double> aux_values;
        Enumeration<Integer> keys;
        ArrayList<Integer> beams;
        String valueIndexKey;
        Integer key, beamblet, totalBeamblets,beamIndex, count_voxel;
        Double radiation, coefficent;
        int diffBeamblets=0;

        for (int o = 0;  o< organs; o++) {
            aux_index = index_dao_ddm.get(o);
            aux_values = value_dao_ddm.get(o);
            keys = aux_index.keys();

            //Recorremos claves de voxel por organo para su evaluación

            count_voxel = 0;
            while(keys.hasMoreElements()){
                GRBLinExpr voxelRadiation= new GRBLinExpr();
                key = keys.nextElement();
                beams = aux_index.get(key);

                // Vamos a sacar el beam (indice del angulo)
                for(int b = 0; b < beams.size(); b++){
                    valueIndexKey = key + "-"+beams.get(b);
                    radiation = aux_values.get(valueIndexKey);
                    beamblet = beams.get(b);
                    totalBeamblets = 0;
                    beamIndex = 0;
                    diffBeamblets=0;
                    for(int z=0; z<bmlts.length;z++) {
                        totalBeamblets+= bmlts[z];
                        if(beamblet < totalBeamblets){
                            beamIndex = z;
                            break;
                        }
                        diffBeamblets+=bmlts[z];
                    }

                    for(int a = 0; a < aperture; a++) {
                        coefficent = (double)sol.aperturesBmlts.get(beamIndex)[a][beamblet-diffBeamblets]*radiation;
                        //coefficent = (double)aper[beamIndex][a][(beamblet-diffBeamblets)]*radiation;
                        if(coefficent!=0 && o==0) {
                            coefficent=coefficent*-1;
                            voxelRadiation.addTerm(coefficent,intensity[beamIndex][a] );
                            //System.out.println(coefficent);
                        }
                        else {
                            voxelRadiation.addTerm(coefficent,intensity[beamIndex][a] );
                        }
                    }
                }

                if(o==0) {
                    voxelRadiation.addConstant(eud[o]);
                }else{
                    int constEud=eud[o]*-1;
                    voxelRadiation.addConstant(constEud);
                }

                GRBLinExpr V=new GRBLinExpr();
                V.addTerm(1,voxel[o][count_voxel]);

                if(o==0) {
                    model.addConstr(V, GRB.EQUAL, voxelRadiation, "voxelRadiation"+o+"["+(count_voxel+1)+"]");
                }else {
                    model.addConstr(V, GRB.GREATER_EQUAL, voxelRadiation, "voxelRadiation"+o+"["+(count_voxel+1)+"]");
                }

                count_voxel++;
            }
        }

        //set model
        GRBQuadExpr objFunc= new GRBQuadExpr();
        for(int o = 0;  o< organs; o++) {
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
        double[][]getIntensity=new double[this.beams][this.aperture];
        for (int i = 0; i < this.beams; ++i) {
            for (int j = 0; j < this.aperture; ++j) {
                getIntensity[i][j]=intensity[i][j].get(GRB.DoubleAttr.X);
                //String varName="intensity"+i+"."+j;
                //getIntensity[i][j]=model.getVarByName(varName).get(GRB.DoubleAttr.X);
                //intensity[i][j] = model.addVar(minIntensity,maxIntensity,0.0, GRB.CONTINUOUS ,"Intensity" + i + "." + j);
            }
        }
        //GRBVar[] vars = model.getVars();
        newIntensity=getIntensity;
        objVal=model.get(GRB.DoubleAttr.ObjVal);

    }

    /**
     * Setea el modelo que minimiza la penalizacion de todos los organos, esta funcion considera que un angulo puede tener mas aperturas que otro
     * no tiene restricciones
     */
    public void setModelPenAllApertures() throws Exception {
        this.model = new GRBModel(env);
        model.set(GRB.StringAttr.ModelName, "Direct Aperture Optimization");

        //set Variables
        // set variables for intensity an V
        GRBVar[][] intensity= new GRBVar[beams][];//intensity for beam,for aperture
        GRBVar[][] voxel=new GRBVar[R.length][];
        int indexi=0,indexj=0;

        for (int i = 0; i < this.beams; ++i) {
            int cantApertureByBeam = sol.getTotalApertureByBeam(i);
            intensity[i] = new GRBVar[cantApertureByBeam];
            for (int j = 0; j < cantApertureByBeam; ++j) {
                indexi=i+1;
                indexj=j+1;
                intensity[i][j] = model.addVar(minIntensity,20,0.0, GRB.CONTINUOUS ,"Intensity" +"["+ indexi + "." + indexj+"]");
                intensity[i][j].set(GRB.DoubleAttr.Start, sol.getIntensityByAperture(i,j));
            }
        }

        for (int i = 0; i < this.R.length; ++i) {
            voxel[i]=new GRBVar[R[i]];
            for (int j = 0; j < R[i]; ++j) {
                indexi=i+1;
                indexj=j+1;
                voxel[i][j] = model.addVar(0.0,1000.0,0.0, GRB.CONTINUOUS ,"v" + indexi + "[" + indexj+"]");

            }
        }

        //set constraints
        ArrayList<Hashtable<Integer, ArrayList<Integer>>> index_dao_ddm = M.index_dao_ddm;
        ArrayList<Hashtable<String, Double>> value_dao_ddm = M.value_dao_ddm;
        Hashtable<Integer, ArrayList<Integer>> aux_index;
        Hashtable<String, Double> aux_values;
        Enumeration<Integer> keys;
        ArrayList<Integer> beams;
        String valueIndexKey;
        int key, beamblet, totalBeamblets,beamIndex, count_voxel;
        double radiation, coefficent;
        int diffBeamblets = 0;

        for (int o = 0;  o< organs; o++) {
            aux_index = index_dao_ddm.get(o);
            aux_values = value_dao_ddm.get(o);
            keys = aux_index.keys();

            //Recorremos claves de voxel por organo para su evaluación
            count_voxel = 0;
            while(keys.hasMoreElements()){
                GRBLinExpr voxelRadiation= new GRBLinExpr();
                key = keys.nextElement();
                beams = aux_index.get(key);

                //vamos a sacar el beam (indice del angulo)
                for(int b = 0; b < beams.size(); b++){

                    valueIndexKey = key+"-"+beams.get(b);
                    radiation = aux_values.get(valueIndexKey);
                    beamblet = beams.get(b);
                    totalBeamblets = 0;
                    beamIndex = 0;
                    diffBeamblets=0;
                    for(int z = 0; z < bmlts.length; z++) {
                        totalBeamblets += bmlts[z];
                        if(beamblet < totalBeamblets){
                            beamIndex = z;
                            break;
                        }
                        diffBeamblets+=bmlts[z];
                    }

                    for(int a = 0; a < aperture ; a++) {
                        coefficent = (double)sol.aperturesBmlts.get(beamIndex)[a][beamblet-diffBeamblets]*radiation;
                        //coefficent = (double)aper[beamIndex][a][(beamblet-diffBeamblets)]*radiation;
                        if(coefficent!=0 && o==0) {
                            coefficent=coefficent*-1;
                            voxelRadiation.addTerm(coefficent,intensity[beamIndex][a] );
                            //System.out.println(coefficent);
                        }
                        else {
                            voxelRadiation.addTerm(coefficent,intensity[beamIndex][a] );
                        }

                    }
                }
                if(o==0) {
                    voxelRadiation.addConstant(eud[o]);
                }
                else{
                    int constEud=eud[o]*-1;
                    voxelRadiation.addConstant(constEud);
                }
                GRBLinExpr V=new GRBLinExpr();
                V.addTerm(1,voxel[o][count_voxel]);

                if(o==0) {
                    model.addConstr(V, GRB.EQUAL, voxelRadiation, "voxelRadiation"+o+"["+(count_voxel+1)+"]");
                }else {
                    model.addConstr(V, GRB.GREATER_EQUAL, voxelRadiation, "voxelRadiation"+o+"["+(count_voxel+1)+"]");
                }

                count_voxel++;
            }
        }

        //set model
        GRBQuadExpr objFunc= new GRBQuadExpr();
        for(int o = 0;  o< organs; o++) {
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
        double[][]getIntensity=new double[this.beams][];
        for (int i = 0; i < this.beams; ++i) {
            int totalApertures = sol.getTotalApertureByBeam(i);
            getIntensity[i]=new double[totalApertures];
            for (int j = 0; j < totalApertures; ++j) {
                getIntensity[i][j]=intensity[i][j].get(GRB.DoubleAttr.X);
                //String varName="intensity"+i+"."+j;
                //getIntensity[i][j]=model.getVarByName(varName).get(GRB.DoubleAttr.X);
                //intensity[i][j] = model.addVar(minIntensity,maxIntensity,0.0, GRB.CONTINUOUS ,"Intensity" + i + "." + j);

            }
        }
        //GRBVar[] vars = model.getVars();
        newIntensity=getIntensity;
        objVal=model.get(GRB.DoubleAttr.ObjVal);

    }

    /**
     * utiliza el modelo minimiza la penalizacion de los organos en riesgo y tiene como restricion el eud 0 para el tumor
     * */
    public void setModelORPen() throws Exception {
        this.model = new GRBModel(env);
        model.set(GRB.StringAttr.ModelName, "Direct Aperture Optimization");

        //set Variables
        // set variables for intensity an V
        GRBVar[][] intensity= new GRBVar[beams][aperture];//intensity for beam,for aperture
        GRBVar[][] voxel=new GRBVar[(R.length-1)][];
        int indexi=0,indexj=0;

        for (int i = 0; i < this.beams; ++i) {
            for (int j = 0; j < this.aperture; ++j) {
                indexi=i+1;
                indexj=j+1;
                intensity[i][j] = model.addVar(minIntensity,20,0.0, GRB.CONTINUOUS ,"Intensity" +"["+ indexi + "." + indexj+"]");
                intensity[i][j].set(GRB.DoubleAttr.Start, sol.getIntensityByAperture(i,j));
            }
        }

        for (int i = 0; i < (this.R.length-1); ++i) {
            voxel[i]=new GRBVar[R[(i+1)]];
            for (int j = 0; j < R[(i+1)]; ++j) {
                indexi=i+1;
                indexj=j+1;
                voxel[i][j] = model.addVar(0.0,50.0,0.0, GRB.CONTINUOUS ,"v" + indexi + "[" + indexj+"]");

            }
        }

        //set constraints
        ArrayList<Hashtable<Integer, ArrayList<Integer>>> index_dao_ddm = M.index_dao_ddm;
        ArrayList<Hashtable<String, Double>> value_dao_ddm = M.value_dao_ddm;
        Hashtable<Integer, ArrayList<Integer>> aux_index;
        Hashtable<String, Double> aux_values;
        Enumeration<Integer> keys;
        ArrayList<Integer> beams;
        String valueIndexKey;
        Integer key, beamblet, totalBeamblets,beamIndex, count_voxel;
        Double radiation, coefficent;
        int diffBeamblets=0;

        for (int o = 0;  o< organs; o++) {
            aux_index = index_dao_ddm.get(o);
            aux_values = value_dao_ddm.get(o);
            keys = aux_index.keys();

            //Recorremos claves de voxel por organo para su evaluación
            count_voxel = 0;
            while(keys.hasMoreElements()){
                GRBLinExpr voxelRadiation= new GRBLinExpr();
                key = keys.nextElement();
                beams = aux_index.get(key);

                for(int b = 0; b<beams.size(); b++){ // de aqui vamos a sacar el beam (indice del angulo)
                    valueIndexKey = key+"-"+beams.get(b);
                    radiation = aux_values.get(valueIndexKey);
                    beamblet = beams.get(b);
                    totalBeamblets = 0;
                    beamIndex = 0;
                    diffBeamblets=0;
                    for(int z=0; z<bmlts.length;z++) {
                        totalBeamblets+= bmlts[z];
                        if(beamblet < totalBeamblets){
                            beamIndex = z;
                            break;
                        }
                        diffBeamblets+=bmlts[z];
                    }
                    for(int a = 0; a < aperture; a++) {
                        coefficent = (double)sol.aperturesBmlts.get(beamIndex)[a][beamblet-diffBeamblets]*radiation;
                        // coefficent = (double)aper[beamIndex][a][(beamblet-diffBeamblets)]*radiation;
                        if(coefficent!=0 && o==0) {

                            voxelRadiation.addTerm(coefficent,intensity[beamIndex][a] );
                            // System.out.println(coefficent);
                        }
                        else {
                            voxelRadiation.addTerm(coefficent,intensity[beamIndex][a] );
                        }
                    }
                }

                if ( o==0 ){
                    // voxelRadiation.addConstant(eud[o]);
                    model.addConstr(voxelRadiation, GRB.GREATER_EQUAL,eud[o] , "gEUD"+o+"["+(count_voxel+1)+"]");
                }
                else{
                    int constEud=eud[o]*-1;
                    voxelRadiation.addConstant(constEud);
                    GRBLinExpr V=new GRBLinExpr();
                    V.addTerm(1,voxel[o-1][count_voxel]);
                    model.addConstr(V, GRB.GREATER_EQUAL, voxelRadiation, "voxelRadiation"+o+"["+(count_voxel+1)+"]");
                }

                count_voxel++;
            }
        }

        //set model
        GRBQuadExpr objFunc= new GRBQuadExpr();
        for(int o = 1;  o< organs; o++) {
            double coef=(double)((weight.get(o)/R[o]));

            for (int j = 0; j < R[o]; ++j) {

                objFunc.addTerm(coef, voxel[o-1][j],voxel[o-1][j]);
                System.out.print("");
            }
        }

        model.setObjective(objFunc, GRB.MINIMIZE);
        model.update();
        model.optimize();
        model.update();
        // writeModel();
        // model.computeIIS();
        // model.write("mod.ilp");
        double[][]getIntensity=new double[this.beams][this.aperture];
        for (int i = 0; i < this.beams; ++i) {
            for (int j = 0; j < this.aperture; ++j) {
                getIntensity[i][j]=intensity[i][j].get(GRB.DoubleAttr.X);
                // String varName="intensity"+i+"."+j;
                // getIntensity[i][j]=model.getVarByName(varName).get(GRB.DoubleAttr.X);
                // intensity[i][j] = model.addVar(minIntensity,maxIntensity,0.0, GRB.CONTINUOUS ,"Intensity" + i + "." + j);
            }
        }
        //GRBVar[] vars = model.getVars();
        newIntensity=getIntensity;
        objVal=model.get(GRB.DoubleAttr.ObjVal);

    }

    public void writeModel() throws GRBException {
        model.write("out1.lp");
        // model.write("out1.mst");
        // model.write("out1.mps");
        // model.write("out1.sol");
    }

}