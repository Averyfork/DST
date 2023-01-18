package edu.isi.karma.research.dynamic;

import com.google.common.collect.Multimap;
import edu.isi.karma.modeling.alignment.*;
import edu.isi.karma.modeling.alignment.learner.ModelLearningGraph;
import edu.isi.karma.modeling.alignment.learner.ModelLearningGraphType;
import edu.isi.karma.modeling.alignment.learner.ModelReader;
import edu.isi.karma.modeling.alignment.learner.PatternWeightSystem;
import edu.isi.karma.modeling.ontology.OntologyManager;
import edu.isi.karma.modeling.research.Params;
import edu.isi.karma.rep.alignment.*;
import edu.isi.karma.rep.alignment.SemanticType.Origin;
import edu.isi.karma.webserver.ContextParametersRegistry;
import edu.isi.karma.webserver.ServletContextParameterMap;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

import static edu.isi.karma.research.dynamic.Request.getRequestList;
import static edu.isi.karma.research.dynamic.ModelRefinement.modelCleaning;
import static edu.isi.karma.research.dynamic.Update4AddingAttributes.update_add;
import static edu.isi.karma.research.dynamic.Update4DroppingAttributes.update_remove;

public class DynamicUpdate_SemanticModels {

    private static Logger logger = LoggerFactory.getLogger(DynamicUpdate_SemanticModels.class);
    private OntologyManager ontologyManager = null;
    private GraphBuilder graphBuilder = null;
    private NodeIdFactory nodeIdFactory = null;
    private List<Node> steinerNodes = null;
    private static double avePre = 0.0;
    private static double aveRecall = 0.0;
    private static double aveF1 = 0.0;

    public DynamicUpdate_SemanticModels(OntologyManager ontologyManager,
                                        List<Node> steinerNodes) {
        if (ontologyManager == null ||
                steinerNodes == null ||
                steinerNodes.isEmpty()) {
            logger.error("cannot instanciate model learner!");
            return;
        }
        GraphBuilder gb = ModelLearningGraph.getInstance(ontologyManager, ModelLearningGraphType.Compact).getGraphBuilder();
        this.ontologyManager = ontologyManager;
        this.steinerNodes = steinerNodes;
        this.graphBuilder = cloneGraphBuilder(gb);
        this.nodeIdFactory = this.graphBuilder.getNodeIdFactory();
    }

    public DynamicUpdate_SemanticModels(GraphBuilder graphBuilder,
                                        List<Node> steinerNodes) {
        if (graphBuilder == null ||
                steinerNodes == null ||
                steinerNodes.isEmpty()) {
            logger.error("cannot instanciate model learner!");
            return;
        }
        this.ontologyManager = graphBuilder.getOntologyManager();
        this.steinerNodes = steinerNodes;
        this.graphBuilder = cloneGraphBuilder(graphBuilder);
        this.nodeIdFactory = this.graphBuilder.getNodeIdFactory();
    }

    private GraphBuilder cloneGraphBuilder(GraphBuilder graphBuilder) {

        GraphBuilder clonedGraphBuilder = null;
        if (graphBuilder == null || graphBuilder.getGraph() == null) {
            clonedGraphBuilder = new GraphBuilderTopK(this.ontologyManager, false);
        } else {
            clonedGraphBuilder = new GraphBuilderTopK(this.ontologyManager, graphBuilder.getGraph());
        }
        return clonedGraphBuilder;
    }

    public void hypothesize(boolean useCorrectTypes, int numberOfCandidates) throws Exception {
        Map<ColumnNode, ColumnNode> mappingToSourceColumns = new HashMap<ColumnNode, ColumnNode>();
        List<ColumnNode> columnNodes = new LinkedList<ColumnNode>();
        for (Node n : steinerNodes)
            if (n instanceof ColumnNode) {
                ColumnNode c = (ColumnNode) n;
                columnNodes.add(c);
                mappingToSourceColumns.put(c, c);
            }

        for (Node n : steinerNodes) {
            if (n instanceof ColumnNode) {
                ColumnNode steinerNode = (ColumnNode) n;
                List<SemanticType> candidateSemanticTypes = getCandidateSteinerSets(steinerNode, useCorrectTypes, numberOfCandidates);
                addSteinerNodeToTheGraph(steinerNode, candidateSemanticTypes);
            }
        }
    }

