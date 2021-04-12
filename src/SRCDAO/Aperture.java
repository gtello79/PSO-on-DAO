package SRCDAO;


import source.Collimator;
import source.Pair;

import java.util.Vector;

public class Aperture {
    private Collimator collimator;
    private int angle;
    private int intensity;
    private int veloc_intensity;
    private Vector<Pair<Integer,Integer>> A;
    private Vector<Pair<Double,Double>> velo_A;

    protected int OPEN_MIN_SETUP = 0;
    protected int OPEN_MAX_SETUP = 1;
    protected int CLOSED_MIN_SETUP = 2 ;
    protected int CLOSED_MAX_SETUP = 3;
    protected int RAND_RAND_SETUP = 4;

    /*--------------------------------------------------METHODS----------------------------------------------------------------------------*/
    public Aperture(Collimator collimator, int angle){
        setAngle(angle);
        setIntensity(0);
        setVeloc_intensity(1);
        this.collimator = collimator;
        this.velo_A = new Vector<>();
        this.A = new Vector<>();

        for(int i = 0; i < collimator.getxDim(); i++)
            velo_A.add(new Pair(1.0,1.0));
    }

    public Aperture(Aperture a){
        setAngle(a.angle);
        setIntensity(a.intensity);
        setVeloc_intensity(a.veloc_intensity);
        setVelo_A(a.velo_A);
        setApertures(a.A);

    }

    public void initializeAperture(int type, int open_apertures){
        Vector<Pair<Integer,Integer>> aux = new Vector<>();

        for(int i = 0; i < collimator.getxDim(); i++){
            if(collimator.getActiveRange(i,angle).getFirst() < 0){
                //Cerrada completamente por inactividad
                aux.add(new Pair(-2, -2));
            }
            else{
                //Todos los beamlets de la fila i en el angulo 'angle'
                int fLeaf = collimator.getActiveRange(i,angle).getFirst() - 1;
                int sLeaf = collimator.getActiveRange(i,angle).getSecond() + 1;

                if(type == OPEN_MAX_SETUP || type == OPEN_MIN_SETUP){
                    //Abierta completamente para los beamlets activos
                    aux.add(new Pair(fLeaf,sLeaf));

                }else if(type == CLOSED_MAX_SETUP || type == CLOSED_MIN_SETUP) {
                    //Cerrada completamente (MEJORAR)
                    aux.add(new Pair(fLeaf,fLeaf+1));

                }else if(type == RAND_RAND_SETUP){
                    ///Se abre aleatoriamente las hojas
                    int index1 = fLeaf + (int)(Math.random()*(sLeaf - fLeaf));

                    if( index1 == sLeaf ){
                        aux.add(new Pair(fLeaf, sLeaf));
                    }else{
                        int index2 = (index1+1) + (int)(Math.random()*(sLeaf-index1));
                        aux.add(new Pair(index1,index2));
                    }
                }else{
                    if(open_apertures > 0){
                        aux.add(new Pair(fLeaf,sLeaf));
                    }else{
                        //Cerrada completamente por inactividad
                        aux.add(new Pair(-2,-2));
                    }
                }
            }
        }
        setApertures(aux);
    }

    public void initializeIntensity(int type, int min_intensity, int max_intensity, int initial_intensity, int r_intensity){
        if( type==OPEN_MIN_SETUP || type==CLOSED_MIN_SETUP ){
            setIntensity(min_intensity);
        }else if( type==OPEN_MAX_SETUP || type==CLOSED_MAX_SETUP ){
            setIntensity(max_intensity);
        }else if( type == RAND_RAND_SETUP){
            setIntensity(r_intensity);
        }else{
            setIntensity(initial_intensity);
        }
    }

