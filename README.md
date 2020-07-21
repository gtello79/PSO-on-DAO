# DAO-PSO

## Detalles para implementación de IMRT en JAVA    
1. Implementar función de Evaluación (Terminado 26-05-2020)
2. Documentar bien cada elemento de collimator y volumen (Empezado 26-05-2020)
3. Unir elementos de Plan (Empezado 28-05-2020)
4. Definir bien Beam 
5. Leer teoría del paper propuesto por los profesores
6. Revisar el error que se produce en el error de la matriz (REVISADO Y FUNCIONANDO)
7. Implementar bien los movimientos y la elaboración de una apertura.

## Estructura del código:

### ¿Qué representa Collimator?
- Tiene coordenadas.
- beam_cood(x)(y) ordenados como en todos los archivos, donde x es fila e y es columna
- Información general de la configuración del beamlet

### ¿Qué es el volume?
- Información de los organos
- Contiene Dose Deposition Matrix para cada angulo
- Tiene el numero de voxels
- D(a)(k,b): Dosis suministrada a el voxel K de los organos por el beamlet b en el angulo A

### ¿Qué es Plan?
- Presenta las configuraciones que podría llevar cada uno de los beam (en este caso 5)
- Está asociado a la función de evaluación.

### ¿Qué es Beam?
- Una parte de la solución
- Presenta una determinada configuración (o proyección) desde ese ángulo hacia el tumor y sus organos aledaños
- Presenta un conjunto de aperturas que representan la forma en que estarán separadas dichas hojas
- La combinación del conjunto de aperturas y la intensidad asociada a cada uno
- Contiene la matriz de intensidad (combinación lineal entre aperturas e intensidades)

### ¿Qué es la apertura?
- Es un vector de pares ordenados, donde cada elemento representa a la distancia entre las hojas de cada fila.
- Presenta el elemento de intensidad asociado a dicha apertura.
- Es bueno que permita tener un método que nos de la oportunidad de mostrar la matriz que se construyó.
- Nivel elemental del problema, nos interesa que la metaheurística se aplique sobre las hojas

## Detalles archivos:
1. testinstance0_70_141_210_280
- Entrega el valor de los angulos
- Entrega el path de la DDM para cada organo

2. test_instance_coordinates
- angulo-Coordenadas ese angulo para cada beamlet

3. Coordinates Beam_angulo

4. DM


## Extras
### Definiciones
- Beamlet
- Beam

### Observaciones del código en C++
- En Station.cpp que verificar si existe otra forma de inicializar las hojas con valores random, no entiendo mucho como funciona
- Las hojas pueden estar bajo el mismo índice? Por ejemplo, la hoja fila 1 de una apertura está en el <1,1>
## Proximas preguntas
- ¿A que se refiere con la clase apertura? ¿Se refiere a la matriz de 0 y 1 junto a su adecuada intensidad?

