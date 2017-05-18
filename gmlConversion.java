import java.io.*;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;

/*
*   Script for updating our current record of which
*   ICON files have been converted to GML
*
*	The generateFile function can create the GML file.
*	The writeNodes and writeEdges will build the graph.
*
*	Manual input is needed for generateFile to create
*	the file name so that it correctly corresponds with 
*	the ICON entry.
*
*	Manual input is needed for writeNodes and writeEdges
*	to parse the data file correctly.
*
*	Manual input is needed for the node and edge classes
*	to determine what type of data will be provided.
*/

public class gmlConversion {

	static String type;
	static String subtype;
	static String description;
	static String host;
	static String citation;
	static String dateGenerated;
	static String totalNodes;
	static String totalEdges;
    static String nodesize;
    static ArrayList<String> graphProperties = new ArrayList<String>();
	static int directed = 0;
    static boolean copy = false;
	//static int weighted = 0;

	/*
	*	Change these class definitions based on data provided.
	*/
	public static class node {
	    public String id;
        public String label;
        public String type;
        boolean inList;
	 };

	public static class edge {
	 	public String source;
	 	public String target;
        public String weight;
        public String label;
        boolean inList;
	 };

	static ArrayList<node> nodeList = new ArrayList<node>();
    static ArrayList<edge> edgeList = new ArrayList<edge>();

	/*
    *   The JSON dump txt file and the data file are
    *	taken as arguments
    */
    public static void main(String [] args) {
    	if (args.length > 0) {

        	File jsonDump = new File(args[0]);
        	File dataFile = new File(args[1]);

        	File gmlFile = generateFile(jsonDump);
        	createLists(dataFile);
        	writeNodes(gmlFile);
        	writeEdges(gmlFile);
        }
    }

    /*
    *	Creates GML file and fills in header information
    *	from JSON txt file.
    *
    *	Manually fill in networkName based on ICON entry name-
    *	code replaces spaces with '_' and no other punctuation than '-'
    *	-- networkName is overarching title of all graphs in that entry
    *
    *	Manually fill in graphName, same protocol as above.
    *
    *   getHeaderInfo takes care of nodesize based on powers of ten - i.e. 0-10 = n1, 11-100 = n2, 101-1000 = n3, 1,001-10,000 = n4
    */
    static File generateFile(File jsonDump) {

    	String networkName = "BookCrossing ratings (2005)";
    	String graphName = "implicit_ratings_2005";
    	String networkNameFile, graphNameFile;
    	networkNameFile = changeName(networkName);
    	graphNameFile = changeName(graphName);

        if (!copy) {
            getHeaderInfo(jsonDump, networkName, graphName);
        }

    	String filename = "gmls\\" + networkNameFile + "_" + graphNameFile + "_" + type + "_" + subtype.replaceAll(" ","_") + "_" + nodesize + ".gml";

    	try {

	    	File gmlFile = new File(filename);
	    	if (gmlFile.createNewFile()) {
		        System.out.println("File created.");
                writeHeader(gmlFile, networkName, graphName);
		    }
		    else {
                copy = true;
		    	gmlFile.delete();
		    	generateFile(jsonDump);
		    }

		    return gmlFile;

		}
		catch (IOException e) {
	      e.printStackTrace();
		}
		return jsonDump;
    }

    /*
    *	Removes punctuation from network and graph names
    *	for the filename. Replaces spaces with '_' and removes
    *	all punctuation besides '-'.
    */
    static String changeName(String networkName) {
    	networkName = networkName.replaceAll(" ","_");
        networkName = networkName.replaceAll(",","");
        networkName = networkName.replaceAll("\\(","");
        networkName = networkName.replaceAll("'","");
        networkName = networkName.replaceAll("\\;","");
        networkName = networkName.replaceAll("&_","");
        networkName = networkName.replaceAll("\\)","");
        networkName = networkName.replaceAll("\\.","");

        return networkName;
    }

