package SRCDAO;
import java.util.*;

import source.Pair;
import source.*;

public class Beam {
    //ID BEAM
    private int angle;

    //Representacion del collimator
    private final Collimator collimator;

    //Representacion de la matriz de intensidad
    private final Matrix I;

    //Informacion de la configuracion tecnica del angulo
    private int maxApertures;
    private int maxIntensity;
    private int minIntensity;
    private int initialIntensity;
    private int stepIntensity;
    private int openApertures;
    private int setup;
    private int collimatorDim;
    private int totalBeamlets;

    /* Apertures (representation 1):
     * Each aperture is represented by a vector of pairs A[i] = (x_ini, x_fin)
     * and an intensity range open (x_ini+1, x_fin-1) of row "r" for aperture d: A[d][r](x_ini, x_fin)
    */
    private final Vector<Aperture> A;
    private Vector<Double> fluenceMap;


    /* ------------------------------------------- GENERAL METHODS ------------------------------------------------------- */
    public Beam(int angle, int maxApertures, int maxIntensity, int initialIntensity, int stepIntensity, int openApertures, int setup, Collimator collimator){
        setAngle(angle);
        setMax_apertures(maxApertures);
        setMax_intensity(maxIntensity);
        setInitialIntensity(initialIntensity);
        setStep_intensity(stepIntensity);
        setOpenApertures(openApertures);
        setSetup(setup);
        setMinIntensity(1);
        setCollimatorDim(collimator.getyDim());
        setTotalBeamlets(collimator.getNangleBeamlets(angle));

        this.collimator = collimator;
        this.A = new Vector<>();
        this.fluenceMap = new Vector<>();

        if(openApertures==-1)
            setOpenApertures(maxApertures);

        // Declaracion de la matriz de intensidad I y se inicializa
        this.I = new Matrix(collimator.getxDim(), collimator.getyDim());

        // Rellenado de la matriz de intensidad
        clearIntensity();

        // Initialize apertures
        initiliazeBeam(setup,openApertures);

        //Se construye la Matriz de Intensidad
        generateIntensities();

    }

    public Beam(Beam b){
        setAngle(b.angle);
        setMax_apertures(b.maxApertures);
        setMax_intensity(b.maxIntensity);
        setInitialIntensity(b.initialIntensity);
        setStep_intensity(b.stepIntensity);
        setOpenApertures(b.openApertures);
        setSetup(b.setup);
        setMinIntensity(b.minIntensity);
        setCollimatorDim(b.getCollimatorDim());
        setTotalBeamlets(b.getTotalBeamlets());

        this.collimator = new Collimator(b.collimator);
        this.A = new Vector<>();
        this.fluenceMap = new Vector<>();

        if(openApertures==-1)
            setOpenApertures(maxApertures);

        // Declaracion de la matriz de intensidad I y se inicializa
        I = new Matrix(collimator.getxDim(), collimator.getyDim());

        // Rellenado de la matriz de intensidad
        clearIntensity();

        for(Aperture a: b.A){
            Aperture copyAperture = new Aperture(a);
            this.A.add(copyAperture);
        }

        //Se construye la Matriz de Intensidad
        generateIntensities();

    }


    public void initiliazeBeam(int type, int openApertures){
        Vector<Integer> levels = new Vector<>();

        //Calculate levels for random Intensity
        int l = (maxIntensity-minIntensity)/stepIntensity ;
        for(int k = 0; k < maxApertures; k++){
            int i = minIntensity + stepIntensity *(int)(Math.random()*(l+1));
            levels.add(i);
        }

        //Inicializacion de cada apertura
        for(int i = 0; i < maxApertures; i++){
            Aperture aux = new Aperture(collimator, angle);
            aux.initializeAperture(type, openApertures);
            aux.initializeIntensity(type, minIntensity, maxIntensity, initialIntensity, levels.get(i));
            openApertures--;
            A.add(aux);
        }
    }


