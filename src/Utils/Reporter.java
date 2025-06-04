// Archivo: Utils/Reporter.java
package Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom; // Mejor para generar IDs aleatorios

import SRCDAO.Aperture;
import SRCDAO.Beam;
import SRCDAO.Plan;
import Swarms.Particle;
import source.Matrix;
import source.Pair;

public class Reporter {

    // Rutas de las carpetas de salida
    private static final String BASE_OUTPUT_DIR = "./OutPlots/";
    private static final String INTENSITY_FOLDER_PATH = BASE_OUTPUT_DIR + "intensityFolder/";
    private static final String APERTURE_FOLDER_PATH = BASE_OUTPUT_DIR + "apertureFolder/";

    // Constantes para delimitadores y nueva línea
    private static final String NEW_LINE = "\n";
    private static final String DELIMITER = ",";

    // Constructor privado para evitar instanciación, ya que usaremos métodos estáticos
    private Reporter() {
        // Asegurarse de que las carpetas de salida existan
        createOutputDirectories();
    }

    private static void createOutputDirectories() {
        try {
            Files.createDirectories(Paths.get(INTENSITY_FOLDER_PATH));
            Files.createDirectories(Paths.get(APERTURE_FOLDER_PATH));
        } catch (IOException e) {
            System.err.println("Error creating output directories: " + e.getMessage());
            // Considerar lanzar una RuntimeException o loggear de forma más robusta
        }
    }

