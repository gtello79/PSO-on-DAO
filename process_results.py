from decimal import DivisionByZero
import seaborn as sns
from matplotlib import pyplot as plt
import os

def find_files(fileptr, search_path):
    
    result = []
    # Wlaking top-down from the root
    for root, dir, files in os.walk(search_path):
        for f_name in files:
            if fileptr in f_name:
                result.append(f_name)
    
    result.sort()
    return result
nExp = 36

file_name = '{}-FinalResults'.format(nExp)


dir_exp = './FinalResults/'
id_experiments = [469]

iter_labels ={
    0: 'Final Solution',
    1: '#Apertura',
    2: 'BoT'
}

for id in id_experiments:
    file_name = f'{id}-FinalResults.txt'
    all_files = find_files(file_name, dir_exp)

    if not all_files :
        print(f'{file_name}: Archivo no encontrado')
        continue

    if len(all_files) != 1:
        print(f'{file_name} Existe mas de un archivo')
        continue
    
    file_selected = all_files[0]
    full_path = dir_exp + file_name
    open_file = open(full_path, 'r')

    values = {}

    index_line = 0

    for line in open_file.readlines():
        
        index_line +=1 

        if index_line == 1:
            print(line)
            continue

        line = line.strip().split()
        keys = [id for id in range(0,len(line))]
        
        for id in keys:

            if id not in values:
                values[id] = []

            character = float(line[id])
            values[id].append(character)
        
    for key, value in values.items():

        total_sum = sum(value)
        n_values = len(value)

        try:
            mean = float(total_sum/n_values)
        except DivisionByZero:
            mean = 0

        print(iter_labels[key], mean)