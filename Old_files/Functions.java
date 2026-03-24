import java.io.*;
import java.util.ArrayList;

import javax.swing.JOptionPane;

public class Functions {

    static Element [][] grid;
    static ArrayList<Element> edge;
    static ArrayList<Element> corner;
    static ArrayList<Element> inside;

    public static boolean Read_doc(String filename, Puzzle puzzle) {

        ArrayList<Element> pieceList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {

            String size [] = reader.readLine().split(" ");
            int width = Integer.parseInt(size[0]);
            int height = Integer.parseInt(size[1]);
            int nb_lines = width * height;
            puzzle.setHeight(height);
            puzzle.setWidth(width);

            char [] tableCaracteres = new char[4];

            String line = "";

            while ((line = reader.readLine()) != null && pieceList.size() < nb_lines){
                System.out.println(line.length());
                if (line.length() >= 4) {
                    for (int j = 0; j < 4; j++) {
                        tableCaracteres[j] = line.charAt(j);
                        if(tableCaracteres[j] != 'F' && tableCaracteres[j] != 'P' && tableCaracteres[j] != 'B'){
                            System.err.println("Incorrect character in file; " + filename);
                            return false;
                        }
                    }
                    pieceList.add(new Element(tableCaracteres));
                } else {
                    System.err.println("File is empty or has incorrect format; " + filename);
                    return false;
                }
            }
            reader.close();

            if(pieceList.size() < nb_lines){
                System.err.println("Not sufficient Elements according size of puzzle; " + filename);
                return false;
            }

            puzzle.setList(pieceList);

        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + filename);
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            System.err.println("Error while file reading; " + filename);
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public static boolean solve_puzzle(Puzzle puzzle){

        int size_puzzle = puzzle.getList().size();
        int height = puzzle.getHeight();
        int width = puzzle.getWidth();

        //Check if there are enough corners and edges to make a puzzle + split into corner, edge and inside elements
        int counter_edges = 0;
        int counter_corners = 0;
        int min_edges = 2*width + 2*(height-2) - 4;

        for (int i = 0; i < size_puzzle; i++) {
            Element e = puzzle.getList().get(i);
            if(Element.check_corner(e)){
                counter_corners++;
                corner.add(e);
            }
            else if(Element.check_edge(e)){
                counter_edges++;
                edge.add(e);
            } else {
                inside.add(e);
            }
        }
        if(counter_corners < 4){
            JOptionPane.showInternalMessageDialog(null, "There are less than 4 corner elements. Found: " + counter_corners,
					"Error", JOptionPane.ERROR_MESSAGE);
            //System.err.println("There are less than 4 corner elements. Found: " + counter_corners);
            return false;
        }
        if(counter_edges < min_edges){
            JOptionPane.showInternalMessageDialog(null, "There are not enough edge elements. Need: " + min_edges + ", Found: " + counter_edges,
					"Error", JOptionPane.ERROR_MESSAGE);
            //System.err.println("There are not enough edge elements. Need: " + min_edges + ", Found: " + counter_edges);
            return false;
        }

        //fill out the outside of puzzle
        if(!fill_outside(edge, corner)){
            return false;
        }

        //fill out the inside of puzzle
        if(!fill_inside(puzzle, inside, 1, 1)){
            return false;
        }
        return false;
    }

    private static boolean fill_outside(ArrayList<Element> edge, ArrayList<Element> corner){

        
        return true;
    }

    //top to bottom, left to right, recursif
    private static boolean fill_inside(Puzzle puzzle, ArrayList<Element> inside, int row, int col){

        int height = puzzle.getHeight();
        int width = puzzle.getWidth();
        
        // If we've filled all rows, solution found
        if(row == height - 1) {
            return true;
        }
        
        // Move to next cell
        int nextRow = row;
        int nextCol = col + 1;
        if(nextCol == width - 1) {
            nextRow = row + 1;
            nextCol = 1;
        }

        for (int i = 0; i < inside.size(); i++) {

            Element e = inside.get(i);
            if(puzzle.getUsed(i) == false){
                
                //Check top
                int topNeighbour_index = puzzle.getPlacement(row-1, col).getIndex();
                Element topNeighbour_e = puzzle.getList().get(topNeighbour_index);
                if(e.check_neighbour(topNeighbour_e, 0)){ //0 for top direction

                }
            }
            

            
        }

        if(fill_inside(puzzle, inside, nextRow, nextCol)) {
                return true;
        }
        
        puzzle.setPlacement(null, row, col);
        puzzle.setUsed(e.getIndex(), false);
    }

}