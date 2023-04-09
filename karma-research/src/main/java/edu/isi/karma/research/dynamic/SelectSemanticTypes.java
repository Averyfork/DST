package edu.isi.karma.research.dynamic;

import edu.isi.karma.modeling.alignment.SemanticModel;
import edu.isi.karma.rep.alignment.*;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import java.util.*;

/**
 * @author A
 */
public class SelectSemanticTypes {

    public ColumnNode n;
    public Set<SemanticType> possibleSemanticType;
    public static HashMap<String, Integer> curInternalNodesQuantity;
    static HashMap<String, Integer> internalNodesQuantityLimit;
    //储存已经确定了type的columnNode
    HashMap<String, String> certainedSemanticTypes;
    //没有确定type的columnNode
    static HashMap<ColumnNode, List<SemanticType>> uncertainedSemanticTypes;
    public static HashMap<String, Integer> trainingModelFrequency;
    HashMap<String, Double> typeScore;


    public static HashMap<String, Integer> getInternalNodesQuantityLimit(final List<SemanticModel> trainingData) {
        HashMap<String, Integer> finalInternalNodesQuantityLimit = new HashMap<>();
        HashMap<String, String> finalInternalNodesConnections = new HashMap<>();
        for (SemanticModel sm : trainingData) {
            HashMap<String, Integer> singleInternalNodesQuantityLimit = new HashMap<>();
            Set<InternalNode> internalNodes = sm.getInternalNodes();
            for (InternalNode in : internalNodes) {
                String nodeUri = in.getUri();
                if (!finalInternalNodesQuantityLimit.containsKey(nodeUri)) {
                    finalInternalNodesQuantityLimit.put(nodeUri, 1);
                }
                if (!singleInternalNodesQuantityLimit.containsKey(nodeUri)) {
                    singleInternalNodesQuantityLimit.put(nodeUri, 1);
                } else {
                    int temp = singleInternalNodesQuantityLimit.get(nodeUri) + 1;
                    singleInternalNodesQuantityLimit.put(nodeUri, temp);
                }
                for (LabeledLink incomingLink : sm.getGraph().incomingEdgesOf(in)) {
                    finalInternalNodesConnections.put(nodeUri, incomingLink.getSource().getUri());
                }
            }
            for (HashMap.Entry<String, Integer> entry :
                    singleInternalNodesQuantityLimit.entrySet()) {
                if (entry.getValue() > finalInternalNodesQuantityLimit.get(entry.getKey())) {
                    finalInternalNodesQuantityLimit.put(entry.getKey(), entry.getValue());
                }
            }
        }
        for (HashMap.Entry<String, String> entry : finalInternalNodesConnections.entrySet()) {
            int temp = finalInternalNodesQuantityLimit.get(entry.getValue());
            if (finalInternalNodesQuantityLimit.get(entry.getKey()) < temp) {
                finalInternalNodesQuantityLimit.put(entry.getKey(), temp);
            }
        }
        return finalInternalNodesQuantityLimit;
    }

