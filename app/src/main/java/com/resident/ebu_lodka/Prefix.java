package com.resident.ebu_lodka;

public enum Prefix {
        NONE('n'),
        TEMP('t'),
        FREQ('f'),
        PERIOD('p'),
        PINSIN('i'),
        PINSOUT('o');

        private char prefix;

        Prefix(char prefix) {
            this.prefix = prefix;
        }

        public char getPrefix() {
            return prefix;
        }

        public String getStringPrefix() {
            return new String(new char[]{prefix});
        }

        public static Prefix valueOf(char prefix) {
            for (Prefix pref : values()) {
                if (prefix == pref.getPrefix()) {
                    return pref;
                }
            }
            return NONE;
        }
    }