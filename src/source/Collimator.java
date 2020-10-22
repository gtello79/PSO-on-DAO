
package source;

import javafx.util.Pair;
import java.io.FileNotFoundException;
import java.util.*;
import java.io.File;

public class Collimator {
    private int nbBeamlets;
    private int xDim;
    private int yDim;
    private int gDim;
    private int nAngles;
    //------------------------------------------------- NUEVAS ESTRUCTURAS -----------------------------------------------------------------------
    //Vector con los pares <angle, FilePath> de cada BeamAngle
    private Vector< Pair< Integer, String> > coord_file;
    //Contiene todos los angulos
    private Vector<Integer> angles;

    //Contiene la misma estructura de angleCoord, pero ahora los pasa indices matriciales
    private SortedMap< Integer, Vector< Pair< Integer, Integer> > > angleCoord;

    //Map Global ID Beamlet - <ID beam, ID Local Beamlet >
    private SortedMap<Integer, Pair<Integer, Integer>> allBeamletIndex;

    //Coordenadas originales (centradas en el tumor o cartesianas)
    private SortedMap< Integer, Vector< Pair< Double, Double> > > angleCoordMatr;

    // Range (i,j) of active beamlets of angle "a" row "r":
    //  Usando angleRowActive.get(a).get(r) obtengo el rango de apertura de la fila r en el angulo a
    //  (-1,-1) indicates a full row is not active
    private SortedMap< Integer, Vector< Pair< Integer, Integer> > > angleRowActive = new TreeMap<>();

    //Obtengo la cantidad de Beamlets por cada beam
    private SortedMap< Integer, Integer> nbAngleBeamlets =  new TreeMap<>();

    //------------------------------------------------- ESTRUCTURA ANTERIORES --------------------------------------------------------------------
    //Usa un vector con todos los valores de X desde los archivos de CoordinatesBeam
    private Vector< Double> xCoord = new Vector<>();

    //Usa un vector con todos los valores de Y desde los archivos de CoordinatesBeam
    private Vector< Double> yCoord = new Vector<>();

    //Agrega el valor de X con sus valores en Y
    private SortedMap< Double , Vector<Double> > beamCoord = new TreeMap<>();

    /*---------------------------------------------------- ALL METHODS ------------------------------------------------------------------------------------ */

    //Constructor del Collimator, esta en orden, no tocar
    public Collimator(String coord_filename, Vector<Integer> angles) throws FileNotFoundException {
        int angle;
        String aux;
        File archivo = new File(coord_filename);
        String delimiter = ";";

        this.coord_file = new Vector<>();
        this.angles = new Vector<>();
        this.angleCoord = new TreeMap<>();
        this.allBeamletIndex = new TreeMap<>();
        this.angleCoordMatr = new TreeMap<>();

        //Lectura del archivo test_instance_coords
        if(!archivo.exists()) {
            System.out.println("ERROR: NO SE ENCUENTRA EL ARCHIVO "+coord_filename);
        }else{
            System.out.println("##READING COLLIMATOR COORDINATES INFO ");
            Scanner reading = new Scanner(archivo);

            while( reading.hasNextLine() ) {
                String actual = reading.nextLine();
                if (actual.isEmpty())
                    continue;
                String[] actualArray = actual.split(delimiter);
                angle = Integer.parseInt(actualArray[0]);

                aux = actualArray[1];
                if (!angles.contains(angle))
                    continue;

                // <angle, path_of_coordinateBeam_angle>
                Pair<Integer, String> to_add = new Pair(angle, aux);
                coord_file.add(to_add);
            }
            reading.close();
            this.nAngles = coord_file.size();
            initializeCoordinates2();
            System.out.println("##  READ " + coord_file.size() + " FILES");
        }
    }

