import os
import os.path
import numpy as np
import re
from numpy import array
import subprocess
import math
import time
import pdb
import shutil
import networkx as nx
import sys
import boto3
from collections import Counter
from Bio.PDB import MMCIFParser, PDBParser, Selection, NeighborSearch, Entity

# import matplotlib.pyplot as plt

def pdb_model(structure_file, water=False):
        """Return a biopython [1] model entity from a structure file.

        Parameters
        ----------
        structure_file: string
            Path to structure file
        water: boolean (default=False)
            True to take into account waker molecules in the structure, False
            otherwise.

        Notes
        -----
        1. http://biopython.org/wiki/Biopython

        """
        accepted_formats = ['cif', 'pdb', 'ent']
        parsers = [MMCIFParser, PDBParser, PDBParser]
        protein_name, file_format = structure_file.rsplit('.', 1)

        try:
            parser = parsers[accepted_formats.index(file_format)]
            parser = parser(QUIET=True)
        except ValueError:
            raise Exception("Accepted structure files are: {}".format(
                accepted_formats))

        structure = parser.get_structure(protein_name, structure_file)
        model = structure[0]

        if not water:
            for chain in model.get_chains():
                for residue in list(chain):
                    hetero_flag = residue.id[0].strip()
                    # Empty strings evaluate to False.  Therefore hetero_flag
                    # returns False if the residue is not a water molecule.
                    if hetero_flag:
                        chain.detach_child(residue.id)
                if not list(chain):
                    model.detach_child(chain.id)

        return model

def label_residue(residue):
    """ Return a string of the label of the biopython [1] residue object.

    The label of the residue is the following:
        Chain + Position

    Parameters
    ----------
    residue: Bio.PDB.Residue.Residue
        The residue to be labeled.

    Notes
    -----
    1. http://biopython.org/wiki/Biopython

    """
    position = str(residue.id[1])
    chain = residue.parent.id

    return chain + ' ' + position  # added space deliminater for split function.

# Biopython used to create adjecency dictionary
def residue_adjacency_CO_test(model, cutoff=5, weight=True):
    """Return residue adjacency dictionary defined by cutoff distance.

    Parameters
    ----------
    model: Bio.PDB.Model
        Model created with the atomic coordinates of the protein file.

    cutoff: int or float
        Distance cutoff defining links between atoms.  Two atoms are adjacent
        if their distance is less than the given cutoff.

    See Also
    --------`````````````
    pdb_model

    """
    # Use only the Atoms specified in paper
    atom_list = Selection.unfold_entities(model, 'A')
    atoms = [atom for atom in atom_list if 'O' in atom.name or 'C' in atom.name]  # or 'C' in atom.name
    # Only looking at oxygen for cliques at the momement, for maximum clique finidng in the residue filteration setup
    # print(atoms)

    neighbor_search = NeighborSearch(atoms)
    atomic_adjacency = {}

    for atom in atoms:
        _res = label_residue(atom.get_parent())
        adjacent_atoms = []
        for adj_atom in neighbor_search.search(atom.coord, cutoff):
            _adj_res = label_residue(adj_atom.parent)
            # Adjacent atoms must be in different residues
            if _adj_res != _res:
                adjacent_atoms.append(adj_atom)
        atomic_adjacency[atom] = adjacent_atoms

    adjacency = {}

    # Create residue adjacency dictionary with string format, see
    # label_residue.
    for atom, neighbors in atomic_adjacency.items():
        residue = label_residue(atom.get_parent())
        adjacency.setdefault(residue, [])

        # Only different residues are connected by an edge (No loops).
        not_in_residue = []
        for neighbor in neighbors:
            neighbor_parent = label_residue(neighbor.get_parent())
            if neighbor_parent is not residue:
                not_in_residue.append(neighbor_parent)

        adjacency[residue].extend(not_in_residue)

    if not weight:
        return adjacency

    # Make new dictionary mapping each residue to its neighbors taking
    # into account the weight.
    weighted_adjacency = {}
    for residue in adjacency:
        counter = Counter(adjacency[residue])
        weighted_adjacency[residue] = {
            neighbor: {'weight': counter[neighbor]}
            for neighbor in counter}

    return weighted_adjacency

def topo_network(model, cutoff=4, weight=True):
    """Return the interaction network of a protein structure.

    The interaction network is defined by a distance cutoff.

    Parameters
    ----------
    model: Bio.PDB.model
        The protein structure.
    cutoff: float
        The distance cutoff defining an interaction between two nodes.
    weight: boolean
        True if atomic interactions are to be considered.
    """

    adjacency_dictionary = residue_adjacency_CO_test(model, cutoff=cutoff,
                                                     weight=weight)

    return nx.Graph(adjacency_dictionary)

