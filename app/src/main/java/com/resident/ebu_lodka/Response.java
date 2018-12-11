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
    List<Byte> pinsIn;
    List<Byte> pinsOut;

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

    public void addPinsIn(byte pinState) {
        pinsIn.add(pinState);
    }

    public void addPinsOut(byte pinState) {
        pinsOut.add(pinState);
    }

    public List<BigDecimal> getTemps() {
        return temps;
    }

    public List<Integer> getFreqs() {
        return freqs;
    }

    public List<Byte> getPinsIn() {
        return pinsIn;
    }

    public List<Byte> getPinsOut() {
        return pinsOut;
    }
}
