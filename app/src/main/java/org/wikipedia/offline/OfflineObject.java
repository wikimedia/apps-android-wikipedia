package org.wikipedia.offline;

import java.util.ArrayList;
import java.util.List;

public class OfflineObject {
    private String url;
    private String lang;
    private List<Integer> usedBy = new ArrayList<>();

    public OfflineObject() {
    }

}