class Pmolecule(object):
    """Create a Pmolecule object.

    The Pmolecule calls a number of methods for the analysis of protein
    structure. This includes the construction of the interaction network of the
    protein.

    Parameters
    ----------
    structure_file = str
        The path to the structure file of the targeted protein. Three
        structure-file formats are accepted: `pdb', `cif', and `ent'.
    water: boolean, default is False
        If false, water molecules are ignored.

    Attributes
    ----------
    model: Bio.PDB.model
        The structural model of the structure. See www.biopython.org.
    path_to_file: str
        The path to the structural file used to instantiate the class.
    """

    def __init__(self, structure_file, water=False):
        self.model = pdb_model(structure_file, water=water)
        self.path_to_file = structure_file

    def get_network(self, cutoff=5, weight=True):
        """Return the interaction network of a protein structure.

        The interaction network is defined by a distance cutoff.

        Parameters
        ----------
        model: Bio.PDB.model
            The protein structure.
        cutoff: float
            The distance cutoff defining an interaction between two nodes.
        weight: boolean
            True if atomic interactions are to be considered.
        """

        return topo_network(self.model, cutoff=cutoff, weight=weight)

    def get_pdbmodel(self):
        return self.model

class FileOperation(object):
    """ generated source for class FileOperation """

    @classmethod
    def getEntriesAsList(cls, fileName):
        """ generated source for method getEntriesAsList """
        #  Return list entries.
        #  An element of that list is one line of the file fileName.
        try:
            with open(fileName, 'r') as fil:
                entries = fil.read().split('\n')
                fil.close()
            return entries    
        except OSError as exc:
            if exc.errno == 36:
                pass
            else:
                raise  # re-raise previously caught exception
    
    @classmethod
    def write2S3(cls, file_name_arg, data_arg, folder_path_arg, typefolder_arg):
        string = data_arg

        file_name = file_name_arg
        lambda_path = "/tmp/" + file_name
        s3_path = folder_path_arg + "/"+ typefolder_arg + "/" + file_name

        with open(lambda_path, 'w+') as file:
            file.write(string)
            file.close()

        s3 = boto3.resource('s3')
        s3.meta.client.upload_file(lambda_path, 's3bucket', s3_path)
    
    @classmethod
    def saveResults(cls, entryNames, fileName, option):
        """ generated source for method saveResults """
        #  Write list entryNames in file fileName.
        #  An element in entryNames is one line in fileName.
        if option == "w":
            try:
                with open(fileName, "w") as f:
                    i = 0
                    while i < len(entryNames):
                        f.write(str(entryNames[i]) + "\n")
                        i += 1
            except Exception as e:
                pass

