/**
 * The Helper class for QR Code encode and decode
 */
package edu.cmu.cc.webtier;

/**
 * Microservice1 helper: QRCoder
 */
public class QRCoder {

    int[] lgMapV1;
    int[] lgMapV2;
    int[] lgMapDecode;

    public QRCoder() {
        this.lgMapV1 = Utils.logisticMap(21);
        this.lgMapV2 = Utils.logisticMap(25);
        this.lgMapDecode = Utils.logisticMap(32);
    }

    /**
     * Encoding functionality
     * @param s : the rawText to be encoded from GET request (%20 in place of empty space)
     * @return the encoded hexCode
     */
    //TODO: Deal with case when it is not encode-able.
    public String encode(String s) {
        String payLoadString = Utils.payload(s);
        if (s.length() <= 13) {
            char[][] qrV1 = Utils.matrixV1();
            Utils.zigzagV1(qrV1, payLoadString);
            String zigzaged = Utils.qr2String(qrV1);
            return Utils.binaryString2Hex(zigzaged);
        } else if (s.length() <= 22) {
            char[][] qrV2 = Utils.matrixV2();
            Utils.zigzagV2(qrV2, payLoadString);
            String zigzaged = Utils.qr2String(qrV2);
            return Utils.binaryString2Hex(zigzaged);
        }
        return "";
    }

    /**
     * Decoding functionality
     * @param s : the HexCode from 32x32 matrix, either from version1 or version 2
     * @return the rawText before encoding (%20 in place of empty space)
     */
    public String decode(String s) {
        char[][] msg = Utils.messageToCharArray(s);
        return Utils.reverseLgDecodeFasterV2(msg);
    }

}
