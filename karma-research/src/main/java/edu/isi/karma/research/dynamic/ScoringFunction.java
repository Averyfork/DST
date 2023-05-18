package edu.isi.karma.research.dynamic;

import edu.isi.karma.modeling.alignment.SemanticModel;
import edu.isi.karma.rep.alignment.ColumnNode;
import edu.isi.karma.rep.alignment.InternalNode;
import edu.isi.karma.rep.alignment.LabeledLink;
import edu.isi.karma.rep.alignment.Node;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.xm.Similarity;

import java.util.*;

import static edu.isi.karma.research.dynamic.SelectSemanticTypes.getCurrentInternalNodesQuantity;

/**
 * @author chl
 */
public class ScoringFunction {


    public static double getNameSimilarity(String s1, String s2) {
        double score = 0.0;
        double pinYinScore = Similarity.pinyinSimilarity(s1, s2);
        double charBasedScore = Similarity.charBasedSimilarity(s1, s2);
        score = 2 * pinYinScore * charBasedScore / (pinYinScore + charBasedScore);
        return score;
    }

    public static boolean judgeSubGraph(SemanticModel currentModel, InternalNode judgingInternalNode,
                                        ArrayList<String> linksTriples) {
        DirectedWeightedMultigraph<Node, LabeledLink> currentGraph = currentModel.getGraph();
        boolean flag = false;
        for (LabeledLink ll : currentGraph.outgoingEdgesOf(judgingInternalNode)) {
            if (linksTriples.contains(ll.getTarget().getUri() + ll.getUri() + ll.getSource().getUri())) {
                flag = true;
                if (ll.getTarget() instanceof InternalNode) {
                    flag = judgeSubGraph(currentModel, (InternalNode) ll.getTarget(), linksTriples);
                    if (!flag) {
                        return false;
                    }
                }
            }
        }
        return flag;
    }

    public static double score4InternalNode(SemanticModel currentModel, InternalNode scoreNode, ColumnNode att,
                                            ArrayList<LabeledLink> links) {
        double score = 0.0;
        double nameSimilarityScore = 0.0;
        double weightScore = 0.0;
        double coherenceScore = 0.0;
        double subGraphScore = 0.0;
        String scoreNodeUri = scoreNode.getUri();
        String attUri = att.getUri();
        int linkNum = links.size();
        DirectedWeightedMultigraph<Node, LabeledLink> currentGraph = currentModel.getGraph();

        double nameSimilarity1 = getNameSimilarity(scoreNodeUri, attUri);
        double nameSimilarity2 = 0.0;
        for (LabeledLink ll : links) {
            weightScore += ll.getWeight();
            coherenceScore += ll.getSource().getModelIds().size();
            nameSimilarity2 += getNameSimilarity(attUri, ll.getSource().getUri());
            nameSimilarity2 += getNameSimilarity(attUri, ll.getUri());
        }
        nameSimilarity2 = nameSimilarity2 / (2 * linkNum);
        nameSimilarityScore = nameSimilarity1 + nameSimilarity2;
        weightScore = weightScore / linkNum;
        coherenceScore = coherenceScore / linkNum;

        if (linkNum == 1) {
            for (LabeledLink ll : currentGraph.edgeSet()) {
                if (ll.getUri().equals(links.get(0).getUri()) &&
                        ll.getSource().getUri().equals(scoreNodeUri)) {
                    subGraphScore += 1;
                }
            }
        } else {
            ArrayList<String> linksTriples = new ArrayList<>();
            for (LabeledLink ll : links) {
                linksTriples.add(ll.getTarget().getUri() + ll.getUri() + ll.getSource().getUri());
            }
            for (InternalNode in : currentModel.getInternalNodes()) {
                if (in.getUri().equals(scoreNodeUri)) {
                    if (judgeSubGraph(currentModel, in, linksTriples)) {
                        subGraphScore += 1;
                    }
                }
            }
        }

        score = nameSimilarityScore + weightScore + coherenceScore + subGraphScore;
        return score;
    }

    public static double score4SemanticModel(SemanticModel currentModel,
                                             DirectedWeightedMultigraph<Node, LabeledLink> G,
                                             HashMap<String, Integer> internalNodesQuantityLimit) {
        double score = 0.0;
        Set<InternalNode> internalNodes = currentModel.getInternalNodes();
        List<ColumnNode> clumnNodes = currentModel.getColumnNodes();
        //计算模型的属性与其相连的类节点之间的相似度分数(包含了coherence，weight，subGraphScore)
        double internalNodeScore = 0.0;
        for (ColumnNode cn : clumnNodes) {
            Object[] cnLink = currentModel.getGraph().incomingEdgesOf(cn).toArray();
            ArrayList<LabeledLink> links = new ArrayList<>();
            InternalNode cnInternalNode = (InternalNode) ((LabeledLink) cnLink[0]).getSource();
            links.add((LabeledLink) cnLink[0]);
            internalNodeScore += score4InternalNode(currentModel, cnInternalNode, cn, links);
        }
        //计算模型QuantityLimit分数
        double quantityLimitScore = 0.0;
        HashMap<String, Integer> curNodesQuantity = getCurrentInternalNodesQuantity(currentModel);
        for(Map.Entry<String , Integer> entry : curNodesQuantity.entrySet()){
            if(entry.getValue() > internalNodesQuantityLimit.get(entry.getKey())){
                quantityLimitScore -= 1;
            }
        }
        score = internalNodeScore + quantityLimitScore;
        return score;
    }
}