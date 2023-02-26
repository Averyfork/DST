package edu.isi.karma.research.dynamic;

import edu.isi.karma.modeling.alignment.LinkIdFactory;
import edu.isi.karma.modeling.alignment.SemanticModel;
import edu.isi.karma.rep.alignment.*;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import java.util.*;

import static edu.isi.karma.research.dynamic.DynamicUpdateUtils.judgeRepeatable;
import static edu.isi.karma.research.dynamic.DynamicUpdate_SemanticModels.cloneNode;

public class Update4AddingAttributes {

    public static Node find_edge_without_label(Node inode, Node v, LabeledLink l,
                                               SemanticModel currentModel,
                                               DirectedWeightedMultigraph G) {
        DirectedWeightedMultigraph currentGraph = currentModel.getGraph();
        for (Object n : currentGraph.vertexSet()) {
            Node nn = (Node) n;
            if (nn.getId() != inode.getId() && nn.getUri().equals(inode.getUri()) && nn.getId() != v.getId()) {
                int flag = 0;
                for (Object l0 : currentGraph.edgesOf(nn)) {
                    if (((LabeledLink) l0).getUri() == l.getUri()) {
                        flag = 1;
                        break;
                    }
                }
                if (flag == 0) {
                    return nn;
                }
            }
        }
        return null;
    }

    public static HashSet<LabeledLink> findAllEdges(Node n1, Node n2, DirectedWeightedMultigraph G) {
        HashSet<LabeledLink> alledges = new HashSet<>();

        Map<Node, Integer> vis1 = new HashMap<>();
        Map<Node, Integer> vis2 = new HashMap<>();
        Map<LabeledLink, Integer> mark1 = new HashMap<>();
        Map<LabeledLink, Integer> mark2 = new HashMap<>();

        Stack<Node> mainStack = new Stack<>();
        mainStack.push(n1);
        vis1.put(n1, 1);
        while (!mainStack.empty()) {
            Node tpnode = mainStack.pop();
            vis1.put(tpnode, 1);
            Set<LabeledLink> nodeSet = G.edgesOf(tpnode);
            for (LabeledLink l : nodeSet) {
                if (!vis1.containsKey(l.getTarget())) {
                    vis1.put(l.getTarget(), 1);
                    mainStack.push(l.getTarget());
                    if (!mark1.containsKey(l)) {
                        mark1.put(l, 1);
                    }
                }
                if (!vis1.containsKey(l.getSource())) {
                    vis1.put(l.getSource(), 1);
                    mainStack.push(l.getSource());
                    if (!mark1.containsKey(l)) {
                        mark1.put(l, 0);
                    }
                }
            }
        }
        mainStack.push(n2);
        vis2.put(n2, 1);
        while (!mainStack.empty()) {
            Node tpnode = mainStack.pop();
            vis2.put(tpnode, 1);
            Set<LabeledLink> nodeSet = G.edgesOf(tpnode);
            for (LabeledLink l : nodeSet) {
                if (!vis2.containsKey(l.getSource())) {
                    vis2.put(l.getSource(), 1);
                    mainStack.push(l.getSource());
                    if (mark1.containsKey(l) && !mark2.containsKey(l) && mark1.get(l) == 1) {
                        alledges.add(l);
                        mark2.put(l, 0);
                    }
                }
                if (!vis2.containsKey(l.getTarget())) {
                    vis2.put(l.getTarget(), 1);
                    mainStack.push(l.getTarget());
                    if (mark1.containsKey(l) && !mark2.containsKey(l) && mark1.get(l) == 0) {
                        alledges.add(l);
                        mark2.put(l, 1);
                    }
                }
            }
        }


        return alledges;

    }