class ParsePDB(object):
    """ generated source for class ParsePDB """

    @classmethod
    def getLines(cls, PDBFile, field, subfield, atom):
        """ generated source for method getLines """
        #  Return a list, the element of which is a line in the file with name PDBFile
        #  that contains the specified atom in a specified field.
        returnList = []
        fileList = []
        line = ""
        i = 0
        if atom != "allatom":
            if (0 == len(atom)) or (len(atom) >= 4):
                print("error! in getLines")
        fileList = FileOperation.getEntriesAsList(PDBFile)
        while i < len(fileList):
            line = str(fileList[i])
            if field == "HETATM" and subfield != "HOH" and line.startswith(field) and line[17:20].strip() in atom:
                returnList.append(line)
            if field == "ATOM" and subfield == "Res" and line.startswith(field) and line[13:17].startswith(atom):
                returnList.append(line)
            if field == "HETATM" and subfield == "HOH" and line.startswith(field) and line[12: 14].startswith(
                    atom) and line[17: 20].startswith(subfield):
                returnList.append(line)
            if subfield == "HET":
                if field == "HETATM" and subfield != "HOH" and line.startswith(field) and line[12:14].startswith(
                        atom) and ("HOH" in line[17: 20]) == False:
                    returnList.append(line)
            if field == "ATOM" and subfield == "ResNoWater" and (
                    line.startswith("ATOM") or line.startswith("HETATM")) and atom == "allatom":
                if ("HOH" in line[17:20]) == False:
                    returnList.append(line)
            if field == "ATOM" and subfield == "Res" and (
                    line.startswith("ATOM") or line.startswith("HETATM")) and atom == "allatom":
                returnList.append(line)
            i += 1
        return returnList

    @classmethod
    def getBondedLines(cls, PDBFile, atom, bondedAtom):
        """ generated source for method getBondedLines """
        returnList = []
        carbonList = []
        oxygenList = []
        fileList = []
        line = ""
        i = 0
        if 0 == len(atom) or len(atom) > 1 or 0 == len(bondedAtom) or len(bondedAtom) > 1:
            print("error! in getBondedLines")
        atom = atom.upper()
        bondedAtom = bondedAtom.upper()
        temp = ""
        fileList = FileOperation.getEntriesAsList(PDBFile)
        while i < len(fileList):
            line = str(fileList[i])
            if ("ATOM" in line[0:4]) and (bondedAtom in line[13]):
                carbonList.append(line)
                if temp == "" and (len(oxygenList) == 0) == False:
                    k = 0
                    while k < len(oxygenList):
                        returnList.append(str(oxygenList[k]))
                        k += 1
                # print(oxygenList)
                # oxygenList = []
                temp = line[13:14]
            if ("ATOM" in line[0:4]) and (atom in line[13]):
                oxygenList.append(line)
                if temp == "C" and ((len(carbonList) == 0) == False):
                    k = 0
                    while k < len(carbonList):
                        returnList.append(str(carbonList[k]))
                        k += 1
                # print(carbonList)
                # carbonList = []
                temp = line[13:14]
            if (("TER" in line[0:4]) or ("HETATM" in line[0:6]) and (0 != len(temp))):
                k = 0
                while k < len(oxygenList):
                    returnList.append(str(oxygenList[k]))
                    k += 1
                # print(oxygenList)
                # oxygenList = []
                temp = ""
            i += 1
        return returnList
    @classmethod
    def vecCaList(cls, caListtest):
        fList = []
        for caitem in caListtest:
            txt = caitem
            re1 = '.*?'  # Non-greedy match on filler
            re2 = '([+-]?\\d*\\.\\d+)(?![-+0-9\\.])'  # Float 1
            re3 = '.*?'  # Non-greedy match on filler
            re4 = '([+-]?\\d*\\.\\d+)(?![-+0-9\\.])'  # Float 2
            re5 = '.*?'  # Non-greedy match on filler
            re6 = '([+-]?\\d*\\.\\d+)(?![-+0-9\\.])'  # Float 3
            rg = re.compile(re1 + re2 + re3 + re4 + re5 + re6, re.IGNORECASE | re.DOTALL)
            m = rg.search(txt)
            if m:
                float1 = m.group(1)
                float2 = m.group(2)
                float3 = m.group(3)
                print ("(" + float1 + ")" + "(" + float2 + ")" + "(" + float3 + ")" + "\n")
            plhld = [float(float1), float(float2), float(float3)]
            ac = array(plhld)
            fList.append(ac)
        return fList
    @classmethod
    def center_of_mass(cls,entity, geometric=False):
        """
        Returns gravitic [default] or geometric center of mass of an Entity.
        Geometric assumes all masses are equal (geometric=True)
        """

        # Structure, Model, Chain, Residue
        if isinstance(entity, Entity.Entity):
            atom_list = entity.get_atoms()
        # List of Atoms
        elif hasattr(entity, '__iter__') and [x for x in entity if x.level == 'A']:
            atom_list = entity
        else:  # Some other weirdo object
            raise ValueError("Center of Mass can only be calculated from the following objects:\n"
                             "Structure, Model, Chain, Residue, list of Atoms.")

        masses = []
        positions = [[], [], []]  # [ [X1, X2, ..] , [Y1, Y2, ...] , [Z1, Z2, ...] ]

        for atom in atom_list:
            masses.append(atom.mass)

            for i, coord in enumerate(atom.coord.tolist()):
                positions[i].append(coord)

        # If there is a single atom with undefined mass complain loudly.
        if 'ukn' in set(masses) and not geometric:
            raise ValueError("Some Atoms don't have an element assigned.\n"
                             "Try adding them manually or calculate the geometrical center of mass instead.")

        if geometric:
            return [sum(coord_list) / len(masses) for coord_list in positions]
        else:
            w_pos = [[], [], []]
            for atom_index, atom_mass in enumerate(masses):
                w_pos[0].append(positions[0][atom_index] * atom_mass)
                w_pos[1].append(positions[1][atom_index] * atom_mass)
                w_pos[2].append(positions[2][atom_index] * atom_mass)

            return [sum(coord_list) / sum(masses) for coord_list in w_pos]


    @classmethod
    def get_res_cliqs(cls,G, M):
        # Find max cliques
        renamed_cliques = []
        cliques = [clique for clique in nx.find_cliques(G) if len(clique) >= 4]
        for cliq in cliques:
            c = []
            for cli in cliq:
                res = M[str(cli.split(' ')[0])][int(cli.split(' ')[1])]
                if (res.get_resname() == 'ASP') or (res.get_resname() == 'GLU') or (res.get_resname() == 'ASN') or (
                        res.get_resname() == 'GLN') or (res.get_resname() == 'SER') or (res.get_resname() == 'THR'):
                    c.append(res)
                else:
                    continue
            renamed_cliques.append(c)
        print('Number of cliques found: ', len(cliques))
        return (renamed_cliques)

    @classmethod
    def centreCa2(cls,resatoms):
        ans = []
        for i, (atomlist) in enumerate(resatoms):
            if not atomlist:
                continue
            elif len(atomlist) > 2:
                ans.append(cls.center_of_mass(atomlist))
        return ans

    @classmethod
    def positionDistFilter(cls,reslist, caList, model):
        # Given a group of residues, for each atom in the residue return the one with the min(dist) from the any of the HETAM CA
        # First get oxygen in each residue
        limit = 1.74
        cutoff = 2.5
        resoxygrouped = []
        for resli in reslist:
            grup = []
            for res in resli:
                atom_list = res.get_list()
                resatoms = [atom for atom in atom_list if 'O' in atom.name]
                print(resatoms)
                for acvec in caList:
                    alpha_carbon = acvec
                    distances = []
                    resoxygroup = []
                    for i, (atom) in enumerate(resatoms):
                        # subtract the two position vectors
                        print(atom.get_coord())
                        diff_vector = alpha_carbon - atom.get_coord()
                        # to get a positive value we square the difference vector
                        # we then take the square root to go back to the original scale
                        distances.append(np.sqrt(np.sum(diff_vector * diff_vector)))
                        dist = np.sqrt(np.sum(diff_vector * diff_vector))
                        print(i)
                        print('The distances are: ', distances)
                        # we get the nearest atom using min(distances) or dist and see if it falls inside
                        # the cutoff
                        if (0 < dist) and (dist > limit) and (dist < cutoff):
                            grup.append(atom)
                        print(res)
            resoxygrouped.append(grup)
        return resoxygrouped
    @classmethod
    def writeresultAtoms(cls, cacmcoord):
        ans = []
        for coord in cacmcoord:
            print(coord)
            cacoord = "HETATM      CA    CA" + "           " + str(coord[0])[:-12] + "  " + str(coord[1])[:-12] + "  " + str(coord[2])[:-12]
            print(cacoord)
            ans.append(cacoord)
        return ans


