package edu.cmu.cc.webtier;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.*;

public class UtilsTweet {

    public static int TWEET_POS = 4;


    public static int wordMatch(String content, String phrase) {
        int count = 0, fromIndex = 0;
        while ((fromIndex = content.indexOf(phrase, fromIndex)) != -1 ){
            count++;
            fromIndex++;
        }
        return count;
    }

    public static int tagCount(String tags, String tag) {
        String[] tagArray = tags.split("#");
        int count = 0;

        for (String s: tagArray) {
            if(s.equals(tag)) {
                count++;
            }
        }
        return count;
    }

    public static List<Map.Entry<Long, Double>> orderResults(HashMap<Long, Double> results) {
        List<Map.Entry<Long, Double>> list = new ArrayList<Map.Entry<Long, Double>>(results.entrySet());
        list.sort(new ScoreComparator());
        return list;
    }

    public static HashMap<Long, Integer> calculateDynamicCount(RowSet<Row> sqlResults, String phrase, String hashtag) {
        HashMap<Long, Integer> scores = new HashMap<>();
        for(Row r: sqlResults) {
            Long user = r.getLong("receiver");
            int contentCount = wordMatch(r.getString("content"), phrase);
            int tagCount = tagCount(r.getString("tags"), hashtag);
            scores.put(user, scores.getOrDefault(user, 0) + contentCount + tagCount);
        }
        return scores;
    }
    

    public static String combineResponse(RowSet<Row> users, HashMap<Long, Integer> count) {
        StringBuilder ret = new StringBuilder();
        // HashMap<Long, Double> scoreFinal = new HashMap<>();
        HashMap<Long, Row> records = new HashMap<>();
        List<Map.Entry<Long, Double>> scoreFinal = new ArrayList<>();
//        List<Row>
        for (Row r: users) {
            Long u = r.getLong("user_b");

            double score = r.getDouble("product_score");
            records.put(u, r);
            if(count.containsKey(u)) {
                // scoreFinal.put(u, score*(1 + (double)Math.log(1 + count.get(u))));
                scoreFinal.add(new AbstractMap.SimpleEntry<Long, Double>(u, score*(1 + Math.log(1 + count.get(u)))));
            } else {
                scoreFinal.add(new AbstractMap.SimpleEntry<Long, Double>(u, score));
            }
        }
        // List<Map.Entry<Long, Double>> orderedUsers = orderResults(scoreFinal);
        scoreFinal.sort(new ScoreComparator());
        int usersLen = scoreFinal.size();
        for(int i = 0; i < usersLen; i++) {
            Map.Entry<Long, Double> e = scoreFinal.get(i);
            Long u = e.getKey();
            Row r = records.get(u);
            String lastScreen = r.getString("latest_screen");
            if (lastScreen == null) {
                lastScreen = "";
            }
            String lastDescription = r.getString("latest_description");
            if (lastDescription == null) {
                lastDescription = "";
            }
            String tweet = r.getString(TWEET_POS);
            ret.append(u.toString()).append("\t")
                    .append(lastScreen).append("\t")
                    .append(lastDescription).append("\t")
                    .append(tweet);
            if (i < usersLen - 1) {
                ret.append("\n");
            }
        }
        return ret.toString();
    }


    public static String rerankResults(RowSet<Row> results, String phrase, String tag) {
        HashMap<Long, String> reponses = new HashMap<>();
        List<Map.Entry<Long, Double>> scoreFinal = new ArrayList<>();
        HashMap<Long, Integer> count = new HashMap<>();
        HashMap<Long, Double> scores = new HashMap<>();
        String latestScreen;
        String latestDes;
        String res;
        int tagCount;
        int phraseCount;
        for(Row r : results) {
            long user = r.getLong("receiver");
            if (!reponses.containsKey(user)) {
                latestScreen = r.getString("latest_screen");
                latestScreen = latestScreen == null ? "" : latestScreen;
                latestDes = r.getString("latest_description");
                latestDes = latestDes == null ? "" : latestDes;
                res = user + "\t" + latestScreen + "\t" + latestDes + "\t" + r.getString(TWEET_POS);
                reponses.put(user, res);
            }
            tagCount = tagCount(r.getString("tags"), tag);
            phraseCount = wordMatch(r.getString("content"), phrase);
            count.put(user, count.getOrDefault(user, 0) + tagCount + phraseCount);
            scores.put(user, r.getDouble("product_score"));
        }
        for(Map.Entry<Long,Double> e : scores.entrySet()) {
            long user = e.getKey();
            e.setValue(e.getValue()*(1 + Math.log(1 + count.getOrDefault(user, 0))));
            scoreFinal.add(e);
        }
        StringBuilder response = new StringBuilder();
        scoreFinal.sort(new ScoreComparator());
        int resultLen = scoreFinal.size();
        for(int i = 0; i < resultLen; i++) {
            Map.Entry<Long, Double> e = scoreFinal.get(i);
            long u = e.getKey();
            response.append(reponses.get(u));
            if (i < resultLen - 1) {
                response.append("\n");
            }
        }
        return response.toString();
    }
}



class ScoreComparator<V extends Comparable<V>>
        implements Comparator<Map.Entry<Long, V>> {
    public int compare(Map.Entry<Long, V> o1, Map.Entry<Long, V> o2) {

        // Call compareTo() on V, which is known to be a Comparable<V>
        int val = o2.getValue().compareTo(o1.getValue());
        if (val == 0) return o2.getKey().compareTo(o1.getKey());
        return val;
    }
}
