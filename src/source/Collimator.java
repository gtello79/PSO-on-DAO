package source;

import javafx.util.Pair;
import java.io.FileNotFoundException;
import java.util.*;
import java.io.File;

public class Collimator {
    private int nbBeamlets;
    private int xDim;
    private int yDim;
    private int nAngles;
    private ArrayList<Integer> angles = new ArrayList<>();
    private Vector< Double> xCoord = new Vector<>();
    private Vector< Double> yCoord = new Vector<>();
    private Vector< Pair< Integer, String> > coord_file = new Vector<Pair<Integer, String>>();
    private SortedMap< Integer, Integer> nbAngleBeamlets =  new TreeMap<>();
    private SortedMap< Double , Vector< Double> > beamCoord = new TreeMap<>();
    private SortedMap< Integer, Vector< Pair< Double, Double> > > angleCoord = new TreeMap<>();

    // Range (i,j) of active beamlets of angle "a" row "r":
    //  angle_row_beam_active[a][r](i,j)
    //  (-1,-1) indicates a full row is not active
    private SortedMap< Integer, Vector< Pair< Integer, Integer> > > angleRowActive = new TreeMap<>();

    public Collimator(String coord_filename, Vector<Integer> angles) throws FileNotFoundException {
        File archivo = new File(coord_filename);
        int angle;
        String delimiter = ";";
        String aux;
        if(!archivo.exists()) {
            System.out.println("ERROR: NO SE ENCUENTRA EL ARCHIVO "+coord_filename);
        }else{
            System.out.println("##READING COLLIMATOR COORDINATES INFO ");
            Scanner reading = new Scanner(archivo);
            while(reading.hasNextLine()){
                String actual = reading.nextLine();
                if(actual.isEmpty()) continue;
                String [] actualArray = actual.split(delimiter);
                angle = Integer.parseInt(actualArray[0]);
                aux = actualArray[1];
                if(!angles.contains(angle)) continue;
                Pair <Integer, String> to_add = new Pair<Integer,String>(angle,aux);
                coord_file.add(to_add);
            }
            reading.close();
            this.nAngles = coord_file.size();
            initializeCoordinates();
            System.out.println("##  READ " + coord_file.size() + " FILES");
        }
    }

    public Collimator(Collimator collimator){
        this.beamCoord = collimator.getBeamCoord();
        this.angleCoord = collimator.getAngleCoord();
        this.xCoord = collimator.getxCoord();
        this.yCoord = collimator.getyCoord();
        this.nbBeamlets = collimator.getNbBeamlets();
        this.xDim = collimator.getxDim();
        this.yDim = collimator.getyDim();
        this.angleRowActive = collimator.getAngleRowActive();
        this.nbAngleBeamlets = collimator.getNbAngleBeamlets();
        this.angles = collimator.getAngles();
        this.coord_file = collimator.getCoord_file();
    }

    private void initializeCoordinates() throws FileNotFoundException {
        double x;
        double y;
        boolean flag;

        for(Pair<Integer, String> temp : coord_file ){
            int angle = temp.getKey();
            angles.add(angle);
            String dir_beam = temp.getValue();
            File coord = new File("src/"+dir_beam);
            if(!coord.exists()){
                System.out.println("NO SE ENCUENTRA EL ARCHIVO "+dir_beam);
            }else{
                Scanner lectura = new Scanner(coord);
                Vector<Pair< Double, Double>> toAdd = new Vector<Pair< Double, Double>>();
                while(lectura.hasNextLine()) {
                    flag = false;
                    String linea_actual = lectura.nextLine();
                    String [] ArrayLinea = linea_actual.split("\t");
                    x = Double.parseDouble(ArrayLinea[1]);
                    y = Double.parseDouble(ArrayLinea[2]);
                    toAdd.add(new Pair(x,y));

                    angleCoord.put(angle,toAdd);
                    if(!beamCoord.containsKey(x)){
                        //New x coordinate
                        Vector<Double> local = new Vector<Double>();
                        local.add(y);
                        beamCoord.put(x,local);
                        insertXorder(x);
                        insertYorder(y);
                    }else{
                        if((beamCoord.get(x)).contains(y))
                        flag=true;
                        //New Y coordinate: insert in order
                        for(int j = 0; j < beamCoord.get(x).size() && !flag; j++){
                            if((beamCoord.get(x)).get(j) > y){
                                beamCoord.get(x).add(j,y);
                                flag=true;
                            }
                        }
                        if(!flag)
                            beamCoord.get(x).add(y);
                        insertYorder(y);
                    }
                }
                toAdd = null;
                lectura.close();
            }
            nbAngleBeamlets.put(angle, angleCoord.get(angle).size());
        }
        xDim = xCoord.size();
        yDim = yCoord.size();
        nbBeamlets = xDim*yDim;
        setActiveBeamlets(angleCoord);

    }

