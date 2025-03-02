
package source;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Collimator {

    public static String DELIMITER = ";";

    private int nbBeamlets;
    private int xDim;
    private int yDim;
    private int gDim;
    private int nAngles;

    // Vector con los pares <angle, FilePath> de cada BeamAngle
    private ArrayList<Pair<Integer, String>> coord_file;

    // Contiene todos los angulos
    private ArrayList<Integer> angles;

    // Contiene la misma estructura de angleCoord, pero ahora los pasa indices
    // matriciales
    private Hashtable<Integer, ArrayList<Pair<Integer, Integer>>> angleCoord;

    // Map Global ID Beamlet - <ID beam, ID Local Beamlet >
    private Hashtable<Integer, Pair<Integer, Integer>> allBeamletIndex;

    // Coordenadas originales (centradas en el tumor o cartesianas)
    private Hashtable<Integer, ArrayList<Pair<Double, Double>>> angleCoordMatr;

    // Range (i,j) of active beamlets of angle "a" row "r":
    // Usando angleRowActive.get(a).get(r) obtengo el rango de apertura de la fila r
    // en el angulo
    // (-1,-1) indica que esta inactiva, es decir, que no tiene beamlets
    private Hashtable<Integer, ArrayList<Pair<Integer, Integer>>> angleRowActive = new Hashtable<>();

    // Obtengo la cantidad de Beamlets por cada beam
    private Hashtable<Integer, Integer> nbAngleBeamlets = new Hashtable<>();

    /*---------------------- ALL METHODS ---------------------------------------------------------------- */
    // Constructor del Collimator, esta en orden, no tocar
    public Collimator(String coord_filename, Vector<Integer> angles) throws FileNotFoundException {

        File archivo = new File(coord_filename);

        this.coord_file = new ArrayList<>();
        this.angles = new ArrayList<>();
        this.angleCoord = new Hashtable<>();
        this.allBeamletIndex = new Hashtable<>();
        this.angleCoordMatr = new Hashtable<>();

        // Lectura del archivo test_instance_coords
        if (!archivo.exists()) {
            throw new FileNotFoundException("ERROR: NO SE ENCUENTRA EL ARCHIVO " + coord_filename);
        }
        ;

        System.out.println("## READING COLLIMATOR COORDINATES INFO ");
        Scanner reading = new Scanner(archivo);

        while (reading.hasNextLine()) {
            String actual = reading.nextLine();
            if (actual.isEmpty())
                continue;

            String[] actualArray = actual.split(DELIMITER);
            int angle = Integer.parseInt(actualArray[0]);
            String aux = actualArray[1];

            if (!angles.contains(angle))
                continue;

            // <angle, path_of_coordinateBeam_angle>
            Pair<Integer, String> to_add = new Pair<>(angle, aux);
            coord_file.add(to_add);
        }
        reading.close();

        this.nAngles = coord_file.size();

        initializeCoordinates();

        System.out.println("##  READ " + coord_file.size() + " FILES");

    }

    public Collimator(Collimator c) {

        this.coord_file = new ArrayList<>();
        this.angles = new ArrayList<>();
        this.angleCoord = new Hashtable<>();
        this.allBeamletIndex = new Hashtable<>();
        this.angleCoordMatr = new Hashtable<>();

        this.nbBeamlets = c.nbBeamlets;
        this.nAngles = c.nAngles;
        this.xDim = c.xDim;
        this.yDim = c.yDim;
        this.gDim = c.gDim;

        this.angles = new ArrayList<>(c.angles);
        this.coord_file = new ArrayList<>(c.coord_file);
        this.angleCoord.putAll(c.angleCoord);
        this.allBeamletIndex.putAll(c.allBeamletIndex);
        this.angleCoordMatr.putAll(c.angleCoordMatr);
        this.nbAngleBeamlets.putAll(c.nbAngleBeamlets);

        setActiveRows();
    }

    // Se inicializan las coordenadas de cada beam del collimator
    private void initializeCoordinates() throws FileNotFoundException {
        double max = -99999;
        int globalID = 0;
        int localID;
        int angle;
        double x;
        double y;

        // Se lee la estructura con ID_Beam - Path Coordinates Beamlet
        for (Pair<Integer, String> temp : coord_file) {
            angle = temp.getFirst();
            String CoordinatePath = temp.getSecond();
            File coordFile = new File("./" + CoordinatePath);

            // Se procede a leer el archivo temp
            if (!coordFile.exists()) {
                throw new FileNotFoundException("NO SE ENCUENTRA EL ARCHIVO " + CoordinatePath);
            }
            Scanner lect = new Scanner(coordFile);

            // Contiene la posicion (original) de todos los beamlets desde un beam
            ArrayList<Pair<Double, Double>> AngleBeamlet = new ArrayList<>();
            Set<Double> filter = new HashSet<>();

            // Se recorren todos los beamlets de 1 beam contenidos en el archivo
            while (lect.hasNextLine()) {
                // ID Local Beamlet - Position 1 Position 2
                String lineaActual = lect.nextLine();
                String[] arrayLinea = lineaActual.split("\t");

                // ID local obtenido desde el mismo temp
                localID = Integer.parseInt(arrayLinea[0]);
                this.allBeamletIndex.put(globalID, new Pair<>(angle, localID));

                // Coordenadas originales a nivel cartesiano
                x = Double.parseDouble(arrayLinea[1]);
                y = Double.parseDouble(arrayLinea[2]);
                filter.add(Math.abs(x));
                filter.add(Math.abs(y));

                AngleBeamlet.add(new Pair<>(x, y));
                globalID++;
            }
            lect.close();

            // Se busca actualizar el mayor elemento de los beamlets para tener referencia
            // de la dimension
            for (Double indexRow : filter) {
                if (indexRow > max) {
                    max = indexRow;
                }
            }

            // Se agrega la lista de los indices cartesianos de cada beamlet pertenecientes
            // a un angulo 'angle'
            this.angles.add(angle);
            this.angleCoordMatr.put(angle, AngleBeamlet);

        }
        // Se define la dimension total del colimator
        gDim = (int) (max * 2) + 1;
        this.xDim = gDim;
        this.yDim = gDim;
        this.nbBeamlets = globalID;

        // Transformación de las coordenadas cartesianas a coordenadas matriciales
        for (Integer idBeam : angleCoordMatr.keySet()) {
            int blperBeam = 0;
            ArrayList<Pair<Integer, Integer>> newCoords = new ArrayList<>();
            ArrayList<Pair<Double, Double>> beamletsBeam = angleCoordMatr.get(idBeam);
            for (Pair<Double, Double> row : beamletsBeam) {
                blperBeam++;

                int newX = (int) (row.getFirst() + max);
                int newY = (int) (row.getSecond() + max);
                newCoords.add(new Pair<>(newX, newY));
            }
            this.nbAngleBeamlets.put(idBeam, blperBeam);
            this.angleCoord.put(idBeam, newCoords);
        }
        setActiveRows();
    }

    // Metodo que obtiene el rango de apertura activo del collimator, si el rango es
    // <a,b>
    // Significa que el rango esta abierto desde a hasta b (resalto que lo incluye)
    void setActiveRows() {
        for (Integer idBeam : angleCoord.keySet()) {
            ArrayList<Pair<Integer, Integer>> activeRange = new ArrayList<>();
            ArrayList<Pair<Integer, Integer>> beamletsBeam = angleCoord.get(idBeam);

            // A partir de la fila r, obtengo todos los valores para las filas
            for (int r = 0; r < gDim; r++) {
                Set<Integer> filter = new HashSet<>();

                // A partir de un indice r asociado a cada fila, se busca los beamlets en ella
                for (Pair<Integer, Integer> beamRow : beamletsBeam) {
                    if (Integer.compare(r, beamRow.getFirst()) == 0) {
                        filter.add(beamRow.getSecond());
                    }
                }

                if (filter.size() == 0) {
                    // Significa que la fila no esta activa, que no tiene beamlets en ella
                    activeRange.add(new Pair<>(-1, -1));
                } else {

                    // Fila abierta
                    ArrayList<Integer> orderItems = new ArrayList<>();
                    for (Integer index : filter) {
                        orderItems.add(index);
                    }
                    Collections.sort(orderItems);
                    int first = orderItems.get(0);
                    int last = orderItems.get(orderItems.size() - 1);
                    activeRange.add(new Pair<>(first, last));
                }
            }
            // Se agrega un vector de rangos para cada fila de un angulo 'angle' que sera la
            // llave del diccionario
            angleRowActive.put(idBeam, activeRange);
        }
    }

    // Transformar la identificacion local (del beam), a una identificador global
    public Pair<Integer, Integer> indexToPos(int index, int angle) {
        int x = (angleCoord.get(angle)).get(index).getFirst();
        int y = (angleCoord.get(angle)).get(index).getSecond();
        return new Pair<>(x, y);
    }

    // Obtengo el rango activo de la fila x en el angulo 'angle'
    public Pair<Integer, Integer> getActiveRange(int x, int angle) {
        // x es la fila del collimator
        return (angleRowActive.get(angle).get(x));
    }

    public int getxDim() {
        return xDim;
    }

    public int getNbAngles() {
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

    public Hashtable<Integer, ArrayList<Pair<Integer, Integer>>> getAngleCoord() {
        return angleCoord;
    }

    public void setAngleCoord(Hashtable<Integer, ArrayList<Pair<Integer, Integer>>> angleCoord) {
        this.angleCoord = angleCoord;
    }

    public Hashtable<Integer, ArrayList<Pair<Double, Double>>> getAngleCoordMatr() {
        return angleCoordMatr;
    }

    public void setAngleCoordMatr(Hashtable<Integer, ArrayList<Pair<Double, Double>>> angleCoordMatr) {
        this.angleCoordMatr = angleCoordMatr;
    }
}