    private List<SemanticType> getCandidateSteinerSets(ColumnNode steinerNode, boolean useCorrectTypes, int numberOfCandidates) {

        if (steinerNode == null)
            return null;

        List<SemanticType> candidateSemanticTypes = null;

        if (!useCorrectTypes) {
            candidateSemanticTypes = steinerNode.getTopKLearnedSemanticTypes(numberOfCandidates);
        } else if (steinerNode.getSemanticTypeStatus() == ColumnSemanticTypeStatus.UserAssigned) {
            candidateSemanticTypes = steinerNode.getUserSemanticTypes();
        }

        if (candidateSemanticTypes == null) {
            logger.error("No candidate semantic type found for the column " + steinerNode.getColumnName());
            return null;
        }

        return candidateSemanticTypes;
    }


    private void addSteinerNodeToTheGraph(ColumnNode steinerNode, List<SemanticType> semanticTypes) {

        if (!this.graphBuilder.addNode(steinerNode)) return;

        if (semanticTypes == null) {
            logger.error("semantic type is null.");
            return;
        }

        for (SemanticType semanticType : semanticTypes) {

            if (semanticType == null) {
                logger.error("semantic type is null.");
                continue;

            }
            if (semanticType.getDomain() == null) {
                logger.error("semantic type does not have any domain");
                continue;
            }

            if (semanticType.getType() == null) {
                logger.error("semantic type does not have any link");
                continue;
            }

            String domainUri = semanticType.getDomain().getUri();
            String propertyUri = semanticType.getType().getUri();
            Double confidence = semanticType.getConfidenceScore();
            Origin origin = semanticType.getOrigin();

            logger.debug("semantic type: " + domainUri + "|" + propertyUri + "|" + confidence + "|" + origin);

            Set<Node> nodesWithSameUriOfDomain = this.graphBuilder.getUriToNodesMap().get(domainUri);
            if (nodesWithSameUriOfDomain == null || nodesWithSameUriOfDomain.isEmpty()) {
                String nodeId = nodeIdFactory.getNodeId(domainUri);
                Node source = new InternalNode(nodeId, new Label(domainUri));
                if (!this.graphBuilder.addNodeAndUpdate(source, null)) continue;
                String linkId = LinkIdFactory.getLinkId(propertyUri, source.getId(), steinerNode.getId());
                LabeledLink link = new DataPropertyLink(linkId, new Label(propertyUri));
                if (!this.graphBuilder.addLink(source, steinerNode, link)) continue;
                ;
            } else {
                for (Node source : nodesWithSameUriOfDomain) {
                    String linkId = LinkIdFactory.getLinkId(propertyUri, source.getId(), steinerNode.getId());
                    LabeledLink link = new DataPropertyLink(linkId, new Label(propertyUri));
                    if (!this.graphBuilder.addLink(source, steinerNode, link)) continue;
                    ;
                }
            }

        }
    }

    private static double roundDecimals(double d, int k) {
        String format = "";
        for (int i = 0; i < k; i++) format += "#";
        DecimalFormat DForm = new DecimalFormat("#." + format);
        return Double.valueOf(DForm.format(d));
    }

    @SuppressWarnings("unused")
    private static void getStatistics1(List<SemanticModel> semanticModels) {
        for (int i = 0; i < semanticModels.size(); i++) {
            SemanticModel source = semanticModels.get(i);
            int attributeCount = source.getColumnNodes().size();
            int nodeCount = source.getGraph().vertexSet().size();
            int linkCount = source.getGraph().edgeSet().size();
            int datanodeCount = 0;
            int classNodeCount = 0;
            for (Node n : source.getGraph().vertexSet()) {
                if (n instanceof InternalNode) classNodeCount++;
                if (n instanceof ColumnNode) datanodeCount++;
            }
            System.out.println(attributeCount + "\t" + nodeCount + "\t" + linkCount + "\t" + classNodeCount + "\t" + datanodeCount);

            List<ColumnNode> columnNodes = source.getColumnNodes();
            getStatistics2(columnNodes);

        }
    }

    private static void getStatistics2(List<ColumnNode> columnNodes) {

        if (columnNodes == null)
            return;

        int numberOfAttributesWhoseTypeIsFirstCRFType = 0;
        int numberOfAttributesWhoseTypeIsInCRFTypes = 0;
        for (ColumnNode cn : columnNodes) {
            List<SemanticType> userSemanticTypes = cn.getUserSemanticTypes();
            List<SemanticType> top4Suggestions = cn.getTopKLearnedSemanticTypes(4);

            for (int i = 0; i < top4Suggestions.size(); i++) {
                SemanticType st = top4Suggestions.get(i);
                if (userSemanticTypes != null) {
                    for (SemanticType t : userSemanticTypes) {
                        if (st.getModelLabelString().equalsIgnoreCase(t.getModelLabelString())) {
                            if (i == 0) numberOfAttributesWhoseTypeIsFirstCRFType++;
                            numberOfAttributesWhoseTypeIsInCRFTypes++;
                            i = top4Suggestions.size();
                            break;
                        }
                    }
                }
            }

        }

        System.out.println(numberOfAttributesWhoseTypeIsInCRFTypes + "\t" + numberOfAttributesWhoseTypeIsFirstCRFType);
    }

