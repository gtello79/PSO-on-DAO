package SRCDAO;


import source.Collimator;
import source.Matrix;
import source.Pair;

import java.util.Vector;

public class Aperture {

    private Collimator collimator;
    private int angle;
    private double intensity;
    private Vector<Pair<Integer,Integer>> A;

    private Vector<Pair<Integer,Integer>> velocityA;
    private double veloc_intensity;

    protected int OPEN_MIN_SETUP = 0;
    protected int OPEN_MAX_SETUP = 1;
    protected int CLOSED_MIN_SETUP = 2 ;
    protected int CLOSED_MAX_SETUP = 3;
    protected int RAND_RAND_SETUP = 4;

    /*--------------------------------------------------METHODS----------------------------------------------------------------------------*/
    public Aperture(Collimator collimator, int angle){
        setAngle(angle);
        setIntensity(0.0);
        setVeloc_intensity(1.0);
        this.collimator = collimator;
        this.velocityA = new Vector<>();
        this.A = new Vector<>();

        for(int i = 0; i < collimator.getxDim(); i++)
            velocityA.add(new Pair(0,0));
    }

    public Aperture(Aperture a){
        setAngle(a.angle);
        setIntensity(a.intensity);
        setVeloc_intensity(a.veloc_intensity);
        setVelocityA(a.velocityA);
        setApertures(a.A);

    }

    public void initializeAperture(int type, int open_apertures){
        Vector<Pair<Integer,Integer>> aux = new Vector<>();

        for(int i = 0; i < collimator.getxDim(); i++){

            if(collimator.getActiveRange(i,angle).getFirst() < 0){ //<-1,-1>
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
                    aux.add(new Pair(fLeaf,fLeaf+1)); //<5,6>

                }else if(type == RAND_RAND_SETUP){
                    ///Se abre aleatoriamente las hojas
                    int middle = Math.floorDiv((fLeaf+sLeaf),2);
                    int index1 = fLeaf + (int)(Math.random()*(middle - fLeaf));

                    if( index1 == sLeaf ){
                        aux.add(new Pair(fLeaf, sLeaf));
                    }else{
                        int index2 = (middle) + (int)(Math.random()*(sLeaf-middle));
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
    public void velAperture(double wAperture, double c1Aperture, double c2Aperture, Aperture BGlobal, Aperture BPersonal){
        double r1 = Math.random();
        double r2 = Math.random();

        Vector<Pair<Integer,Integer>> BG = BGlobal.getApertures();
        Vector<Pair<Integer,Integer>> BP = BPersonal.getApertures();
        for(int i = 0; i < collimator.getxDim(); i++){
            Pair<Integer,Integer> aux_G = BG.get(i);
            Pair<Integer,Integer> aux_P = BP.get(i);

            if( collimator.getActiveRange(i,angle).getFirst() < 0 ) continue;

            Integer first = (int)(wAperture*velocityA.get(i).getFirst() + c1Aperture*r1*( aux_G.getFirst() - A.get(i).getFirst() )  +  c2Aperture*r2*(aux_P.getFirst() - A.get(i).getFirst() ) );
            Integer second = (int)(wAperture*velocityA.get(i).getSecond() + c1Aperture*r1*( aux_G.getSecond() - A.get(i).getSecond() )  +  c2Aperture*r2*(aux_P.getSecond() - A.get(i).getSecond() ) );

            //if(first < 0) first = -1.0;
            //if(first > 0) first = 1.0;

            //if(second < 0) second = -1.0;
            //if(second > 0) second = 1.0;

            velocityA.set(i, new Pair(first, second));
        }
    }

    public void velIntensity(double wIntensity, double c1Intensity, double c2Intensity, Aperture BGlobal, Aperture BPersonal){
        double bG = BGlobal.getIntensity();
        double bP = BPersonal.getIntensity();
        double r1 = Math.random();
        double r2 = Math.random();

        double val = wIntensity*veloc_intensity + r1*c1Intensity*(bG - intensity) + r2*c2Intensity*(bP - intensity);

        //if(val < 0) val = -1.0;
        //if(val > 0) val = 1.0;

        setVeloc_intensity(val);
    }

    public void movAperture(){

        for(int i = 0; i < A.size(); i++){

            Pair<Integer, Integer> auxVelocity = new Pair(velocityA.get(i).getFirst(), velocityA.get(i).getSecond() );
            if(A.get(i).getFirst() < -1)
                continue;

            int limit_inf = collimator.getActiveRange(i,angle).getFirst();
            int limit_sup = collimator.getActiveRange(i,angle).getSecond();

            int first = (velocityA.get(i).getFirst() + A.get(i).getFirst());
            int second = (velocityA.get(i).getSecond() + A.get(i).getSecond());

            if(first < limit_inf  || first > limit_sup){
                first = limit_inf-1;
                auxVelocity.setFirst(0);
            }

            if(second < limit_inf  || second > limit_sup ){
                second = limit_sup+1;
                auxVelocity.setSecond(0);
            }

            if(first > second) {
                int val = (first + second)/2;
                first = (val);
                second = (val)+1;
                auxVelocity.setFirst(0);
                auxVelocity.setSecond(0);
            }

            Pair<Integer, Integer> newApertures = new Pair(first, second);
            A.set(i, newApertures);
            //this.velocityA.set(i, auxVelocity);
        }
    }

    public void moveIntensity(double max_intensity){
        double val = getIntensity() + getVeloc_intensity();

        if(val > max_intensity){
            val = max_intensity;
            //veloc_intensity = 0;
        }else if(val < 0 ) {
            val = 0;
            //veloc_intensity = 0;
        }

        double newIntensity = ((double)Math.round(val*1000)/1000);

        setIntensity(newIntensity);

    }

    /*---------------------------------------------------GETTERS AND SETTERS------------------------------------------------------------*/

    public Pair<Integer,Integer> getOpBeam(int i){
        return A.get(i);
    };

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public double getIntensity() {
        return intensity;
    }

    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }

    public double getVeloc_intensity() {
        return veloc_intensity;
    }

    public void setVeloc_intensity(double veloc_intensity) {
        this.veloc_intensity = veloc_intensity;
    }

    public void setApertures(Vector<Pair<Integer,Integer>> Aperture){
        Vector<Pair<Integer,Integer>> newAper = new Vector<>();

        for(Pair<Integer,Integer> x: Aperture){
            Pair<Integer, Integer> newShape = new Pair(x.getFirst(), x.getSecond());
            newAper.add(newShape);
        }
        this.A = newAper;
    }

    public void setVelocityA(Vector<Pair<Integer,Integer>> velocAperture){
        Vector<Pair<Integer,Integer>> newVelocity = new Vector<>();

        for(Pair<Integer,Integer> x: velocAperture){
            Pair<Integer,Integer> newShape = new Pair(x.getFirst(), x.getSecond());
            newVelocity.add(newShape);
        }
        this.velocityA = newVelocity;
    }

    public Vector<Pair<Integer,Integer>> getApertures(){
        return A;
    }

}
