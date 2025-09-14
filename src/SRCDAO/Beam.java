package SRCDAO;

import java.security.KeyException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import javafx.util.Pair;


import source.*;

public class Beam {
    private int angle; // ID BEAM

    // Informacion de la configuracion tecnica del angulo
    private int maxApertures;
    private int maxIntensity;
    private int minIntensity;
    private int initialIntensity;
    private int stepIntensity;
    private int openApertures;
    private int setup;
    private int collimatorDim;
    private int totalBeamlets;
    private int aperturesUnused;
    private double beamOnTime;

    private final Collimator collimator; // Representacion del collimator
    private Matrix I; // Representacion de la matriz de intensidad

    /*
     * Apertures (representation 1):
     * Each aperture is represented by a vector of pairs A[i] = (x_ini, x_fin)
     * and an intensity range open (x_ini+1, x_fin-1) of row "r" for aperture d:
     * A[d][r](x_ini, x_fin)
     */
    private ArrayList<Aperture> A;
    private ArrayList<Double> fluenceMap;

    /*
     * ------------------------------------------- GENERAL METHODS
     * -------------------------------------------------------
     */
    public Beam(int angle, int maxApertures, int maxIntensity, int minIntensity, int initialIntensity,
            int stepIntensity, int openApertures, int setup, Collimator collimator) {
        setAngle(angle);
        setMax_apertures(maxApertures);
        setMax_intensity(maxIntensity);
        setMinIntensity(minIntensity);
        setInitialIntensity(initialIntensity);
        setStep_intensity(stepIntensity);
        setOpenApertures(openApertures);
        setSetup(setup);
        setCollimatorDim(collimator.getyDim());
        setTotalBeamlets(collimator.getNangleBeamlets(angle));

        this.collimator = collimator;
        this.A = new ArrayList<>();
        this.fluenceMap = new ArrayList<>();
        this.aperturesUnused = 0;
        this.beamOnTime = 0.0;

        if (openApertures == -1)
            setOpenApertures(maxApertures);

        // Declaracion de la matriz de intensidad I y se inicializa
        this.I = new Matrix(this.collimatorDim, collimator.getyDim());

        // Limpiado de beamlets con valor 0
        clearIntensity();

        // Initialize apertures
        initiliazeBeam(setup, openApertures);

        // Se construye la Matriz de Intensidad
        generateIntensities();

    }

    public Beam(Beam b) {
        setAngle(b.angle); // Setea el ID del Beam
        setMax_apertures(b.maxApertures);
        setMax_intensity(b.maxIntensity);
        setMinIntensity(b.minIntensity);
        setInitialIntensity(b.initialIntensity);
        setStep_intensity(b.stepIntensity);
        setOpenApertures(b.openApertures);
        setSetup(b.setup);
        setMinIntensity(b.minIntensity);
        setCollimatorDim(b.getCollimatorDim());
        setTotalBeamlets(b.getTotalBeamlets());

        this.collimator = new Collimator(b.collimator);
        this.A = new ArrayList<>();
        this.fluenceMap = new ArrayList<>();
        this.aperturesUnused = b.aperturesUnused;
        this.beamOnTime = b.beamOnTime;

        if (openApertures == -1)
            setOpenApertures(maxApertures);

        // Declaracion de la matriz de intensidad I y se inicializa
        I = new Matrix(b.getCollimatorDim(), b.getCollimatorDim());

        // Rellenado de la matriz de intensidad
        clearIntensity();

        for (Aperture a : b.A) {
            Aperture copyAperture = new Aperture(a);
            this.A.add(copyAperture);
        }

        // Se construye la Matriz de Intensidad
        generateIntensities();

    }