    public static Node cloneNode(Node v, SemanticModel currentModel, DynamicUpdate_SemanticModels modelLearner, DirectedWeightedMultigraph G) {
        DirectedWeightedMultigraph currentGraph = currentModel.getGraph();

        String nodeId = modelLearner.nodeIdFactory.getNodeId(v.getUri());
        Node node = new InternalNode(nodeId, new Label(v.getUri()));
        G.addVertex(node);

        HashSet<LabeledLink> templ = new HashSet<>();
        Map<LabeledLink, Node> mp1 = new HashMap<>();
        Map<LabeledLink, Node> mp2 = new HashMap<>();
        ObjectPropertyType objectPropertyType = null;

        Set<DefaultLink> e = G.incomingEdgesOf(v);
        for (DefaultLink dl : e) {
            String linkId = LinkIdFactory.getLinkId(dl.getUri(), dl.getSource().getId(), node.getId());
            if (dl.getType() == LinkType.ObjectPropertyLink) {
                ObjectPropertyLink ol = (ObjectPropertyLink) dl;
                if (objectPropertyType == null) objectPropertyType = ol.getObjectPropertyType();
                LabeledLink link = new ObjectPropertyLink(linkId, new Label(ol.getUri()), ol.getObjectPropertyType());
                //G.addEdge(ol.getSource(),node, link);
                templ.add(link);
                mp1.put(link, ol.getSource());
                mp2.put(link, node);
            } else if (dl.getType() != LinkType.CompactObjectPropertyLink && dl.getType() != LinkType.CompactSubClassLink) {
                LabeledLink l = (LabeledLink) dl;
                LabeledLink link = new ObjectPropertyLink(linkId, new Label(l.getUri()), objectPropertyType);
                //G.addEdge(node,l.getTarget(), link);
                templ.add(link);
                mp1.put(link, l.getSource());
                mp2.put(link, node);
            }
        }
        Set<DefaultLink> f = G.outgoingEdgesOf(v);
        for (DefaultLink dl : f) {
            String linkId = LinkIdFactory.getLinkId(dl.getUri(), node.getId(), dl.getTarget().getId());
            if (dl.getType() == LinkType.ObjectPropertyLink) {
                ObjectPropertyLink ol = (ObjectPropertyLink) dl;
                LabeledLink link = new ObjectPropertyLink(linkId, new Label(ol.getUri()), ol.getObjectPropertyType());
                //G.addEdge(node,ol.getTarget(), link);
                templ.add(link);
                mp1.put(link, node);
                mp2.put(link, ol.getTarget());

            } else if (dl.getType() != LinkType.CompactObjectPropertyLink && dl.getType() != LinkType.CompactSubClassLink) {
                LabeledLink l = (LabeledLink) dl;
                LabeledLink link = new DataPropertyLink(linkId, new Label(l.getUri()));
                //G.addEdge(node,l.getTarget(), link);
                templ.add(link);
                mp1.put(link, node);
                mp2.put(link, l.getTarget());
            }
        }

        for (DefaultLink dl : f) {
            if (dl.getSource().getUri().equals(node.getUri()) && dl.getTarget().getUri().equals(node.getUri())) {
                String linkId = LinkIdFactory.getLinkId(dl.getUri(), v.getId(), node.getId());
                if (dl.getType() == LinkType.ObjectPropertyLink) {
                    ObjectPropertyLink ol = (ObjectPropertyLink) dl;
                    LabeledLink link = new ObjectPropertyLink(linkId, new Label(ol.getUri()), ol.getObjectPropertyType());
                    G.addEdge(v, node, link);
                } else if (dl.getType() != LinkType.CompactObjectPropertyLink && dl.getType() != LinkType.CompactSubClassLink) {
                    LabeledLink link = new ObjectPropertyLink(linkId, new Label(dl.getUri()), objectPropertyType);
                    G.addEdge(v, node, link);
                }
                break;
            }
        }
        for (LabeledLink l : templ) {
            G.addEdge(mp1.get(l), mp2.get(l), l);
        }


        return node;
    }


