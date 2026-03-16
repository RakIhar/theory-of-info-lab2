package org.example;

public class Cryptography {

    //Многочлен: x^35 + x^2 + 1
//    private static final long POLYNOM_MASK =
//            (1L << 34) | (1L << 1);

    //35-битная маска
    private static final long MASK_35_BITS = (1L << 35) - 1;

    public record Result(byte[] encryptedData, byte[] keyStream) {};

    public static Result encryptWithKey(byte[] data, long initialStateOfRegister) {
        byte[] result = new byte[data.length];
        byte[] keyStream = new byte[data.length];
        long lfsrState = initialStateOfRegister & MASK_35_BITS;

        for (int i = 0; i < data.length; i++) {
            byte keyStreamByte = 0;

            for (int bit = 0; bit < 8; bit++) {

                //выходной
                long outputBit = (lfsrState >> 34) & 1;

                long feedback = 0;

                feedback ^= (lfsrState >> 34) & 1; // x^35
                feedback ^= (lfsrState >> 1) & 1;  // x^2

                //в джафффке нет беззнаковых целочисленных типов, поэтому маска
                lfsrState = ((lfsrState << 1) & MASK_35_BITS) | feedback;

                keyStreamByte = (byte) ((keyStreamByte << 1) | outputBit);
            }

            keyStream[i] = keyStreamByte;
            result[i] = (byte) (data[i] ^ keyStreamByte);
        }

        return new Result(result, keyStream);
    }
}