    public void initiliazeBeam(int type, int openApertures) {
        Vector<Double> levels = new Vector<>();

        // Calculate levels for random Intensity
        int l = (maxIntensity - minIntensity) / stepIntensity;
        for (int k = 0; k < maxApertures; k++) {
            double i = minIntensity + stepIntensity * (Math.random() * (l + 1));
            levels.add(i);
        }

        // Inicializacion de cada apertura
        for (int index_aperture = 0; index_aperture < maxApertures; index_aperture++) {
            Aperture aux = new Aperture(collimator, angle);
            aux.initializeAperture(type, openApertures, index_aperture);
            aux.initializeIntensity(type, minIntensity, maxIntensity, initialIntensity, levels.get(index_aperture));
            openApertures--;
            A.add(aux);
        }
    }

    public void generateIntensities() {
        this.aperturesUnused = 0;
        this.beamOnTime = 0.0;
        Pair<Integer, Integer> limits;
        clearIntensity();

        for (int a = 0; a < A.size(); a++) {
            Aperture ap = A.get(a);
            double apIntensity = ap.getIntensity();

            this.beamOnTime += apIntensity;

            if (apIntensity < 1.0) {
                this.aperturesUnused++;
            }

            for (int i = 0; i < this.collimatorDim; i++) {
                limits = collimator.getActiveRange(i, angle);

                if (limits.getKey() == -1)
                    continue;

                Pair<Integer, Integer> apertureRow = ap.getOpBeam(i);

                for (int j = apertureRow.getKey() + 1; j < apertureRow.getValue(); j++) {
                    double newIntensity = (this.I.getPos(i, j) + apIntensity);
                    I.setPos(i, j, newIntensity);
                }
            }
        }
        buildIntensityVector();
    }

    public void clearIntensity() {
        // Para cada fila
        for (int i = 0; i < this.collimatorDim; i++) {
            Pair<Integer, Integer> limits = collimator.getActiveRange(i, angle);
            // Para cada celda
            for (int j = 0; j < this.collimatorDim; j++) {
                // Establece el beamlet con intensidad 0
                if (j >= limits.getKey() && j <= limits.getValue()) {
                    this.I.setPos(i, j, 0);
                } else {
                    // Deja el beamlet -1 que significa "no utilizado"
                    this.I.setPos(i, j, -1);
                }
            }
        }
    }

    // Construye el vector de intensidad a partir de la matriz de intensidad
    public void buildIntensityVector() {
        this.fluenceMap = new ArrayList<>();
        // A partir del total de beamlets, consulta la posición de cada uno de ellos
        for (int i = 0; i < totalBeamlets; i++) {
            Pair<Integer, Integer> pos = collimator.indexToPos(i, angle);
            int x = pos.getKey();
            int y = pos.getValue();

            this.fluenceMap.add(I.getPos(x, y));
        }

    }

