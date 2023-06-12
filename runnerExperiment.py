from os import system
from sys import argv
import random
import os
from time import time

def main():

    folder_experiments = './ExperimentsFiles/'
    folder_finals = './FinalResults/'
    
    UID = str(int(random.random()*1000))
    exp_iter = 10
    results = []
    instance = 85
    nThreads = 3
    intensityOptimized = False
    # Default params
    size = 418

    c1_aperture = 1.8751
    c2_aperture = 0.2134
    iner_aperture = 0.5774
    cn_aperture = 1.6641

    c1_intensity = 0.3158
    c2_intensity = 1.7017
    iner_intensity = 0.5331
    cn_intensity = 1.2389

    if(argv.__contains__("size")):
        index = argv.index("size")
        size = int(argv[index+1])
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
    if(argv.__contains__("i")):
        index = argv.index("i")
        instance = int(argv[index+1])
    if(argv.__contains__("cnAperture")):
        index = argv.index("cnAperture")
        cn_aperture = float(argv[index+1])
    if(argv.__contains__("cnIntensity")):
        index = argv.index("cnIntensity")
        cn_intensity = float(argv[index+1])

    CURR_DIR = os.getcwd()
    print("CURR: {}".format(CURR_DIR))
    iter = int(40000/size)
    init_time = time()

    #Compilacion
    compiler_command = "javac --class-path src:$LD_LIBRARY_PATH/gurobi.jar src/com/company/Main.java "
    try:
        system(compiler_command)
    except SystemError as e:
        print(e)

    config_params = "i "               + str(instance)         + " size "          + str(size) +\
                    " c1Aperture "      + str(c1_aperture)      + " c2Aperture "    + str(c2_aperture) + \
                    " inerAperture "    + str(iner_aperture)    + " c1Intensity "   + str(c1_intensity) +\
                    " c2Intensity "     + str(c2_intensity)     + " inerIntensity " + str(iner_intensity) + " iter " + str(iter)+ " nThreads " + str(nThreads)+ \
                    " cnAperture "      + str(cn_aperture)      + " cnIntensity "   + str(cn_intensity)  + " "


    if intensityOptimized:
        config_params += " intensityOptimized " + str(intensityOptimized)

    execute_command = "java -classpath ./src:$LD_LIBRARY_PATH/gurobi.jar com.company.Main " + config_params

    print("Experiments ID ", UID)
    print("PARAMS ", config_params)
    for i in range(exp_iter):

        #Ejecutar experimento
        name_iter = UID + "-"+str(i)+"+PSOTEST.txt"
        result_path = folder_experiments + name_iter

        # Ejecucion
        try:
            system(execute_command + ">" +result_path)
        except SystemError as e:
            print(e.with_traceback)
        #Guardar resultados
        actual_exp = open(result_path, 'r')

        final_exp = actual_exp.readlines()[-1]
        results.append(final_exp)
        print(final_exp)

        actual_exp.close()


    results_path = folder_finals + UID + "-FinalResults.txt"
    final_results = open(results_path, 'a')
    config_params = config_params + '\n'

    final_results.write(config_params)
    for r in results:
        final_results.write(r)
    final_results.close()
    
    final_time = time()

    total_exec_time = final_time - init_time
    print(f'EXECUTION TIME: {total_exec_time} [s]')

main()
