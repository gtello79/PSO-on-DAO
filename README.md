# DAO-PSO

## Detalles para implementación de IMRT en JAVA    
1. Implementar función de Evaluación (Terminado )
2. Documentar bien cada elemento de collimator y volumen (Empezado)
3. Unir elementos de Plan (Terminado)
4. Definir bien Beam (Terminado)
5. Leer teoría del paper propuesto por los profesores (No es muy detallista con AS)
6. Revisar el error que se produce en el error de la matriz (REVISADO Y FUNCIONANDO)
7. Implementar bien los movimientos y la elaboración de una apertura. (Terminado)
8. Existe un error luego que se limpia la matriz, revisar,
9. Revisar nueva conjetura utilizada para determinar beamlets disponibles

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
- Representa a una propuesta a solución

### ¿Qué es Beam?
- Una parte de la solución
- Presenta una determinada configuración (o proyección) desde ese ángulo hacia el tumor y sus organos aledaños
- Presenta un conjunto de aperturas que representan la forma en que estarán separadas dichas hojas
- La combinación del conjunto de aperturas y la intensidad asociada a cada uno
- Contiene la matriz de intensidad (combinación lineal entre aperturas e intensidades)
-
### ¿Qué es la apertura?
- Es un vector de pares ordenados, donde cada elemento representa a la distancia entre las hojas de cada fila.
- Presenta el elemento de intensidad asociado a dicha apertura.
- Es bueno que permita tener un método que nos de la oportunidad de mostrar la matriz que se construyó.
- Nivel elemental del problema, nos interesa que la metaheurística se aplique sobre las hojas
- Tiene un intensidad asociada a ella que va acorde a la intensidad ajustada al beam
- A continuación se presentan las configuraciones de inicialización de las particulas    
    OPEN_MIN_SETUP = 0;
    OPEN_MAX_SETUP = 1;
    CLOSED_MIN_SETUP = 2 ;
    CLOSED_MAX_SETUP = 3;
    RAND_RAND_SETUP = 4;

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
- Dose Deposition Matrix

### Observaciones del código en C++
- En Station.cpp que verificar si existe otra forma de inicializar las hojas con valores random, no entiendo mucho como funciona
- Las hojas pueden estar bajo el mismo índice? Por ejemplo, la hoja fila 1 de una apertura está en el <1,1> 

### Cambios de estructura
#### (13-08) 
	- Ahora la apertura se refleja de la siguiente forma:
	 [<1,3>, ...] Las apertura es un vector de pares, donde 1 es HASTA QUE POSICION CUBRE LA HOJA IZQUIERDA, y 3 HASTA QUE POSICION CUBRE LA HOJA DERECHA. 			
	- Dicho esto, con el primer par vemos que la unica casilla abierta es la casilla 2.
	- El par estará en un rango de [-1, max_eje], recalcar que es inclusive.
	- El valor <-2,-2> para el par de hojas desde ahora representa que no se pueden abrir jamás


## Proximas preguntas

## Parametros asociados y experimentos interesantes
- Observar la inicializacion de las aperturas 
- Como se comporta a medida que todas los 'plan' inicializan de la misma manera?
- Observar lo relevante que es el movimiento de las apertura (cuanto cambia si muevo todos los pares de hojas de sólo 1 apertura, o todas a la vez)
- Observar lo relevante que es el movimiento de una hoja (cuanto mejora si las muevo todas a la vez, o solo 1)
- Ver como se comporta una población random vs a un set de buenas soluciones