    public static String[] test(int source, int target) throws Exception {
        String result = "";
        ServletContextParameterMap contextParameters = ContextParametersRegistry.getInstance().getDefault();
        List<SemanticModel> semanticModels =
                ModelReader.importSemanticModelsFromJsonFiles(Params.MODEL_DIR, Params.MODEL_MAIN_FILE_EXT);
        contextParameters.setParameterValue(ServletContextParameterMap.ContextParameter.USER_CONFIG_DIRECTORY, Params.ROOT_DIR + "config\\");
        List<SemanticModel> trainingData = new ArrayList<SemanticModel>();

        OntologyManager ontologyManager = new OntologyManager(contextParameters.getId());
        File ff = new File(Params.ONTOLOGY_DIR);
        File[] files = ff.listFiles();
        for (File f : files) {
            ontologyManager.doImport(f, "UTF-8");
        }
        ontologyManager.updateCache();

        ModelLearningGraph modelLearningGraph = null;
        DynamicUpdate_SemanticModels modelLearner;
        boolean useCorrectType = true;
        boolean randomModel = false;
        int numberOfCandidates = 1;
        int numberOfKnownModels;
        String[] oneResult = {};
        int i = source;
        {
            int newSourceIndex = target;
            SemanticModel newSource = semanticModels.get(newSourceIndex);
            SemanticModel currentModel = semanticModels.get(source);
            String dotresultDir = "D:\\Day_day_up\\Web-Karma-master\\vagrant\\karma\\" + Params.DATASET_NAME + "\\results\\" + currentModel.getName() + "\\result_model_dot\\";
            String jsonresultDir = "D:\\Day_day_up\\Web-Karma-master\\vagrant\\karma\\" + Params.DATASET_NAME + "\\results\\" + currentModel.getName() + "\\result_model_json\\";
            numberOfKnownModels = 27;
            while (numberOfKnownModels <= semanticModels.size() - 1) {
                trainingData.clear();
                int j = 0, count = 0;
                while (count < numberOfKnownModels) {
                    if (j != newSourceIndex) {
                        trainingData.add(semanticModels.get(j));
                        count++;
                    }
                    j++;
                }

                modelLearningGraph = ModelLearningGraph.getEmptyInstance(ontologyManager, ModelLearningGraphType.Compact);
                SemanticModel correctModel = newSource;
                List<ColumnNode> columnNodes = correctModel.getColumnNodes();

                List<Node> steinerNodes = new LinkedList<Node>(columnNodes);
                modelLearner = new DynamicUpdate_SemanticModels(ontologyManager, steinerNodes);
                //long start = System.currentTimeMillis();

                if (randomModel) {
                    modelLearner = new DynamicUpdate_SemanticModels(new GraphBuilder(ontologyManager, false), steinerNodes);
                } else {
                    for (SemanticModel sm : trainingData)
                        modelLearningGraph.addModelAndUpdate(sm, PatternWeightSystem.JWSPaperFormula);
                    modelLearner.graphBuilder = modelLearningGraph.getGraphBuilder();
                    modelLearner.nodeIdFactory = modelLearner.graphBuilder.getNodeIdFactory();

                    modelLearner.hypothesize(useCorrectType, numberOfCandidates);
                    DirectedWeightedMultigraph G = modelLearner.graphBuilder.getGraph();
                    Request R = getRequestList(currentModel, correctModel, G);
//                    String outName_G = Params.OUTPUT_DIR+ "Graph_G" + Params.GRAPHVIS_OUT_DETAILS_FILE_EXT;
//                    GraphVizUtil.exportJGraphToGraphviz(
//                            modelLearner.graphBuilder.getGraph(),
//                            "Graph_G",
//                            false,
//                            GraphVizLabelType.LocalId,
//                            GraphVizLabelType.LocalUri,
//                            true,
//                            true,
//                            outName_G
//                    );

                    Multimap<String, Node> requestList = R.getRequest_list();
                    Collection removeList = requestList.get("remove");
                    Collection addList = requestList.get("add");
                    int modifyNum = ((removeList.size()) + (addList.size()));
                    result += "modify num: " + modifyNum;
                    System.out.println("modify num: " + ((removeList.size()) + (addList.size())));
                    long start = System.currentTimeMillis();

                    for (Object o : removeList) {
                        Node n = (Node) o;
                        update_remove(n, currentModel, G);
                    }
                    Map<String, String> extratSemanticTypes = new HashMap<>();
                    for (Object o : addList) {
                        Node n = (Node) o;
                        update_add(n, currentModel, G, trainingData, modelLearner, extratSemanticTypes);
                    }

                    modelCleaning(currentModel, G);
                    long elapsedTimeMillis = System.currentTimeMillis() - start;
                    float elapsedTimeSec = elapsedTimeMillis / 1000F;
                    System.out.println("time: " + elapsedTimeSec);

                    ModelEvaluation me = currentModel.evaluate(correctModel);
                    logger.info("To " + target + "precision: " + me.getPrecision() +
                            ", recall: " + me.getRecall() +
                            ", time: " + elapsedTimeSec);

                    result += (", precision: " + me.getPrecision() +
                            ", recall: " + me.getRecall() +
                            ", time: " + elapsedTimeSec);
                    if (source != target) {
                        avePre += me.getPrecision();
                        aveRecall += me.getRecall();
                        double F1 = 2 * me.getPrecision() * me.getRecall() / (me.getPrecision() + me.getRecall());
                        aveF1 += F1;
//                        String outputPath = Params.OUTPUT_DIR;
//                        String outName = outputPath + semanticModels.get(newSourceIndex).getName() + Params.GRAPHVIS_OUT_DETAILS_FILE_EXT;
//                        GraphVizUtil.exportSemanticModelToGraphviz(
//                            currentModel,
//                            GraphVizLabelType.LocalId,
//                            GraphVizLabelType.LocalUri, false,
//                            false,
//                            outName
//                    );
                        GraphVizUtil.exportSemanticModelToGraphviz(
                                currentModel,
                                GraphVizLabelType.LocalId,
                                GraphVizLabelType.LocalUri, false,
                                false,
                                dotresultDir + semanticModels.get(newSourceIndex).getName() + Params.GRAPHVIS_OUT_DETAILS_FILE_EXT
                        );
                        currentModel.writeJson(jsonresultDir + semanticModels.get(newSourceIndex).getName() + Params.GRAPH_JSON_FILE_EXT);

                        oneResult = new String[]{semanticModels.get(source).getName(), String.valueOf(modifyNum), String.valueOf(me.getPrecision()), String.valueOf(me.getRecall()), String.valueOf(F1)};
                        //oneResult = new String[]{semanticModels.get(newSourceIndex).getName(), String.valueOf(modifyNum), String.valueOf(me.getPrecision()), String.valueOf(me.getRecall()), String.valueOf(elapsedTimeSec)};
                    }
                }
                numberOfKnownModels++;
            }
        }
        return oneResult;
    }

