# DAO-PSO

## Detalles para implementación de IMRT en JAVA    
Implementar función de Evaluación (Terminado 26-05-2020)
Documentar bien cada elemento de collimator y volumen (Empezado 26-05-2020)
Unir elementos de Plan (Empezado 27-05-2020)
Definir bien Beam 
Leer teoría del paper propuesto por los profesores
Revisar el error que se produce en el error de la matriz


## Estructura del código:

¿Qué representa Collimator?
Tiene coordenadas.
beam_cood[x}[y] ordenados como en todos los archivos, donde x es fila e y es columna
Información general de la configuración del beamlet


## ¿Qué es el volume?
Información de los organos
Contiene Dose Deposition Matrix para cada angulo
Tiene el numero de voxels
D[a](k,b): Dosis suministrada a el voxel K de los organos por el beamlet b en el angulo A

## Detalles archivos:
testinstance0_70_141_210_280
Entrega el valor de los angulos
Entrega el path de la DDM para cada organo

test_instance_coordinates
    angulo - Coordenadas ese angulo

Coordinates Beam_angulo

DM