    public void generateIntensities(){
        Pair<Integer,Integer> aux;
        clearIntensity();

        for(Aperture ap : A) {
            double apIntensity = ap.getIntensity();
            for (int i = 0; i < collimator.getxDim(); i++) {
                aux = collimator.getActiveRange(i, angle);

                if (aux.getFirst() < 0 || ap.getOpBeam(i).getFirst() < -1)
                    continue;

                for (int j = ap.getOpBeam(i).getFirst()+1 ; j < ap.getOpBeam(i).getSecond(); j++) {
                    double newIntensity = (this.I.getPos(i, j) + apIntensity);
                    I.setPos(i,j, newIntensity);
                }
            }
        }

        buildIntensityVector();
    }

    public void buildIntensityVector(){
        this.fluenceMap = new Vector<>();
        for(int i = 0; i < collimator.getxDim(); i++){
            Pair<Integer,Integer> x = collimator.getActiveRange(i,angle);
            if(x.getFirst() < 0 )
                continue;
            for(int j = x.getFirst(); j <= x.getSecond(); j++){
                fluenceMap.add(I.getPos(i,j));
            }
        }
    }

    public void clearIntensity(){
        // Rellenado de la matriz de intensidad
        for(int i = 0; i < collimator.getxDim(); i++){
            for(int j = 0; j < collimator.getyDim(); j++){
                if(j >= collimator.getActiveRange(i,angle).getFirst() && j <= collimator.getActiveRange(i,angle).getSecond() ) {
                    this.I.setPos(i,j,0);
                }else{
                    this.I.setPos(i,j,-1);
                }
            }
        }
    }


    /* ------------------------------------------------ PSO METHODS ----------------------------------*/
    public void CalculateVelocity(double c1Aperture, double c2Aperture, double wAperture,double c1Intensity, double c2Intensity, double wIntensity, Beam BGlobal, Beam BPersonal){
        for(int i = 0; i < A.size() ; i++){
            Aperture x = A.get(i);
            Aperture bG = BGlobal.getAperture(i);
            Aperture bP = BPersonal.getAperture(i);
            x.velAperture(wAperture,c1Aperture,c2Aperture,bG,bP);
            x.velIntensity(wIntensity,c1Intensity,c2Intensity,bG,bP);
        }
    }

    public void CalculatePosition(){
        for(Aperture x: A){
            x.movAperture();
            x.moveIntensity(maxIntensity);
        }
        generateIntensities();
    }

    /* ------------------------------------------------- GETTER Y SETTERS ---------------------------------------------------*/

    public Vector<Double> getIntensityVector(){
        return fluenceMap;
    }

    public int getIdBeam() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public void setMax_apertures(int maxApertures) {
        this.maxApertures = maxApertures;
    }

    public void setMax_intensity(int max_intensity) {
        this.maxIntensity = max_intensity;
    }

    public void setMinIntensity(int minIntensity) {
        this.minIntensity = minIntensity;
    }

    public void setInitialIntensity(int initialIntensity) {
        this.initialIntensity = initialIntensity;
    }

    public void setStep_intensity(int step_intensity) {
        this.stepIntensity = step_intensity;
    }

    public void setOpenApertures(int openApertures) {
        this.openApertures = openApertures;
    }

    public void setSetup(int setup) {
        this.setup = setup;
    }

    public Matrix getIntensitisMatrix(){
        return I;
    }

    public Aperture getAperture(int id){
        return A.get(id);
    }

    public Vector<Aperture> getApertures(){
        return A;
    }

    public int getCollimatorDim() {
        return collimatorDim;
    }

    public void setCollimatorDim(int collimatorDim) {
        this.collimatorDim = collimatorDim;
    }

    public int getTotalBeamlets() {
        return totalBeamlets;
    }

    public void setTotalBeamlets(int totalBeamlets) {
        this.totalBeamlets = totalBeamlets;
    }


    /*-------------------------------------------------------- PRINTERS -------------------------------------- */

    public void printIntensityMatrix(){
        System.out.println(angle + ": ");
        for(int i = 0; i < collimator.getxDim(); i++){
            for(int j = 0; j < collimator.getyDim(); j++){
                System.out.print(I.getPos(i,j) + " ");
            }
            System.out.println();
        }
    }

    public void printFluenceMapOnBeam(){
        System.out.println("Id Beam: " + angle);
        for (Double i: fluenceMap){
            System.out.print(i + " ");
        }
        System.out.println();
    }
}
