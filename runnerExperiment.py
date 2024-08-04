from os import system
from sys import argv
import os
from time import time
from argparse import ArgumentParser

CURR_DIR = os.getcwd()
LD_LIBRARY_GUROBI_PATH = '/opt/gurobi1101/linux64/lib/gurobi.jar'
folder_experiments = './ExperimentsFiles/'
folder_finals = './FinalResults/'

def UUID_exp() -> str:
    from datetime import datetime

    # Get current date and time
    now = datetime.now()

    # Format date as YYYY-MM-DD
    date_string = now.strftime("%Y%m%d")

    # Format time as HH:MM (24-hour format)
    time_string = now.strftime("%H%M")

    # Combine date and time strings with a space
    datetime_string = f"{date_string}_{time_string}"

    return datetime_string

UID = UUID_exp()

def parse_arguments():
    parser = ArgumentParser(description="PSO Algorithm Parameters")
    parser.add_argument("--exp_iter", type=int, help="Experiment iteration")
    parser.add_argument("--i", dest="instance", type=int, help="Instance number")
    parser.add_argument("--size", type=int, help="Swarm size")
    parser.add_argument(
        "--c1Aperture", type=float, help="Learning constant c1 for aperture"
    )
    parser.add_argument(
        "--c2Aperture", type=float, help="Learning constant c2 for aperture"
    )
    parser.add_argument("--inerAperture", type=float, help="Inertia for aperture")
    parser.add_argument(
        "--c1Intensity", type=float, help="Learning constant c1 for intensity"
    )
    parser.add_argument(
        "--c2Intensity", type=float, help="Learning constant c2 for intensity"
    )
    parser.add_argument("--inerIntensity", type=float, help="Inertia for intensity")
    parser.add_argument("--iter", type=int, help="N° Iteration for the swarm")

    args = parser.parse_args()
    return args

def main():

    
    results = []
    exp_iter = 10
    nThreads = 3
    N_EVALUATIONS = 5000
    intensityOptimized = False

    params_args = parse_arguments()

    cn_aperture = 1.6641
    cn_intensity = 1.2389

    # PSO PARAMETERS
    instance = params_args.instance or 85
    size = params_args.size or 418
    c1_aper = params_args.c1Aperture or 1.8751
    c2_aper = params_args.c2Aperture or 0.2134
    w_aper = params_args.inerAperture or 0.5774
    c1_int = params_args.c1Intensity or 0.3158
    c2_int = params_args.c2Intensity or 1.7017
    w_int = params_args.inerIntensity or 0.5331
    iter = params_args.iter or (N_EVALUATIONS // size)

    if(argv.__contains__("cnAperture")):
        index = argv.index("cnAperture")
        cn_aperture = float(argv[index+1])
    if(argv.__contains__("cnIntensity")):
        index = argv.index("cnIntensity")
        cn_intensity = float(argv[index+1])


    iter = int(40000/size)
    init_time = time()

    #Compilacion
    compiler_command = f"javac --class-path src:{LD_LIBRARY_GUROBI_PATH} src/com/company/Main.java "
    try:
        system(compiler_command)
    except SystemError as e:
        print(e)

    config_params = "i "                + str(instance)         + " size "          + str(size) +\
                    " c1Aperture "      + str(c1_aper)      + " c2Aperture "    + str(c2_aper) + \
                    " inerAperture "    + str(w_aper)    + " c1Intensity "   + str(c1_int) +\
                    " c2Intensity "     + str(c2_int)     + " inerIntensity " + str(w_int) + " iter " + str(iter)+ " nThreads " + str(nThreads)+ \
                    " cnAperture "      + str(cn_aperture)      + " cnIntensity "   + str(cn_intensity)  + " "


    if intensityOptimized:
        config_params += " intensityOptimized " + str(intensityOptimized)

    execute_command = f"java -classpath ./src:{LD_LIBRARY_GUROBI_PATH} com.company.Main " + config_params

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