    public void regenerateApertures() {
        ArrayList<Pair<Integer, Integer>> functionalApertures = this.getTransposeMatrix();
        ArrayList<Integer> aperturesUnUsedList = new ArrayList<>();

        // Reconocimiento de aperturas con baja intensidad
        for (int a = 0; a < A.size(); a++) {
            Aperture aperture = A.get(a);
            if (aperture.getIntensity() < 1.0)
                aperturesUnUsedList.add(a);
        }

        for (int a = 0; a < aperturesUnUsedList.size(); a++) {
            int indexAperture = aperturesUnUsedList.get(a);
            Aperture aperture = A.get(indexAperture);

            for (int indexRow = 0; indexRow < this.collimatorDim; indexRow++) {
                Pair<Integer, Integer> limits = collimator.getActiveRange(indexRow, angle);
                if (limits.getKey() == -1)
                    continue;

                Pair<Integer, Integer> functionalRow = functionalApertures.get(indexRow);

                if (functionalRow.getKey() + 1 == limits.getKey()
                        && functionalRow.getValue() - 1 == limits.getValue()) {
                    // La fila es completamente irradiada -> Se ciera en las apertura inutilizadas
                    Pair<Integer, Integer> newRow = new Pair<>(limits.getKey(), limits.getKey() + 1);
                    aperture.setRow(indexRow, newRow);
                    continue;

                } else if (functionalRow.getKey() + 1 > limits.getKey()
                        && functionalRow.getValue() - 1 < limits.getValue()) {
                    // Falta irradiar por la izquierda y por la derecha
                    if (Math.random() <= 0.5) {

                        // De forma aleatoria, la apertura irradiara por la izquierda
                        int apertureLeft = limits.getKey() - 1;
                        int apertureRight = functionalRow.getKey() + 1;
                        Pair<Integer, Integer> newRow = new Pair<>(apertureLeft, apertureRight);

                        aperture.setRow(indexRow, newRow);

                        functionalRow = new Pair<>(apertureLeft, functionalRow.getValue());

                    } else {
                        // De forma aleatoria, la apertura irradiara por la derecha
                        int apertureLeft = functionalRow.getValue() - 1;
                        int apertureRight = limits.getValue() + 1;

                        Pair<Integer, Integer> newRow = new Pair<>(apertureLeft, apertureRight);

                        aperture.setRow(indexRow, newRow);

                        functionalRow = new Pair<>(functionalRow.getKey(), apertureRight);
                    }
                } else {

                    // Falta irradiar unicamente por la izquierda
                    if (functionalRow.getKey() + 1 > limits.getKey()) {

                        int apertureLeft = limits.getKey() - 1;
                        int apertureRight = functionalRow.getKey() + 1;

                        Pair<Integer, Integer> newRow = new Pair<>(apertureLeft, apertureRight);
                        aperture.setRow(indexRow, newRow);

                        functionalRow = new Pair<>(apertureLeft, functionalRow.getValue());

                    } else if (functionalRow.getValue() - 1 < limits.getValue()) {
                        // Falta irradiar unicamente por la derecha

                        int apertureLeft = functionalRow.getValue() - 1;
                        int apertureRight = limits.getValue() + 1;
                        Pair<Integer, Integer> newRow = new Pair<>(apertureLeft, apertureRight);

                        aperture.setRow(indexRow, newRow);
                        functionalRow = new Pair<>(functionalRow.getKey(), apertureRight);

                    }
                }
                functionalApertures.set(indexRow, functionalRow);
            }

        }

    }

    // Get a general aperture with the shapes adjusted using intensity > 1
    public ArrayList<Pair<Integer, Integer>> BuildTransposeAperture() {
        ArrayList<Pair<Integer, Integer>> aperturesOnOperation = new ArrayList<>();

        for (int a = 0; a < A.size(); a++) {
            Aperture aperture = A.get(a);

            if (aperture.getIntensity() >= minIntensity) {
                if (aperturesOnOperation.size() == 0) {
                    // Get a template of the first aperture with intensity mayor of 1.0
                    aperturesOnOperation = new ArrayList<>(aperture.getApertures());
                } else {
                    for (int r = 0; r < aperturesOnOperation.size(); r++) {

                        // Compare the index of each row on the current aperture with the Template
                        // Generated
                        Pair<Integer, Integer> rowLimits = collimator.getActiveRange(r, angle);
                        if (rowLimits.getValue() == -1)
                            continue;

                        Pair<Integer, Integer> rowAperture = aperture.getOpBeam(r);
                        Pair<Integer, Integer> rowTranspose = aperturesOnOperation.get(r);

                        int firstLeaf = rowTranspose.getKey();
                        int secondLeaf = rowTranspose.getValue();

                        if (rowAperture.getKey() <= firstLeaf) {
                            firstLeaf = rowAperture.getKey();
                        }

                        if (rowAperture.getValue() >= secondLeaf) {
                            secondLeaf = rowAperture.getValue();
                        }

                        aperturesOnOperation.set(r, new Pair<>(firstLeaf, secondLeaf));

                    }
                }
            }
        }

        // Se asegura que no exista un sector intermedio sin irradiar
        for (int r = 0; r < aperturesOnOperation.size(); r++) {
            Pair<Integer, Integer> row = aperturesOnOperation.get(r);
            ArrayList<Integer> beamletsOnRow = new ArrayList<>();

            if (row.getValue() == -2)
                continue;

            // Beamlets utilizados en la fila actual
            for (int x = row.getKey() + 1; x < row.getValue(); x++) {
                beamletsOnRow.add(x);
            }

            // Check if the beamlet is proyected at least one time for an Aperture
            ArrayList<Integer> copyBeamlets = new ArrayList<>();
            for (int a = 0; a < A.size(); a++) {
                Aperture aperture = A.get(a);
                if (aperture.getIntensity() >= 1.0) {
                    Pair<Integer, Integer> apertureRow = aperture.getOpBeam(r);
                    for (int b = 0; b < beamletsOnRow.size(); b++) {
                        int beamlet = beamletsOnRow.get(b);
                        if (beamlet >= apertureRow.getKey() && beamlet <= apertureRow.getValue()) {
                            copyBeamlets.add(beamlet);
                        }
                    }
                }
            }
            // Delete the proyected beamlets and only stay the beamlets hide between leaf
            for (Integer b : copyBeamlets)
                beamletsOnRow.remove(b);

            // Translate the Leaf's to irradiate the beamLets abandoned
            if (beamletsOnRow.size() != 0) {
                Pair<Integer, Integer> newRow = null;
                Collections.sort(beamletsOnRow);

                int maxindex = beamletsOnRow.get(beamletsOnRow.size() - 1);
                int minindex = beamletsOnRow.get(0);

                int d1 = Math.abs(minindex - row.getValue() - 1);
                int d2 = Math.abs(maxindex - row.getKey() + 1);

                if (d1 <= d2) {
                    newRow = new Pair<>(row.getKey(), minindex + 1);
                } else {
                    newRow = new Pair<>(row.getValue(), maxindex - 1);
                }
                aperturesOnOperation.set(r, newRow);

            }
        }

        return aperturesOnOperation;
    }

