package com.resident.ebu_lodka;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by resident on 28.03.18.
 */

public class Response {
    List<BigDecimal> temps;
    List<Integer> freqs;
    List<Integer> pinsIn;
    List<Integer> pinsOut;
    int period;

    public Response() {
        temps = new ArrayList<>();
        freqs = new ArrayList<>();
        pinsIn = new ArrayList<>();
        pinsOut = new ArrayList<>();
    }

    public void addTemp(BigDecimal temp) {
        temps.add(temp);
    }

    public void addFreq(int freq) {
        freqs.add(freq);
    }

    public void addPinsIn(Integer pinState) {
        pinsIn.add(pinState);
    }

    public void addPinsOut(Integer pinState) {
        pinsOut.add(pinState);
    }

    public List<BigDecimal> getTemps() {
        return temps;
    }

    public List<Integer> getFreqs() {
        return freqs;
    }

    public List<Integer> getPinsIn() {
        return pinsIn;
    }

    public List<Integer> getPinsOut() {
        return pinsOut;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }
}
