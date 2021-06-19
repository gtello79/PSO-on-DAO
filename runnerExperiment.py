from os import system

import random
import subprocess

def main():
    folder_experiments = './ExperimentsFiles/'
    folder_finals = './FinalResults/'
    execute_command = "java -cp src com.company.Main"
    compiler_command = "javac --class-path src src/com/company/Main.java"


    UID = str(int(random.random()*1000))
    exp_iter = 6
    results = []
    
    # Compilacion del codigo   
    system(compiler_command)

    for i in range(exp_iter):
        
        #Ejecutar experimento
        name_iter = UID + "-"+str(i)+"+PSOTEST.txt"
        result_path = folder_experiments + name_iter
        
        #Ejecucion
        system(execute_command + ">" +result_path)

        #Guardar resultados
        actual_exp = open(result_path, 'r')
        
        final_exp = actual_exp.readlines()[-1]
        results.append(final_exp)
        print(final_exp)
        
        actual_exp.close()


    results_path = folder_finals + UID + "-FinalResults.txt"
    final_results = open(results_path, 'a')

    for r in results:
        final_results.write(r)
    
    final_results.close()
    

main()