    public static HashMap<String, String> getSemanticTypes(Collection addList, SemanticModel currentModel,
                                                       DirectedWeightedMultigraph G,
                                                       final List<SemanticModel> trainingData,
                                                       int k) {
        HashMap<String, String> resultMap = new HashMap<>();
        //开始对数据集中的每个语义模型出现次数进行计算
        final HashMap<String, Integer> trainingModelFrequency = new HashMap<String, Integer>();
        List<String> remainedEdgeFromCurrentModel = new ArrayList<String>();
        for (SemanticModel sm : trainingData) {
            trainingModelFrequency.put(sm.getId(), 0);
        }
        for (Object o : currentModel.getGraph().edgeSet()) {
            LabeledLink l = (LabeledLink) o;
            Node tar = l.getTarget();
            if (tar instanceof ColumnNode) {
                remainedEdgeFromCurrentModel.add(l.getSource().getId() + l.getUri() +
                        ((ColumnNode) l.getTarget()).getColumnName());
            } else {
                remainedEdgeFromCurrentModel.add(l.getSource().getId() + l.getUri() + (l.getTarget()).getId());
            }
        }
        String currentModelId = currentModel.getId();
        for (Object o : G.edgeSet()) {
            DefaultLink dl = (DefaultLink) o;
            LabeledLink l = null;
            if (dl.getType() != LinkType.CompactObjectPropertyLink && dl.getType() != LinkType.CompactSubClassLink) {
                l = (LabeledLink) dl;
            } else {
                continue;
            }
            String judges = null;
            if (l.getTarget() instanceof ColumnNode) {
                judges = (l.getSource().getId() + l.getUri() + ((ColumnNode) l.getTarget()).getColumnName());
            } else {
                judges = (l.getSource().getId() + l.getUri() + (l.getTarget()).getId());
            }
            if (l.getModelIds().contains(currentModelId) && remainedEdgeFromCurrentModel.contains(judges)) {
                //如果包含currentModelId，并且这条边没有被删除，说明这条边是currentModel中的边
                for (String s : l.getModelIds()) {
                    int temp = trainingModelFrequency.get(s) + 1;
                    trainingModelFrequency.put(s, temp);
                }
            }
        }
        HashMap<String, Double> typeConfidence = new HashMap();
        //准备完后，开始进行所有属性semanticType的判定，依据coherence（权值都是100）
        //首先将所有属性的所有type加入一个列表，分别求出其modelId，以此为依据分配权重。
        Map<LabeledLink, List<String>> allSemanticTypes = new HashMap<>();
        for (Object o : addList) {
            ColumnNode n = (ColumnNode) o;
            List<SemanticType> nTypes = n.getTopKLearnedSemanticTypes(k);
            List<String> existedOntology = new ArrayList<String>();
            //用于筛除掉同source node是URI不同ID的边，如type1和type2，只需要考虑其中一个即可
            for (Object lo : G.incomingEdgesOf(n)) {
                LabeledLink l = (LabeledLink) lo;
                if (!existedOntology.contains(l.getSource().getUri())) {
                    existedOntology.add(l.getSource().getUri());
                    List<String> idList = new ArrayList<String>();
                    allSemanticTypes.put(l, idList);
                }

            }
            for (SemanticType nType : nTypes) {
                typeConfidence.put(nType.getModelLabelString() + nType.getHNodeId(), nType.getConfidenceScore() * 15);
            }
        }
        for (Map.Entry<LabeledLink, List<String>> me : allSemanticTypes.entrySet()) {
            LabeledLink key = me.getKey();
            List<String> values = me.getValue();
            String targetNodeName = ((ColumnNode) key.getTarget()).getColumnName().
                    toLowerCase().replaceAll("\\s*", "").replaceAll("_", "");
            String linkUri = key.getUri();
            String sourceUri = key.getSource().getUri();
            for (SemanticModel sm : trainingData) {
                for (ColumnNode cn : sm.getColumnNodes()) {
                    String tempTNN = cn.getColumnName().toLowerCase().
                            replaceAll("\\s*", "").replaceAll("_", "");
                    if (targetNodeName.contains(tempTNN) || tempTNN.contains(targetNodeName)) {
                        //说明此时sm中的cn和进行判断的属性是同一属性,cn有且仅有一条边，看这条边的semanticType是否是我们目前判断的semanticType
                        List<LabeledLink> cnLinks = new ArrayList<LabeledLink>(sm.getGraph().edgesOf(cn));
                        if (cnLinks.get(0).getUri().equals(linkUri) &&
                                cnLinks.get(0).getSource().getUri().equals(sourceUri)) {
                            values.add(sm.getId());
                            break;
                        }
                    }
                }
            }
        }
        List<Map.Entry<LabeledLink, List<String>>> entryList = new LinkedList<>(allSemanticTypes.entrySet());
        int sortPoint = 0;
        int threeDivPoint = entryList.size() / 3;
        Collections.sort(entryList, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                Map.Entry e1 = (Map.Entry<LabeledLink, List<String>>) o1;
                Map.Entry e2 = (Map.Entry<LabeledLink, List<String>>) o2;
                String e1type = ((LabeledLink) e1.getKey()).getSource().getUri() + "|" +
                        ((LabeledLink) e1.getKey()).getUri() + ((LabeledLink) e1.getKey()).getTarget().getId();
                String e2type = ((LabeledLink) e2.getKey()).getSource().getUri() + "|" +
                        ((LabeledLink) e2.getKey()).getUri() + ((LabeledLink) e2.getKey()).getTarget().getId();
                double cost1, cost2;
                int coherence1 = 0, coherence2 = 0;
                List<String> modelId1 = (List<String>) e1.getValue();
                List<String> modelId2 = (List<String>) e2.getValue();
                for (String s : modelId1) {
                    coherence1 += trainingModelFrequency.get(s);
                }
                for (String s : modelId2) {
                    coherence2 += trainingModelFrequency.get(s);
                }
                if (coherence1 == 0 && coherence2 == 0) {
                    coherence1 = modelId1.size();
                    coherence2 = modelId2.size();
                }
                cost1 = coherence1 + typeConfidence.get(e1type);
                cost2 = coherence2 + typeConfidence.get(e2type);
                if (cost1 != cost2) {
                    return cost1 < cost2 ? 1 : -1;
                } else {
                    return 0;
                }
            }
        });
        for (Map.Entry<LabeledLink, List<String>> me : entryList) {
            LabeledLink l = me.getKey();
            List<String> curModelIds = me.getValue();
            ColumnNode curCoNode = (ColumnNode) l.getTarget();
            InternalNode curInNode = (InternalNode) l.getSource();
            if (!resultMap.containsKey(curCoNode.getLocalId())) {
                resultMap.put(curCoNode.getLocalId(), curInNode.getUri() + l.getUri());
                //此时更新语义模型频率
                {
                    for (String id : curModelIds) {
                        int temp = trainingModelFrequency.get(id) + 1;
                        trainingModelFrequency.put(id, temp);
                    }
                }
            }
        }
        return resultMap;
    }

    public static HashMap<String, Integer> getCurrentInternalNodesQuantity(SemanticModel currentModel){
        HashMap<String, Integer> curInternalNodesQuantity = new HashMap<>();
        for (InternalNode in : currentModel.getInternalNodes()) {
            if (!curInternalNodesQuantity.containsKey(in.getId())) {
                curInternalNodesQuantity.put(in.getId(), 1);
            } else {
                curInternalNodesQuantity.put(in.getId(),
                        curInternalNodesQuantity.get(in.getId()) + 1);
            }
        }
        return curInternalNodesQuantity;
    }

    public static HashMap<String, Integer> calculateTrainingModelFrequency(SemanticModel currentModel,
                                                                           final List<SemanticModel> trainingData,
                                                                           DirectedWeightedMultigraph G){
        List<String> remainedEdgeFromCurrentModel = new ArrayList<String>();
        final HashMap<String, Integer> trainingModelFrequency = new HashMap<String, Integer>();

        for (SemanticModel sm : trainingData) {
            trainingModelFrequency.put(sm.getId(), 0);
        }
        for (Object o : currentModel.getGraph().edgeSet()) {
            LabeledLink l = (LabeledLink) o;
            Node tar = l.getTarget();
            if (tar instanceof ColumnNode) {
                remainedEdgeFromCurrentModel.add(l.getSource().getId() + l.getUri() +
                        ((ColumnNode) l.getTarget()).getColumnName());
            } else {
                remainedEdgeFromCurrentModel.add(l.getSource().getId() + l.getUri() + (l.getTarget()).getId());
            }
        }
        String currentModelId = currentModel.getId();
        for (Object o : G.edgeSet()) {
            DefaultLink dl = (DefaultLink) o;
            LabeledLink l = null;
            if (dl.getType() != LinkType.CompactObjectPropertyLink && dl.getType() != LinkType.CompactSubClassLink) {
                l = (LabeledLink) dl;
            } else {
                continue;
            }
            String judges = null;
            if (l.getTarget() instanceof ColumnNode) {
                judges = (l.getSource().getId() + l.getUri() + ((ColumnNode) l.getTarget()).getColumnName());
            } else {
                judges = (l.getSource().getId() + l.getUri() + (l.getTarget()).getId());
            }
            if (l.getModelIds().contains(currentModelId) && remainedEdgeFromCurrentModel.contains(judges)) {
                //如果包含currentModelId，并且这条边没有被删除，说明这条边是currentModel中的边
                for (String s : l.getModelIds()) {
                    int temp = trainingModelFrequency.get(s) + 1;
                    trainingModelFrequency.put(s, temp);
                }
            }
        }
        return trainingModelFrequency;
    }


    public static HashMap<ColumnNode, List<SemanticType>> getSemanticTypes1(List<ColumnNode> addList, SemanticModel currentModel,
                                                                            DirectedWeightedMultigraph G,
                                                                            final List<SemanticModel> trainingData,
                                                                            int numberOfCandidates) {

        SelectSemanticTypes sST = new SelectSemanticTypes();
        //储存目前的语义模型中各个类节点的个数
        curInternalNodesQuantity = getCurrentInternalNodesQuantity(currentModel);
        //计算训练集中各个类节点的上限个数
        internalNodesQuantityLimit = getInternalNodesQuantityLimit(trainingData);
        //开始对数据集中的每个语义模型出现次数进行计算,目的是给予训练集中不同语义模型不同的权重
        trainingModelFrequency = calculateTrainingModelFrequency(currentModel,trainingData,G);
        //用于记录那些Type不确定的column node的id
        for (ColumnNode n : addList) {
            HashMap<LabeledLink, List<String>> nodeSemanticTypes = new HashMap<>();
            List<SemanticType> nTypes = n.getTopKLearnedSemanticTypes(numberOfCandidates);
            String columnNodeId = n.getLocalId();
            double i = 0, aveConfidence = 0;
            for (SemanticType nType : nTypes) {
                aveConfidence += nType.getConfidenceScore();
            }
            aveConfidence /= nTypes.size();
            for (SemanticType nType : nTypes) {
                //typeScore.put(nType.getModelLabelString() + nType.getHNodeId(), nType.getConfidenceScore() * 15);
                //此处进行判断，confidence最高的两个默认加入，之后按照平均值，删除confidence过于低的语义类型
                if (i < 2) {
                    sST.typeScore.put(nType.getDomain().getUri()+nType.getType().getUri(),
                            nType.getConfidenceScore());
                } else {
                    if (nType.getConfidenceScore() > aveConfidence) {
                        sST.typeScore.put(nType.getDomain().getUri()+nType.getType().getUri(),
                                nType.getConfidenceScore());
                    }
                }
                i++;
            }

            List<String> existedOntology = new ArrayList<String>();
            //用于筛除掉同source node是URI不同ID的边，如type1和type2，只需要考虑其中一个即可
            for (Object lo : G.incomingEdgesOf(n)) {
                LabeledLink l = (LabeledLink) lo;
                String typeString = l.getSource().getUri()+l.getLabel().getUri();
                if (!existedOntology.contains(typeString) &&
                        sST.typeScore.containsKey(typeString)){
                    existedOntology.add(typeString);
                    List<String> idList = new ArrayList<String>();
                    nodeSemanticTypes.put(l, idList);
                }
            }
            //计算训练集中，此semanticType出现在哪些模型中，不考虑columnNode
            for (Map.Entry<LabeledLink, List<String>> me : nodeSemanticTypes.entrySet()) {
                LabeledLink key = me.getKey();
                List<String> values = me.getValue();
                String targetNodeName = ((ColumnNode) key.getTarget()).getColumnName().
                        toLowerCase().replaceAll("\\s*", "").replaceAll("_", "");
                String linkUri = key.getUri();
                String sourceUri = key.getSource().getUri();
                for (SemanticModel sm : trainingData) {
                    if(sm.getInternalNodes().contains(key.getSource())){
                        for(LabeledLink ll : sm.getGraph().outgoingEdgesOf(key.getSource())){
                            if (ll.getUri().equals(linkUri)){
                                values.add(sm.getId());
                            }
                        }
                    }
                }
            }
            List<Map.Entry<LabeledLink, List<String>>> entryList = new LinkedList<>(nodeSemanticTypes.entrySet());
            Collections.sort(entryList, new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    Map.Entry e1 = (Map.Entry<LabeledLink, List<String>>) o1;
                    Map.Entry e2 = (Map.Entry<LabeledLink, List<String>>) o2;
                    String e1type = ((LabeledLink) e1.getKey()).getSource().getUri()+((LabeledLink) e1.getKey()).getUri();
                    String e2type = ((LabeledLink) e2.getKey()).getSource().getUri()+((LabeledLink) e2.getKey()).getUri();
                    double cost1, cost2;
                    int coherence1 = 0, coherence2 = 0;
                    List<String> modelId1 = (List<String>) e1.getValue();
                    List<String> modelId2 = (List<String>) e2.getValue();
                    for (String s : modelId1) {
                        coherence1 += trainingModelFrequency.get(s);
                    }
                    for (String s : modelId2) {
                        coherence2 += trainingModelFrequency.get(s);
                    }
                    if (coherence1 == 0 && coherence2 == 0) {
                        coherence1 = modelId1.size();
                        coherence2 = modelId2.size();
                    }
                    cost1 = coherence1 + sST.typeScore.get(e1type) * 10;
                    cost2 = coherence2 + sST.typeScore.get(e2type) * 10;
                    if (cost1 != cost2) {
                        return cost1 < cost2 ? 1 : -1;
                    } else {
                        return 0;
                    }
                }
            });

            Iterator<HashMap.Entry<LabeledLink, List<String>>> it = entryList.iterator();
            List<HashMap.Entry<LabeledLink, List<String>>> possibleTypes = new ArrayList<>();
            int possibleNum = 0;
            while (it.hasNext()) {
                HashMap.Entry<LabeledLink, List<String>> me = it.next();
                LabeledLink l = me.getKey();
                List<String> curModelIds = me.getValue();
                InternalNode curInNode = (InternalNode) l.getSource();
                int currentQuantity = 0;
                //getId带数字标号，getUri()则不带
                if (curInternalNodesQuantity.containsKey(curInNode.getUri())) {
                    currentQuantity = curInternalNodesQuantity.get(curInNode.getUri());
                }
                int maxQuantity = internalNodesQuantityLimit.get(curInNode.getUri());
                if (!sST.certainedSemanticTypes.containsKey(columnNodeId)) {
                    if (currentQuantity < maxQuantity) {
                        possibleNum++;
                        possibleTypes.add(me);
                    }
                    //没有后续的type的话，判断是否能唯一确定semanticType
                    if (it.hasNext()) {
                        continue;
                    } else {
                        if (possibleNum == 0) {
                            uncertainedSemanticTypes.put(n,new ArrayList<>());
                            for(SemanticType nType : nTypes){
                                if(sST.typeScore.containsKey(nType.getDomain().getUri()+nType.getType().getUri())){
                                    uncertainedSemanticTypes.get(nType).add(nType);
                                }
                            }

                        } else if (possibleNum == 1) {
                            List<String> resultInNodeIds = possibleTypes.get(0).getValue();
                            LabeledLink resultLink = possibleTypes.get(0).getKey();
                            sST.certainedSemanticTypes.put(columnNodeId, resultLink.getSource().getUri() +
                                    resultLink.getUri());
                            curInternalNodesQuantity.put(resultLink.getSource().getId(),
                                    currentQuantity + 1);
                            //此时更新语义模型频率
                            for (String id : resultInNodeIds) {
                                int temp = trainingModelFrequency.get(id) + 1;
                                trainingModelFrequency.put(id, temp);
                            }
                        } else {

                        }
                    }
                }
            }
        }

        return uncertainedSemanticTypes;
    }
}