    /*
     * ------------------------------------------------- GETTER Y SETTERS
     * ---------------------------------------------------
     */

    public void setIntensityByAperture(double[] intensitySolver) {
        for (int a = 0; a < A.size(); a++) {
            Aperture aperture = A.get(a);
            aperture.setIntensity(intensitySolver[a]);
        }
    }

    public boolean getProyectedBeamLetByAperture(int idAperture, int indexBeamlet) {
        Aperture ap = A.get(idAperture);
        return ap.getProyectedBeamLet(indexBeamlet);
    }

    public Double getIntensityByAperture(int apertureIndex) throws KeyException {
        double intensity;
        try {
            intensity = A.get(apertureIndex).getIntensity();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            throw new IndexOutOfBoundsException("La intensidad no ha sido encontrada ");
        }
        return intensity;
    }

    public int getAperturesUnused() {
        return this.aperturesUnused;
    }

    public ArrayList<Double> getIntensityVector() {
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

    public Matrix getIntensitisMatrix() {
        return I;
    }

    public Aperture getAperture(int id) {
        return A.get(id);
    }

    public ArrayList<Aperture> getApertures() {
        return this.A;
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

    public ArrayList<Pair<Integer, Integer>> getTransposeMatrix() {
        return this.BuildTransposeAperture();
    }

    public double getBeamOnTime() {
        return this.beamOnTime;
    }

    /*
     * ------------------------------------------------ PSO METHODS
     * ----------------------------------
     */
    public void CalculateVelocity(double c1Aperture, double c2Aperture, double wAperture, double cnAperture,
            double c1Intensity, double c2Intensity, double wIntensity, double cnIntensity, Beam BGlobal,
            Beam BPersonal) {
        for (int i = 0; i < A.size(); i++) {
            Aperture x = A.get(i);
            Aperture bG = BGlobal.getAperture(i);
            Aperture bP = BPersonal.getAperture(i);

            x.velAperture(wAperture, c1Aperture, c2Aperture, cnAperture, bG, bP);
            x.velIntensity(wIntensity, c1Intensity, c2Intensity, cnIntensity, bG, bP);
        }
    }

    public void CalculatePosition() {
        this.beamOnTime = 0.0;
        for (Aperture x : A) {
            x.movAperture();
            x.moveIntensity(maxIntensity);
            this.beamOnTime += x.getIntensity();
        }
        generateIntensities();
    }

}
