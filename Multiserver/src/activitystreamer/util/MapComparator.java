package activitystreamer.util;

import org.json.simple.JSONObject;

import java.util.Comparator;
import java.util.Map;

public class MapComparator implements Comparator<String> {

    Map<String, JSONObject> base;

    public MapComparator(Map<String, JSONObject> base){
        this.base = base;
    }
    @Override
    public int compare(String o1, String o2) {
        int level1 = (Integer) base.get(o1).get("level");
        int level2 = (Integer) base.get(o2).get("level");
        int rank1 = (Integer) base.get(o1).get("rank");
        int rank2 = (Integer) base.get(o2).get("rank");
        if(level1 > level2){
            return 1;
        }else if(level1 < level2){
            return - 1;
        }else if(level1 == level2){
            if(rank1 > rank2){
                return 1;
            }else if(rank1 < rank2){
                return -1;
            }
        }
        return 0;
    }
}
