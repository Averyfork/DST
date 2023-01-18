/*******************************************************************************
 * Copyright 2012 University of Southern California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This code was developed by the Information Integration Group as part 
 * of the Karma project at the Information Sciences Institute of the 
 * University of Southern California.  For more information, publications, 
 * and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/

package edu.isi.karma.modeling.research;

import java.io.File;

// TODO this needs to be refactored into a properties file.
public class Params {

	public static boolean RESEARCH_MODE = true;
//	public static String DATASET_NAME = "museum-new-crm-semtyper";
//	public static String DATASET_NAME = "museum-new-edm-semtyper";
	public static String DATASET_NAME = "museum-new-crm-dgl";
//	public static String DATASET_NAME = "museum-new-edm-dgl";
//	public static String DATASET_NAME = "museum-29-edm";
//	public static String DATASET_NAME = "weapon-lod";
//	public static String DATASET_NAME = "museum-29-crm";

	
	public static String ROOT_DIR = "D:"+ File.separator+"Day_day_up"+File.separator+"Web-Karma-master"+File.separator+"vagrant"+File.separator+"karma"+File.separator + DATASET_NAME + File.separator;
	
	public static String ONTOLOGY_DIR = ROOT_DIR + "preloaded-ontologies"+File.separator;
	
	public static String OUTPUT_DIR = ROOT_DIR + "output"+File.separator;

	public static String GRAPHS_DIR = ROOT_DIR + "alignment-graph"+File.separator;
	public static String MODEL_DIR = ROOT_DIR + "models-json"+File.separator;
	public static String GRAPHVIS_DIR = ROOT_DIR + "models-graphviz"+File.separator;
	public static String SOURCE_DIR = ROOT_DIR + "sources"+File.separator;
	public static String R2RML_DIR = ROOT_DIR + "models-r2rml"+File.separator;
	public static String RESULTS_DIR = ROOT_DIR + "results"+File.separator;
	
	public static String GRAPH_JSON_FILE_EXT = ".graph.json";
	public static String GRAPH_GRAPHVIZ_FILE_EXT = ".dot";

	public static String MODEL_NEW_FILE_EXT = ".json";
	public static String MODEL_MAIN_FILE_EXT = ".model.json";

	public static String GRAPHVIS_MAIN_FILE_EXT = ".model.dot";
	public static String GRAPHVIS_OUT_FILE_EXT = ".out.dot";
	public static String GRAPHVIS_OUT_DETAILS_FILE_EXT = ".out.details.dot";

	public static String LOD_DIR = ROOT_DIR + "lod"+File.separator;
	public static String PATTERNS_INPUT_DIR = "patterns"+File.separator+"input"+File.separator;
	public static String PATTERNS_OUTPUT_DIR = "patterns"+File.separator+"output"+File.separator;
	public static String LOD_OBJECT_PROPERIES_FILE = "object-properties.csv";
	public static String LOD_DATA_PROPERIES_FILE = "data-properties.csv";
	
}
