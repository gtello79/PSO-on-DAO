# DAO-PSO

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


### Definiciones
- Beamlet
- Beam
- Dose Deposition Matrix

## Proximas preguntas

## Parametros asociados y experimentos interesantes
- Observar la inicializacion de las aperturas 
- Como se comporta a medida que todas los 'plan' inicializan de la misma manera?
- Observar lo relevante que es el movimiento de las apertura (cuanto cambia si muevo todos los pares de hojas de sólo 1 apertura, o todas a la vez)
- Observar lo relevante que es el movimiento de una hoja (cuanto mejora si las muevo todas a la vez, o solo 1)
- Ver como se comporta una población random vs a un set de buenas soluciones


## Requisitos para ejecutar el algoritmo
- Tener habilitada una licencia de Gurobi, si no existe una licencia activa, el algoritmo se ejecutará únicamente bajo PSO, no habrá función de reparación.
- Tener instalado Java 11