    /**
     * Genera un ID de reporte aleatorio.
     * Es estático porque no depende del estado de una instancia de Reporter.
     * @return Un ID de reporte aleatorio.
     */
    public static String generateRandomReportId() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(0, 80));
    }

    /**
     * Método principal para generar cualquier tipo de reporte.
     * Utiliza polimorfismo sobrecargado para aceptar diferentes tipos de datos base.
     *
     * @param data El objeto (Particle, Plan, Beam) sobre el cual generar el reporte.
     * @param reportType El tipo de reporte a generar, usando la enumeración ReportType.
     * @param reportId Un ID único para el reporte. Si es null o vacío, se generará uno aleatorio.
     * @throws IOException Si ocurre un error de E/S durante la escritura del archivo.
     * @throws IllegalArgumentException Si el tipo de datos no es soportado para el reporte.
     */
    public static void generateReport(Object data, ReportType reportType, String reportId) throws IOException {
        createOutputDirectories(); // Asegurar directorios antes de cada operación

        String effectiveId = (reportId != null && !reportId.trim().isEmpty()) ? reportId : generateRandomReportId();

        switch (reportType) {
            case INTENSITY_MATRIX_CSV:
                if (data instanceof Particle) {
                    intensityMatrixToCSV(((Particle) data).getCurrentPlan(), effectiveId);
                } else if (data instanceof Plan) {
                    intensityMatrixToCSV((Plan) data, effectiveId);
                } else {
                    throw new IllegalArgumentException("Unsupported data type for INTENSITY_MATRIX_CSV: " + data.getClass().getSimpleName());
                }
                break;

            case ALL_APERTURES_CSV:
                if (data instanceof Particle) {
                    apertureMatrix(((Particle) data).getCurrentPlan(), effectiveId);
                } else {
                    throw new IllegalArgumentException("Unsupported data type for ALL_APERTURES_CSV: " + data.getClass().getSimpleName());
                }
                break;

            case ALL_APERTURES_AMPL:
                if (data instanceof Particle) {
                    printAperturesToAMPL(((Particle) data).getCurrentPlan(), effectiveId);
                } else {
                    throw new IllegalArgumentException("Unsupported data type for ALL_APERTURES_AMPL: " + data.getClass().getSimpleName());
                }
                break;

            case ALL_COMPONENTS_TXT:
                if (data instanceof Particle) {
                    exportIntensityMatrixAndApertures(((Particle) data).getCurrentPlan(), effectiveId);
                } else if (data instanceof Plan) {
                    exportIntensityMatrixAndApertures((Plan) data, effectiveId);
                } else {
                    throw new IllegalArgumentException("Unsupported data type for ALL_COMPONENTS_TXT: " + data.getClass().getSimpleName());
                }
                break;

            case INTENSITY_VECTOR_TXT:
                if (data instanceof Particle) {
                    intensityVector(((Particle) data).getCurrentPlan(), effectiveId);
                } else if (data instanceof Plan) {
                    intensityVector((Plan) data, effectiveId);
                } else {
                    throw new IllegalArgumentException("Unsupported data type for INTENSITY_VECTOR_TXT: " + data.getClass().getSimpleName());
                }
                break;

            case TRANSPOSE_APERTURE_TXT:
                if (data instanceof Particle) {
                    printTransposeMatrix(((Particle) data).getCurrentPlan().getByID(0), effectiveId); // Asumiendo que siempre es el beam 0
                } else if (data instanceof Beam) {
                    printTransposeMatrix((Beam) data, effectiveId);
                } else {
                    throw new IllegalArgumentException("Unsupported data type for TRANSPOSE_APERTURE_TXT: " + data.getClass().getSimpleName());
                }
                break;

            case INTENSITY_MATRIX_TXT:
            case ALL_APERTURES_TXT:
                // Estos casos estaban vacíos en la implementación original.
                // Si se necesitan, se deben implementar aquí.
                System.out.println("Report type " + reportType + " is not implemented yet.");
                break;

            default:
                throw new IllegalArgumentException("Unknown report type: " + reportType);
        }
        System.out.println("DONE - Report generated with ID: " + effectiveId + " for type: " + reportType);
    }

    /**
     * Sobrecarga para facilitar el uso sin especificar un ID.
     * Un ID aleatorio será generado automáticamente.
     */
    public static void generateReport(Object data, ReportType reportType) throws IOException {
        generateReport(data, reportType, null);
    }

    // --- Métodos Privados para la Escritura de Reportes ---

    private static void writeToFile(String filePath, ReportContentWriter writer) throws IOException {
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            writer.write(fileWriter);
            fileWriter.flush();
        }
    }

    // Interfaz funcional para la lógica de escritura específica del contenido
    @FunctionalInterface
    private interface ReportContentWriter {
        void write(FileWriter writer) throws IOException;
    }

    private static void intensityMatrixToCSV(Plan plan, String uid) throws IOException {
        for (Beam beam : plan.getAngle_beam()) {
            Matrix matrix = beam.getIntensitisMatrix();
            String fileName = uid + "-intensityMatrix" + beam.getIdBeam() + ".csv";
            String filePath = INTENSITY_FOLDER_PATH + fileName;

            writeToFile(filePath, writer -> {
                writer.append(String.valueOf(beam.getIdBeam())).append(NEW_LINE);
                for (int i = 0; i < matrix.getX(); i++) {
                    for (int j = 0; j < matrix.getY(); j++) {
                        writer.append(String.valueOf(matrix.getPos(i, j))).append(DELIMITER);
                    }
                    writer.append(NEW_LINE);
                }
            });
        }
    }

    private static void apertureMatrix(Plan plan, String uid) throws IOException {
        for (Beam beam : plan.getAngle_beam()) {
            int gDim = beam.getCollimatorDim();
            ArrayList<Aperture> apertureVector = beam.getApertures();
            String fileName = uid + "-Apertures" + beam.getIdBeam() + ".csv";
            String filePath = APERTURE_FOLDER_PATH + fileName;

            writeToFile(filePath, writer -> {
                writer.append(String.valueOf(beam.getIdBeam())).append(NEW_LINE);
                for (Aperture a : apertureVector) {
                    List<Pair<Integer, Integer>> shapes = a.getApertures(); // Usar List
                    writer.append(Double.toString(a.getIntensity())).append(NEW_LINE);

                    for (int i = 0; i < gDim; i++) {
                        for (int j = 0; j < gDim; j++) {
                            if (j > shapes.get(i).getFirst() && j < shapes.get(i).getSecond() && shapes.get(i).getFirst() != -2) {
                                writer.append("1");
                            } else if (shapes.get(i).getFirst() == -2) {
                                writer.append("-1");
                            } else {
                                writer.append("0");
                            }
                            writer.append(DELIMITER);
                        }
                        writer.append(NEW_LINE);
                    }
                    writer.append(NEW_LINE);
                }
            });
        }
    }

    private static void intensityVector(Plan plan, String uid) throws IOException {
        String fileName = uid + "-FluenceMap.txt";
        String filePath = INTENSITY_FOLDER_PATH + fileName;

        writeToFile(filePath, writer -> {
            StringBuilder vectorChain = new StringBuilder();
            for (double i : plan.getFluenceMap()) {
                vectorChain.append(i).append(DELIMITER).append(" ");
            }
            // Eliminar el último ", " si existe
            if (vectorChain.length() > 2) {
                vectorChain.setLength(vectorChain.length() - 2);
            }
            writer.append(vectorChain.toString());
        });
    }

    private static void exportIntensityMatrixAndApertures(Plan plan, String uid) throws IOException {
        List<Beam> beams = plan.getAngle_beam(); // Usar List
        for (int b = 0; b < plan.getNBeam(); b++) {
            Beam beam = beams.get(b);
            String fileName = uid + "-Desc_Beam" + beam.getIdBeam() + ".txt";
            String filePath = BASE_OUTPUT_DIR + fileName; // En la carpeta general

            Matrix intensityMatrix = beam.getIntensitisMatrix();
            List<Aperture> aperturesSet = beam.getApertures(); // Usar List

            writeToFile(filePath, writer -> {
                writer.append("Score,").append(String.valueOf(plan.getEval())).append(NEW_LINE);
                writer.append("Beam,").append(String.valueOf(beam.getIdBeam())).append(NEW_LINE);

                // Writting Intensity Matrix on File
                for (int i = 0; i < intensityMatrix.getX(); i++) {
                    for (int j = 0; j < intensityMatrix.getY(); j++) {
                        writer.append(String.valueOf(intensityMatrix.getPos(i, j))).append(DELIMITER);
                    }
                    writer.append(NEW_LINE);
                }
                writer.append(NEW_LINE);

                // Writting Apertures on file
                for (int a = 0; a < aperturesSet.size(); a++) {
                    Aperture aperture = aperturesSet.get(a);
                    double intensity = aperture.getIntensity();
                    List<Pair<Integer, Integer>> shapes = aperture.getApertures(); // Usar List

                    writer.append("Aperture,").append(String.valueOf(intensity)).append(NEW_LINE);

                    for (int i = 0; i < intensityMatrix.getX(); i++) {
                        Pair<Integer, Integer> pair = shapes.get(i);
                        for (int j = 0; j < intensityMatrix.getY(); j++) {
                            String value;
                            // Cerrada permanentemente - nota: usa intensityMatrix.getPos(i,j) para esto,
                            // lo cual es extraño si se supone que la apertura es la que define si está cerrada.
                            // Debería ser shapes.get(i).getFirst() == -2 para indicar la inactividad de la hoja.
                            // Si -1 es un valor mágico para Matrix, debería ser una constante.
                            if (intensityMatrix.getPos(i, j) == -1) { // Lógica a revisar
                                value = String.valueOf(-1);
                            } else {
                                if (j > pair.getFirst() && j < pair.getSecond()) {
                                    value = String.valueOf(1);
                                } else {
                                    value = String.valueOf(0);
                                }
                            }
                            writer.append(value).append(DELIMITER);
                        }
                        writer.append(NEW_LINE);
                    }
                    writer.append(NEW_LINE);
                }
            });
        }
    }

    private static void printAperturesToAMPL(Plan plan, String uid) { // uid no se usa aquí, pero se mantiene para consistencia
        int[] beamAngles = plan.getBeamletsByBeam();
        for (int i = 0; i < beamAngles.length; i++) {
            int totalBeamlets = beamAngles[i];
            // El '5' aquí es un número mágico. ¿Es el número de aperturas por haz? Debería ser una constante o parámetro.
            // O tp.getN_apertures() si existe.
            for (int a = 0; a < 5; a++) {
                System.out.println("param x" + (i + 1) + (a + 1) + " :=");
                for (int j = 0; j < totalBeamlets; j++) {
                    System.out.println((j + 1) + "    " + plan.getProyectedBeamLetByApertureOnBeam(i, a, j));
                }
                System.out.println(";");
                System.out.println("");
            }
        }
    }

    private static void printTransposeMatrix(Beam beam, String uid) throws IOException {
        // Asumiendo que getTransposeMatrix() devuelve la matriz transpuesta de las formas de las aperturas
        List<Pair<Integer, Integer>> shapes = beam.getTransposeMatrix(); // Usar List
        String fileName = "TransposedMatrixBeam" + beam.getIdBeam() + ".txt";
        String filePath = BASE_OUTPUT_DIR + fileName; // En la carpeta general

        Matrix intensityMatrix = beam.getIntensitisMatrix(); // Parece que esta matriz se usa para dimensiones/valores especiales

        writeToFile(filePath, writer -> {
            writer.append("Beam,").append(String.valueOf(beam.getIdBeam())).append(NEW_LINE);
            writer.append(NEW_LINE); // Una nueva línea adicional

            for (int i = 0; i < intensityMatrix.getX(); i++) { // Usando dimensiones de intensityMatrix
                Pair<Integer, Integer> pair = shapes.get(i);
                for (int j = 0; j < intensityMatrix.getY(); j++) { // Usando dimensiones de intensityMatrix
                    String value;
                    if (intensityMatrix.getPos(i, j) == -1) { // Lógica a revisar
                        value = String.valueOf(-1);
                    } else {
                        if (j > pair.getFirst() && j < pair.getSecond()) {
                            value = String.valueOf(1);
                        } else {
                            value = String.valueOf(0);
                        }
                    }
                    writer.append(value).append(DELIMITER);
                }
                writer.append(NEW_LINE);
            }
        });
    }
}