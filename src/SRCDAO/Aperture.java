package SRCDAO;

import javafx.util.Pair;
import source.Collimator;

import java.util.Random;
import java.util.Vector;

public class Aperture {
    private Collimator collimator;
    private int angle;
    private int intensity;
    private int veloc_intensity;
    private Vector<Pair<Integer,Integer>> A;
    private Vector<Pair<Integer,Integer>> velo_A;

    protected int OPEN_MIN_SETUP = 0;
    protected int OPEN_MAX_SETUP = 1;
    protected int CLOSED_MIN_SETUP = 2 ;
    protected int CLOSED_MAX_SETUP = 3;
    protected int RAND_RAND_SETUP = 4;

    /*--------------------------------------------------METHODS----------------------------------------------------------------------------*/
    public Aperture(Collimator collimator, int angle){
        setAngle(angle);
        setIntensity(0);
        setVeloc_intensity(0);
        this.collimator = collimator;
        this.velo_A = new Vector<>();
        this.A = new Vector<>();

        for(int i = 0; i < collimator.getxDim(); i++)
            velo_A.add(new Pair(0,0));
    }

    public void initializeAperture(int type, int open_apertures){
        Vector<Pair<Integer,Integer>> aux = new Vector<>();

        if(type == OPEN_MAX_SETUP || type == OPEN_MIN_SETUP){
            for(int i = 0; i < collimator.getxDim(); i++){
                int fLeaf = collimator.getActiveRange(i,angle).getKey()-1;
                int sLeaf = collimator.getActiveRange(i,angle).getValue()+1;
                if(collimator.getActiveRange(i,angle).getKey() < 0) {
                    //Cerrada completamente por inactividad
                    aux.add(new Pair(-2, -2));
                } else{
                    //Habilitada solo con los beamlet Activos
                    aux.add(new Pair(fLeaf,sLeaf));
                }
            }
        }else if (type == CLOSED_MAX_SETUP || type == CLOSED_MIN_SETUP){
            for(int i = 0; i < collimator.getxDim(); i++){
                //Cerrada completamente
                aux.add(new Pair(-2,-2));
            }
        }else if (type == RAND_RAND_SETUP){
            for(int i = 0; i < collimator.getxDim(); i++){
                int fLeaf = collimator.getActiveRange(i,angle).getKey()-1 ;
                int sLeaf = collimator.getActiveRange(i,angle).getValue()+1;
                if(collimator.getActiveRange(i,angle).getKey() < 0){
                    //Cerrada completamente por inactividad
                    aux.add(new Pair(-2,-2));
                }else{
                    Random r =  new Random(System.currentTimeMillis());
                    int index1 = fLeaf + r.nextInt( sLeaf - fLeaf);
                    if( index1 == sLeaf ){
                        aux.add(new Pair(fLeaf, sLeaf));
                    }else{
                        int index2 = index1 + r.nextInt(sLeaf-index1+1);
                        aux.add(new Pair(index1,index2));
                    }
                }
            }
        }else{
            for(int i = 0; i < collimator.getxDim(); i++){
                if(open_apertures > 0){
                    int fLeaf = collimator.getActiveRange(i,angle).getKey()-1;
                    int sLeaf = collimator.getActiveRange(i,angle).getValue()+1;
                    aux.add(new Pair(fLeaf,sLeaf));
                }else{
                    //Cerrada completamente por inactividad
                    aux.add(new Pair(-2,-2));
                }
            }
        }
        setApertures(aux);
    }

    public void initializeIntensity(int type, int min_intensity, int max_intensity, int initial_intensity, int r_intensity){
        if(type==OPEN_MIN_SETUP || type==CLOSED_MIN_SETUP){
            setIntensity(min_intensity);
        }else if(type==OPEN_MAX_SETUP || type==CLOSED_MAX_SETUP){
            setIntensity(max_intensity);
        }else if( type == RAND_RAND_SETUP){
            setIntensity(r_intensity);
        }else{
            setIntensity(initial_intensity);
        }
    }

    /*----------------------------------------------------PSO METHODS--------------------------------------------------------------------------------*/
    public void velAperture(double w, double c1, double c2, Aperture BGlobal, Aperture BPersonal){
        Random r =  new Random(System.currentTimeMillis());
        int counter = 0;
        double r1 = r.nextDouble();
        double r2 = r.nextDouble();

        Vector<Pair<Integer,Integer>> BG = BGlobal.getApertures();
        Vector<Pair<Integer,Integer>> BP = BPersonal.getApertures();

        for(int i = 0; i < collimator.getxDim(); i++){
            Pair<Integer,Integer> aux_G = BG.get(i);
            Pair<Integer,Integer> aux_P = BP.get(i);
            if(collimator.getActiveRange(i,angle).getKey() < 0) continue;

            int first = (int)(w*velo_A.get(i).getKey() + c1*r1*(A.get(i).getKey()-aux_G.getKey())  +  c2*r2*(A.get(i).getKey()-aux_P.getKey()) );
            int second = (int)(w*velo_A.get(i).getValue() + c1*r1*(A.get(i).getValue()-aux_G.getValue())  +  c2*r2*(A.get(i).getValue()-aux_P.getValue()) );

            velo_A.set(i, new Pair(first,second));

        }
    }

    public void velIntensity(double w, double c1, double c2, Aperture BGlobal, Aperture BPersonal){
        Random r =  new Random(System.currentTimeMillis());
        int bG = BGlobal.getIntensity();
        int bP = BPersonal.getIntensity();
        double r1 = r.nextDouble();
        double r2 = r.nextDouble();

        int val = (int)(w*veloc_intensity + r1*c1*(intensity-bG) + r2*c2*(intensity-bP));
        setVeloc_intensity(val);
    }

    public void movAperture(){
        for(int i = 0; i < velo_A.size(); i++){
            if(A.get(i).getKey() < 0) continue;
            int limit_inf = collimator.getActiveRange(i,angle).getKey();
            int limit_sup = collimator.getActiveRange(i,angle).getValue();

            int first = velo_A.get(i).getKey() + A.get(i).getKey();
            int second = velo_A.get(i).getValue() + A.get(i).getValue();

            System.out.println(A.get(i).getKey() + " " + velo_A.get(i).getKey() + "  ---  " + A.get(i).getValue() + " " + velo_A.get(i).getValue());

            if(first > second) {
                int val = (first + second)/2;
                first = (val);
                second = (val) + 1;
            }


            if(first < limit_inf ) first = limit_inf;

            if(second > limit_sup) second = limit_sup;

            System.out.println(first + " " + second);

            A.set(i, new Pair(first,second));
        }
    }

    public void moveIntensity(int max_intensity){
        int val = getIntensity() + getVeloc_intensity();
        if(val > 0 && val < max_intensity) setIntensity(val);
    }

    /*---------------------------------------------------GETTERS AND SETTERS------------------------------------------------------------*/

    public Pair<Integer,Integer> getOpBeam(int i){
        return A.get(i);
    };

    public int getAngle() {
        return angle;
    }

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
        int i = 0;
        for(Pair<Integer,Integer> x: Aper){
            A.add(i,x);
            i++;
        }
    }

    public Vector<Pair<Integer,Integer>> getApertures(){
        return A;
    }


    /*------------------------------------------PRINTERS -----------------------------------------------------------------*/
    public void printAperture(){
        System.out.println("Intensity: "+ intensity);
        int x = 0;
        for(Pair<Integer,Integer> aperture : A){
            System.out.println(x + " (" +aperture.getKey() + " , " + aperture.getValue() + ") ");
            x++;
        }
    }


}
