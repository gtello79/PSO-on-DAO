import daoPlotter.classes.Beamlet as Beamlet
import sys
class Collimator:

    angles: list = []
    n_angles: int = 0
    beamlets_position: list = []
    beamlets_by_beam: dict = dict()
    beamlets_by_beam_list: list = []
    total_beamlets: int = 0
    activate_range: dict = dict()
    coordinates_by_beam: dict = dict()
    
    x: int = 0
    y: int = 0

    # Posicion local de un beamlets desde su respectivo index_beam
    # (index_angle, local_position)
    beamlets_local_position: list = []

    def __init__(self, angles, path, scenario_tag="") -> None:
        
        self.angles = angles
        self.n_angles = len(angles)
        self.total_beamlets = 0
        self.beamlets_by_beam = dict()
        self.beamlets_position = list()
        self.coordinates_by_beam = dict()
        self.activate_range = dict()
        self.scenario_tag = scenario_tag
        self.x = 0
        self.y = 0

        max_value = 0
        # Get Coordinates beam by angle
        for angle in self.angles:
            actual_angle = path.get(angle)
            try:
                with open(actual_angle, "r") as actual_file:
                    beamlets_on_beam = 0
                    for line in actual_file:
                        _, x, y = line.strip().split("\t")
                        position_x = float(x)
                        position_y = float(y)
                        index_ins = [position_x, position_y]

                        self.beamlets_position.append(index_ins)
                        beamlets_on_beam += 1

                        # Get the max position of the index
                        max_on_list = max([abs(position_x), abs(position_y)])
                        if max_on_list > max_value:
                            max_value = max_on_list
            except FileNotFoundError:
                raise FileNotFoundError(f'El archivo {actual_file} no se encuentra')

            # Set the total beamlets of the collimator
            self.total_beamlets += beamlets_on_beam

            # Annotate the total of beamlets on each beam
            self.beamlets_by_beam[angle] = beamlets_on_beam
            self.beamlets_by_beam_list.append(beamlets_on_beam)

        # Set the dimension of the collimator matrix
        dimension = int(max_value * 2) + 1
        self.x = dimension
        self.y = dimension

        # transform coordinates from Cartesian to matrices
        index_angle = 0
        index_beamlet = 0
        for angle in self.angles:
            local_position = 0
            coordinates_on_beam = list()
            beamlets_list = self.beamlets_by_beam[angle]

            # Iterate beamlets on the beam
            for _ in range(beamlets_list):
                new_x, new_y = self.beamlets_position[index_beamlet]
                new_x = int(new_x + max_value)
                new_y = int(new_y + max_value)

                self.beamlets_position[index_beamlet] = [new_x, new_y]
                coordinates_on_beam.append([new_x, new_y])
                index_beamlet += 1

                # Guarda el indice del angulo a buscar local de un beamlet 
                # con respecto al listado completo
                self.beamlets_local_position.append((index_angle, local_position))
                local_position +=1

            # Separate the coordinates by beam
            self.coordinates_by_beam[angle] = coordinates_on_beam
            
            # Continue with the next angle
            index_angle += 1

        self.initialize_coordinates()

    def initialize_coordinates(self):
        # Iterate per angle
        for angle in self.angles:
            range_list = []
            position_list = self.coordinates_by_beam.get(angle)

            # Search the rows on the collimator
            for axis_y in range(self.y):
                # get the active range of the row
                rows_cells = set(
                    [
                        beamlet_p[1]
                        for beamlet_p in position_list
                        if beamlet_p[0] == axis_y
                    ]
                )

                # annotate if the row is active
                rows_cells = list(rows_cells)
                if not rows_cells:
                    # Case where the row don't use beamlets
                    first_leaf = -1
                    second_leaf = -1
                else:
                    # Case where the row uses at least one beamlet
                    first_leaf = min(rows_cells)
                    second_leaf = max(rows_cells)

                range_list.append([first_leaf, second_leaf])

            # set the range using a list by angle
            self.activate_range.update({angle: range_list})

    def get_total_beamlets_by_beam(self, id_angle: int) -> int:
        try:
            value = self.beamlets_by_beam[id_angle]
        except (KeyError, TypeError) as e:
            print(f"ERROR AL OBTENER EL TOTAL DE BEAMLETS POR BEAM {e}")
            sys.exit(1)

        return value

    def get_active_range_by_beam(self, id_angle: str) -> list:
        if id_angle not in self.angles:
            sys.exit(1)

        return self.activate_range.get(id_angle)

    def index_to_position(self, index_beamlet, angle) -> list:
        """
            index_beamlet: Local beamlet numeration on the provided angle
            angle: Angle of the beam
        """
        try:
            x, y = self.coordinates_by_beam.get(angle)[index_beamlet]
        except (KeyError, ValueError) as e:
            print(f"Angle: {angle} Index: {index_beamlet}")
            print(f"ERROR AL OBTENER LAS COORDENADAS DEL BEAMLET {e}")
            sys.exit(1)

        return [x, y]

    # Generar getters y setters para atributos estaticos
    def get_angles(self) -> list:
        return self.angles

    def get_n_angles(self) -> int:
        return self.n_angles

    def get_beamlets_position(self) -> list:
        return self.beamlets_position

    def get_beamlets_by_beam(self) -> dict:
        return self.beamlets_by_beam

    def get_total_beamlets(self) -> int:
        return self.total_beamlets

    def get_activate_range(self) -> dict:
        return self.activate_range

    def get_coordinates_by_beam(self) -> dict:
        return self.coordinates_by_beam

    def get_x(self) -> int:
        return self.x

    def get_y(self) -> int:
        return self.y

    def set_angles(self, angles: list):
        self.angles = angles

    def set_n_angles(self, n_angles: int):
        self.n_angles = n_angles

    def set_beamlets_position(self, beamlets_position: list):
        self.beamlets_position = beamlets_position