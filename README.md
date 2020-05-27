# DAO-PSO

## Detalles para implementación de IMRT en JAVA    
1. Implementar función de Evaluación (Terminado 26-05-2020)
2. Documentar bien cada elemento de collimator y volumen (Empezado 26-05-2020)
3. Unir elementos de Plan (Empezado 27-05-2020)
4. Definir bien Beam 
5. Leer teoría del paper propuesto por los profesores
6. Revisar el error que se produce en el error de la matriz


## Estructura del código:

### ¿Qué representa Collimator?
- Tiene coordenadas.
- beam_cood[x}[y] ordenados como en todos los archivos, donde x es fila e y es columna
- Información general de la configuración del beamlet


### ¿Qué es el volume?
- Información de los organos
- Contiene Dose Deposition Matrix para cada angulo
- Tiene el numero de voxels
- D[a](k,b): Dosis suministrada a el voxel K de los organos por el beamlet b en el angulo A

## Detalles archivos:
1. testinstance0_70_141_210_280
- Entrega el valor de los angulos
- Entrega el path de la DDM para cada organo

2. test_instance_coordinates
- angulo - Coordenadas ese angulo

3. Coordinates Beam_angulo

4. DM
