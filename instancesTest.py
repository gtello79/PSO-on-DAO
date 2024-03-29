from os import system
from sys import argv
import random
import sys

def main():

    folder_experiments = './ExperimentsFiles/'
    folder_finals = './FinalResults/'
    
    UID = str(int(random.random()*1000))
    exp_iter = 5
    results = []

    # Default params
    size = 2
    iter = 5

    c1_aperture = 1.0
    c2_aperture = 1.0
    iner_aperture = 1.0

    c1_intensity = 1.0
    c2_intensity = 1.0
    iner_intensity = 1.0
    instance = range(85,97)

    if(argv.__contains__("size")):
        index = argv.index("size")
        size = int(argv[index+1])
    if(argv.__contains__("iter")):
        index = argv.index("iter")
        iter = int(argv[index+1])
    if(argv.__contains__("c1Aperture")):
        index = argv.index("c1Aperture")
        c1_aperture = float(argv[index+1])
    if(argv.__contains__("c2Aperture")):
        index = argv.index("c2Aperture")
        c2_aperture = float(argv[index+1])
    if(argv.__contains__("inerAperture")):
        index = argv.index("inerAperture")
        iner_aperture = float(argv[index+1])
    if(argv.__contains__("c1Intensity")):
        index = argv.index("c1Intensity")
        c1_intensity = float(argv[index+1])
    if(argv.__contains__("c2Intensity")):
        index = argv.index("c2Intensity")
        c2_intensity = float(argv[index+1])    
    if(argv.__contains__("inerIntensity")):
        index = argv.index("inerIntensity")
        iner_intensity = float(argv[index+1])    
    if(argv.__contains__("exp_iter")):
        index = argv.index("exp_iter")
        exp_iter = int(argv[index+1])
    
    #Compilacion
    compiler_command = "javac --class-path src:$LD_LIBRARY_PATH/gurobi.jar src/com/company/Main.java "
    system(compiler_command)

    config_params = "size "+str(size)+" iter "+str(iter) \
                    + " c1Aperture " + str(c1_aperture) + " c2Aperture " + str(c2_aperture) + " inerAperture " + str(iner_aperture) \
                    + " c1Intensity " + str(c1_intensity) + " c2Intensity " + str(c2_intensity) + " inerIntensity " + str(iner_intensity)
                    
    execute_command = "java -classpath ./src:$LD_LIBRARY_PATH/gurobi.jar com.company.Main " + config_params
    for i in instance:
        system(execute_command + " i " + str(i) )
        
main()