    /*----------------------------------------------------PSO METHODS--------------------------------------------------------------------------------*/
    public void velAperture(double w, double c1, double c2, Aperture BGlobal, Aperture BPersonal){
        double r1 = Math.random();
        double r2 = Math.random();

        Vector<Pair<Integer,Integer>> BG = BGlobal.getApertures();
        Vector<Pair<Integer,Integer>> BP = BPersonal.getApertures();
        for(int i = 0; i < collimator.getxDim(); i++){
            Pair<Integer,Integer> aux_G = BG.get(i);
            Pair<Integer,Integer> aux_P = BP.get(i);

            if(collimator.getActiveRange(i,angle).getFirst() < 0) continue;


            Double first = (w*velo_A.get(i).getFirst() + c1*r1*( aux_G.getFirst() - A.get(i).getFirst() )  +  c2*r2*(aux_P.getFirst() - A.get(i).getFirst() ) );
            Double second = (w*velo_A.get(i).getSecond() + c1*r1*( aux_G.getSecond() - A.get(i).getSecond() )  +  c2*r2*(aux_P.getSecond() - A.get(i).getSecond() ) );

            velo_A.set(i, new Pair(first, second));
        }

    }

    public void velIntensity(double w, double c1, double c2, Aperture BGlobal, Aperture BPersonal){
        int bG = BGlobal.getIntensity();
        int bP = BPersonal.getIntensity();
        double r1 = Math.random();
        double r2 = Math.random();

        int val = (int)(w*veloc_intensity + r1*c1*(bG - intensity) + r2*c2*(bP - intensity));
        setVeloc_intensity(val);
    }

    public void movAperture(){
        for(int i = 0; i < A.size(); i++){
            if(A.get(i).getFirst() < -1)
                continue;

            int limit_inf = collimator.getActiveRange(i,angle).getFirst();
            int limit_sup = collimator.getActiveRange(i,angle).getSecond();


            int first = (int)(velo_A.get(i).getFirst() + A.get(i).getFirst());
            int second = (int)(velo_A.get(i).getSecond() + A.get(i).getSecond());

            //System.out.println("DESPUES: "+first + " " + second);

            if(first < limit_inf  || first > limit_sup) first = limit_inf;

            if(second < limit_inf  || second > limit_sup ) second = limit_sup;

            if(first > second) {
                int val = (first + second)/2;
                first = (val);
                second = (val) + 1;
            }

            Pair<Integer, Integer> newApertures = new Pair(first, second);
            A.set(i, newApertures);

        }
    }

    public void moveIntensity(int max_intensity){
        //System.out.println("PREVIO: "+getIntensity());

        int val = getIntensity() + getVeloc_intensity();

        if(val > max_intensity) val = max_intensity;

        if(val < 0 ) val = 0;

        setIntensity(val);

        //System.out.println("Final: "+val);
    }

    /*---------------------------------------------------GETTERS AND SETTERS------------------------------------------------------------*/

    public Pair<Integer,Integer> getOpBeam(int i){
        return A.get(i);
    };

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public int getVeloc_intensity() {
        return veloc_intensity;
    }

    public void setVeloc_intensity(int veloc_intensity) {
        this.veloc_intensity = veloc_intensity;
    }

    public void setApertures(Vector<Pair<Integer,Integer>> Aper){
        Vector<Pair<Integer,Integer>> newAper = new Vector<>();

        for(Pair<Integer,Integer> x: Aper){
            Pair<Integer, Integer> newShape = new Pair(x.getFirst(), x.getSecond());
            newAper.add(newShape);
        }
        this.A = newAper;
    }

    public void setVelo_A(Vector<Pair<Double,Double>> velocAperture){
        Vector<Pair<Double,Double>> newVelocity = new Vector<>();

        for(Pair<Double,Double> x: velocAperture){
            Pair<Double,Double> newShape = new Pair(x.getFirst(), x.getSecond());
            newVelocity.add(newShape);
        }
        this.velo_A = newVelocity;
    }

    public Vector<Pair<Integer,Integer>> getApertures(){
        return A;
    }

    /*------------------------------------------PRINTERS -----------------------------------------------------------------*/
    public void printAperture(){
        System.out.println("Intensity: "+ intensity);
        int x = 0;
        for(Pair<Integer,Integer> aperture : A){
            System.out.println(x + " (" +aperture.getFirst() + " , " + aperture.getSecond() + ") ");
            x++;
        }
    }


}