    public static <labeledlink> void update_add(Node u, SemanticModel currentModel, DirectedWeightedMultigraph G,
                                                final List<SemanticModel> trainingData,
                                                DynamicUpdate_SemanticModels modelLearner,
                                                Map<String, String> extratSemanticTypes,
                                                int numberOfCandidates) throws Exception {
        HashSet<LabeledLink> temp_U = new HashSet<>();
        List<InternalNode> currentInternalNodes = new ArrayList<>(currentModel.getInternalNodes());
        DirectedWeightedMultigraph currentGraph = currentModel.getGraph();
        String targetAttributeId = ((ColumnNode) u).getId();
        Node v = null;
        LabeledLink ll = null;
        List<LabeledLink> eu = new ArrayList<>();

        if (numberOfCandidates == 1) {
            eu = new ArrayList<>(G.incomingEdgesOf(u));
            if (eu.size() == 0) {
                return;
            }
        } else if (numberOfCandidates == 4) {
            List<LabeledLink> teu = new ArrayList<>(G.incomingEdgesOf(u));
            eu = new ArrayList<>();
            if (teu.isEmpty()) {
                return;
            }
            String chosenClassNodeUri = extratSemanticTypes.get(targetAttributeId);
            for (LabeledLink l : teu) {
                if ((l.getSource().getUri() + l.getUri()).equals(chosenClassNodeUri)) {
                    eu.add(l);
                }
            }
        }

        String linkedInternalNodeUri = eu.get(0).getSource().getUri();
        String linkUri = eu.get(0).getUri();

        boolean isAllowRepeat = judgeRepeatable(linkedInternalNodeUri, linkUri, trainingData);
        if (eu.size() == 1) {

            ll = eu.get(0);
            v = ll.getSource();
            if (!isAllowRepeat) {

                if (currentGraph.containsVertex(v)) {
                    for (Object l : currentGraph.edgesOf(v)) {
                        LabeledLink tl = (LabeledLink) l;
                        if (tl.getUri() == ll.getUri()) {
                            int mark = 0;
                            for (Object n : G.vertexSet()) {
                                Node nn = (Node) n;
                                if (nn.getId() != v.getId() && nn.getUri().equals(v.getUri())) {

                                    int flag = 0;
                                    if (currentGraph.containsVertex(nn)) {
                                        for (Object l0 : currentGraph.edgesOf(nn)) {
                                            if (((LabeledLink) l0).getUri() == ll.getUri()) {
                                                flag = 1;
                                                break;
                                            }
                                        }
                                    }
                                    if (flag == 0) {
                                        mark = 1;
                                        v = nn;
                                        String linkId = LinkIdFactory.getLinkId(ll.getUri(), v.getId(), u.getId());
                                        LabeledLink link = new DataPropertyLink(linkId, new Label(ll.getUri()));
                                        G.addEdge(v, u, link);
                                        ll = link;
                                        break;
                                    }
                                }
                                if (mark == 1) {
                                    break;
                                }
                            }
                            if (mark == 0) {
                                v = cloneNode(v, currentModel, modelLearner, G);
                                String linkId = LinkIdFactory.getLinkId(ll.getUri(), v.getId(), u.getId());
                                LabeledLink link = new DataPropertyLink(linkId, new Label(ll.getUri()));
                                G.addEdge(v, u, link);
                                ll = link;
                                break;
                            }
                        }
                    }
                }
            }
        } else if (eu.size() > 1) {

            final String targetNodeName = ((ColumnNode) u).getColumnName().toLowerCase();
            Collections.sort(eu, new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {

                    String s1 = ((LabeledLink) o1).getSource().getId();
                    String s2 = ((LabeledLink) o2).getSource().getId();
                    int coherence1 = 0;
                    int coherence2 = 0;
                    if (coherence1 == 0 && coherence2 == 0) {
                        coherence1 = ((LabeledLink) o1).getSource().getModelIds().size();
                        coherence2 = ((LabeledLink) o2).getSource().getModelIds().size();
                    }
                    if (coherence1 == 0 && coherence2 == 0) {
                        //前面modelid从大到小，但是判断不出来的时候，直接看标号则是从小到大，因此我们取负数
                        coherence1 = (int) (((LabeledLink) o1).getSource().getId().charAt(((LabeledLink) o1).getSource().getId().length() - 1)) - (int) ('0');
                        coherence2 = (int) (((LabeledLink) o2).getSource().getId().charAt(((LabeledLink) o2).getSource().getId().length() - 1)) - (int) ('0');
                        coherence1 *= (-1);
                        coherence2 *= (-1);
                    }
                    if (coherence1 != coherence2) {
                        return coherence1 < coherence2 ? 1 : -1;
                    } else {
                        return 0;
                    }
                }
            });
            if (!eu.isEmpty()) {
                boolean isInternalNodeEnough = false;
                for (LabeledLink l : eu) {
                    boolean isHaveLink = false;
                    String s = l.getUri();
                    String NodeId = l.getSource().getId();
                    Node sourceNode = null;
                    for (Node n : currentInternalNodes) {
                        if (n.getId().equals(NodeId)) {
                            sourceNode = n;
                            break;
                        }
                    }
                    if (sourceNode != null) {
                        for (LabeledLink currentNodeLink : currentModel.getGraph().outgoingEdgesOf(sourceNode)) {
                            if (currentNodeLink instanceof DataPropertyLink) {
                                String ss = currentNodeLink.getUri();
                                if (ss.equals(s)) {
                                    isHaveLink = true;
                                    break;
                                }
                            }
                        }
                        if (isHaveLink) {
                            continue;
                        } else {
                            isInternalNodeEnough = true;
                            ll = l;
                            v = l.getSource();
                            break;
                        }
                    } else {
                        isInternalNodeEnough = true;
                        ll = l;
                        v = l.getSource();
                        break;
                    }
                }
                if (!isInternalNodeEnough && isAllowRepeat) {
                    for (LabeledLink l : eu) {
                        boolean isHaveLink = false;
                        String s = l.getUri() + ((ColumnNode) u).getColumnName();
                        String NodeId = l.getSource().getId();
                        Node sourceNode = null;
                        for (Node n : currentInternalNodes) {
                            if (n.getId().equals(NodeId)) {
                                sourceNode = n;
                                break;
                            }
                        }
                        if (sourceNode != null) {
                            for (LabeledLink currentNodeLink : currentModel.getGraph().outgoingEdgesOf(sourceNode)) {
                                if (currentNodeLink instanceof DataPropertyLink) {
                                    String ss = currentNodeLink.getUri() + ((ColumnNode) currentNodeLink.getTarget()).getColumnName();
                                    if (ss.equals(s)) {
                                        isHaveLink = true;
                                        break;
                                    }
                                }
                            }
                            if (isHaveLink) {
                                continue;
                            } else {
                                isInternalNodeEnough = true;
                                ll = l;
                                v = l.getSource();
                                break;
                            }
                        } else {
                            isInternalNodeEnough = true;
                            ll = l;
                            v = l.getSource();
                            break;
                        }
                    }
                }
                if (!isInternalNodeEnough) {
                    List<String> sameIdFromG = new ArrayList<>();
                    for (LabeledLink eull : eu) {
                        sameIdFromG.add(eull.getSource().getId());
                    }
                    for (InternalNode in : currentInternalNodes) {
                        if (in.getUri().equals(linkedInternalNodeUri)) {
                            if (!sameIdFromG.contains(in.getId())) {
                                boolean isavailable = true;
                                for (Object ino : currentGraph.outgoingEdgesOf(in)) {
                                    LabeledLink inll = (LabeledLink) ino;
                                    if (inll.getUri().equals(linkUri)) {
                                        isavailable = false;
                                        break;
                                    }
                                }
                                if (isavailable) {
                                    isInternalNodeEnough = true;
                                    v = in;
                                    String linkId = LinkIdFactory.getLinkId(linkUri, v.getId(), u.getId());
                                    ll = new DataPropertyLink(linkId, new Label(linkUri));
                                }
                            }
                        }
                    }
                    if (!isInternalNodeEnough) {
                        v = cloneNode(eu.get(0).getSource(), currentModel, modelLearner, G);
                        String linkId = LinkIdFactory.getLinkId(linkUri, v.getId(), u.getId());
                        LabeledLink link = new DataPropertyLink(linkId, new Label(linkUri));
                        G.addEdge(v, u, link);
                        ll = link;
                    }
                }
            }
        }
        if (!currentGraph.containsVertex(v)) {
            currentGraph.addVertex(v);
            Set<DefaultLink> e = G.edgesOf(v);
            for (DefaultLink l : e) {
                if ((currentGraph.containsVertex(l.getSource()) && currentGraph.containsVertex(l.getTarget())) && l.getType() == LinkType.ObjectPropertyLink) {
                    temp_U.add((LabeledLink) l);
                }
            }
            if (temp_U.isEmpty()) {
                currentGraph.removeVertex(v);
                HashMap<Node, Integer> vis = new HashMap<Node, Integer>();
                HashMap<Node, Double> dis = new HashMap<Node, Double>();
                HashMap<Node, LabeledLink> prelink = new HashMap<Node, LabeledLink>();
                HashMap<Node, Node> prenode = new HashMap<Node, Node>();
                class Edge implements Comparable<Edge> {
                    int w;
                    Node to;

                    Edge() {
                        to = null;
                        w = 0;
                    }

                    Edge(Node m_to, int m_w) {
                        to = m_to;
                        w = m_w;
                    }

                    @Override
                    public int compareTo(Edge obj) {
                        return this.w - obj.w;
                    }
                }

                Queue<Edge> pq = new PriorityQueue<>();
                pq.add(new Edge(v, 0));
                dis.put(v, (double) 0);
                while (!pq.isEmpty()) {
                    Node to = pq.peek().to;
                    int d = pq.peek().w;
                    pq.poll();
                    if (vis.containsKey(to)) {
                        continue;
                    }
                    vis.put(to, 1);
                    if (currentModel.getGraph().vertexSet().contains(to)) {
                        Node inode = to;
                        LabeledLink minEdge = prelink.get(to);
                        boolean has_same_label = false;
                        for (Object l : currentGraph.edgesOf(inode)) {
                            if (((LabeledLink) l).getUri().equals(minEdge.getUri())) {
                                has_same_label = true;
                                break;
                            }
                        }
                        if (has_same_label) {
                            Node tempn = find_edge_without_label(inode, prenode.get(to), minEdge, currentModel, G);
                            if (tempn != null) {
                                inode = tempn;
                                String linkId = LinkIdFactory.getLinkId(minEdge.getUri(), inode.getId(), prenode.get(to).getId());
                                LabeledLink link = new DataPropertyLink(linkId, new Label(minEdge.getUri()));
                                G.addEdge(inode, prenode.get(to), link);
                                prelink.put(inode, link);
                                prenode.put(inode, prenode.get(to));
                            }
                        }
                        to = inode;
                        while (to != v) {
                            currentGraph.addVertex(prenode.get(to));
                            currentGraph.addEdge(prelink.get(to).getSource(), prelink.get(to).getTarget(), prelink.get(to));
                            to = prenode.get(to);
                        }
                        break;
                    }
                    for (Object l : G.edgesOf(to)) {
                        DefaultLink dl = (DefaultLink) l;
                        LabeledLink tl = null;
                        if (dl.getType() != LinkType.CompactObjectPropertyLink && dl.getType() != LinkType.CompactSubClassLink) {
                            tl = (LabeledLink) dl;
                        } else {
                            continue;
                        }
                        if (!vis.containsKey(tl.getTarget()) && (!dis.containsKey(tl.getTarget()) || dis.get(tl.getTarget()) > d + tl.getWeight() + 10000) && !G.outgoingEdgesOf(tl.getTarget()).isEmpty()) {
                            dis.put(tl.getTarget(), d + tl.getWeight() + 10000);
                            pq.add(new Edge(tl.getTarget(), (int) (d + tl.getWeight() + 10000)));
                            prenode.put(tl.getTarget(), to);
                            prelink.put(tl.getTarget(), tl);
                        }
                        if (!vis.containsKey(tl.getSource()) && (!dis.containsKey(tl.getSource()) || dis.get(tl.getSource()) > d + tl.getWeight() + 10000) && !G.outgoingEdgesOf(tl.getSource()).isEmpty()) {
                            dis.put(tl.getSource(), d + tl.getWeight() + 10000);
                            pq.add(new Edge(tl.getSource(), (int) (d + tl.getWeight() + 10000)));
                            prenode.put(tl.getSource(), to);
                            prelink.put(tl.getSource(), tl);
                        }
                    }
                }
                pq.clear();
                if (!currentGraph.containsVertex(v)) {
                    currentGraph.addVertex(v);
                }
            }
            if (!temp_U.isEmpty()) {
                List<LabeledLink> U = new ArrayList<LabeledLink>(temp_U);
                Collections.sort(U, new Comparator() {

                    @Override
                    public int compare(Object o1, Object o2) {
                        int coherence1 = 0;
                        int coherence2 = 0;
                        coherence1 = ((LabeledLink) o1).getSource().getModelIds().size();
                        coherence2 = ((LabeledLink) o2).getSource().getModelIds().size();
                        if (coherence1 == coherence2) {
                            coherence1 = (int) (((LabeledLink) o1).getSource().getId().charAt(((LabeledLink) o1).getSource().getId().length() - 1)) - (int) ('0');
                            coherence2 = (int) (((LabeledLink) o2).getSource().getId().charAt(((LabeledLink) o2).getSource().getId().length() - 1)) - (int) ('0');
                            coherence1 *= (-1);
                            coherence2 *= (-1);
                        }
                        if (coherence1 != coherence2) {
                            return coherence1 < coherence2 ? 1 : -1;
                        } else {
                            return 0;
                        }
                    }
                });

                int chosenIn = 0;
                Node inode = null;
                LabeledLink minEdge = U.get(chosenIn);
                boolean isfind = false;
                for (; chosenIn < U.size(); chosenIn++) {
                    minEdge = U.get(chosenIn);
                    boolean has_same_label = false;
                    if (v == minEdge.getSource()) {
                        inode = minEdge.getTarget();
                    } else {
                        inode = minEdge.getSource();
                    }
                    for (Object l : currentGraph.edgesOf(inode)) {
                        if (((LabeledLink) l).getUri().equals(U.get(chosenIn).getUri())) {
                            has_same_label = true;
                            break;
                        }
                    }
                    if (!has_same_label) {
                        currentGraph.addEdge(minEdge.getSource(), minEdge.getTarget(), minEdge);
                        isfind = true;
                        break;
                    } else {
                        continue;
                    }
                }
                if (!isfind) {
                    currentGraph.addEdge(U.get(0).getSource(), U.get(0).getTarget(), U.get(0));
                }
                for (int i = chosenIn; i < U.size(); i++) {
                    minEdge = U.get(i);
                    Node sourceNode = minEdge.getSource();
                    Node targetNode = minEdge.getTarget();
                    HashSet<LabeledLink> alledges = findAllEdges(sourceNode, targetNode, currentGraph);
                    if (!alledges.isEmpty()) {
                        List<LabeledLink> E = new ArrayList<LabeledLink>(alledges);
                        LabeledLink maxWeightEdge = E.get(0);
                        for (LabeledLink l : E) {
                            if (l.getWeight() > maxWeightEdge.getWeight()) {
                                maxWeightEdge = l;
                            }
                        }
                        if (maxWeightEdge.getWeight() > minEdge.getWeight()) {
                            currentGraph.removeEdge(maxWeightEdge);
                            currentGraph.addEdge(minEdge.getSource(), minEdge.getTarget(), minEdge);
                        }
                    }
                }
            }
        }
        currentGraph.addVertex(u);
        currentModel.getColumnNodes().add((ColumnNode) u);
        currentModel.getMappingToSourceColumns().put((ColumnNode) u, (ColumnNode) u);
        currentGraph.addEdge(v, u, ll);
    }

    public static Map<String, String> getSemanticTypes(Collection addList, SemanticModel currentModel,
                                                       DirectedWeightedMultigraph G,
                                                       final List<SemanticModel> trainingData,
                                                       int k) {
        Map<String, String> resultMap = new HashMap<>();
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
                remainedEdgeFromCurrentModel.add(l.getSource().getId() + l.getUri() + ((ColumnNode) l.getTarget()).getColumnName());
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
        HashMap<String, Double> typeconfidence = new HashMap();
        //准备完后，开始进行所有属性semantictype的判定，依据coherence（权值都是100）
        //首先将所有属性的所有type加入一个列表，分别求出其modelid，以此为依据分配权重。
        Map<LabeledLink, List<String>> allSemanticTypes = new HashMap<>();
        for (Object o : addList) {
            ColumnNode n = (ColumnNode) o;
            List<SemanticType> ntypes = n.getTopKLearnedSemanticTypes(k);
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
            for (SemanticType ntype : ntypes) {
                typeconfidence.put(ntype.getModelLabelString() + ntype.getHNodeId(), ntype.getConfidenceScore() * 15);
            }
        }
        for (Map.Entry<LabeledLink, List<String>> me : allSemanticTypes.entrySet()) {
            LabeledLink key = me.getKey();
            List<String> values = me.getValue();
            String targetNodeName = ((ColumnNode) key.getTarget()).getColumnName().toLowerCase().replaceAll("\\s*", "").replaceAll("_", "");
            String linkUri = key.getUri();
            String sourceUri = key.getSource().getUri();
            for (SemanticModel sm : trainingData) {
                for (ColumnNode cn : sm.getColumnNodes()) {
                    String tempTNN = cn.getColumnName().toLowerCase().replaceAll("\\s*", "").replaceAll("_", "");
                    if (targetNodeName.contains(tempTNN) || tempTNN.contains(targetNodeName)) {
                        //说明此时sm中的cn和进行判断的属性是同一属性,cn有且仅有一条边，看这条边的semantictype是否是我们目前判断的semantictype
                        List<LabeledLink> cnLinks = new ArrayList<LabeledLink>(sm.getGraph().edgesOf(cn));
                        if (cnLinks.get(0).getUri().equals(linkUri) && cnLinks.get(0).getSource().getUri().equals(sourceUri)) {
                            values.add(sm.getId());
                            break;
                        }
                    }
                }
            }
        }
        List<Map.Entry<LabeledLink, List<String>>> entryList = new LinkedList<>(allSemanticTypes.entrySet());
        int sortPoint = 0;
        int threedivpoint = entryList.size() / 3;
        Collections.sort(entryList, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                Map.Entry e1 = (Map.Entry<LabeledLink, List<String>>) o1;
                Map.Entry e2 = (Map.Entry<LabeledLink, List<String>>) o2;
                String e1type = ((LabeledLink) e1.getKey()).getSource().getUri() + "|" + ((LabeledLink) e1.getKey()).getUri() + ((LabeledLink) e1.getKey()).getTarget().getId();
                String e2type = ((LabeledLink) e2.getKey()).getSource().getUri() + "|" + ((LabeledLink) e2.getKey()).getUri() + ((LabeledLink) e2.getKey()).getTarget().getId();
                double cost1 = 0, cost2 = 0;
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
                cost1 = coherence1 + typeconfidence.get(e1type);
                cost2 = coherence2 + typeconfidence.get(e2type);
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

    public static void update4AddingAttributes(Collection addList, SemanticModel currentModel,
                                               DirectedWeightedMultigraph G,
                                               final List<SemanticModel> trainingData,
                                               DynamicUpdate_SemanticModels modelLearner,
                                               int numberOfCandidates,
                                               boolean useCorrectType) {
        Map<String, String> extratSemanticTypes = new HashMap<>();
        if (numberOfCandidates == 1) {
            extratSemanticTypes = new HashMap<>();
        } else if (numberOfCandidates == 4) {
            extratSemanticTypes = getSemanticTypes(addList, currentModel, G, trainingData, numberOfCandidates);
        }
        for (Object o : addList) {
            Node n = (Node) o;
            try {
                update_add(n, currentModel, G, trainingData, modelLearner, extratSemanticTypes, numberOfCandidates);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