    /*
    *   Gets the data needed for the header comments by parsing the JSON txt
    *   and includes: network name, graph name, description, hosted by, citation,
    *   and date generated.
    *
    *   Saves string values in public variables.
    *
    *   The graph header includes: directed, weighted, type, subtype, total nodes,
    *   and total edges.
    */
    static void getHeaderInfo(File jsonDump, String networkName, String graphName) {
        try {

            String line = null;
            FileReader fileReader = new FileReader(jsonDump.getAbsolutePath());
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            dateGenerated = dateFormat.format(cal.getTime());

            while((line = bufferedReader.readLine()) != null) {

                if(line.contains(networkName)) {
                    String descriptionLine = line;
                    for (int i = 0; i < 12; i++) {
                        descriptionLine = bufferedReader.readLine();
                        if (descriptionLine.contains("networkDomain")) {
                            type = descriptionLine.substring(descriptionLine.indexOf(": \"")+3,descriptionLine.indexOf("\","));
                        }
                        else if (descriptionLine.contains("subDomain")) {
                            subtype = descriptionLine.substring(descriptionLine.indexOf(": \"")+3,descriptionLine.indexOf("\","));
                        }
                        else if (descriptionLine.contains("description")) {
                            description = descriptionLine.substring(descriptionLine.indexOf(": \"")+3,descriptionLine.indexOf("\","));
                        }
                        else if (descriptionLine.contains("hostedBy")) {
                            if (descriptionLine.contains("null")) {
                                host = "No host data";
                            }
                            else {
                                host = descriptionLine.substring(descriptionLine.indexOf(": \"")+3,descriptionLine.indexOf("\","));
                            }
                        }
                        else if (descriptionLine.contains("citation")) {
                            citation = descriptionLine.substring(descriptionLine.indexOf(": \"")+3,descriptionLine.indexOf("\","));
                            citation = citation.replaceAll("\"","'");
                        }
                        else if (descriptionLine.contains("graphProperties")) {
                            descriptionLine = descriptionLine.substring(descriptionLine.indexOf(": \"")+3,descriptionLine.indexOf("\","));
                            System.out.println("Getting properties");
                            String[] curLine = descriptionLine.split(", ");
                            for (int j = 0; j < curLine.length; j++) {
                                if (curLine[j].contains("Directed")) {
                                    directed = 1;
                                }
                                else {
                                    graphProperties.add(curLine[j]);
                                }
                            }
                        }
                    }
                }
                else if (line.contains(graphName)) {
                    String descriptionLine = line;
                    for (int i = 0; i < 3; i++) {
                        descriptionLine = bufferedReader.readLine();
                        if (descriptionLine.contains("nodes")) {
                            totalNodes = descriptionLine.substring(descriptionLine.indexOf(": ")+2,descriptionLine.indexOf(","));
                            if (Integer.parseInt(totalNodes) <= 10) {
                                nodesize = "n1";
                            }
                            else if (Integer.parseInt(totalNodes) <= 100) {
                                nodesize = "n2";
                            }
                            else if (Integer.parseInt(totalNodes) <= 1000) {
                                nodesize = "n3";
                            }
                            else if (Integer.parseInt(totalNodes) <= 10000) {
                                nodesize = "n4";
                            }
                            else if (Integer.parseInt(totalNodes) <= 100000) {
                                nodesize = "n5";
                            }
                            else if (Integer.parseInt(totalNodes) <= 1000000) {
                                nodesize = "n6";
                            }
                            else if (Integer.parseInt(totalNodes) <= 10000000){
                                nodesize = "n7";
                            }
                            else {
                                nodesize = "n8";
                            }
                        }
                        else if (descriptionLine.contains("edges")) {
                            totalEdges = descriptionLine.substring(descriptionLine.indexOf(": ")+2,descriptionLine.indexOf(","));
                        }
                    }
                }
            } 
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    *	Creates the header comments for the GML file from getHeaderInfo 
    *   and writes to it.
    */
    static void writeHeader(File gmlFile, String networkName, String graphName) {
    	try {

    		String line = null;
    		FileWriter fileWriter = new FileWriter(gmlFile.getAbsolutePath());
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            String descriptionComments = "comments \"" + networkName + " : " + graphName + "\n";

            descriptionComments = descriptionComments + description + "\n" + host + "\n" + "Citation: " + citation + "\n" + "nodes: " + totalNodes + " edges: " + totalEdges + "\nGML generated " + dateGenerated + "\"\n";
            String graphComments = "graph \n[\ndirected " + directed + "\nproperties \""; 
            for (int i = 0; i < graphProperties.size(); i++) {
                if (i != graphProperties.size()-1) {
                    graphComments = graphComments + graphProperties.get(i) + ", ";
                }
                else {
                    graphComments = graphComments + graphProperties.get(i) + "\"";
                }
            }
            graphComments = graphComments + "\ntype \"" + type + "\"\nsubtype \"" + subtype + "\"\n";
            bufferedWriter.write(descriptionComments);
            bufferedWriter.write(graphComments);
            System.out.println("Wrote headers.");
            bufferedWriter.close();
    	}
    	catch (IOException e) {
			e.printStackTrace();
		}
    }

    /*
    *	Creates the nodeList and edgeList based on classes declared
    *	at top of file.
    *
    *	This will be the most modified piece of code depending on the
    *	network being converted.
    *
    *	Make sure to parse the data file properly to build nodes and edges.
    *	This will be different for many different files.
    *
    *	Modification should take place within the while loop.
    */
    static void createLists(File dataFile) {
    	try {

    		String dataFileName = dataFile.getAbsolutePath();
    		FileReader fileReader = new FileReader(dataFileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = null;
            int n1ID = 1;
            int n2ID = 1;

            while((line = bufferedReader.readLine()) != null) {
            	
            	if (!line.contains("User-ID")) {
                    String[]split = line.split(";");

                    //for (String id : split) {
                    //if(Integer.parseInt(split[2].replaceAll("\"","")) != 0) {
                        //System.out.println("hello");

                        node n1 = new node();
                        n1.id = Integer.toString(n1ID);
                        n1.label = split[0].replaceAll("\"", "");
                        n1.type = "userID";
                        //n1.label = split[2];
                        node n2 = new node();
                        n2.id = Integer.toString(n2ID);
                        n2.label = split[1].replaceAll("\"","");
                        n2.type = "ISBN";
                        //n2.label = split[3];

                            //are the nodes in the nodeList already?
                    		for (node n : nodeList) {
                    			if (n1.label.equals(n.label)) {
                    				n1.inList = true;
                                    n1.id = n.id;
                    			}
                                if (n2.label.equals(n.label)) {
                                    n2.inList = true;
                                    n2.id = n.id;
                                }
                                if(n2.inList && n1.inList) {
                                    break;
                                }
                            }

                            //Based on loops above, check if we should increment the ID.
                    		if (!n1.inList) {
                    			nodeList.add(n1);
                                n1ID++;
                                //System.out.println(source.id);
                    		}
                            if (!n2.inList) {
                                nodeList.add(n2);
                                n2ID++;
                                //System.out.println(source.id);
                            }

                            edge newEdge = new edge();
                            newEdge.source = n1.id;
                            newEdge.target = n2.id;
                            edgeList.add(newEdge);
                    //}
            	}
            }
    	}
    	catch (IOException e) {
			e.printStackTrace();
		}
    }

    /*
    *	Writes the nodes to the GML file.
    *
    *	For different graphs, the properties for what are
    *	written will need to change in the for loop to match
    *	the class definition.
    */
    static void writeNodes(File gmlFile) {
    	try {

    		FileWriter fileWriter = new FileWriter(gmlFile.getAbsolutePath(),true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			ArrayList<String> toWrite = new ArrayList<String>();

			for (node n : nodeList) {
				String nodeInfo = "node \n[\nid " + n.id + "\nlabel " + n.label + "\ntype " + n.type + "\n]\n";
				toWrite.add(nodeInfo);
			}
			for (String w : toWrite) {
				bufferedWriter.write(w);
			}

			System.out.println("Wrote node list.");
			bufferedWriter.close();
    	}
    	catch (IOException e) {
			e.printStackTrace();
		}
    }

    /*
    *	Writes the edges to the GML file.
    *
    *	For different graphs, the properties for what are
    *	written will need to change in the for loop to match
    *	the class definition.
    */
    static void writeEdges(File gmlFile) {
    	try {
    		FileWriter fileWriter = new FileWriter(gmlFile.getAbsolutePath(),true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			ArrayList<String> toWrite = new ArrayList<String>();

			for (edge e : edgeList) {
				String edgeInfo = "edge \n[\nsource " + e.source + "\ntarget " + e.target  + "\nweight " + e.weight + "\n]\n";
				toWrite.add(edgeInfo);
			}
			for (String w : toWrite) {
				bufferedWriter.write(w);
			}
			bufferedWriter.write("]");

			System.out.println("Wrote edge list.");
			bufferedWriter.close();
    	}
    	catch (IOException e) {
			e.printStackTrace();
		}
    }


}