class MUG(object):
    """ This is the MUG Python class """ 
    
    @classmethod
    def runPrediction(self, fileName, PDBID, folder_path_arg, typefolder_arg, metal = 'CA'):
        """ generated source for method getMetal """
        caListtest = ParsePDB.getLines(fileName, "HETATM", metal, metal)
        print(caListtest)
        caList = ParsePDB.vecCaList(caListtest)
        print(caList)
        molecule = Pmolecule(fileName)
        # Create Biograph, uses Biopython for stucture and networkx for drawing the graphs
        Graph = molecule.get_network()
        # nx.draw(Graph, with_labels = True) #, with_labels = True
        model = molecule.get_pdbmodel()
        adjmatrix = residue_adjacency_CO_test(model, cutoff=4, weight=True)
        reslist = ParsePDB.get_res_cliqs(Graph, model)
        # finding Ca binding site from remaining max clique residue
        positionDistFilterans = ParsePDB.positionDistFilter(reslist, caList, model)
        print('The length is: ', len(positionDistFilterans))
        print(positionDistFilterans)
        # Center of mass coordinates
        centreCa2ans = ParsePDB.centreCa2(positionDistFilterans)
        print(centreCa2ans)
        resultAtoms = ParsePDB.writeresultAtoms(centreCa2ans)
        #########
        fileList = []
        resultList = []
        allAtom = []
        allAtomNoWater = []
        currDirectory = os.getcwd()
        fileList = FileOperation.getEntriesAsList(fileName)
        fileName = currDirectory + "/inputdata/" + PDBID + ".pdb"
        caList = ParsePDB.getLines(fileName, "HETATM", metal, metal)
        allAtom = ParsePDB.getLines(fileName, "ATOM", "Res", "allatom")
        pdb_site_list = allAtom + resultAtoms
        FileOperation.saveResults(pdb_site_list, currDirectory + "/" + PDBID + "_site.pdb", "w")
        locfileName = PDBID + "_site.pdb"
        FileOperation.write2S3(locfileName, pdb_site_list, folder_path_arg, typefolder_arg)
        print("File: " + currDirectory + "/predictionResults/" + PDBID + "_site.pdb" + " just written")
        print('Completed!')
        resultList = []