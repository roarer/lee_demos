package com.eastaeon.floatapp.music.lrc;

public class LrcContent implements Comparable{
    private String lrcStr;
    private long lrcTime;
	
    public String getLrcStr() {
        return lrcStr;
    }
    public void setLrcStr(String lrcStr) {
        this.lrcStr = lrcStr;
    }
    public long getLrcTime() {
        return lrcTime;
    }
    public void setLrcTime(long lrcTime) {
        this.lrcTime = lrcTime;
    }
	
@Override
    public int compareTo(Object o) {

        LrcContent f = (LrcContent)o;

        if (lrcTime > f.lrcTime) {
            return 1;
        }
        else if (lrcTime <  f.lrcTime) {
            return -1;
        }
        else {
            return 0;
        }

    }

    @Override
    public String toString(){
        return this.lrcStr;
    }	
}
