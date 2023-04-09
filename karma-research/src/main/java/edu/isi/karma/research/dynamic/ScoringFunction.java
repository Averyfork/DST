package edu.isi.karma.research.dynamic;

import edu.isi.karma.modeling.alignment.SemanticModel;
import edu.isi.karma.rep.alignment.ColumnNode;
import edu.isi.karma.rep.alignment.InternalNode;
import edu.isi.karma.rep.alignment.LabeledLink;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.xm.Similarity;
import org.xm.tendency.word.HownetWordTendency;

import java.util.ArrayList;
import java.util.Set;

public class ScoringFunction {


    public static double getNameSimilarity(String s1, String s2) {
        double score = 0.0;
        double pinYinScore = Similarity.pinyinSimilarity(s1, s2);
        double charBasedScore = Similarity.charBasedSimilarity(s1, s2);
        score = 2 * pinYinScore * charBasedScore / (pinYinScore + charBasedScore) ;
        return score;
    }

    public static double score4InternalNode(SemanticModel currentModel, InternalNode in, ColumnNode cn,
                                            DirectedWeightedMultigraph G, ArrayList<LabeledLink> links) {
        double score = 0.0;
        String inUri = in.getUri();
        String cnUri = cn.getUri();


        double nameSimilarity1 = getNameSimilarity(inUri,cnUri);
        int linkNum = links.size();
        double weightScore = 0.0;
        double coherenceScore = 0.0;
        for(LabeledLink ll : links){
            weightScore += ll.getWeight();
//            coherenceScore +=
        }
        return score;
    }
    public static double score4SemanticModel(SemanticModel currentModel) {
        double score = 0.0;
        Set<InternalNode> internalNodes = currentModel.getInternalNodes();
        for (InternalNode in : internalNodes) {

        }

        return score;
    }

    public static void main(String[] args) {
        String word1 = "death";
        String word2 = "deathYear";
        double cilinSimilarityResult = Similarity.cilinSimilarity(word1, word2);
        double pinyinSimilarityResult = Similarity.pinyinSimilarity(word1, word2);
        double conceptSimilarityResult = Similarity.conceptSimilarity(word1, word2);
        double charBasedSimilarityResult = Similarity.charBasedSimilarity(word1, word2);

        System.out.println(word1 + " vs " + word2 + " 词林相似度值：" + cilinSimilarityResult);
        System.out.println(word1 + " vs " + word2 + " 拼音相似度值：" + pinyinSimilarityResult);
        System.out.println(word1 + " vs " + word2 + " 概念相似度值：" + conceptSimilarityResult);
        System.out.println(word1 + " vs " + word2 + " 字面相似度值：" + charBasedSimilarityResult);
        double result = Similarity.phraseSimilarity(word1, word2);
        System.out.println(result);
        HownetWordTendency hownet = new HownetWordTendency();
        double sim = hownet.getTendency(word1);
        System.out.println(word1 + ":" + sim);
        System.out.println(word2 + hownet.getTendency(word2));

    }
}