    // Insert a new X coordinate in the xcoord vector
    private void insertXorder(double x) {
        Boolean flag = false;
        if(xCoord.contains(x))
            flag=true;
        for(int i = 0; i < xCoord.size() && !flag; i++){
            if(xCoord.get(i) > x){
                xCoord.add(i,x);
                flag=true;
            }
        }
        if(!flag)
            xCoord.add(x);
    }

    // Insert a new Y coordinate in the ycoord vector
    private void insertYorder(double y) {
        Boolean flag = false;
        if(yCoord.contains(y))
            flag=true;
        for(int i = 0; i < yCoord.size() && !flag; i++){
            if(yCoord.get(i) > y){
                yCoord.add(i,y);
                flag = true;
            }
        }
        if(!flag)
            yCoord.add(y);
    }

    void setActiveBeamlets(SortedMap<Integer, Vector<Pair<Double,Double>>> coord){
        double nmax, nmin;
        int selmax = 0, selmin = 0;
        boolean flag;

        for(Map.Entry<Integer,Vector<Pair<Double,Double>>> angle : coord.entrySet()){
            int i = angle.getKey();
            Vector<Pair<Integer,Integer>> to_add = new Vector<Pair<Integer,Integer>>();
            for(int j = 0; j < xCoord.size(); j++){
                flag = false;
                nmax = -9999999;
                nmin = 9999999;
                for(int s = 0; s < (coord.get(i)).size(); s++){
                    if((angle.getValue()).get(s).getKey() == xCoord.get(j)){
                        if((angle.getValue()).get(s).getValue() < nmin)
                            nmin = (angle.getValue()).get(s).getValue();
                        if((angle.getValue()).get(s).getValue() > nmax)
                            nmax = (angle.getValue()).get(s).getValue();
                        flag = true;
                    }
                }
                if(flag){
                    for(int s = 0; s < yCoord.size(); s++){
                        if(yCoord.get(s) == nmin) selmin = s;

                        if(yCoord.get(s) == nmax) selmax = s;
                    }
                }else {
                    selmin = -1;
                    selmax = -1;
                }
                to_add.add(new Pair(selmin,selmax));
            }
            angleRowActive.put(i,to_add);
        }

    }

    public Pair<Integer,Integer> indexToPos(int index,int angle){
        double x = (angleCoord.get(angle)).get(index).getKey();
        double y= (angleCoord.get(angle)).get(index).getValue();
        int posx = (int)(x - xCoord.firstElement());
        int posy = (int)(y - yCoord.firstElement());
        //System.out.println(x + " -- " + posx);
        Pair <Integer,Integer> r = new Pair(posx,posy);
        return r;
    }

    public boolean isActiveBeamAngle(int x, int y, int angle){
        return angleRowActive.get(angle).get(x).getKey() <= y && angleRowActive.get(angle).get(x).getValue() >= y;
    }

    public Pair<Integer,Integer> getActiveRange(int x, int angle){
        return (angleRowActive.get(angle).get(x));
    }

    public int getxDim() {
        return xDim;
    }

    public int getNbAngles(){
        return nAngles;
    }

    public int getAngle(int i) {
        return angles.get(i);
    }

    public int getNbBeamlets() {
        return nbBeamlets;
    }

    public int getNangleBeamlets(int angle) {
        return nbAngleBeamlets.get(angle);
    }

    public int getyDim() {
        return yDim;
    }

    public ArrayList<Integer> getAngles() {
        return angles;
    }

    public Vector<Double> getxCoord() {
        return xCoord;
    }

    public Vector<Double> getyCoord() {
        return yCoord;
    }

    public Vector<Pair<Integer, String>> getCoord_file() {
        return coord_file;
    }

    public SortedMap<Double, Vector<Double>> getBeamCoord() {
        return beamCoord;
    }

    public SortedMap<Integer, Vector<Pair<Double, Double>>> getAngleCoord() {
        return angleCoord;
    }

    public SortedMap<Integer, Vector<Pair<Integer, Integer>>> getAngleRowActive() {
        return angleRowActive;
    }

    public SortedMap<Integer, Integer> getNbAngleBeamlets() {
        return nbAngleBeamlets;
    }

}