    //Se inicializan las coordenadas de cada beam del collimator
    private void initializeCoordinates2() throws  FileNotFoundException{
        double max = -99999;
        int globalID = 0;
        int localID;
        double x;
        double y;

        //Se lee la estructura con ID_Beam - Path Coordinates Beamlet
        for(Pair<Integer,String> temp: coord_file){
            int angle = temp.getKey();
            this.angles.add(angle);
            String CoordinatePath = temp.getValue();
            File coordFile = new File("src/"+CoordinatePath);

            //Se procede a leer el archivo temp
            if(!coordFile.exists()){
                System.out.println("NO SE ENCUENTRA EL ARCHIVO "+CoordinatePath);
            }else{
                //Contiene la posicion (original) de todos los beamlets de un beam
                Vector<Pair<Double,Double>> AngleBeamlet = new Vector<>();
                Scanner lect = new Scanner(coordFile);
                Set<Double> filter = new HashSet<>();

                //Se recorren todos los beamlets de 1 beam contenidos en el archivo
                while(lect.hasNextLine()) {
                    String lineaActual = lect.nextLine();
                    String[] arrayLinea = lineaActual.split("\t");

                    localID =  Integer.parseInt(arrayLinea[0]);
                    //Coordenadas originales a nivel cartesiano
                    x = Double.parseDouble(arrayLinea[1]);
                    y = Double.parseDouble(arrayLinea[2]);
                    filter.add(x);
                    filter.add(y);

                    this.allBeamletIndex.put(globalID, new Pair(angle, localID));
                    AngleBeamlet.add(new Pair(x, y));
                    globalID++;
                }
                //Se busca actualizar el mayor elemento de los beamlets para tener referencia de la dimension
                for(Double indexRow : filter){
                    if(Math.abs(indexRow) > max){
                        max = Math.abs(indexRow);
                    }
                }
                lect.close();
                this.angleCoordMatr.put(angle,AngleBeamlet);
            }
        }
        //Se asegura la dimension total del colimator
        gDim = (int)(max*2);
        this.xDim = gDim;
        this.yDim = gDim;
        this.nbBeamlets = globalID;

        //Se realiza la transformación de las coordenadas cartesianas a coordenadas matriciales
        for ( Integer idBeam : angleCoordMatr.keySet() ){
            int blperBeam = 0;
            Vector<Pair<Double,Double>> beamletsBeam = angleCoordMatr.get(idBeam);
            Vector<Pair<Integer,Integer>> newCoords = new Vector<>();

            for( Pair<Double,Double> row : beamletsBeam ) {
                blperBeam++;
                int newX = (int)(row.getKey() + max);
                int newY = (int)(row.getValue() + max);
                newCoords.add(new Pair(newX,newY));
            }
            this.nbAngleBeamlets.put(idBeam, blperBeam);
            this.angleCoord.put(idBeam, newCoords);
        }
        setActiveRows();
    }

    //Metodo que obtiene el rango de apertura activo del collimator, si el rango es <a,b>
    //Significa que el rnago esta abierto desde a hasta b (resalto que lo incluye)
    void setActiveRows() {
        for (Integer idBeam : angleCoord.keySet())
        {
            Vector<Pair<Integer, Integer>> beamletsBeam = angleCoord.get(idBeam);
            Vector<Pair<Integer, Integer>> activeRange = new Vector<>();

            //A partir de la fila r, obtengo todos los valores para las filas
            for (int r = 0; r < gDim; r++) {
                Set<Integer> filter = new HashSet<>();

                for (Pair<Integer, Integer> beamRow : beamletsBeam) {
                    if ( Integer.compare(r , beamRow.getKey()) == 0 ) {
                        filter.add(beamRow.getValue());
                    }
                }
                if(filter.size() == 0){
                    activeRange.add(new Pair(-1,-1));
                }else{
                    Vector<Integer> orderItems = new Vector<>();
                    for (Integer index: filter){
                        orderItems.add(index);
                    }
                    Collections.sort(orderItems);
                    int first = orderItems.get(0);
                    int last = orderItems.get(orderItems.size()-1);
                    activeRange.add(new Pair(first,last));
                }
            }
            angleRowActive.put(idBeam,activeRange);
        }
    }

    //Transformar la identificacion local del beam, a una identificacion global
    public Pair<Integer,Integer> indexToPos(int index,int angle){
        int x = (angleCoord.get(angle)).get(index).getKey();
        int y= (angleCoord.get(angle)).get(index).getValue();
        Pair <Integer,Integer> r = new Pair(x,y);
        return r;
    }

    //Obtengo el rango activo de la fila x en el angulo 'angle'
    public Pair<Integer,Integer> getActiveRange(int x, int angle){
        //x es la fila del collimator
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

    public Vector<Integer> getAngles() {
        return angles;
    }

    /*--------------------------------------------------- PRINT ALL COORDINATES ------------------------------------------------------------*/
    public void printActiveRange(){
        int angle = 0;
        System.out.println("Angle: "+ angle);
        for(int i = 0; i < gDim ; i++){
            System.out.println(getActiveRange(i,angle));
        }
    }
}