    public static void main(String[] args) throws Exception {
//        int sourceNum = 27;
//        int source = 27;
//        int not = source;
//        File wholePRResult = new File("D:\\Day_day_up\\Web-Karma-master\\vagrant\\karma\\"+Params.DATASET_NAME+"\\results\\WholeResultPR.csv");
//        CSVWriter wholewriter = new CSVWriter(new FileWriter(wholePRResult));
//        String[] wholeheadArr = {"Target", "Precision", "Recall", "F1"};
//        wholewriter.writeNext(wholeheadArr);
//
//        for(;source <= sourceNum; source++){
//            aveF1 = 0;
//            avePre = 0;
//            aveRecall = 0;
//            List<SemanticModel> semanticModels =
//                    ModelReader.importSemanticModelsFromJsonFiles(Params.MODEL_DIR, Params.MODEL_MAIN_FILE_EXT);
//            String prresultFileDir = "D:\\Day_day_up\\Web-Karma-master\\vagrant\\karma\\"+Params.DATASET_NAME+"\\results\\"+ semanticModels.get(source).getName()+"\\dynamic_result.csv";
//            String[] headArr = {"SourceDataSource", "ModifyAmount","Precision", "Recall", "F1"};
//            File prfile = new File(prresultFileDir);
//            if(!prfile.exists()){
//                prfile.createNewFile();
//            }
//            CSVWriter writer = new CSVWriter(new FileWriter(prfile));
//            writer.writeNext(headArr);
//
//            for (int i = 0; i <= sourceNum; i++) {
//                if(i!=source){
//                    writer.writeNext(test(i, source));
//                }
//            }
//            String[] ave = {" "," ",String.valueOf(avePre/sourceNum),String.valueOf(aveRecall/sourceNum),String.valueOf(aveF1 / sourceNum)};
//            writer.writeNext(ave);
//            writer.flush();
//            String[] wholeResult = new String[]{semanticModels.get(source).getName(),String.valueOf(avePre/sourceNum),String.valueOf(aveRecall/sourceNum),String.valueOf(aveF1 / sourceNum)};
//            wholewriter.writeNext(wholeResult);
//            wholewriter.flush();
//
//            System.out.println("average precision: " + avePre/sourceNum + "  average recall: " + aveRecall/sourceNum + "  average F1:" + aveF1/sourceNum);
//        }
        test(0, 10);
////        int source = 2;
////        for (int i = 0; i <= 28; i++) {
////            if(i!=source){
////                System.out.println(test(source, i));
////            }
////        }


    }
}